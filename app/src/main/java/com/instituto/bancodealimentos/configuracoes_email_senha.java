package com.instituto.bancodealimentos;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

public class configuracoes_email_senha extends AppCompatActivity {

    private TextInputEditText edtNome, edtEmail, edtSenha;
    private MaterialButton btnSalvar, btnSair, btnExcluir;
    private ImageButton btnVoltar;

    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuracoes_email_senha);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        db   = FirebaseFirestore.getInstance();

        if (user == null) {
            goToLogin();
            return;
        }

        // Views
        edtNome   = findViewById(R.id.edtNome);
        edtEmail  = findViewById(R.id.edtEmail);
        edtSenha  = findViewById(R.id.edtSenha);
        btnSalvar = findViewById(R.id.btnSalvar);
        btnSair   = findViewById(R.id.btnSair);
        btnExcluir= findViewById(R.id.btnExcluir);
        btnVoltar = findViewById(R.id.btn_voltar);

        preencherDados();

        btnVoltar.setOnClickListener(v -> finish());

        // === Pop-up de Sair ===
        btnSair.setOnClickListener(v -> mostrarDialogSair());

        btnSalvar.setOnClickListener(v -> salvarAlteracoes());

        btnExcluir.setOnClickListener(v -> mostrarDialogExclusao());
    }

    private void preencherDados() {
        if (user.getDisplayName() != null) edtNome.setText(user.getDisplayName());
        if (user.getEmail() != null)       edtEmail.setText(user.getEmail());
        // Senha NUNCA é legível; campo serve para definir NOVA senha (deixe vazio por padrão)
        edtSenha.setText("");
    }

    // ======== SALVAR ALTERAÇÕES ========
    private void salvarAlteracoes() {
        final String novoNome  = safe(edtNome.getText());
        final String novoEmail = safe(edtEmail.getText());
        final String novaSenha = safe(edtSenha.getText());

        // Atualiza nome
        if (novoNome.length() > 0 && !novoNome.equals(user.getDisplayName())) {
            UserProfileChangeRequest req =
                    new UserProfileChangeRequest.Builder().setDisplayName(novoNome).build();
            user.updateProfile(req);
        }

        // Encadeia atualizações sensíveis (email/senha), tratando reautenticação quando necessário
        updateEmailThenPassword(novoEmail, novaSenha);
    }

    private void updateEmailThenPassword(final String novoEmail, final String novaSenha) {
        final boolean querMudarEmail = novoEmail.length() > 0 && !novoEmail.equals(user.getEmail());
        final boolean querMudarSenha = novaSenha.length() >= 6; // mínima do Firebase

        if (!querMudarEmail && !querMudarSenha) {
            Toast.makeText(this, "Alterações salvas.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1) Atualizar e-mail (se necessário)
        if (querMudarEmail) {
            user.updateEmail(novoEmail)
                    .addOnSuccessListener(unused -> {
                        // 2) Atualizar senha (se necessário)
                        if (querMudarSenha) {
                            updatePassword(novaSenha);
                        } else {
                            Toast.makeText(this, "Alterações salvas.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (e instanceof FirebaseAuthRecentLoginRequiredException) {
                            pedirReautenticacao(() -> updateEmailThenPassword(novoEmail, novaSenha));
                        } else {
                            Toast.makeText(this, "Falha ao atualizar e-mail: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        } else if (querMudarSenha) {
            updatePassword(novaSenha);
        }
    }

    private void updatePassword(final String novaSenha) {
        user.updatePassword(novaSenha)
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Alterações salvas.", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e -> {
                    if (e instanceof FirebaseAuthRecentLoginRequiredException) {
                        pedirReautenticacao(() -> updatePassword(novaSenha));
                    } else {
                        Toast.makeText(this, "Falha ao atualizar senha: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ======== REAUTENTICAÇÃO (para e-mail/senha/delete) ========
    private void pedirReautenticacao(final Runnable onSuccess) {
        // Dialog pedindo a senha atual
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(12));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));
        bg.setColor(0xFFFFFFFF);
        root.setBackground(bg);

        EditText edt = new EditText(this);
        edt.setHint("Digite sua senha atual");
        edt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        edt.setPadding(dp(12), dp(12), dp(12), dp(12));

        MaterialButton confirmar = new MaterialButton(this);
        confirmar.setText("Confirmar");
        confirmar.setAllCaps(false);
        confirmar.setTypeface(Typeface.DEFAULT_BOLD);
        confirmar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF1E3A8A));
        confirmar.setTextColor(0xFFFFFFFF);

        root.addView(edt, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams lpBtn = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        lpBtn.topMargin = dp(16);
        root.addView(confirmar, lpBtn);

        d.setContentView(root);
        dimDialog(d);

        confirmar.setOnClickListener(v -> {
            String senhaAtual = edt.getText() == null ? "" : edt.getText().toString().trim();
            if (senhaAtual.isEmpty()) {
                Toast.makeText(this, "Informe sua senha atual.", Toast.LENGTH_SHORT).show();
                return;
            }
            String emailAtual = user.getEmail();
            if (emailAtual == null) {
                Toast.makeText(this, "Conta sem e-mail (Google). Refaça login.", Toast.LENGTH_LONG).show();
                d.dismiss();
                return;
            }
            AuthCredential cred = EmailAuthProvider.getCredential(emailAtual, senhaAtual);
            user.reauthenticate(cred)
                    .addOnSuccessListener(unused -> { d.dismiss(); onSuccess.run(); })
                    .addOnFailureListener(err -> {
                        Toast.makeText(this, "Senha incorreta: " + err.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });

        d.show();
    }

    // ======== EXCLUSÃO DE CONTA ========
    private void mostrarDialogExclusao() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Card container
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(20), dp(20), dp(16));
        card.setGravity(Gravity.CENTER_HORIZONTAL);

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setCornerRadius(dp(16));
        cardBg.setColor(0xFFFFFFFF);
        card.setBackground(cardBg);

        // Ícone dentro de círculo rosado
        LinearLayout circle = new LinearLayout(this);
        circle.setGravity(Gravity.CENTER);
        GradientDrawable cBg = new GradientDrawable();
        cBg.setShape(GradientDrawable.OVAL);
        cBg.setColor(0xFFFFE5E5); // rosado claro
        circle.setBackground(cBg);
        int cSize = dp(48);
        LinearLayout.LayoutParams lpCircle = new LinearLayout.LayoutParams(cSize, cSize);

        ImageView ic = new ImageView(this);
        ic.setImageResource(R.drawable.ic_warning_red); // vetor vermelho
        ic.setColorFilter(0xFFEF4444, PorterDuff.Mode.SRC_IN);
        circle.addView(ic, new LinearLayout.LayoutParams(dp(24), dp(24)));

        // Título
        androidx.appcompat.widget.AppCompatTextView titulo = new androidx.appcompat.widget.AppCompatTextView(this);
        titulo.setText("Excluir conta");
        titulo.setTextSize(18);
        titulo.setTypeface(Typeface.DEFAULT_BOLD);
        titulo.setTextColor(0xFF111827);

        // Mensagem
        androidx.appcompat.widget.AppCompatTextView msg = new androidx.appcompat.widget.AppCompatTextView(this);
        msg.setText("Tem certeza de que deseja excluir sua conta?\nEsta ação é permanente e não pode ser desfeita.");
        msg.setTextSize(14);
        msg.setTextColor(0xFF4B5563);
        msg.setGravity(Gravity.CENTER);

        // Botão vermelho
        MaterialButton btnSim = new MaterialButton(this);
        btnSim.setText("Sim, excluir conta");
        btnSim.setAllCaps(false);
        btnSim.setTypeface(Typeface.DEFAULT_BOLD);
        btnSim.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFD32F2F));
        btnSim.setTextColor(0xFFFFFFFF);
        btnSim.setCornerRadius(dp(12));

        // Botão cinza
        MaterialButton btnCancelar = new MaterialButton(this);
        btnCancelar.setText("Cancelar");
        btnCancelar.setAllCaps(false);
        btnCancelar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE5E7EB));
        btnCancelar.setTextColor(0xFF374151);
        btnCancelar.setCornerRadius(dp(12));

        // Montagem
        card.addView(circle, lpCircle);

        LinearLayout.LayoutParams lpTit = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpTit.topMargin = dp(12);
        card.addView(titulo, lpTit);

        LinearLayout.LayoutParams lpMsg = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpMsg.topMargin = dp(8);
        card.addView(msg, lpMsg);

        LinearLayout.LayoutParams lpBtn = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        lpBtn.topMargin = dp(16);
        card.addView(btnSim, lpBtn);

        LinearLayout.LayoutParams lpBtn2 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        lpBtn2.topMargin = dp(8);
        card.addView(btnCancelar, lpBtn2);

        dialog.setContentView(card);
        dimDialog(dialog); // escurece o fundo
        dialog.show();

        btnCancelar.setOnClickListener(v -> dialog.dismiss());
        btnSim.setOnClickListener(v -> {
            dialog.dismiss();
            confirmarExclusaoConta();
        });
    }

    private void confirmarExclusaoConta() {
        // Precisará de reautenticação recente
        Runnable continuar = () -> {
            final String uid = user.getUid();
            // primeiro apaga o documento do usuário (se existir)
            db.collection("users").document(uid).delete()
                    .addOnCompleteListener(task -> {
                        // depois apaga o usuário do Auth
                        user.delete()
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(this, "Conta excluída.", Toast.LENGTH_SHORT).show();
                                    goToLogin();
                                })
                                .addOnFailureListener(e -> {
                                    if (e instanceof FirebaseAuthRecentLoginRequiredException) {
                                        pedirReautenticacao(this::confirmarExclusaoConta);
                                    } else {
                                        Toast.makeText(this, "Falha ao excluir conta: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                    });
        };

        // Tenta direto; se exigir login recente, chamaremos o diálogo
        user.delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Conta excluída.", Toast.LENGTH_SHORT).show();
                    goToLogin();
                })
                .addOnFailureListener(e -> {
                    if (e instanceof FirebaseAuthRecentLoginRequiredException) {
                        pedirReautenticacao(continuar);
                    } else {
                        Toast.makeText(this, "Falha ao excluir conta: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ======== POP-UP "SAIR" ========
    private void mostrarDialogSair() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(20), dp(20), dp(16));
        card.setGravity(Gravity.CENTER_HORIZONTAL);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));
        bg.setColor(0xFFFFFFFF);
        card.setBackground(bg);

        // Título
        android.widget.TextView titulo = new android.widget.TextView(this);
        titulo.setText("Sair");
        titulo.setTextSize(18);
        titulo.setTypeface(Typeface.DEFAULT_BOLD);
        titulo.setTextColor(0xFF111827);

        // Mensagem
        android.widget.TextView msg = new android.widget.TextView(this);
        msg.setText("Tem certeza de que deseja sair?\nSua conta não será excluída, mas você precisará fazer login novamente para acessá-la.");
        msg.setTextSize(14);
        msg.setTextColor(0xFF4B5563);
        msg.setGravity(Gravity.CENTER);

        // Botão azul (confirmar)
        MaterialButton btnConfirmar = new MaterialButton(this);
        btnConfirmar.setText("Sair");
        btnConfirmar.setAllCaps(false);
        btnConfirmar.setTypeface(Typeface.DEFAULT_BOLD);
        btnConfirmar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF1E3A8A));
        btnConfirmar.setTextColor(0xFFFFFFFF);
        btnConfirmar.setCornerRadius(dp(12));

        // Botão cinza (cancelar)
        MaterialButton btnCancelar = new MaterialButton(this);
        btnCancelar.setText("Cancelar");
        btnCancelar.setAllCaps(false);
        btnCancelar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE5E7EB));
        btnCancelar.setTextColor(0xFF374151);
        btnCancelar.setCornerRadius(dp(12));

        // Montagem
        card.addView(titulo, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams lpMsg = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpMsg.topMargin = dp(8);
        card.addView(msg, lpMsg);

        LinearLayout.LayoutParams lpB1 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        lpB1.topMargin = dp(16);
        card.addView(btnConfirmar, lpB1);

        LinearLayout.LayoutParams lpB2 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        lpB2.topMargin = dp(8);
        card.addView(btnCancelar, lpB2);

        dialog.setContentView(card);
        dimDialog(dialog);
        dialog.show();

        btnCancelar.setOnClickListener(v -> dialog.dismiss());
        btnConfirmar.setOnClickListener(v -> {
            dialog.dismiss();

            // Desloga do Firebase
            FirebaseAuth.getInstance().signOut();

            // (Opcional) encerra sessão Google, se existir
            try {
                com.google.android.gms.auth.api.signin.GoogleSignInClient g =
                        com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(
                                this,
                                new com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                                        com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                                ).requestEmail().build()
                        );
                g.signOut();
            } catch (Exception ignored) {}

            goToLogin();
        });
    }

    // ======== UTILS ========
    private void goToLogin() {
        Intent i = new Intent(this, MainActivity.class); // sua tela inicial
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private String safe(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }

    private int dp(int v) {
        float d = getResources().getDisplayMetrics().density;
        return (int) (v * d + 0.5f);
    }

    private void dimDialog(Dialog d) {
        Window w = d.getWindow();
        if (w != null) {
            w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            w.setBackgroundDrawable(new GradientDrawable()); // transparente para ver o dim
            WindowManager.LayoutParams lp = w.getAttributes();
            lp.dimAmount = 0.6f; // deixa o fundo mais escuro
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            w.setAttributes(lp);
        }
    }
}