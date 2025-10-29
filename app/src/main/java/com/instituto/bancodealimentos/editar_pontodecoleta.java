package com.instituto.bancodealimentos;

import android.app.AlertDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.location.Address;
import android.location.Geocoder;
import android.content.res.ColorStateList;
import android.graphics.Color;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

public class editar_pontodecoleta extends AppCompatActivity {

    private EditText etNome, etEndereco, etLatitude, etLongitude;
    private Button btnBuscar, btnCancelar, btnSalvar;
    private TextView tvErro, tvLinkManual, tvTitulo;
    private Spinner spDisponibilidade;

    private FirebaseFirestore db;
    private String docId;  // id do documento no Firestore

    // Cores (hex) para o botão Salvar
    private static final String COR_ATIVO   = "#2563EB";  // azul
    private static final String COR_INATIVO = "#9CA3AF";  // cinza
    private static final String TEXTO_ATIVO = "#FFFFFF";
    private static final String TEXTO_INATIVO = "#FFFFFF";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowInsetsHelper.setupEdgeToEdge(this);
        setContentView(R.layout.activity_editar_pontodecoleta);

        // Aplicar insets
        WindowInsetsHelper.applyTopInsets(findViewById(R.id.header));


        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();

        ImageButton btnVoltar = findViewById(R.id.btn_voltar);
        etNome       = findViewById(R.id.etNome);
        etEndereco   = findViewById(R.id.etEndereco);
        etLatitude   = findViewById(R.id.etLatitude);
        etLongitude  = findViewById(R.id.etLongitude);
        btnBuscar    = findViewById(R.id.btnBuscar);
        btnCancelar  = findViewById(R.id.btnCancelar);
        btnSalvar    = findViewById(R.id.btnSalvar);
        tvErro       = findViewById(R.id.tvErro);
        tvLinkManual = findViewById(R.id.tvLinkManual);
        spDisponibilidade = findViewById(R.id.spDisponibilidade);
        tvTitulo     = findViewById(R.id.tvTitulo);

        tvTitulo.setText("Editar Ponto de Coleta");
        btnSalvar.setText("Salvar alterações");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Disponível", "Indisponível"});
        spDisponibilidade.setAdapter(adapter);

        btnVoltar.setOnClickListener(v -> finish());
        btnCancelar.setOnClickListener(v -> finish());

        // --- recebe dados do ponto via Intent ---
        docId = getIntent().getStringExtra("docId");
        etNome.setText(getIntent().getStringExtra("nome"));
        etEndereco.setText(getIntent().getStringExtra("endereco"));
        etLatitude.setText(getIntent().getStringExtra("lat"));
        etLongitude.setText(getIntent().getStringExtra("lng"));
        String disp = getIntent().getStringExtra("disponibilidade");
        if (disp != null) spDisponibilidade.setSelection("Indisponível".equalsIgnoreCase(disp) ? 1 : 0);

