package com.instituto.bancodealimentos;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

public class pontosdecoleta extends AppCompatActivity {

    private static final String COLECAO = "pontos";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration pontosListener;

    private boolean alive = false;
    private boolean listenerAttached = false;

    private View rootView;
    private RecyclerView rv;
    private EditText etBusca;
    private ImageButton btnVoltar;
    private PontoColetaAdapter adapter;

    private final List<PontoColeta> allData = new ArrayList<>();
    private final List<PontoColeta> visibleData = new ArrayList<>();
    private String currentQuery = "";

    private FusedLocationProviderClient fused;
    private LocationRequest locRequest;
    private LocationCallback locCallback;
    private Location lastLocation;

    private final Retry.Backoff retry = new Retry.Backoff(5, 300);

    private final ActivityResultLauncher<String[]> locationPermsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean fine = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                boolean coarse = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                boolean granted = fine || coarse;

                if (granted) {
                    fetchLastLocationSafe();
                    startLocationUpdatesSafe();
                } else {
                    Toast.makeText(this, "Permissão de localização negada.", Toast.LENGTH_SHORT).show();
                    lastLocation = null;
                    aplicarFiltroEOrdenacao();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pontosdecoleta);
        rootView = findViewById(android.R.id.content);

        View header = findViewById(R.id.header);
        if (header != null) {
            ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
                Insets sb = insets.getInsets(WindowInsetsCompat.Type.statusBars());
                v.setPadding(v.getPaddingLeft(), sb.top, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
            ViewCompat.requestApplyInsets(header);
        }

        btnVoltar = findViewById(R.id.btn_voltar);
        etBusca   = findViewById(R.id.etBusca);
        rv        = findViewById(R.id.rvPontos);
        if (btnVoltar != null) btnVoltar.setOnClickListener(v -> onBackPressed());

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PontoColetaAdapter(visibleData);
        rv.setAdapter(adapter);

        if (etBusca != null) {
            etBusca.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentQuery = s != null ? s.toString() : "";
                    aplicarFiltroEOrdenacao();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        fused = LocationServices.getFusedLocationProviderClient(this);
        locRequest = LocationRequest.create()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000)
                .setFastestInterval(2000);
        locCallback = new LocationCallback() {
            @Override public void onLocationResult(@NonNull LocationResult result) {
                Location loc = result.getLastLocation();
                if (loc != null) {
                    lastLocation = loc;
                    recalcDistances(allData);
                    aplicarFiltroEOrdenacao();
                }
            }
        };
    }

    @Override protected void onStart() {
        super.onStart();
        alive = true;
        listenerAttached = false;
        ensureLocationFlow();
        startOrReload();
    }

    @Override protected void onStop() {
        super.onStop();
        alive = false;
        detachListener();
        stopLocationUpdatesSafe();
    }

    private void startOrReload() {
        if (!alive) return;

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            // não navega pro login; só informa e permite tentar de novo
            Snackbar.make(rootView, "Sessão não disponível. Tente novamente.", Snackbar.LENGTH_LONG)
                    .setAction("Tentar de novo", v -> startOrReload())
                    .show();
            return;
        }

