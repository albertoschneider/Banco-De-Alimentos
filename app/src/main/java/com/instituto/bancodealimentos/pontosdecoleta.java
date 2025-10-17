package com.instituto.bancodealimentos;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class pontosdecoleta extends AppCompatActivity {

    private static final String COLECAO = "pontos";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private RecyclerView rv;
    private EditText etBusca;
    private ImageButton btnVoltar;
    private View root;

    private PontoColetaAdapter adapter;

    private final List<PontoColeta> allData = new ArrayList<>();
    private final List<PontoColeta> visibleData = new ArrayList<>();
    private String currentQuery = "";

    private FusedLocationProviderClient fused;
    private LocationRequest locRequest;
    private LocationCallback locCallback;
    private Location lastLocation;

    private final ActivityResultLauncher<String[]> locationPermsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean fine = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                boolean coarse = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                boolean granted = fine || coarse;

                if (granted) {
                    fetchLastLocationSafe();
                    startLocationUpdatesSafe();
                } else {
                    Snackbar.make(root, "Permissão de localização negada. A lista funcionará sem ordenar por distância.", Snackbar.LENGTH_LONG).show();
                    lastLocation = null;
                    aplicarFiltroEOrdenacao();
                }
            });

    private boolean alive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pontosdecoleta);

        root = findViewById(android.R.id.content);

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

        if (btnVoltar != null) btnVoltar.setOnClickListener(v -> finish());

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
    }

    @Override protected void onStart() {
        super.onStart();
        alive = true;

        // Gate: se sessão caiu, Splash decide rota
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, SplashActivity.class));
            finish();
            return;
        }

        ensureLocationFlow();
        carregarPontosUmaVez();
    }

    @Override protected void onStop() {
        super.onStop();
        alive = false;
        stopLocationUpdatesSafe();
        if (rv != null) rv.setAdapter(null);
    }

    // ===== Permissões & Localização =====
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
        try {
            fused.requestLocationUpdates(locRequest, locCallback, getMainLooper());
        } catch (SecurityException ignored) {}
    }

    private void stopLocationUpdatesSafe() {
        if (fused != null && locCallback != null) {
            try { fused.removeLocationUpdates(locCallback); } catch (Exception ignored) {}
        }
    }

    // ===== Firestore =====
    private void carregarPontosUmaVez() {
        db.collection(COLECAO)
                .get()
                .addOnSuccessListener(this::aplicarSnapshot)
                .addOnFailureListener(e ->
                        Snackbar.make(root, "Erro ao carregar pontos: " + e.getMessage(), Snackbar.LENGTH_LONG).show());
    }

    private void aplicarSnapshot(@NonNull QuerySnapshot snap) {
        if (!alive) return;
        allData.clear();

        for (DocumentSnapshot d : snap.getDocuments()) {
            String nome = d.getString("nome");
            String endereco = d.getString("endereco");
            String disp = d.getString("disponibilidade");

            // ---- location pode ser GeoPoint OU Map {lat,lng} ----
            Double lat = null, lng = null;

            // 1) tenta GeoPoint
            GeoPoint gp = null;
            try { gp = d.getGeoPoint("location"); } catch (Throwable ignore) {}
            if (gp != null) {
                lat = gp.getLatitude();
                lng = gp.getLongitude();
            } else {
                // 2) tenta Map
                try {
                    Map<String, Object> map = (Map<String, Object>) d.get("location");
                    if (map != null) {
                        lat = toDouble(map.get("lat"));
                        lng = toDouble(map.get("lng"));
                    }
                } catch (Throwable ignore) { /* mantém null */ }
            }

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

    private Double toDouble(Object v) {
        if (v == null) return null;
        if (v instanceof Double) return (Double) v;
        if (v instanceof Float)  return ((Float) v).doubleValue();
        if (v instanceof Long)   return ((Long) v).doubleValue();
        if (v instanceof Integer)return ((Integer) v).doubleValue();
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return null; }
    }

    // ===== Filtro & Ordenação =====
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
        if (rv != null && rv.getAdapter() == null) rv.setAdapter(adapter);
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
