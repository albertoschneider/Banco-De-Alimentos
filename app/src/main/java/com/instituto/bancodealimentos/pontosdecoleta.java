package com.instituto.bancodealimentos;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
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

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class pontosdecoleta extends AppCompatActivity {

    private static final int RC_LOCATION = 1010;

    private RecyclerView rv;
    private PontoColetaAdapter adapter;
    private final List<PontoColeta> data = new ArrayList<>();

    // Localização (Fused Location Provider)
    private FusedLocationProviderClient fused;
    private LocationRequest locRequest;
    private LocationCallback locCallback;
    private Location ultimaLocalizacao;

    // Firestore
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLECAO = "pontos"; // ou "pontos_coleta"

    // OSMDROID
    private MapView map;
    private MyLocationNewOverlay myLocationOverlay;
    private boolean jaCentralizeiNoUsuario = false;

    // Ícone custom dos pontos de coleta (vector)
    private Drawable pontoColetaIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // user-agent obrigatório para osmdroid
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_pontosdecoleta);

        ImageButton back = findViewById(R.id.btn_voltar);
        if (back != null) back.setOnClickListener(v -> onBackPressed());

        // Lista
        rv = findViewById(R.id.rvPontos);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PontoColetaAdapter(data);
        rv.setAdapter(adapter);

        // Mapa (OSM)
        map = findViewById(R.id.map_osm);
        if (map != null) {
            map.setTileSource(TileSourceFactory.MAPNIK);
            map.setMultiTouchControls(true);

            // Overlay da minha localização (com ícone ic_maps convertido para Bitmap)
            myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
            Drawable iconeVector = AppCompatResources.getDrawable(this, R.drawable.ic_maps);
            Bitmap iconeBitmap = drawableToBitmap(iconeVector);
            if (iconeBitmap != null) {
                myLocationOverlay.setPersonIcon(iconeBitmap);
                myLocationOverlay.setPersonHotspot(
                        iconeBitmap.getWidth() / 2f,
                        iconeBitmap.getHeight() / 2f
                );
            }
            myLocationOverlay.enableMyLocation();
            myLocationOverlay.enableFollowLocation();
            map.getOverlays().add(myLocationOverlay);
            map.getController().setZoom(15.0);

            // Carrega o ícone vector para os marcadores dos pontos de coleta
            pontoColetaIcon = AppCompatResources.getDrawable(this, R.drawable.ic_ponto_coleta);
        }

        // Localização (Fused)
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
                    if (!jaCentralizeiNoUsuario) {
                        centralizarNoUsuarioSePossivel();
                        jaCentralizeiNoUsuario = true;
                    }
                    atualizarDistanciasNaLista();
                }
            }
        };

        carregarPontosDoFirestore(); // plota marcadores
        obterUltimaLocalizacao();    // centraliza/calc distâncias
    }

    // Converte Drawable (inclui vector XML) para Bitmap
    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable == null) return null;
        int w = drawable.getIntrinsicWidth();
        int h = drawable.getIntrinsicHeight();
        if (w <= 0 || h <= 0) {
            float px = 48f * getResources().getDisplayMetrics().density; // fallback ~48dp
            w = h = (int) px;
        }
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bmp;
    }

    // ---------- Firestore ----------
    private void carregarPontosDoFirestore() {
        db.collection(COLECAO).get()
                .addOnSuccessListener(sn -> {
                    data.clear();

                    // remove marcadores antigos (mantém overlay de localização)
                    if (map != null) {
                        List<?> overlays = new ArrayList<>(map.getOverlays());
                        for (Object o : overlays) {
                            if (o instanceof Marker) map.getOverlays().remove(o);
                        }
                    }

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

                        // distância inicial se já houver localização
                        if (ultimaLocalizacao != null && lat != null && lng != null) {
                            float[] res = new float[1];
                            Location.distanceBetween(
                                    ultimaLocalizacao.getLatitude(), ultimaLocalizacao.getLongitude(),
                                    lat, lng, res
                            );
                            p.setDistanciaKm(res[0] / 1000.0);
                        }

                        data.add(p);

                        // marcador no mapa
                        adicionarMarcadorOSM(p.getNome(), lat, lng);
                    }

                    ordenarSePossivel();
                    adapter.notifyDataSetChanged();

                    // sem localização ainda? centraliza no 1º ponto válido
                    if (ultimaLocalizacao == null && map != null) {
                        for (PontoColeta p : data) {
                            if (p.getLat() != null && p.getLng() != null) {
                                map.getController().setZoom(13.0);
                                map.getController().setCenter(
                                        new org.osmdroid.util.GeoPoint(p.getLat(), p.getLng())
                                );
                                break;
                            }
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erro ao carregar pontos: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    // ---------- Marcadores OSM ----------
    private void adicionarMarcadorOSM(String titulo, Double lat, Double lng) {
        if (map == null || lat == null || lng == null) return;

        Marker m = new Marker(map);
        m.setPosition(new org.osmdroid.util.GeoPoint(lat, lng));
        m.setTitle(titulo != null ? titulo : "Ponto de coleta");

        // Ícone vector para o ponto de coleta + âncora no centro/base (estilo Google)
        if (pontoColetaIcon != null) m.setIcon(pontoColetaIcon);
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        m.setOnMarkerClickListener((marker, mapView) -> {
            abrirDialogoRota(titulo, lat, lng);
            return true;
        });
        map.getOverlays().add(m);
        map.invalidate();
    }

    private void abrirDialogoRota(String titulo, Double lat, Double lng) {
        if (lat == null || lng == null) return;

        final String[] opcoes = new String[]{"Ver rota", "Ver no mapa"};
        new AlertDialog.Builder(this)
                .setTitle(titulo != null ? titulo : "Ponto de coleta")
                .setItems(opcoes, (dialog, which) -> {
                    if (which == 0) {
                        android.net.Uri uri = android.net.Uri.parse("google.navigation:q=" + lat + "," + lng + "&mode=d");
                        startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                .setPackage("com.google.android.apps.maps"));
                    } else {
                        String label;
                        try {
                            label = titulo != null ? URLEncoder.encode(titulo, "UTF-8") : "Ponto";
                        } catch (Exception e) {
                            label = "Ponto";
                        }
                        android.net.Uri uri = android.net.Uri.parse("geo:0,0?q=" + lat + "," + lng + "(" + label + ")");
                        startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                .setPackage("com.google.android.apps.maps"));
                    }
                })
                .show();
    }

    private void centralizarNoUsuarioSePossivel() {
        if (map == null || ultimaLocalizacao == null) return;
        map.getController().setZoom(15.0);
        map.getController().setCenter(
                new org.osmdroid.util.GeoPoint(
                        ultimaLocalizacao.getLatitude(),
                        ultimaLocalizacao.getLongitude()
                )
        );
    }

    // ---------- Localização ----------
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
                ultimaLocalizacao = loc;
                if (!jaCentralizeiNoUsuario) {
                    centralizarNoUsuarioSePossivel();
                    jaCentralizeiNoUsuario = true;
                }
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

    // ---------- Ciclo de vida ----------
    @Override
    protected void onResume() {
        super.onResume();
        if (map != null) map.onResume();

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
        if (map != null) map.onPause();
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