        // Primer — sem listener
        db.collection(COLECAO).get()
                .addOnSuccessListener(snap -> {
                    if (!alive) return;
                    atualizarListaComSnapshot(snap);
                    if (!listenerAttached) attachListener();
                })
                .addOnFailureListener(e -> handlePrimerError(e, this::startOrReload));
    }

    private void attachListener() {
        detachListener();
        listenerAttached = true;

        pontosListener = db.collection(COLECAO)
                .addSnapshotListener((snap, err) -> {
                    if (!alive) return;

                    if (err != null) {
                        handleLiveError(err);
                        return;
                    }
                    if (snap == null) { showEmptyList(); return; }
                    atualizarListaComSnapshot(snap);
                });
    }

    private void detachListener() {
        listenerAttached = false;
        if (pontosListener != null) {
            try { pontosListener.remove(); } catch (Exception ignored) {}
            pontosListener = null;
        }
    }

    private void handlePrimerError(Exception e, Runnable retryJob) {
        showEmptyList();

        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException fe = (FirebaseFirestoreException) e;
            switch (fe.getCode()) {
                case UNAUTHENTICATED:
                case PERMISSION_DENIED:
                case UNAVAILABLE:
                case DEADLINE_EXCEEDED:
                    if (retry.canRetry()) {
                        Snackbar.make(rootView, "Reconectando…", Snackbar.LENGTH_SHORT).show();
                        retry.schedule(retryJob::run);
                        return;
                    }
            }
        }
        Snackbar.make(rootView, "Erro ao carregar pontos: " + e.getMessage(), Snackbar.LENGTH_LONG)
                .setAction("Tentar de novo", v -> retryJob.run())
                .show();
    }

    private void handleLiveError(Exception e) {
        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException.Code c = ((FirebaseFirestoreException) e).getCode();
            if (c == FirebaseFirestoreException.Code.UNAUTHENTICATED ||
                    c == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                    c == FirebaseFirestoreException.Code.UNAVAILABLE) {
                if (retry.canRetry()) retry.schedule(this::attachListener);
            }
        }
        Snackbar.make(rootView, "Conexão instável. Mantendo lista atual.", Snackbar.LENGTH_SHORT).show();
    }

    private void atualizarListaComSnapshot(@NonNull QuerySnapshot snap) {
        allData.clear();

        for (DocumentSnapshot d : snap.getDocuments()) {
            String nome = d.getString("nome");
            String endereco = d.getString("endereco");
            String disp = d.getString("disponibilidade");
            GeoPoint gp = d.getGeoPoint("location");
            Double lat = gp != null ? gp.getLatitude() : null;
            Double lng = gp != null ? gp.getLongitude() : null;

            PontoColeta p = new PontoColeta(
                    nome != null ? nome : "",
                    endereco != null ? endereco : "",
                    disp,
                    lat, lng
            );
            allData.add(p);
        }

        recalcDistances(allData);
        aplicarFiltroEOrdenacao();
    }

    private void showEmptyList() {
        visibleData.clear();
        adapter.notifyDataSetChanged();
    }

    // ------- localização -------
    private void ensureLocationFlow() {
        if (hasLocationPermission()) {
            fetchLastLocationSafe();
            startLocationUpdatesSafe();
        } else {
            locationPermsLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void fetchLastLocationSafe() {
        if (!hasLocationPermission() || fused == null) return;
        try {
            fused.getLastLocation()
                    .addOnSuccessListener(loc -> {
                        if (loc != null) {
                            lastLocation = loc;
                            recalcDistances(allData);
                            aplicarFiltroEOrdenacao();
                        }
                    });
        } catch (SecurityException ignored) {}
    }

    private void startLocationUpdatesSafe() {
        if (!hasLocationPermission() || fused == null || locCallback == null || locRequest == null) return;
        try { fused.requestLocationUpdates(locRequest, locCallback, getMainLooper()); } catch (SecurityException ignored) {}
    }

    private void stopLocationUpdatesSafe() {
        if (fused != null && locCallback != null) {
            try { fused.removeLocationUpdates(locCallback); } catch (Exception ignored) {}
        }
    }

    // ------- filtro/ordenacao -------
    private void aplicarFiltroEOrdenacao() {
        String q = currentQuery.trim().toLowerCase(Locale.ROOT);

        visibleData.clear();
        if (q.isEmpty()) {
            visibleData.addAll(allData);
        } else {
            for (PontoColeta p : allData) {
                boolean matchNome = p.getNome() != null && p.getNome().toLowerCase(Locale.ROOT).contains(q);
                boolean matchEnd  = p.getEndereco() != null && p.getEndereco().toLowerCase(Locale.ROOT).contains(q);
                if (matchNome || matchEnd) visibleData.add(p);
            }
        }

        if (lastLocation != null) {
            Collections.sort(visibleData, (a, b) -> Double.compare(a.getDistanciaKm(), b.getDistanciaKm()));
        } else {
            Collections.sort(visibleData, (a, b) -> {
                String an = a.getNome() != null ? a.getNome() : "";
                String bn = b.getNome() != null ? b.getNome() : "";
                return an.compareToIgnoreCase(bn);
            });
        }

        adapter.notifyDataSetChanged();
    }

    private void recalcDistances(List<PontoColeta> list) {
        if (lastLocation == null) {
            for (PontoColeta p : list) p.setDistanciaKm(0.0);
            return;
        }
        for (PontoColeta p : list) {
            if (p.getLat() != null && p.getLng() != null) {
                float[] res = new float[1];
                Location.distanceBetween(
                        lastLocation.getLatitude(), lastLocation.getLongitude(),
                        p.getLat(), p.getLng(), res
                );
                p.setDistanciaKm(res[0] / 1000.0);
            } else {
                p.setDistanciaKm(0.0);
            }
        }
    }
}
