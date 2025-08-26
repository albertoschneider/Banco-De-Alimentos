package com.instituto.bancodealimentos;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

public class configuracoes_google extends AppCompatActivity {

    private ImageButton btn_voltar;
    private ImageView imgFoto;
    private TextView tvNome, tvEmail;
    private MaterialButton btnDesconectarGoogle;
    private MaterialButton btnSairGoogle;

    private FirebaseAuth auth;
    private FirebaseUser user;
    private GoogleSignInClient googleClient;

    private Runnable pendingAfterReauth;

    private final ActivityResultLauncher<Intent> reauthGoogleLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Intent data = result.getData();
                if (data == null) { toast("Reautenticação cancelada."); return; }
                try {
                    GoogleSignInAccount acct = GoogleSignIn.getSignedInAccountFromIntent(data)
                            .getResult(ApiException.class);
                    String idToken = acct.getIdToken();
                    if (idToken == null) { toast("Falha ao obter token do Google. Verifique o SHA-1/SHA-256 e o google-services.json."); return; }
                    AuthCredential cred = GoogleAuthProvider.getCredential(idToken, null);
                    user.reauthenticate(cred)
                            .addOnSuccessListener(unused -> {
                                if (pendingAfterReauth != null) { pendingAfterReauth.run(); pendingAfterReauth = null; }
                            })
                            .addOnFailureListener(e -> toast("Reautenticação falhou: " + e.getMessage()));
                } catch (ApiException e) {
                    toast("Reautenticação cancelada.");
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuracoes_google);