        // watchers para habilitar/desabilitar o botão conforme campos mudam
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { atualizarEstadoSalvar(); }
            @Override public void afterTextChanged(Editable s) {}
        };
        etNome.addTextChangedListener(watcher);
        etEndereco.addTextChangedListener(watcher);
        etLatitude.addTextChangedListener(watcher);
        etLongitude.addTextChangedListener(watcher);

        // Atualiza estado inicial com os dados já preenchidos
        atualizarEstadoSalvar();

        // buscar coordenadas / colar manual
        btnBuscar.setOnClickListener(v -> {
            String endereco = etEndereco.getText().toString().trim();
            if (TextUtils.isEmpty(endereco)) {
                Toast.makeText(this, "Digite um endereço.", Toast.LENGTH_SHORT).show();
                return;
            }
            tvErro.setVisibility(android.view.View.GONE);
            buscarCoordenadas(endereco);
        });

        tvLinkManual.setOnClickListener(v -> abrirDialogManual());

        btnSalvar.setOnClickListener(v -> salvarAlteracoes());
    }

    /** Habilita/desabilita o botão de salvar e troca cores (hex). */
    /** Habilita/desabilita o botão de salvar e aplica as cores iguais à outra tela */
    private void atualizarEstadoSalvar() {
        boolean ok =
                !TextUtils.isEmpty(etNome.getText().toString().trim()) &&
                        !TextUtils.isEmpty(etEndereco.getText().toString().trim()) &&
                        !TextUtils.isEmpty(etLatitude.getText().toString().trim()) &&
                        !TextUtils.isEmpty(etLongitude.getText().toString().trim());

        btnSalvar.setEnabled(ok);

        if (ok) {
            // botão ativo
            btnSalvar.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F4B400")));
            btnSalvar.setTextColor(Color.parseColor("#004E7C"));
        } else {
            // botão desativado
            btnSalvar.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#9CA3AF"))); // cinza
            btnSalvar.setTextColor(Color.parseColor("#FFFFFF")); // texto branco mesmo desabilitado
        }
    }


    private void salvarAlteracoes() {
        if (TextUtils.isEmpty(docId)) {
            Toast.makeText(this, "ID inválido.", Toast.LENGTH_LONG).show();
            return;
        }
        String nome = etNome.getText().toString().trim();
        String endereco = etEndereco.getText().toString().trim();
        String lat = etLatitude.getText().toString().trim();
        String lng = etLongitude.getText().toString().trim();
        String disp = spDisponibilidade.getSelectedItem().toString();

        if (TextUtils.isEmpty(nome) || TextUtils.isEmpty(endereco) ||
                TextUtils.isEmpty(lat) || TextUtils.isEmpty(lng)) {
            Toast.makeText(this, "Preencha todos os campos.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> location = new HashMap<>();
        location.put("lat", Double.parseDouble(lat));
        location.put("lng", Double.parseDouble(lng));

        Map<String, Object> doc = new HashMap<>();
        doc.put("nome", nome);
        doc.put("endereco", endereco);
        doc.put("location", location);
        doc.put("disponibilidade", disp);

        btnSalvar.setEnabled(false);
        DocumentReference ref = db.collection("pontos").document(docId);
        ref.update(doc)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Alterações salvas!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSalvar.setEnabled(true);
                    atualizarEstadoSalvar();
                    Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // ====== utilitários ======
    private void abrirDialogManual() {
        EditText input = new EditText(this);
        input.setHint("Ex: -29.6000826,-51.1621926");

        new AlertDialog.Builder(this)
                .setTitle("Colar coordenadas")
                .setView(input)
                .setPositiveButton("Usar", (d, w) -> {
                    String text = input.getText().toString().trim();
                    if (text.contains(",")) {
                        String[] p = text.split(",");
                        try {
                            etLatitude.setText(String.format(Locale.US, "%.8f", Double.parseDouble(p[0].trim())));
                            etLongitude.setText(String.format(Locale.US, "%.8f", Double.parseDouble(p[1].trim())));
                            atualizarEstadoSalvar();
                        } catch (Exception ignored) {}
                    } else {
                        Toast.makeText(this, "Formato inválido. Use lat,lng", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void buscarCoordenadas(String endereco) {
        new AsyncTask<String, Void, double[]>() {
            @Override protected double[] doInBackground(String... s) {
                try {
                    Geocoder g = new Geocoder(editar_pontodecoleta.this, Locale.getDefault());
                    List<Address> list = g.getFromLocationName(s[0], 1);
                    if (list != null && !list.isEmpty()) {
                        Address a = list.get(0);
                        return new double[]{a.getLatitude(), a.getLongitude()};
                    }
                } catch (Exception ignored) {}

                try {
                    String encoded = URLEncoder.encode(endereco, "UTF-8");
                    String urlStr = "https://nominatim.openstreetmap.org/search?q=" + encoded + "&format=json&limit=1&addressdetails=0";
                    URL url = new URL(urlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);
                    conn.setRequestProperty("User-Agent", "BancoDeAlimentos/1.0 (email@dominio.com)");
                    conn.setRequestProperty("Accept", "application/json");
                    if (conn.getResponseCode() == 200) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder(); String line;
                        while ((line = br.readLine()) != null) sb.append(line);
                        br.close();
                        String json = sb.toString();
                        int latIdx = json.indexOf("\"lat\":\"");
                        int lonIdx = json.indexOf("\"lon\":\"");
                        if (latIdx != -1 && lonIdx != -1) {
                            int latEnd = json.indexOf("\"", latIdx + 7);
                            int lonEnd = json.indexOf("\"", lonIdx + 7);
                            return new double[]{
                                    Double.parseDouble(json.substring(latIdx + 7, latEnd)),
                                    Double.parseDouble(json.substring(lonIdx + 7, lonEnd))
                            };
                        }
                    }
                } catch (Exception ignored) {}
                return null;
            }
            @Override protected void onPostExecute(double[] r) {
                if (r != null) {
                    etLatitude.setText(String.format(Locale.US, "%.8f", r[0]));
                    etLongitude.setText(String.format(Locale.US, "%.8f", r[1]));
                    tvErro.setVisibility(android.view.View.GONE);
                } else {
                    tvErro.setVisibility(android.view.View.VISIBLE);
                }
                atualizarEstadoSalvar();
            }
        }.execute(endereco);
    }
}