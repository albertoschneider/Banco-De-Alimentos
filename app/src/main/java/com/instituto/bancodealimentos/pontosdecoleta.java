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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
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

    // ===== Config =====
    private static final int RC_LOCATION = 1010;
    private static final String COLECAO = "pontos"; // Firestore collection

    // ===== UI =====
    private RecyclerView rv;
    private EditText etBusca;
    private ImageButton btnVoltar;
    private PontoColetaAdapter adapter;

    // ===== Dados =====
    private final List<PontoColeta> allData = new ArrayList<>();     // lista completa vinda do Firestore
    private final List<PontoColeta> visibleData = new ArrayList<>(); // lista exibida (após filtro/ordenação)
    private String currentQuery = "";                                 // texto do filtro

    // ===== Firestore =====
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration pontosListener; // para remover no onDestroy

    // ===== Localização =====
    private FusedLocationProviderClient fused;
    private LocationRequest locRequest;
    private LocationCallback locCallback;
    private Location lastLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pontosdecoleta);

        View header = findViewById(R.id.header); // o ConstraintLayout do topo
        ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop() + sb.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
        ViewCompat.requestApplyInsets(header);

        // ---- Bind UI
        btnVoltar = findViewById(R.id.btn_voltar);
        etBusca   = findViewById(R.id.etBusca);
        rv        = findViewById(R.id.rvPontos);

        if (btnVoltar != null) btnVoltar.setOnClickListener(v -> onBackPressed());

        // ---- Recycler
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PontoColetaAdapter(visibleData);
        rv.setAdapter(adapter);

        // ---- Filtro de busca (nome/endereço)
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

        // ---- Localização
        fused = LocationServices.getFusedLocationProviderClient(this);
        locRequest = LocationRequest.create()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000)
                .setFastestInterval(2000);

        locCallback = new LocationCallback() {
            @Override public void onLocationResult(LocationResult result) {
                if (result == null) return;
                Location loc = result.getLastLocation();
                if (loc != null) {
                    lastLocation = loc;
                    // Recalcula distâncias e reordena com base na nova localização
                    recalcDistances(allData);
                    aplicarFiltroEOrdenacao();
                }
            }
        };

        // ---- Firestore (tempo real)
        iniciarListenerFirestore();

        // ---- Uma última localização rápida para já popular distâncias iniciais
        obterUltimaLocalizacao();
    }

    // ===================== Firestore =====================
    private void iniciarListenerFirestore() {
        pontosListener = db.collection(COLECAO).addSnapshotListener((snap, err) -> {
            if (err != null) {
                Toast.makeText(this, "Erro Firestore: " + err.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            if (snap == null) return;
            atualizarListaComSnapshot(snap);
        });
    }

    private void atualizarListaComSnapshot(@NonNull QuerySnapshot snap) {
        allData.clear();

        for (DocumentSnapshot d : snap.getDocuments()) {
            String nome = d.getString("nome");
            String endereco = d.getString("endereco");
            String disp = d.getString("disponibilidade"); // "Disponível" / "Indisponível"
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

        // Calcula distâncias na lista completa
        recalcDistances(allData);

        // Aplica filtro + ordenação e atualiza a UI
        aplicarFiltroEOrdenacao();
    }

    // ===================== Filtro & Ordenação =====================
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
        // Se tiver localização, ordena por distância; senão, por nome (alfabético)
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
            // Sem localização — zera as distâncias só para não exibir valores antigos
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

    // ===================== Localização =====================
    private boolean temPermissaoLocalizacao() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void pedirPermissao() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                RC_LOCATION
        );
    }

    private void obterUltimaLocalizacao() {
        if (!temPermissaoLocalizacao()) {
            pedirPermissao();
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fused.getLastLocation().addOnSuccessListener(loc -> {
            if (loc != null) {
                lastLocation = loc;
                recalcDistances(allData);
                aplicarFiltroEOrdenacao();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!temPermissaoLocalizacao()) {
            pedirPermissao();
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fused.requestLocationUpdates(locRequest, locCallback, getMainLooper());
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (fused != null && locCallback != null) {
            fused.removeLocationUpdates(locCallback);
        }
    }

    @Override
    protected void onDestroy() {
        if (pontosListener != null) {
            pontosListener.remove();
            pontosListener = null;
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RC_LOCATION) {
            boolean granted = true;
            for (int r : grantResults) granted &= (r == PackageManager.PERMISSION_GRANTED);
            if (granted) {
                obterUltimaLocalizacao();
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fused.requestLocationUpdates(locRequest, locCallback, getMainLooper());
                }
            } else {
                Toast.makeText(this, "Permissão de localização negada.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}