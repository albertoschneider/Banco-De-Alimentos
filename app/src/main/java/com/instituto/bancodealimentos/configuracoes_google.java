package com.instituto.bancodealimentos;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
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
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.*;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

public class configuracoes_google extends AppCompatActivity {

    private ImageButton btn_voltar;
    private ImageView imgFoto;
    private TextView tvNome, tvEmail;
    private MaterialButton btnDesconectarGoogle;

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
                    if (idToken == null) { toast("Falha ao obter token do Google."); return; }
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

        View header = findViewById(R.id.header); // o ConstraintLayout do topo
        ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop() + sb.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
        ViewCompat.requestApplyInsets(header);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        if (user == null) { goToStart(); return; }

        // Para reautenticar com Google quando necessário
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getString(R.string.default_web_client_id))
                .build();
        googleClient = GoogleSignIn.getClient(this, gso);

        btn_voltar = findViewById(R.id.btn_voltar);
        imgFoto = findViewById(R.id.imgFoto);
        tvNome  = findViewById(R.id.tvNome);
        tvEmail = findViewById(R.id.tvEmail);
        btnDesconectarGoogle = findViewById(R.id.btnDesconectarGoogle);

        // Preenche (somente leitura)
        tvNome.setText(user.getDisplayName() != null ? user.getDisplayName() : "");
        tvEmail.setText(user.getEmail() != null ? user.getEmail() : "");
        Uri photo = user.getPhotoUrl();
        Glide.with(this).load(photo).placeholder(R.drawable.ic_person_24).error(R.drawable.ic_person_24).circleCrop().into(imgFoto);

        btn_voltar.setOnClickListener(v -> finish());
        btnDesconectarGoogle.setOnClickListener(v -> mostrarDialogDesconectar());
    }

    /* ===== POP-UP "Desvincular Google" ===== */
    private void mostrarDialogDesconectar() {
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
        msg.setText("Tem certeza de que deseja desvincular sua conta do Google?\nEsta ação afeta o acesso por este provedor.");
        msg.setTextSize(14);
        msg.setTextColor(0xFF4B5563);
        msg.setGravity(Gravity.CENTER);

        MaterialButton btnSim = new MaterialButton(this);
        btnSim.setText("Sim, desvincular");
        btnSim.setAllCaps(false);
        btnSim.setTypeface(Typeface.DEFAULT_BOLD);
        btnSim.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFD32F2F));
        btnSim.setTextColor(0xFFFFFFFF);
        btnSim.setCornerRadius(dp(12));

        MaterialButton btnCancelar = new MaterialButton(this);
        btnCancelar.setText("Cancelar");
        btnCancelar.setAllCaps(false);
        btnCancelar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE5E7EB));
        btnCancelar.setTextColor(0xFF374151);
        btnCancelar.setCornerRadius(dp(12));

        card.addView(circle, lpCircle);
        LinearLayout.LayoutParams lpTit = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpTit.topMargin = dp(12);
        card.addView(titulo, lpTit);
        LinearLayout.LayoutParams lpMsg = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpMsg.topMargin = dp(8);
        card.addView(msg, lpMsg);
        LinearLayout.LayoutParams lpB1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        lpB1.topMargin = dp(16);
        card.addView(btnSim, lpB1);
        LinearLayout.LayoutParams lpB2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        lpB2.topMargin = dp(8);
        card.addView(btnCancelar, lpB2);

        dialog.setContentView(card);
        dimDialog(dialog);
        dialog.show();

        btnCancelar.setOnClickListener(v -> dialog.dismiss());
        btnSim.setOnClickListener(v -> {
            dialog.dismiss();
            unlinkGoogleSomente();
        });
    }

    /* ===== UNLINK GOOGLE — sem criar senha ===== */
    private void unlinkGoogleSomente() {
        user.unlink("google.com")
                .addOnSuccessListener(result -> {
                    toast("Conta do Google desvinculada.");
                    // Mantém o usuário logado com os provedores restantes (se houver).
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

    /* ===== Reautenticação Google quando exigida ===== */
    private void reauthWithGoogle() {
        Intent it = googleClient.getSignInIntent();
        reauthGoogleLauncher.launch(it);
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

    private void dimDialog(Dialog d) {
        Window w = d.getWindow();
        if (w != null) {
            w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            w.setBackgroundDrawable(new GradientDrawable());
            WindowManager.LayoutParams lp = w.getAttributes();
            lp.dimAmount = 0.6f;
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            w.setAttributes(lp);
        }
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
}