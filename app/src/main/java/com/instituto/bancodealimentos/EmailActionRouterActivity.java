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
        if (link == null) { finishToHome(); return; }

        final String modeRaw = link.getQueryParameter("mode");   // resetPassword, verifyEmail, recoverEmail, verifyAndChangeEmail
        final String oob     = link.getQueryParameter("oobCode");

        if (oob == null || oob.isEmpty() || modeRaw == null || modeRaw.isEmpty()) {
            toast("Link inválido.");
            finishToHome();
            return;
        }

        final String mode = modeRaw.toLowerCase();

        switch (mode) {
            case "resetpassword":
                // Valida o código e envia para a tela de definir nova senha
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

            case "verifyemail":
                // Verificação de e-mail (ex.: cadastro novo)
                FirebaseAuth.getInstance().applyActionCode(oob)
                        .addOnSuccessListener(unused -> {
                            toast("E-mail verificado com sucesso!");
                            goToSettingsOrLogin();
                        })
                        .addOnFailureListener(e -> {
                            toast("Falha ao verificar: " + e.getMessage());
                            finishToHome();
                        });
                break;

            case "verifyandchangeemail":
                // Verificar e ALTERAR e-mail (fluxo verifyBeforeUpdateEmail)
                // Primeiro checa se o código é válido (opcional, só pra mensagem mais clara)
                FirebaseAuth.getInstance().checkActionCode(oob)
                        .addOnSuccessListener(result -> FirebaseAuth.getInstance().applyActionCode(oob)
                                .addOnSuccessListener(unused -> {
                                    // E-mail já foi alterado ao aplicar o código
                                    startActivity(new Intent(this, EmailAtualizadoSucessoActivity.class)
                                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                                    finish();
                                })
                                .addOnFailureListener(err -> {
                                    toast("Não foi possível confirmar: " + err.getMessage());
                                    finishToHome();
                                }))
                        .addOnFailureListener(e -> {
                            toast("Código inválido ou expirado.");
                            finishToHome();
                        });
                break;

            case "recoveremail":
                // Recuperar e-mail (desfazer alteração)
                FirebaseAuth.getInstance().checkActionCode(oob)
                        .addOnSuccessListener(info -> FirebaseAuth.getInstance().applyActionCode(oob)
                                .addOnSuccessListener(unused -> {
                                    toast("E-mail recuperado com sucesso.");
                                    goToSettingsOrLogin();
                                })
                                .addOnFailureListener(err -> {
                                    toast("Falha ao recuperar e-mail.");
                                    finishToHome();
                                }))
                        .addOnFailureListener(e -> {
                            toast("Link inválido ou expirado.");
                            finishToHome();
                        });
                break;

            default:
                // Qualquer outro modo desconhecido
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
