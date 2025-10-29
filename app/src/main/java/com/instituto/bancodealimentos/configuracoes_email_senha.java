package com.instituto.bancodealimentos;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

public class configuracoes_email_senha extends AppCompatActivity {

    private TextInputEditText edtNome, edtEmail, edtSenha;
    private MaterialButton btnSalvar, btnSair, btnAlterarEmail;
    private ImageButton btnVoltar;
    private TextView tvAlterarSenha, tvSenhaMsg, tvExcluirConta;

    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseFirestore db;

    // Cooldown "Alterar Senha"
    private SharedPreferences prefs;
    private static final String PREFS = "pw_reset_prefs";
    private static final String K_LEVEL = "pw_level";
    private static final String K_LAST_CLICK = "pw_last_click";
    private static final String K_CD_END_AT = "pw_cd_end_at";
    private static final long TEN_MIN_MS = 10 * 60 * 1000L;
    private CountDownTimer pwdTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowInsetsHelper.setupEdgeToEdge(this);
        setContentView(R.layout.activity_configuracoes_email_senha);

        // Aplicar insets
        WindowInsetsHelper.applyTopInsets(findViewById(R.id.header));


        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        db   = FirebaseFirestore.getInstance();
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        if (user == null) { goToLogin(); return; }

        // Views
        btnVoltar       = findViewById(R.id.btn_voltar);
        edtNome         = findViewById(R.id.edtNome);
        edtEmail        = findViewById(R.id.edtEmail);
        edtSenha        = findViewById(R.id.edtSenha);
        btnSalvar       = findViewById(R.id.btnSalvar);
        btnSair         = findViewById(R.id.btnSair);
        btnAlterarEmail = findViewById(R.id.btnAlterarEmail);
        tvAlterarSenha  = findViewById(R.id.tvAlterarSenha);
        tvSenhaMsg      = findViewById(R.id.tvSenhaMsg);
        tvExcluirConta  = findViewById(R.id.tvExcluirConta);

        // Ações
        btnVoltar.setOnClickListener(v -> finish());
        btnSalvar.setOnClickListener(v -> salvarAlteracoes());
        btnSair.setOnClickListener(v -> mostrarDialogSair());
        tvExcluirConta.setOnClickListener(v -> mostrarDialogExclusao());
        btnAlterarEmail.setOnClickListener(v ->
                startActivity(new Intent(this, NovoEmailActivity.class))
        );

        tvAlterarSenha.setOnClickListener(v -> trySendResetLinkWithCooldown());
        restoreCooldownIfRunning();

