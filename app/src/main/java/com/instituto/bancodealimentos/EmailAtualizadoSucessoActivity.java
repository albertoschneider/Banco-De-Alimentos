package com.instituto.bancodealimentos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

public class EmailAtualizadoSucessoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowInsetsHelper.setupEdgeToEdge(this);
        setContentView(R.layout.activity_email_atualizado_sucesso);

        // Aplicar insets
        WindowInsetsHelper.applyTopInsets(findViewById(R.id.header));

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.header), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(v.getPaddingLeft(), sb.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        ImageButton back = findViewById(R.id.btn_voltar);
        back.setOnClickListener(v -> finish());

        MaterialButton btn = findViewById(R.id.btnBackSettings);
        btn.setOnClickListener(v -> {
            startActivity(new Intent(this, configuracoes_email_senha.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        });
    }
}