        View header = findViewById(R.id.header);
        if (header != null) {
            ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
                Insets sb = insets.getInsets(WindowInsetsCompat.Type.statusBars());
                v.setPadding(v.getPaddingLeft(), sb.top, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
            ViewCompat.requestApplyInsets(header);
        }

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        if (user == null) { goToStart(); return; }

        // === GoogleSignInClient DINÂMICO (sem depender de R.string.default_web_client_id em compile-time) ===
        String webClientId = getWebClientIdOrNull();
        GoogleSignInOptions.Builder gsoBuilder =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail();

        if (webClientId != null && !webClientId.isEmpty()) {
            gsoBuilder.requestIdToken(webClientId);
        } else {
            toast("Atenção: WEB CLIENT ID não encontrado nos recursos.\n" +
                    "Adicione SHA-1/SHA-256 no Firebase e baixe o google-services.json atualizado.");
        }
        googleClient = GoogleSignIn.getClient(this, gsoBuilder.build());

        btn_voltar = findViewById(R.id.btn_voltar);
        imgFoto = findViewById(R.id.imgFoto);
        tvNome  = findViewById(R.id.tvNome);
        tvEmail = findViewById(R.id.tvEmail);
        btnDesconectarGoogle = findViewById(R.id.btnDesconectarGoogle);
        btnSairGoogle = findViewById(R.id.btnSairGoogle);

        // Preenche (Auth)
        tvNome.setText(user.getDisplayName() != null ? user.getDisplayName() : "");
        tvEmail.setText(user.getEmail() != null ? user.getEmail() : "");
        Uri photo = user.getPhotoUrl();
        Glide.with(this).load(photo)
                .placeholder(R.drawable.ic_person_24)
                .error(R.drawable.ic_person_24)
                .circleCrop()
                .into(imgFoto);

        // Fallback: busca nome no Firestore se displayName veio vazio
        FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    CharSequence atual = tvNome.getText();
                    boolean vazio = (atual == null || atual.toString().trim().isEmpty());
                    if (vazio && doc != null && doc.exists()) {
                        String nomeFS = doc.getString("nome");
                        if (nomeFS != null && !nomeFS.trim().isEmpty()) {
                            tvNome.setText(nomeFS);
                        }
                    }
                });

        btn_voltar.setOnClickListener(v -> finish());
        btnDesconectarGoogle.setOnClickListener(v -> mostrarDialogDesvincular());
        btnSairGoogle.setOnClickListener(v -> mostrarDialogSairGoogle());
    }

    /** Busca o recurso gerado automaticamente pelo plugin (sem referenciar R.string em compile-time). */
    private String getWebClientIdOrNull() {
        int resId = getResources().getIdentifier("default_web_client_id", "string", getPackageName());
        return (resId != 0) ? getString(resId) : null;
    }

    /* ===== POP-UP "Desvincular Google" (com checkbox) ===== */
    private void mostrarDialogDesvincular() {
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

        LinearLayout circle = new LinearLayout(this);
        circle.setGravity(Gravity.CENTER);
        GradientDrawable cBg = new GradientDrawable();
        cBg.setShape(GradientDrawable.OVAL);
        cBg.setColor(0xFFFFE5E5);
        circle.setBackground(cBg);
        int cSize = dp(48);
        LinearLayout.LayoutParams lpCircle = new LinearLayout.LayoutParams(cSize, cSize);

        ImageView ic = new ImageView(this);
        ic.setImageResource(R.drawable.ic_warning_red);
        ic.setColorFilter(0xFFEF4444, PorterDuff.Mode.SRC_IN);
        circle.addView(ic, new LinearLayout.LayoutParams(dp(24), dp(24)));

        TextView titulo = new TextView(this);
        titulo.setText("Desvincular conta do Google");
        titulo.setTextSize(18);
        titulo.setTypeface(Typeface.DEFAULT_BOLD);
        titulo.setTextColor(0xFF111827);

        TextView msg = new TextView(this);
        msg.setText("Tem certeza de que deseja desvincular sua conta do Google?\nVocê será desconectado.");
        msg.setTextSize(14);
        msg.setTextColor(0xFF4B5563);
        msg.setGravity(Gravity.CENTER);

        // Checkbox de confirmação
        CheckBox cb = new CheckBox(this);
        cb.setText("*Entendo que, uma vez desvinculada, a conta não poderá ser recuperada automaticamente.");
        cb.setTextColor(0xFF6B7280);
        cb.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);

        MaterialButton btnSim = new MaterialButton(this);
        btnSim.setText("Sim, desvincular");
        btnSim.setAllCaps(false);
        btnSim.setTypeface(Typeface.DEFAULT_BOLD);
        btnSim.setCornerRadius(dp(12));
        // começa DESABILITADO
        btnSim.setEnabled(false);
        btnSim.setBackgroundTintList(ColorStateList.valueOf(0xFFD1D5DB));
        btnSim.setTextColor(0xFF9CA3AF);

        // quando marcar, habilita (vermelho)
        cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
            btnSim.setEnabled(isChecked);
            btnSim.setBackgroundTintList(ColorStateList.valueOf(isChecked ? 0xFFD32F2F : 0xFFD1D5DB));
            btnSim.setTextColor(isChecked ? 0xFFFFFFFF : 0xFF9CA3AF);
        });

        MaterialButton btnCancelar = new MaterialButton(this);
        btnCancelar.setText("Cancelar");
        btnCancelar.setAllCaps(false);
        btnCancelar.setBackgroundTintList(ColorStateList.valueOf(0xFFE5E7EB));
        btnCancelar.setTextColor(0xFF374151);
        btnCancelar.setCornerRadius(dp(12));

        card.addView(circle, lpCircle);
        LinearLayout.LayoutParams lpTit = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpTit.topMargin = dp(12);
        card.addView(titulo, lpTit);

        LinearLayout.LayoutParams lpMsg = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpMsg.topMargin = dp(8);
        card.addView(msg, lpMsg);

        LinearLayout.LayoutParams lpCb = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpCb.topMargin = dp(10);
        card.addView(cb, lpCb);

        LinearLayout.LayoutParams lpB1 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        lpB1.topMargin = dp(16);
        card.addView(btnSim, lpB1);

        LinearLayout.LayoutParams lpB2 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        lpB2.topMargin = dp(8);
        card.addView(btnCancelar, lpB2);

        dialog.setContentView(card);
        sizeAndDimDialog(dialog, 0.9f);
        dialog.show();

        btnCancelar.setOnClickListener(v -> dialog.dismiss());
        btnSim.setOnClickListener(v -> {
            dialog.dismiss();
            unlinkGoogleSomente();
        });
    }

    /* ===== POP-UP "Sair da conta" (Google) – menor largura ===== */
    private void mostrarDialogSairGoogle() {
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
        msg.setText("Tem certeza de que deseja sair?\nSua conta não será excluída, mas você precisará fazer login novamente.");
        msg.setTextSize(14);
        msg.setTextColor(0xFF4B5563);
        msg.setGravity(Gravity.CENTER);

        MaterialButton btnConfirmar = new MaterialButton(this);
        btnConfirmar.setText("Sair");
        btnConfirmar.setAllCaps(false);
        btnConfirmar.setTypeface(Typeface.DEFAULT_BOLD);
        btnConfirmar.setCornerRadius(dp(12));
        btnConfirmar.setBackgroundTintList(ColorStateList.valueOf(0xFF1E3A8A));
        btnConfirmar.setTextColor(0xFFFFFFFF);

        MaterialButton btnCancelar = new MaterialButton(this);
        btnCancelar.setText("Cancelar");
        btnCancelar.setAllCaps(false);
        btnCancelar.setCornerRadius(dp(12));
        btnCancelar.setBackgroundTintList(ColorStateList.valueOf(0xFFE5E7EB));
        btnCancelar.setTextColor(0xFF374151);

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
            signOutAndGoHome();
        });
    }

    /* ===== UNLINK GOOGLE — também faz sign-out e volta pra Main ===== */
    private void unlinkGoogleSomente() {
        if (user == null) { goToStart(); return; }

        user.unlink("google.com")
                .addOnSuccessListener(result -> {
                    toast("Conta do Google desvinculada.");
                    signOutAndGoHome();
                })
                .addOnFailureListener(e -> {
                    if (e instanceof FirebaseAuthRecentLoginRequiredException) {
                        pendingAfterReauth = this::unlinkGoogleSomente;
                        reauthWithGoogle();
                    } else {
                        toast("Falha ao desvincular: " + e.getMessage());
                    }
                });
    }

    /* ===== Sign-out completo + navegação ===== */
    private void signOutAndGoHome() {
        if (googleClient != null) {
            googleClient.signOut()
                    .addOnCompleteListener(t -> {
                        FirebaseAuth.getInstance().signOut();
                        goToStart();
                    })
                    .addOnFailureListener(err -> {
                        FirebaseAuth.getInstance().signOut();
                        goToStart();
                    });
        } else {
            FirebaseAuth.getInstance().signOut();
            goToStart();
        }
    }

    private void reauthWithGoogle() {
        // Garante que o client usado para reautenticar tenha requestIdToken se o recurso existir
        String webClientId = getWebClientIdOrNull();
        GoogleSignInOptions.Builder builder =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail();
        if (webClientId != null && !webClientId.isEmpty()) {
            builder.requestIdToken(webClientId);
        }
        googleClient = GoogleSignIn.getClient(this, builder.build());
        reauthGoogleLauncher.launch(googleClient.getSignInIntent());
    }

    /* ===== Utilidades ===== */
    private void goToStart() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
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

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
}
