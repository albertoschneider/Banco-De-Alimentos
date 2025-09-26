package com.instituto.bancodealimentos;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class EmailActionRouterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(@Nullable Intent intent) {
        Uri link = (intent != null) ? intent.getData() : null;
        if (link == null) { finish(); return; }

        String mode = link.getQueryParameter("mode");
        String oob = link.getQueryParameter("oobCode");

        if (oob == null || oob.isEmpty()) { finish(); return; }

        switch (mode != null ? mode : "") {
            case "resetPassword":
                FirebaseAuth.getInstance().verifyPasswordResetCode(oob)
                        .addOnSuccessListener(email -> {
                            Intent i = new Intent(this, ResetarSenhaActivity.class);
                            i.putExtra(ResetarSenhaActivity.EXTRA_OOBCODE, oob);
                            startActivity(i);
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Link inválido ou expirado.", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                break;

            case "verifyEmail":
                FirebaseAuth.getInstance().applyActionCode(oob)
                        .addOnSuccessListener(unused -> {
                            startActivity(new Intent(this, EmailAtualizadoSucessoActivity.class));
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Falha ao verificar e-mail: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            finish();
                        });
                break;

            case "recoverEmail":
                FirebaseAuth.getInstance().checkActionCode(oob)
                        .addOnSuccessListener(info -> FirebaseAuth.getInstance().applyActionCode(oob)
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(this, "E-mail recuperado com sucesso.", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(err -> {
                                    Toast.makeText(this, "Falha ao recuperar e-mail.", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                        )
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Link inválido ou expirado.", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                break;

            default:
                finish();
        }
    }
}
