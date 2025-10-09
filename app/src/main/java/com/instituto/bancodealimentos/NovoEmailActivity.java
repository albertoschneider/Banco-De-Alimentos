package com.instituto.bancodealimentos;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseAuth;

public class NovoEmailActivity extends AppCompatActivity {

    private TextInputEditText edtNovoEmail;
    private MaterialButton btnEnviarLink;
    private TextView tvStatusEnvio, tvDicaSpam;

    private FirebaseUser user;

    private SharedPreferences prefs;
    private static final String PREFS = "email_change_prefs";
    private static final String K_LEVEL = "em_level";
    private static final String K_LAST  = "em_last";
    private static final String K_END   = "em_end";
    private static final long TEN_MIN_MS = 10 * 60 * 1000L;
    private CountDownTimer timer;

    // ContinueUrl → abre o app via DeepLinkSuccessActivity
    private static final String CONTINUE_URL = "https://albertoschneider.github.io/success/email-alterado/";

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

        edtNovoEmail  = findViewById(R.id.edtNovoEmail);
        btnEnviarLink = findViewById(R.id.btnEnviarLink);
        tvStatusEnvio = findViewById(R.id.tvStatusEnvio);
        tvDicaSpam    = findViewById(R.id.tvDicaSpam);

        ImageButton back = findViewById(R.id.btn_voltar);
        back.setOnClickListener(v -> finish());

        btnEnviarLink.setOnClickListener(v -> trySend());
        restoreCooldownIfRunning();
    }

    private void trySend() {
        String email = edtNovoEmail.getText() == null ? "" : edtNovoEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showMsg("Digite um e-mail válido.");
            return;
        }

        long now = System.currentTimeMillis();
        long last = prefs.getLong(K_LAST, 0L);
        int level = prefs.getInt(K_LEVEL, -1);
        if (now - last >= TEN_MIN_MS) level = -1;

        int newLevel = Math.min(level + 1, 4);
        int cooldown = (newLevel + 1) * 60;

        ActionCodeSettings acs = ActionCodeSettings.newBuilder()
                .setUrl(CONTINUE_URL)
                .setHandleCodeInApp(true)
                .setAndroidPackageName(getPackageName(), true, null)
                .build();

        user.verifyBeforeUpdateEmail(email, acs)
                .addOnSuccessListener(unused -> {
                    showMsg(
                            "Enviamos um link para " + email + ".\n" +
                                    "Abra o e-mail e toque no link para confirmar a alteração.\n" +
                                    "Se não encontrar, verifique abas Promoções/Atualizações e a pasta Spam e marque como \"Não é spam\"."
                    );
                    tvDicaSpam.setVisibility(TextView.VISIBLE);
                    startCooldown(cooldown);
                    prefs.edit()
                            .putInt(K_LEVEL, newLevel)
                            .putLong(K_LAST, now)
                            .putLong(K_END,  now + cooldown * 1000L)
                            .apply();
                })
                .addOnFailureListener(e -> {
                    if (e instanceof FirebaseAuthRecentLoginRequiredException) {
                        showMsg("Sua sessão expirou. Volte e reautentique para continuar.");
                    } else {
                        showMsg("Falha ao enviar o link: " + e.getMessage());
                    }
                });
    }

    private void startCooldown(int secs) {
        setEnabled(false);
        showMsg("Enviamos o link. Você poderá reenviar em " + format(secs) + ".");
        if (timer != null) timer.cancel();
        timer = new CountDownTimer(secs * 1000L, 1000L) {
            @Override public void onTick(long ms) {
                showMsg("Enviamos o link. Você poderá reenviar em " + format((int)(ms/1000L)) + ".");
            }
            @Override public void onFinish() {
                setEnabled(true);
                tvStatusEnvio.setVisibility(TextView.GONE);
                tvDicaSpam.setVisibility(TextView.GONE);
            }
        }.start();
    }

    private void restoreCooldownIfRunning() {
        long end = prefs.getLong(K_END, 0L);
        long now = System.currentTimeMillis();
        if (end > now) {
            startCooldown((int)((end - now)/1000L));
        } else {
            setEnabled(true);
            tvStatusEnvio.setVisibility(TextView.GONE);
            tvDicaSpam.setVisibility(TextView.GONE);
        }
    }

    private void setEnabled(boolean enabled) {
        btnEnviarLink.setEnabled(enabled);
        btnEnviarLink.setAlpha(enabled ? 1f : 0.6f);
    }

    private void showMsg(String s) {
        tvStatusEnvio.setText(s);
        tvStatusEnvio.setVisibility(TextView.VISIBLE);
    }

    private String format(int secs) {
        int m = secs / 60, s = secs % 60;
        return String.format("%02d:%02d", m, s);
    }
}
