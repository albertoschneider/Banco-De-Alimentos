package com.instituto.bancodealimentos;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class pontosdecoleta extends AppCompatActivity {

    private static final int RC_LOCATION = 1010;

    private RecyclerView rv;
    private PontoColetaAdapter adapter;
    private final List<PontoColeta> data = new ArrayList<>();

    private FusedLocationProviderClient fused;
    private LocationRequest locRequest;
    private LocationCallback locCallback;
    private Location ultimaLocalizacao;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLECAO = "pontos"; // ou "pontos_coleta" se esse for seu nome

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pontosdecoleta);

        ImageButton back = findViewById(R.id.btn_voltar);
        if (back != null) back.setOnClickListener(v -> onBackPressed());

        rv = findViewById(R.id.rvPontos);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PontoColetaAdapter(data);
        rv.setAdapter(adapter);

        fused = LocationServices.getFusedLocationProviderClient(this);
        locRequest = LocationRequest.create()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000)
                .setFastestInterval(2000);

        locCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                if (result == null) return;
                Location loc = result.getLastLocation();
                if (loc != null) {
                    ultimaLocalizacao = loc;
                    atualizarDistanciasNaLista();
                }
            }
        };

        carregarPontosDoFirestore();
        obterUltimaLocalizacao(); // pega uma vez assim que abre
    }

    private void carregarPontosDoFirestore() {
        db.collection(COLECAO).get()
                .addOnSuccessListener(sn -> {
                    data.clear();
                    for (DocumentSnapshot d : sn.getDocuments()) {
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

                        if (ultimaLocalizacao != null && lat != null && lng != null) {
                            float[] res = new float[1];
                            Location.distanceBetween(
                                    ultimaLocalizacao.getLatitude(), ultimaLocalizacao.getLongitude(),
                                    lat, lng, res
                            );
                            p.setDistanciaKm(res[0] / 1000.0);
                        }

                        data.add(p);
                    }
                    ordenarSePossivel();
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erro ao carregar pontos: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void obterUltimaLocalizacao() {
        if (!temPermissaoLocalizacao()) {
            pedirPermissao();
            return;
        }

        // Checagem explícita para satisfazer o Lint
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fused.getLastLocation().addOnSuccessListener(loc -> {
            if (loc != null) {
                ultimaLocalizacao = loc;
                atualizarDistanciasNaLista();
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Erro ao obter localização: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    private void atualizarDistanciasNaLista() {
        if (ultimaLocalizacao == null || data.isEmpty()) return;

        for (PontoColeta p : data) {
            if (p.getLat() != null && p.getLng() != null) {
                float[] res = new float[1];
                Location.distanceBetween(
                        ultimaLocalizacao.getLatitude(), ultimaLocalizacao.getLongitude(),
                        p.getLat(), p.getLng(), res
                );
                p.setDistanciaKm(res[0] / 1000.0);
            }
        }
        ordenarSePossivel();
        adapter.notifyDataSetChanged();
    }

    private void ordenarSePossivel() {
        if (ultimaLocalizacao != null) {
            Collections.sort(data, (a, b) -> Double.compare(a.getDistanciaKm(), b.getDistanciaKm()));
        }
    }

    private boolean temPermissaoLocalizacao() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void pedirPermissao() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                RC_LOCATION);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!temPermissaoLocalizacao()) {
            pedirPermissao();
            return;
        }

        // Checagem explícita para satisfazer o Lint
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RC_LOCATION) {
            boolean granted = true;
            for (int r : grantResults) granted &= (r == PackageManager.PERMISSION_GRANTED);
            if (granted) {
                // Checagem explícita antes de chamar APIs de localização
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                obterUltimaLocalizacao();
                fused.requestLocationUpdates(locRequest, locCallback, getMainLooper());
            } else {
                Toast.makeText(this, "Permissão de localização negada.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}