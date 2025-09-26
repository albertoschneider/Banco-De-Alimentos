package com.instituto.bancodealimentos;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class ResetarSenhaActivity extends AppCompatActivity {

    public static final String EXTRA_OOBCODE = "oobCode";

    private TextInputEditText etNova, etConfirmar;
    private String oobCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resetar_senha);

        // Header com o MESMO tratamento de insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.header), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(v.getPaddingLeft(), sb.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        oobCode = getIntent().getStringExtra(EXTRA_OOBCODE);
        if (oobCode == null) { finish(); return; }

        etNova = findViewById(R.id.etNovaSenha);
        etConfirmar = findViewById(R.id.etConfirmarSenha);
        MaterialButton btnSalvar = findViewById(R.id.btnSalvar);
        ImageButton back = findViewById(R.id.btn_voltar);
        back.setOnClickListener(v -> finish());

        btnSalvar.setOnClickListener(v -> {
            String s1 = val(etNova);
            String s2 = val(etConfirmar);
            if (TextUtils.isEmpty(s1) || s1.length() < 6) { toast("Senha mínima de 6 caracteres"); return; }
            if (!s1.equals(s2)) { toast("As senhas devem ser iguais"); return; }

            FirebaseAuth.getInstance().confirmPasswordReset(oobCode, s1)
                    .addOnSuccessListener(unused -> {
                        toast("Senha alterada com sucesso!");
                        Intent i = new Intent(this, configuracoes_email_senha.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(i);
                        finish();
                    })
                    .addOnFailureListener(e -> toast("Erro ao definir senha: " + e.getMessage()));
        });
    }

    private String val(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString();
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
