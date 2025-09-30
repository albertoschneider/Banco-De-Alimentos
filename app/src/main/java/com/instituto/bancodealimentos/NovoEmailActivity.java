package com.instituto.bancodealimentos;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Patterns;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;

public class NovoEmailActivity extends AppCompatActivity {

    private TextInputEditText etNovoEmail;
    private MaterialButton btnEnviar;
    private android.widget.TextView tvEmailMsg;

    private FirebaseUser user;

    private SharedPreferences prefs;
    private static final String PREFS = "email_change_prefs";
    private static final String K_LEVEL = "em_level";
    private static final String K_LAST = "em_last";
    private static final String K_END = "em_end";
    private static final long TEN_MIN_MS = 10 * 60 * 1000L;
    private CountDownTimer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_novo_email);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.header), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(v.getPaddingLeft(), sb.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { finish(); return; }

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        etNovoEmail = findViewById(R.id.etNovoEmail);
        btnEnviar = findViewById(R.id.btnEnviar);
        tvEmailMsg = findViewById(R.id.tvEmailMsg);
        ImageButton back = findViewById(R.id.btn_voltar);
        back.setOnClickListener(v -> finish());

        btnEnviar.setOnClickListener(v -> trySend());
        restoreCooldownIfRunning();
    }

    private void trySend() {
        String email = etNovoEmail.getText() == null ? "" : etNovoEmail.getText().toString().trim();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            toast("Digite um e-mail válido");
            return;
        }

        long now = System.currentTimeMillis();
        long last = prefs.getLong(K_LAST, 0L);
        int level = prefs.getInt(K_LEVEL, -1);
        if (now - last >= TEN_MIN_MS) level = -1;

        int newLevel = Math.min(level + 1, 4);
        int cooldown = (newLevel + 1) * 60;

        // Sem ActionCodeSettings: Hosting/App Links resolvem o deep link
        user.verifyBeforeUpdateEmail(email)
                .addOnSuccessListener(unused -> {
                    toast("Enviamos um link para " + email + ". Confirme para concluir.");
                    startCooldown(cooldown);
                    prefs.edit().putInt(K_LEVEL, newLevel)
                            .putLong(K_LAST, now)
                            .putLong(K_END, now + cooldown * 1000L)
                            .apply();
                })
                .addOnFailureListener(e -> {
                    if (e instanceof FirebaseAuthRecentLoginRequiredException) {
                        toast("Sessão expirada. Volte e reautentique.");
                    } else {
                        toast("Falha ao enviar link: " + e.getMessage());
                    }
                });
    }

    private void startCooldown(int secs) {
        setEnabled(false);
        showMsg("Reenviar em " + format(secs));
        if (timer != null) timer.cancel();
        timer = new CountDownTimer(secs * 1000L, 1000L) {
            @Override public void onTick(long ms) { showMsg("Reenviar em " + format((int)(ms/1000L))); }
            @Override public void onFinish() { setEnabled(true); tvEmailMsg.setVisibility(android.view.View.GONE); }
        }.start();
    }

    private void restoreCooldownIfRunning() {
        long end = prefs.getLong(K_END, 0L);
        long now = System.currentTimeMillis();
        if (end > now) startCooldown((int)((end - now)/1000L));
        else setEnabled(true);
    }

    private void setEnabled(boolean enabled) {
        btnEnviar.setEnabled(enabled);
        btnEnviar.setAlpha(enabled ? 1f : 0.6f);
    }

    private void showMsg(String s) {
        tvEmailMsg.setText(s);
        tvEmailMsg.setVisibility(android.view.View.VISIBLE);
    }

    private String format(int secs) {
        int m = secs/60, s = secs%60;
        return String.format("%02d:%02d", m, s);
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
