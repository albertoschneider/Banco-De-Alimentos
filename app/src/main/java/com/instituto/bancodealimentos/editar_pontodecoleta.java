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

    private EditText etNome, etRua, etNumero, etBairro, etCidade, etEstado, etCep;
    private EditText etLatitude, etLongitude;
    private Button btnCancelar, btnSalvar;
    private TextView tvErro, tvLinkManual, tvTitulo;
    private Spinner spDisponibilidade;

    private FirebaseFirestore db;
    private String docId;
    private boolean isSearching = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. SEMPRE chamar setupEdgeToEdge PRIMEIRO
        WindowInsetsHelper.setupEdgeToEdge(this);

        setContentView(R.layout.activity_editar_pontodecoleta);

        // 2. Aplicar insets no HEADER (24dp extra no topo)
        WindowInsetsHelper.applyTopInsets(findViewById(R.id.header));

        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();

        ImageButton btnVoltar = findViewById(R.id.btn_voltar);
        etNome       = findViewById(R.id.etNome);
        etRua        = findViewById(R.id.etRua);
        etNumero     = findViewById(R.id.etNumero);
        etBairro     = findViewById(R.id.etBairro);
        etCidade     = findViewById(R.id.etCidade);
        etEstado     = findViewById(R.id.etEstado);
        etCep        = findViewById(R.id.etCep);
        etLatitude   = findViewById(R.id.etLatitude);
        etLongitude  = findViewById(R.id.etLongitude);
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

        // Recebe dados do ponto via Intent
        docId = getIntent().getStringExtra("docId");
        etNome.setText(getIntent().getStringExtra("nome"));

        String enderecoCompleto = getIntent().getStringExtra("endereco");
        parsearEndereco(enderecoCompleto);

        etLatitude.setText(getIntent().getStringExtra("lat"));
        etLongitude.setText(getIntent().getStringExtra("lng"));
        String disp = getIntent().getStringExtra("disponibilidade");
        if (disp != null) spDisponibilidade.setSelection("Indisponível".equalsIgnoreCase(disp) ? 1 : 0);

        // Watchers para habilitar/desabilitar o botão e buscar coordenadas
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                atualizarEstadoSalvar();
                if (todosEnderecosCamposPreenchidos()) {
                    buscarCoordenadasAutomaticamente();
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        etNome.addTextChangedListener(watcher);
        etRua.addTextChangedListener(watcher);
        etNumero.addTextChangedListener(watcher);
        etBairro.addTextChangedListener(watcher);
        etCidade.addTextChangedListener(watcher);
        etEstado.addTextChangedListener(watcher);
        etCep.addTextChangedListener(watcher);

        atualizarEstadoSalvar();

        tvLinkManual.setOnClickListener(v -> abrirDialogManual());

        btnSalvar.setOnClickListener(v -> salvarAlteracoes());
    }

    private void parsearEndereco(String enderecoCompleto) {
        if (TextUtils.isEmpty(enderecoCompleto)) return;

        String[] partes = enderecoCompleto.split(",");

        if (partes.length >= 1) etRua.setText(partes[0].trim());
        if (partes.length >= 2) {
            String parte2 = partes[1].trim();
            if (parte2.matches("\\d+.*")) {
                etNumero.setText(parte2);
                if (partes.length >= 3) etBairro.setText(partes[2].trim());
                if (partes.length >= 4) etCidade.setText(partes[3].trim());
                if (partes.length >= 5) etEstado.setText(partes[4].trim());
                if (partes.length >= 6) etCep.setText(partes[5].trim());
            } else {
                etBairro.setText(parte2);
                if (partes.length >= 3) etCidade.setText(partes[2].trim());
                if (partes.length >= 4) etEstado.setText(partes[3].trim());
                if (partes.length >= 5) etCep.setText(partes[4].trim());
            }
        }
    }

    private boolean todosEnderecosCamposPreenchidos() {
        String rua = etRua.getText().toString().trim();
        String cidade = etCidade.getText().toString().trim();

        return !TextUtils.isEmpty(rua) && !TextUtils.isEmpty(cidade);
    }

    private String montarEnderecoCompleto() {
        StringBuilder sb = new StringBuilder();

        String rua = etRua.getText().toString().trim();
        String numero = etNumero.getText().toString().trim();
        String bairro = etBairro.getText().toString().trim();
        String cidade = etCidade.getText().toString().trim();

        if (!TextUtils.isEmpty(rua)) {
            sb.append(rua);
            if (!TextUtils.isEmpty(numero)) {
                sb.append(", ").append(numero);
            }
        }

        if (!TextUtils.isEmpty(bairro)) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(bairro);
        }

        if (!TextUtils.isEmpty(cidade)) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(cidade);
        }

        return sb.toString();
    }

    private void buscarCoordenadasAutomaticamente() {
        if (isSearching) return;

        String endereco = montarEnderecoCompleto();
        if (TextUtils.isEmpty(endereco)) return;

        isSearching = true;
        tvErro.setVisibility(View.GONE);
        buscarCoordenadas(endereco);
    }

    private void atualizarEstadoSalvar() {
        boolean ok =
                !TextUtils.isEmpty(etNome.getText().toString().trim()) &&
                        todosEnderecosCamposPreenchidos() &&
                        !TextUtils.isEmpty(etLatitude.getText().toString().trim()) &&
                        !TextUtils.isEmpty(etLongitude.getText().toString().trim());

        btnSalvar.setEnabled(ok);

        if (ok) {
            btnSalvar.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F4B400")));
            btnSalvar.setTextColor(Color.parseColor("#004E7C"));
        } else {
            btnSalvar.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#9CA3AF")));
            btnSalvar.setTextColor(Color.parseColor("#FFFFFF"));
        }
    }

    private void salvarAlteracoes() {
        if (TextUtils.isEmpty(docId)) {
            Toast.makeText(this, "ID inválido.", Toast.LENGTH_LONG).show();
            return;
        }
        String nome = etNome.getText().toString().trim();
        String enderecoCompleto = montarEnderecoCompleto();
        String lat = etLatitude.getText().toString().trim();
        String lng = etLongitude.getText().toString().trim();
        String disp = spDisponibilidade.getSelectedItem().toString();

        if (TextUtils.isEmpty(nome) || TextUtils.isEmpty(enderecoCompleto) ||
                TextUtils.isEmpty(lat) || TextUtils.isEmpty(lng)) {
            Toast.makeText(this, "Preencha todos os campos.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> location = new HashMap<>();
        location.put("lat", Double.parseDouble(lat));
        location.put("lng", Double.parseDouble(lng));

        Map<String, Object> doc = new HashMap<>();
        doc.put("nome", nome);
        doc.put("endereco", enderecoCompleto);
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
            @Override
            protected double[] doInBackground(String... s) {
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
                    String urlStr = "https://nominatim.openstreetmap.org/search?q=" + encoded +
                            "&format=json&limit=1&addressdetails=0";
                    URL url = new URL(urlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);
                    conn.setRequestProperty("User-Agent", "BancoDeAlimentos/1.0 (email@dominio.com)");
                    conn.setRequestProperty("Accept", "application/json");
                    if (conn.getResponseCode() == 200) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
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

            @Override
            protected void onPostExecute(double[] r) {
                isSearching = false;
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