        preencherDados();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u != null) {
            u.reload().addOnCompleteListener(t -> preencherDados());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pwdTimer != null) pwdTimer.cancel();
    }

    private void preencherDados() {
        // Nome
        String nomeAuth = user.getDisplayName();
        if (nomeAuth != null && !nomeAuth.trim().isEmpty()) {
            edtNome.setText(nomeAuth);
        } else {
            FirebaseFirestore.getInstance()
                    .collection("usuarios")
                    .document(user.getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc != null && doc.exists()) {
                            String nomeFS = doc.getString("nome");
                            if (nomeFS != null && !nomeFS.trim().isEmpty()) {
                                edtNome.setText(nomeFS);
                            }
                        }
                    });
        }

        if (user.getEmail() != null) edtEmail.setText(user.getEmail());
        edtSenha.setText("");
    }

    private void salvarAlteracoes() {
        final String novoNome  = safe(edtNome.getText());
        final String novoEmail = safe(edtEmail.getText());
        final String novaSenha = safe(edtSenha.getText());

        // Atualiza nome no Auth
        if (novoNome.length() > 0 && !novoNome.equals(user.getDisplayName())) {
            UserProfileChangeRequest req =
                    new UserProfileChangeRequest.Builder().setDisplayName(novoNome).build();
            user.updateProfile(req);
        }

        // Atualiza e-mail e/ou senha (com reautenticação se precisar)
        updateEmailThenPassword(novoEmail, novaSenha);
    }

    private void updateEmailThenPassword(final String novoEmail, final String novaSenha) {
        final boolean querMudarEmail = novoEmail.length() > 0 && !novoEmail.equals(user.getEmail());
        final boolean querMudarSenha = novaSenha.length() >= 6;

        if (!querMudarEmail && !querMudarSenha) {
            Toast.makeText(this, "Alterações salvas.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (querMudarEmail) {
            user.updateEmail(novoEmail)
                    .addOnSuccessListener(unused -> {
                        if (querMudarSenha) updatePassword(novaSenha);
                        else Toast.makeText(this, "Alterações salvas.", Toast.LENGTH_SHORT).show();
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

    // ======== Reset-link + cooldown (e-mail) ========
    private void trySendResetLinkWithCooldown() {
        if (user.getEmail() == null) {
            Toast.makeText(this, "Conta sem e-mail. Refaça login.", Toast.LENGTH_LONG).show();
            return;
        }
        long now = System.currentTimeMillis();
        long last = prefs.getLong(K_LAST_CLICK, 0L);
        int level = prefs.getInt(K_LEVEL, -1); // -1 primeira vez
        if (now - last >= TEN_MIN_MS) level = -1;

        int newLevel = Math.min(level + 1, 4);      // 0..4
        int cooldownSecs = (newLevel + 1) * 60;     // 60..300

        // ContinueUrl → volta pro app via DeepLinkSuccessActivity
        ActionCodeSettings settings = ActionCodeSettings.newBuilder()
                .setUrl("https://albertoschneider.github.io/success/senha-redefinida/")
                .setHandleCodeInApp(true)
                .setAndroidPackageName(getPackageName(), true, null)
                .build();

        FirebaseAuth.getInstance()
                .sendPasswordResetEmail(user.getEmail(), settings)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Enviamos um link para " + user.getEmail(), Toast.LENGTH_LONG).show();
                    startPwdCooldown(cooldownSecs);
                    prefs.edit()
                            .putInt(K_LEVEL, newLevel)
                            .putLong(K_LAST_CLICK, now)
                            .putLong(K_CD_END_AT, now + cooldownSecs * 1000L)
                            .apply();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Não foi possível enviar o link: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void startPwdCooldown(int secs) {
        if (tvAlterarSenha == null) return;
        setPwdLinkEnabled(false);
        showPwdMsg("Enviamos um link. Reenviar em " + format(secs));

        if (pwdTimer != null) pwdTimer.cancel();
        pwdTimer = new CountDownTimer(secs * 1000L, 1000L) {
            @Override public void onTick(long ms) {
                int s = (int) Math.max(0, ms / 1000L);
                showPwdMsg("Enviamos um link. Reenviar em " + format(s));
            }
            @Override public void onFinish() {
                setPwdLinkEnabled(true);
                if (tvSenhaMsg != null) tvSenhaMsg.setVisibility(View.GONE);
            }
        }.start();
    }

    private void restoreCooldownIfRunning() {
        long endAt = prefs.getLong(K_CD_END_AT, 0);
        long now = System.currentTimeMillis();
        if (endAt > now) {
            int remaining = (int) ((endAt - now) / 1000L);
            startPwdCooldown(remaining);
        } else {
            setPwdLinkEnabled(true);
            if (tvSenhaMsg != null) tvSenhaMsg.setVisibility(View.GONE);
        }
    }

    private void setPwdLinkEnabled(boolean enabled) {
        tvAlterarSenha.setEnabled(enabled);
        tvAlterarSenha.setTextColor(enabled ? Color.parseColor("#004E7C") : Color.parseColor("#9CA3AF"));
        tvAlterarSenha.setAlpha(enabled ? 1f : 0.7f);
        if (tvSenhaMsg != null && enabled) tvSenhaMsg.setVisibility(View.GONE);
    }

    private void showPwdMsg(String msg) {
        if (tvSenhaMsg == null) return;
        tvSenhaMsg.setText(msg);
        tvSenhaMsg.setVisibility(View.VISIBLE);
    }

    // ======== POPUP: SAIR ========
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

        TextView titulo = new TextView(this);
        titulo.setText("Sair");
        titulo.setTextSize(18);
        titulo.setTypeface(Typeface.DEFAULT_BOLD);
        titulo.setTextColor(0xFF111827);

        TextView msg = new TextView(this);
        msg.setText("Tem certeza de que deseja sair?\nSua conta não será excluída, mas você precisará fazer login novamente para acessá-la.");
        msg.setTextSize(14);
        msg.setTextColor(0xFF4B5563);
        msg.setGravity(Gravity.CENTER);

        MaterialButton btnConfirmar = new MaterialButton(this);
        btnConfirmar.setText("Sair da conta");
        btnConfirmar.setAllCaps(false);
        btnConfirmar.setTypeface(Typeface.DEFAULT_BOLD);
        btnConfirmar.setCornerRadius(dp(12));
        btnConfirmar.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#004E7C")));
        btnConfirmar.setTextColor(0xFFFFFFFF);

        MaterialButton btnCancelar = new MaterialButton(this);
        btnCancelar.setText("Cancelar");
        btnCancelar.setAllCaps(false);
        btnCancelar.setCornerRadius(dp(12));
        btnCancelar.setBackgroundTintList(ColorStateList.valueOf(0xFFE5E7EB));
        btnCancelar.setTextColor(0xFF374151);

        // Montagem
        card.addView(titulo);
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
        sizeAndDimDialog(dialog, 0.9f);
        dialog.show();

        btnCancelar.setOnClickListener(v -> dialog.dismiss());
        btnConfirmar.setOnClickListener(v -> {
            dialog.dismiss();
            FirebaseAuth.getInstance().signOut();
            goToLogin();
        });
    }

    // ======== POPUP: EXCLUIR CONTA ========
    private void mostrarDialogExclusao() {
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

        TextView titulo = new TextView(this);
        titulo.setText("Excluir conta");
        titulo.setTextSize(18);
        titulo.setTypeface(Typeface.DEFAULT_BOLD);
        titulo.setTextColor(0xFF111827);

        TextView msg = new TextView(this);
        msg.setText("Tem certeza de que deseja excluir sua conta?\nEsta ação é permanente e não pode ser desfeita.");
        msg.setTextSize(14);
        msg.setTextColor(0xFF4B5563);
        msg.setGravity(Gravity.CENTER);

        CheckBox cb = new CheckBox(this);
        cb.setText("Entendo que esta ação é permanente e não pode ser desfeita.");
        cb.setTextColor(0xFF6B7280);
        cb.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);

        MaterialButton btnExcluir = new MaterialButton(this);
        btnExcluir.setText("Sim, excluir conta");
        btnExcluir.setAllCaps(false);
        btnExcluir.setTypeface(Typeface.DEFAULT_BOLD);
        btnExcluir.setCornerRadius(dp(12));
        // começa DESABILITADO e CINZA:
        btnExcluir.setEnabled(false);
        btnExcluir.setBackgroundTintList(ColorStateList.valueOf(0xFFD1D5DB));
        btnExcluir.setTextColor(0xFF9CA3AF);

        MaterialButton btnCancelar = new MaterialButton(this);
        btnCancelar.setText("Cancelar");
        btnCancelar.setAllCaps(false);
        btnCancelar.setCornerRadius(dp(12));
        btnCancelar.setBackgroundTintList(ColorStateList.valueOf(0xFFE5E7EB));
        btnCancelar.setTextColor(0xFF374151);

        cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
            btnExcluir.setEnabled(isChecked);
            btnExcluir.setBackgroundTintList(ColorStateList.valueOf(
                    isChecked ? Color.parseColor("#D32F2F") : 0xFFD1D5DB
            ));
            btnExcluir.setTextColor(isChecked ? 0xFFFFFFFF : 0xFF9CA3AF);
        });

        // Montagem
        card.addView(titulo);
        LinearLayout.LayoutParams lpMsg = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpMsg.topMargin = dp(8);
        card.addView(msg, lpMsg);

        LinearLayout.LayoutParams lpCb = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpCb.topMargin = dp(12);
        card.addView(cb, lpCb);

        LinearLayout.LayoutParams lpB1 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        lpB1.topMargin = dp(16);
        card.addView(btnExcluir, lpB1);

        LinearLayout.LayoutParams lpB2 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        lpB2.topMargin = dp(8);
        card.addView(btnCancelar, lpB2);

        dialog.setContentView(card);
        sizeAndDimDialog(dialog, 0.9f);
        dialog.show();

        btnCancelar.setOnClickListener(v -> dialog.dismiss());
        btnExcluir.setOnClickListener(v -> {
            dialog.dismiss();
            confirmarExclusaoConta();
        });
    }

    private void confirmarExclusaoConta() {
        Runnable continuar = () -> {
            final String uid = user.getUid();
            // apaga dados no Firestore (ajuste conforme seu schema)
            db.collection("usuarios").document(uid).delete()
                    .addOnCompleteListener(task -> {
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

    // ======== REAUTENTICAÇÃO ========
    private void pedirReautenticacao(final Runnable onSuccess) {
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));
        bg.setColor(0xFFFFFFFF);
        root.setBackground(bg);

        TextView titulo = new TextView(this);
        titulo.setText("Confirmar senha");
        titulo.setTextSize(18);
        titulo.setTypeface(Typeface.DEFAULT_BOLD);
        titulo.setTextColor(0xFF111827);

        EditText edt = new EditText(this);
        edt.setHint("Digite sua senha atual");
        edt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        edt.setPadding(dp(12), dp(12), dp(12), dp(12));

        MaterialButton confirmar = new MaterialButton(this);
        confirmar.setText("Confirmar");
        confirmar.setAllCaps(false);
        confirmar.setTypeface(Typeface.DEFAULT_BOLD);
        confirmar.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#004E7C")));
        confirmar.setTextColor(0xFFFFFFFF);
        confirmar.setCornerRadius(dp(12));

        root.addView(titulo);
        LinearLayout.LayoutParams lpEdt = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpEdt.topMargin = dp(10);
        root.addView(edt, lpEdt);

        LinearLayout.LayoutParams lpBtn = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        lpBtn.topMargin = dp(16);
        root.addView(confirmar, lpBtn);

        d.setContentView(root);
        sizeAndDimDialog(d, 0.9f);

        confirmar.setOnClickListener(v -> {
            String senhaAtual = edt.getText() == null ? "" : edt.getText().toString().trim();
            if (senhaAtual.isEmpty()) {
                Toast.makeText(this, "Informe sua senha atual.", Toast.LENGTH_SHORT).show();
                return;
            }
            String emailAtual = user.getEmail();
            if (emailAtual == null) {
                Toast.makeText(this, "Conta sem e-mail. Refaça login.", Toast.LENGTH_LONG).show();
                d.dismiss();
                return;
            }
            AuthCredential cred = EmailAuthProvider.getCredential(emailAtual, senhaAtual);
            user.reauthenticate(cred)
                    .addOnSuccessListener(unused -> { d.dismiss(); onSuccess.run(); })
                    .addOnFailureListener(err ->
                            Toast.makeText(this, "Senha incorreta: " + err.getMessage(), Toast.LENGTH_LONG).show()
                    );
        });

        d.show();
    }

    // ======== UTILS ========
    private void goToLogin() {
        Intent i = new Intent(this, MainActivity.class);
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

    private void sizeAndDimDialog(Dialog d, float widthPercent) {
        Window w = d.getWindow();
        if (w != null) {
            int screenW = getResources().getDisplayMetrics().widthPixels;
            int desiredW = (int) (screenW * widthPercent);
            w.setLayout(desiredW, WindowManager.LayoutParams.WRAP_CONTENT);
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = w.getAttributes();
            lp.dimAmount = 0.6f;
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            w.setAttributes(lp);
        }
    }

    private String format(int secs) {
        int m = secs / 60;
        int s = secs % 60;
        return String.format("%02d:%02d", m, s);
    }
}