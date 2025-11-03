package com.instituto.bancodealimentos;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.FirebaseAuth;

public class EsqueciSenhaActivity extends AppCompatActivity {

    private TextInputEditText etEmail;

    // CORRIGIDO: URL aponta para a tela de sucesso que redireciona para ResetarSenhaActivity
    private static final String URL_RESETAR_SENHA =
            "https://albertoschneider.github.io/success/resetar-senha/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowInsetsHelper.setupEdgeToEdge(this);
        setContentView(R.layout.activity_esqueci_senha);

        // Aplicar insets
        WindowInsetsHelper.applyTopInsets(findViewById(R.id.header));

        etEmail = findViewById(R.id.etEmail);
        MaterialButton btn = findViewById(R.id.btnEnviarLink);
        ImageButton back = findViewById(R.id.btn_voltar);

        if (back != null) {
            back.setOnClickListener(v -> finish());
        }

        btn.setOnClickListener(v -> {
            String email = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Digite um e-mail válido", Toast.LENGTH_SHORT).show();
                return;
            }

            // CORRIGIDO: ActionCodeSettings configurado para abrir no app
            ActionCodeSettings settings = ActionCodeSettings.newBuilder()
                    .setUrl(URL_RESETAR_SENHA)
                    .setHandleCodeInApp(true) // IMPORTANTE: processa no app
                    .setAndroidPackageName(getPackageName(), true, null)
                    .build();

            FirebaseAuth.getInstance().sendPasswordResetEmail(email, settings)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this,
                                "Link de redefinição enviado para " + email + ". Verifique sua caixa de entrada.",
                                Toast.LENGTH_LONG).show();
                        finish(); // Volta para a tela de login
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Erro ao enviar e-mail: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });
    }
}