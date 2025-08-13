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
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public class pontosdecoleta extends AppCompatActivity implements OnMapReadyCallback {

    private static final int RC_LOCATION = 1010;

    private GoogleMap gMap;
    private FusedLocationProviderClient fused;

    private RecyclerView rv;
    private PontoColetaAdapter adapter;
    private final List<PontoColeta> data = new ArrayList<>();

    private Location ultimaLocalizacao; // usada para calcular distância

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

        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.map_container, mapFragment)
                .commit();
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;
        enableMyLocation();
    }

    private void enableMyLocation() {
        boolean fine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (!fine && !coarse) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    RC_LOCATION
            );
            return;
        }

        if (gMap != null) gMap.setMyLocationEnabled(true);

        fused.getLastLocation().addOnSuccessListener(this, location -> {
            // guarda a última localização (pode ser null)
            ultimaLocalizacao = location;

            if (location != null) {
                moveCameraTo(location);
            } else {
                Toast.makeText(this, "Não foi possível obter a localização.", Toast.LENGTH_SHORT).show();
            }

            // carrega os pontos do Firestore mesmo que a localização seja null (sem distância)
            carregarPontosDoFirestore();
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Erro ao obter localização: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    private void moveCameraTo(@NonNull Location location) {
        LatLng here = new LatLng(location.getLatitude(), location.getLongitude());
        gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(here, 14f));
        gMap.addMarker(new MarkerOptions().position(here).title("Você está aqui"));
    }

    private void carregarPontosDoFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("pontos").get()
                .addOnSuccessListener(sn -> {
                    data.clear();

                    if (gMap != null) {
                        // opcional: limpar marcadores antigos antes de adicionar novos
                        gMap.clear();
                        // recoloca o marcador do usuário (se tivermos a última localização)
                        if (ultimaLocalizacao != null) {
                            LatLng me = new LatLng(ultimaLocalizacao.getLatitude(), ultimaLocalizacao.getLongitude());
                            gMap.addMarker(new MarkerOptions().position(me).title("Você está aqui"));
                            gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(me, 14f));
                        }
                    }

                    for (DocumentSnapshot d : sn.getDocuments()) {
                        String nome = d.getString("nome");
                        String endereco = d.getString("endereco");
                        String disp = d.getString("disponibilidade");
                        GeoPoint gp = d.getGeoPoint("location");

                        double distanciaKm = 0.0;
                        if (ultimaLocalizacao != null && gp != null) {
                            float[] res = new float[1];
                            Location.distanceBetween(
                                    ultimaLocalizacao.getLatitude(), ultimaLocalizacao.getLongitude(),
                                    gp.getLatitude(), gp.getLongitude(),
                                    res
                            );
                            distanciaKm = res[0] / 1000.0;
                        }

                        data.add(new PontoColeta(
                                nome != null ? nome : "",
                                endereco != null ? endereco : "",
                                distanciaKm,
                                disp
                        ));

                        if (gMap != null && gp != null && nome != null) {
                            LatLng ll = new LatLng(gp.getLatitude(), gp.getLongitude());
                            gMap.addMarker(new MarkerOptions().position(ll).title(nome));
                        }
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erro ao carregar pontos: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RC_LOCATION) {
            boolean granted = true;
            for (int r : grantResults) granted &= (r == PackageManager.PERMISSION_GRANTED);
            if (granted) {
                enableMyLocation();
            } else {
                Toast.makeText(this, "Permissão de localização negada.", Toast.LENGTH_SHORT).show();
                // Mesmo sem localização, ainda dá para carregar os pontos (sem distância)
                carregarPontosDoFirestore();
            }
        }
    }
}