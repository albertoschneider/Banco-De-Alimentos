package com.instituto.bancodealimentos;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class DeepLinkSuccessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handle(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handle(intent);
    }

    private void handle(@Nullable Intent intent) {
        Uri data = intent != null ? intent.getData() : null;
        if (data == null) { finishToHome(); return; }

        String path = data.getPath(); // ex.: /success/senha-redefinida/ ou /success/email-verificado/ ou /success/email-alterado/
        if (path == null) { finishToHome(); return; }

        // Usuário logado?
        boolean logged = FirebaseAuth.getInstance().getCurrentUser() != null;

        if (path.startsWith("/success/senha-redefinida")) {
            // Você pediu:
            // - se logado → volta pro menu com toast
            // - se não logado (esqueceu a senha) → volta pra tela inicial (login) com toast
            Toast.makeText(this, "Senha alterada com sucesso!", Toast.LENGTH_SHORT).show();
            Intent dest = new Intent(this, logged ? menu.class : telalogin.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(dest);
            finish();
            return;
        }

        if (path.startsWith("/success/email-alterado")) {
            // Tela dedicada de sucesso de e-mail alterado
            startActivity(new Intent(this, EmailAtualizadoSucessoActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
            return;
        }

        if (path.startsWith("/success/email-verificado")) {
            // Após verificar o e-mail, manda pro menu (ou configurações, se preferir)
            Toast.makeText(this, "E-mail verificado com sucesso!", Toast.LENGTH_SHORT).show();
            Intent dest = new Intent(this, logged ? menu.class : telalogin.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(dest);
            finish();
            return;
        }

        // Qualquer outro success → home
        finishToHome();
    }

    private void finishToHome() {
        startActivity(new Intent(this, SplashActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }
}
