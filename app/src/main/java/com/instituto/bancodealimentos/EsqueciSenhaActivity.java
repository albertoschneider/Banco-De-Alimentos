package com.instituto.bancodealimentos;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.FirebaseAuth;

public class EsqueciSenhaActivity extends AppCompatActivity {

    private TextInputEditText etEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_esqueci_senha);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.header), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(v.getPaddingLeft(), sb.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        etEmail = findViewById(R.id.etEmail);
        MaterialButton btn = findViewById(R.id.btnEnviarLink);
        ImageButton back = findViewById(R.id.btn_voltar);
        back.setOnClickListener(v -> finish());

        btn.setOnClickListener(v -> {
            String email = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Digite um e-mail válido", Toast.LENGTH_SHORT).show();
                return;
            }

            // IMPORTANTE: handleCodeInApp(true) + seu domínio do Hosting
            ActionCodeSettings settings = ActionCodeSettings.newBuilder()
                    .setUrl("https://barc-2025.web.app/finish") // qualquer rota do seu Hosting
                    .setHandleCodeInApp(true)
                    .setAndroidPackageName(getPackageName(), true, null)
                    .build();

            FirebaseAuth.getInstance().sendPasswordResetEmail(email, settings)
                    .addOnSuccessListener(unused ->
                            Toast.makeText(this, "Se o e-mail existir, enviaremos um link.", Toast.LENGTH_LONG).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });
    }
}
