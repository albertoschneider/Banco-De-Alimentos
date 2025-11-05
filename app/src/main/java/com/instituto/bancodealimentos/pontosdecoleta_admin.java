package com.instituto.bancodealimentos;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
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

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class pontosdecoleta_admin extends AppCompatActivity {

    private EditText etNome, etRua, etNumero, etBairro, etCidade, etEstado, etCep;
    private EditText etLatitude, etLongitude;
    private Button btnCancelar, btnSalvar;
    private TextView tvErro, tvLinkManual;
    private Spinner spDisponibilidade;

    private FirebaseFirestore db;
    private boolean isSearching = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pontosdecoleta_admin);

        ImageButton btnVoltar = findViewById(R.id.btn_voltar);
        etNome = findViewById(R.id.etNome);
        etRua = findViewById(R.id.etRua);
        etNumero = findViewById(R.id.etNumero);
        etBairro = findViewById(R.id.etBairro);
        etCidade = findViewById(R.id.etCidade);
        etEstado = findViewById(R.id.etEstado);
        etCep = findViewById(R.id.etCep);
        etLatitude = findViewById(R.id.etLatitude);
        etLongitude = findViewById(R.id.etLongitude);
        btnCancelar = findViewById(R.id.btnCancelar);
        btnSalvar = findViewById(R.id.btnSalvar);
        tvErro = findViewById(R.id.tvErro);
        tvLinkManual = findViewById(R.id.tvLinkManual);
        spDisponibilidade = findViewById(R.id.spDisponibilidade);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Disponível", "Indisponível"}
        );
        spDisponibilidade.setAdapter(adapter);

        btnVoltar.setOnClickListener(v -> finish());
        btnCancelar.setOnClickListener(v -> finish());

        // TextWatcher para buscar coordenadas automaticamente
        TextWatcher addressWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                atualizarEstadoSalvar();
                if (todosEnderecosCamposPreenchidos()) {
                    buscarCoordenadasAutomaticamente();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        // Adiciona watchers em todos os campos de endereço
        etRua.addTextChangedListener(addressWatcher);
        etNumero.addTextChangedListener(addressWatcher);
        etBairro.addTextChangedListener(addressWatcher);
        etCidade.addTextChangedListener(addressWatcher);
        etEstado.addTextChangedListener(addressWatcher);
        etCep.addTextChangedListener(addressWatcher);

        atualizarEstadoSalvar();

        tvLinkManual.setOnClickListener(v -> abrirDialogManual());

        // Firestore
        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();

        btnSalvar.setOnClickListener(v -> salvarNoFirestore());
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

    private void salvarNoFirestore() {
        String nome = etNome.getText().toString().trim();
        String enderecoCompleto = montarEnderecoCompleto();
        String lat = etLatitude.getText().toString().trim();
        String lng = etLongitude.getText().toString().trim();
        String disp = spDisponibilidade.getSelectedItem().toString();

        if (TextUtils.isEmpty(nome)) {
            Toast.makeText(this, "Digite o nome do ponto.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(enderecoCompleto)) {
            Toast.makeText(this, "Preencha os campos de endereço.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(lat) || TextUtils.isEmpty(lng)) {
            Toast.makeText(this, "Aguarde a busca das coordenadas ou informe-as manualmente.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> doc = new HashMap<>();
        doc.put("nome", nome);
        doc.put("endereco", enderecoCompleto);

        Map<String, Object> location = new HashMap<>();
        location.put("lat", Double.parseDouble(lat));
        location.put("lng", Double.parseDouble(lng));
        doc.put("location", location);

        doc.put("disponibilidade", disp);
        doc.put("ativo", true);

        btnSalvar.setEnabled(false);

        db.collection("pontos")
                .add(doc)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Ponto salvo!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSalvar.setEnabled(true);
                    Toast.makeText(this, "Erro ao salvar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void atualizarEstadoSalvar() {
        boolean ok = !TextUtils.isEmpty(etLatitude.getText().toString().trim())
                && !TextUtils.isEmpty(etLongitude.getText().toString().trim());

        btnSalvar.setEnabled(ok);

        if (ok) {
            btnSalvar.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F4B400")));
            btnSalvar.setTextColor(Color.parseColor("#004E7C"));
        } else {
            btnSalvar.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#9CA3AF")));
            btnSalvar.setTextColor(Color.parseColor("#FFFFFF"));
        }
    }

    private void abrirDialogManual() {
        EditText input = new EditText(this);
        input.setHint("Ex: -29.6000826,-51.1621926");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Colar coordenadas")
                .setView(input)
                .setPositiveButton("Usar", null)
                .setNegativeButton("Cancelar", (d, w) -> d.dismiss())
                .create();

        dialog.setOnShowListener(dlg -> {
            Button b = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            b.setOnClickListener(v -> {
                String text = input.getText().toString().trim();
                if (TextUtils.isEmpty(text)) {
                    input.setError("Cole as coordenadas");
                    return;
                }
                if (text.contains(",")) {
                    String[] parts = text.split(",");
                    if (parts.length == 2) {
                        try {
                            double lat = Double.parseDouble(parts[0].trim());
                            double lon = Double.parseDouble(parts[1].trim());
                            etLatitude.setText(String.format(Locale.US, "%.8f", lat));
                            etLongitude.setText(String.format(Locale.US, "%.8f", lon));
                            tvErro.setVisibility(View.GONE);
                            atualizarEstadoSalvar();
                            dialog.dismiss();
                            return;
                        } catch (Exception ignored) { }
                    }
                }
                Toast.makeText(this, "Formato inválido. Use lat,lng", Toast.LENGTH_LONG).show();
            });
        });

        dialog.show();
    }

    private void buscarCoordenadas(String endereco) {
        new AsyncTask<String, Void, double[]>() {
            @Override
            protected double[] doInBackground(String... strings) {
                String query = strings[0];

                // Geocoder nativo
                try {
                    Geocoder geocoder = new Geocoder(pontosdecoleta_admin.this, Locale.getDefault());
                    List<Address> list = geocoder.getFromLocationName(query, 1);
                    if (list != null && !list.isEmpty()) {
                        Address a = list.get(0);
                        return new double[]{a.getLatitude(), a.getLongitude()};
                    }
                } catch (Exception ignored) { }

                // Fallback: Nominatim
                try {
                    String encoded = URLEncoder.encode(query, "UTF-8");
                    String urlStr = "https://nominatim.openstreetmap.org/search?q=" + encoded +
                            "&format=json&limit=1&addressdetails=0";
                    URL url = new URL(urlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);
                    conn.setRequestProperty("User-Agent", "BancoDeAlimentos/1.0 (seuemail@dominio.com)");
                    conn.setRequestProperty("Accept", "application/json");
                    int code = conn.getResponseCode();
                    if (code == 200) {
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
                            String latS = json.substring(latIdx + 7, latEnd);
                            String lonS = json.substring(lonIdx + 7, lonEnd);
                            double lat = Double.parseDouble(latS);
                            double lon = Double.parseDouble(lonS);
                            return new double[]{lat, lon};
                        }
                    }
                } catch (UnsupportedEncodingException e) {
                    return null;
                } catch (Exception ignored) { }

                return null;
            }

            @Override
            protected void onPostExecute(double[] result) {
                isSearching = false;
                if (result != null) {
                    etLatitude.setText(String.format(Locale.US, "%.8f", result[0]));
                    etLongitude.setText(String.format(Locale.US, "%.8f", result[1]));
                    tvErro.setVisibility(View.GONE);
                    atualizarEstadoSalvar();
                } else {
                    tvErro.setVisibility(View.VISIBLE);
                    etLatitude.setText("");
                    etLongitude.setText("");
                    atualizarEstadoSalvar();
                }
            }
        }.execute(endereco);
    }
}