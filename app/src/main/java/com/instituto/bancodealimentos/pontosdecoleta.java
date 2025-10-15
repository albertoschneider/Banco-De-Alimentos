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

    // Firestore
    private static final String COLECAO = "pontos";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration pontosListener;

    // Auth
    private FirebaseAuth auth;
    private FirebaseAuth.AuthStateListener authListener;

    // UI
    private RecyclerView rv;
    private EditText etBusca;
    private ImageButton btnVoltar;
    private PontoColetaAdapter adapter;
    private View rootView;

    // Dados
    private final List<PontoColeta> allData = new ArrayList<>();
    private final List<PontoColeta> visibleData = new ArrayList<>();
    private String currentQuery = "";

    // Localização
    private FusedLocationProviderClient fused;
    private LocationRequest locRequest;
    private LocationCallback locCallback;
    private Location lastLocation;

    private boolean isStarted = false;

    // Permissões
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

        // Header: insets
        View header = findViewById(R.id.header);
        if (header != null) {
            ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
                Insets sb = insets.getInsets(WindowInsetsCompat.Type.statusBars());
                v.setPadding(v.getPaddingLeft(), sb.top, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
            ViewCompat.requestApplyInsets(header);
        }

        // UI
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

        // Localização
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

        // Auth
        auth = FirebaseAuth.getInstance();
        authListener = firebaseAuth -> {
            if (!isStarted) return;
            if (firebaseAuth.getCurrentUser() != null) {
                iniciarListenerFirestore();
            } else {
                Snackbar.make(rootView, "Reconectando…", Snackbar.LENGTH_SHORT).show();
                stopQueryIfAny();
                showEmptyList();
            }
        };
    }

    @Override protected void onStart() {
        super.onStart();
        isStarted = true;
        if (authListener != null) auth.addAuthStateListener(authListener);
        if (auth.getCurrentUser() != null) iniciarListenerFirestore();
        ensureLocationFlow();
    }

    @Override protected void onStop() {
        super.onStop();
        isStarted = false;
        if (authListener != null) auth.removeAuthStateListener(authListener);
        stopQueryIfAny();
        stopLocationUpdatesSafe();
    }

    private void stopQueryIfAny() {
        if (pontosListener != null) {
            try { pontosListener.remove(); } catch (Exception ignored) {}
            pontosListener = null;
        }
    }

    private void showEmptyList() {
        visibleData.clear();
        adapter.notifyDataSetChanged();
    }

    // ====== Permissões & Localização ======
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

    // ====== Firestore ======
    private void iniciarListenerFirestore() {
        if (!isStarted) return;

        // Regras exigem login; se não tiver user ainda, apenas aguarde o authListener
        if (auth.getCurrentUser() == null) return;

        stopQueryIfAny();

        pontosListener = db.collection(COLECAO).addSnapshotListener((snap, err) -> {
            if (!isStarted) return;

            if (err != null) {
                handleFirestoreError(err);
                return;
            }
            if (snap == null) {
                showEmptyList();
                return;
            }
            atualizarListaComSnapshot(snap);
        });
    }

    private void handleFirestoreError(Exception e) {
        stopQueryIfAny();
        showEmptyList();

        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException fe = (FirebaseFirestoreException) e;
            switch (fe.getCode()) {
                case UNAUTHENTICATED:
                    Snackbar.make(rootView, "Sessão expirada. Toque para tentar novamente.", Snackbar.LENGTH_LONG)
                            .setAction("Tentar de novo", v -> iniciarListenerFirestore())
                            .show();
                    return;
                case PERMISSION_DENIED:
                    Snackbar.make(rootView, "Sem permissão para ler os pontos de coleta.", Snackbar.LENGTH_LONG)
                            .setAction("Tentar de novo", v -> iniciarListenerFirestore())
                            .show();
                    return;
                default:
                    // Continua para o genérico
            }
        }

        Snackbar.make(rootView, "Erro Firestore: " + e.getMessage(), Snackbar.LENGTH_LONG)
                .setAction("Tentar de novo", v -> iniciarListenerFirestore())
                .show();
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

    // ====== Filtro & Ordenação ======
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

        ordenar(visibleData);
        adapter.notifyDataSetChanged();
    }

    private void ordenar(List<PontoColeta> list) {
        if (lastLocation != null) {
            Collections.sort(list, (a, b) -> Double.compare(a.getDistanciaKm(), b.getDistanciaKm()));
        } else {
            Collections.sort(list, (a, b) -> {
                String an = a.getNome() != null ? a.getNome() : "";
                String bn = b.getNome() != null ? b.getNome() : "";
                return an.compareToIgnoreCase(bn);
            });
        }
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
