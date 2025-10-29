package com.instituto.bancodealimentos;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AlterarPixWhatsappActivity extends AppCompatActivity {

    private EditText etPixNome, etPixCidade, etPixChave, etWhats;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowInsetsHelper.setupEdgeToEdge(this);
        setContentView(R.layout.activity_alterar_pix_whatsapp);

        // Aplicar insets
        WindowInsetsHelper.applyTopInsets(findViewById(R.id.header));

        // header com padding de status bar (igual suas telas edge-to-edge)

        ImageButton btnVoltar = findViewById(R.id.btn_voltar);
        btnVoltar.setOnClickListener(v -> finish());

        etPixNome   = findViewById(R.id.etPixNome);
        etPixCidade = findViewById(R.id.etPixCidade);
        etPixChave  = findViewById(R.id.etPixChave);
        etWhats     = findViewById(R.id.etWhats);

        // pré-carrega dados salvos
        etPixNome.setText(SettingsRepository.getPixNome(this));
        etPixCidade.setText(SettingsRepository.getPixCidade(this));
        etPixChave.setText(SettingsRepository.getPixChave(this));
        etWhats.setText(SettingsRepository.getWhats(this));

        findViewById(R.id.btnSalvar).setOnClickListener(v -> salvar());
    }

    private void salvar() {
        String nome   = etPixNome.getText().toString().trim();
        String cidade = etPixCidade.getText().toString().trim();
        String chave  = etPixChave.getText().toString().trim();
        String whats  = etWhats.getText().toString();

        if (TextUtils.isEmpty(nome))   { etPixNome.setError("Informe o nome da conta"); etPixNome.requestFocus(); return; }
        if (TextUtils.isEmpty(cidade)) { etPixCidade.setError("Informe a cidade");     etPixCidade.requestFocus(); return; }
        if (TextUtils.isEmpty(chave))  { etPixChave.setError("Informe a chave PIX");   etPixChave.requestFocus(); return; }

        String digits = SettingsRepository.digits(whats);
        if (!TextUtils.isEmpty(digits) && digits.length() < 10) {
            etWhats.setError("Número muito curto (use DDD). Ex.: 51999998888");
            etWhats.requestFocus();
            return;
        }

        SettingsRepository.savePix(this, nome, cidade, chave);
        SettingsRepository.saveWhats(this, whats);

        Toast.makeText(this, "Dados atualizados com sucesso.", Toast.LENGTH_SHORT).show();
        finish();
    }
}