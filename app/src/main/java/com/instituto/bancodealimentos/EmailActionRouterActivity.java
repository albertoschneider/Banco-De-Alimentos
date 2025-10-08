package com.instituto.bancodealimentos;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.ActionCodeResult;
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
        if (link == null) { finishToHome(); return; }

        final String mode = link.getQueryParameter("mode");   // resetPassword, verifyEmail, recoverEmail, verifyAndChangeEmail...
        final String oob  = link.getQueryParameter("oobCode");

        if (oob == null || oob.isEmpty() || mode == null || mode.isEmpty()) {
            toast("Link inválido.");
            finishToHome();
            return;
        }

        switch (mode) {
            case "resetPassword":
                FirebaseAuth.getInstance().verifyPasswordResetCode(oob)
                        .addOnSuccessListener(email -> {
                            Intent i = new Intent(this, ResetarSenhaActivity.class);
                            i.putExtra(ResetarSenhaActivity.EXTRA_OOBCODE, oob);
                            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(i);
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            toast("Link inválido ou expirado.");
                            finishToHome();
                        });
                break;

            case "verifyEmail":
            case "verifyAndChangeEmail":
            case "recoverEmail":
                FirebaseAuth.getInstance().checkActionCode(oob)
                        .addOnSuccessListener(result -> {
                            final int op = result.getOperation();
                            FirebaseAuth.getInstance().applyActionCode(oob)
                                    .addOnSuccessListener(unused -> {
                                        // Se o modo da URL já for verifyAndChangeEmail, tratamos como "e-mail alterado"
                                        if ("verifyAndChangeEmail".equals(mode)) {
                                            startActivity(new Intent(this, EmailAtualizadoSucessoActivity.class)
                                                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                                            finish();
                                            return;
                                        }

                                        // Demais operações (compatível com versões antigas do SDK)
                                        if (op == ActionCodeResult.VERIFY_EMAIL) {
                                            toast("E-mail verificado com sucesso!");
                                            goToSettingsOrLogin();
                                        } else if (op == ActionCodeResult.RECOVER_EMAIL) {
                                            toast("E-mail recuperado com sucesso.");
                                            goToSettingsOrLogin();
                                        } else {
                                            goToSettingsOrLogin();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        toast("Não foi possível confirmar: " + e.getMessage());
                                        finishToHome();
                                    });
                        })
                        .addOnFailureListener(e -> {
                            toast("Código inválido ou expirado.");
                            finishToHome();
                        });
                break;

            default:
                finishToHome();
        }
    }

    private void goToSettingsOrLogin() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            startActivity(new Intent(this, configuracoes_email_senha.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        } else {
            startActivity(new Intent(this, telalogin.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }
        finish();
    }

    private void finishToHome() {
        startActivity(new Intent(this, SplashActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
