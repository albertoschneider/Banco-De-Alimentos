package com.instituto.bancodealimentos;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

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
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;

public class NovoEmailActivity extends AppCompatActivity {

    private TextInputEditText edtNovoEmail;
    private MaterialButton btnEnviarLink;
    private TextView tvStatusEnvio, tvDicaSpam;

    private FirebaseAuth auth;
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

        auth = FirebaseAuth.getInstance();
        auth.useAppLanguage(); // PT-BR nos e-mails do Firebase
        user = auth.getCurrentUser();
        if (user == null) { finish(); return; }

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        edtNovoEmail  = findViewById(R.id.edtNovoEmail);
        btnEnviarLink = findViewById(R.id.btnEnviarLink);
        tvStatusEnvio = findViewById(R.id.tvStatusEnvio);
        tvDicaSpam    = findViewById(R.id.tvDicaSpam);

        ImageButton back = findViewById(R.id.btn_voltar);
        back.setOnClickListener(v -> finish());

        btnEnviarLink.setOnClickListener(v -> startFlow());
        restoreCooldownIfRunning();
    }

    private void startFlow() {
        String email = edtNovoEmail.getText() == null ? "" : edtNovoEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showMsg("*Digite um e-mail válido.", true);
            return;
        }
        if (user.getEmail() != null && email.equalsIgnoreCase(user.getEmail())) {
            showMsg("*Informe um e-mail diferente do atual.", true);
            return;
        }

        setEnabled(false);

        // 1) PRÉ-CHECA: se já existe conta com este e-mail → mostra erro e para.
        FirebaseAuth.getInstance().fetchSignInMethodsForEmail(email)
                .addOnSuccessListener(result -> {
                    boolean jaExiste = result.getSignInMethods() != null && !result.getSignInMethods().isEmpty();
                    if (jaExiste) {
                        setEnabled(true);
                        showMsg("*Este e-mail já está em uso. Use outro.", true);
                    } else {
                        // 2) Se não existe, segue para enviar verifyBeforeUpdateEmail
                        sendVerifyBeforeUpdateEmail(email);
                    }
                })
                .addOnFailureListener(e -> {
                    setEnabled(true);
                    showMsg("*Falha ao validar e-mail: " + e.getMessage(), true);
                });
    }

    private void sendVerifyBeforeUpdateEmail(String novoEmail) {
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

        user.verifyBeforeUpdateEmail(novoEmail, acs)
                .addOnSuccessListener(unused -> {
                    showMsg(
                            "Enviamos um link para " + novoEmail + ".\n" +
                                    "Abra o e-mail e toque no link para confirmar a alteração.\n" +
                                    "Se não encontrar, verifique Promoções/Atualizações/Spam e marque como \"Não é spam\".",
                            false
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
                        // 3) Sessão ficou velha: reautentica e tenta de novo
                        pedirReautenticacao(() -> sendVerifyBeforeUpdateEmail(novoEmail));
                    } else if (e instanceof FirebaseAuthException) {
                        String code = ((FirebaseAuthException) e).getErrorCode();
                        switch (code) {
                            case "ERROR_EMAIL_ALREADY_IN_USE":
                                showMsg("*Este e-mail já está em uso. Use outro.", true);
                                break;
                            case "ERROR_INVALID_EMAIL":
                                showMsg("*E-mail inválido.", true);
                                break;
                            default:
                                showMsg("*Falha ao enviar o link (" + code + "). Tente novamente.", true);
                                break;
                        }
                        setEnabled(true);
                    } else {
                        showMsg("*Falha ao enviar o link: " + e.getMessage(), true);
                        setEnabled(true);
                    }
                });
    }

    private void startCooldown(int secs) {
        setEnabled(false);
        showMsg("Enviamos o link. Você poderá reenviar em " + format(secs) + ".", false);
        if (timer != null) timer.cancel();
        timer = new CountDownTimer(secs * 1000L, 1000L) {
            @Override public void onTick(long ms) {
                showMsg("Enviamos o link. Você poderá reenviar em " + format((int)(ms/1000L)) + ".", false);
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

    private void showMsg(String s, boolean isError) {
        tvStatusEnvio.setText(s);
        tvStatusEnvio.setTextColor(isError ? 0xFFEF4444 : 0xFF10B981); // vermelho vivo / verde claro
        tvStatusEnvio.setVisibility(TextView.VISIBLE);
    }

    private String format(int secs) {
        int m = secs / 60, s = secs % 60;
        return String.format("%02d:%02d", m, s);
    }

    // ========= Reautenticação inline =========
    private void pedirReautenticacao(final Runnable onSuccess) {
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));
        bg.setColor(0xFFFFFFFF);

        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(12));
        root.setBackground(bg);
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("Confirmar senha");
        title.setTextSize(18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(0xFF111827);

        final EditText edt = new EditText(this);
        edt.setHint("Digite sua senha atual");
        edt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        edt.setPadding(dp(12), dp(12), dp(12), dp(12));

        MaterialButton confirmar = new MaterialButton(this);
        confirmar.setText("Confirmar");
        confirmar.setAllCaps(false);
        confirmar.setTypeface(Typeface.DEFAULT_BOLD);
        confirmar.setCornerRadius(dp(12));
        confirmar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#004E7C")));
        confirmar.setTextColor(0xFFFFFFFF);

        root.addView(title);
        android.widget.LinearLayout.LayoutParams lpEdt = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        lpEdt.topMargin = dp(10);
        root.addView(edt, lpEdt);

        android.widget.LinearLayout.LayoutParams lpBtn = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        lpBtn.topMargin = dp(16);
        root.addView(confirmar, lpBtn);

        d.setContentView(root);
        sizeAndDimDialog(d, 0.9f);
        d.show();

        confirmar.setOnClickListener(v -> {
            String senhaAtual = edt.getText() == null ? "" : edt.getText().toString().trim();
            if (senhaAtual.isEmpty()) {
                showMsg("*Informe sua senha atual para confirmar.", true);
                return;
            }
            String emailAtual = (user != null) ? user.getEmail() : null;
            if (emailAtual == null) {
                showMsg("*Sessão inválida. Faça login novamente.", true);
                d.dismiss();
                return;
            }
            AuthCredential cred = EmailAuthProvider.getCredential(emailAtual, senhaAtual);
            user.reauthenticate(cred)
                    .addOnSuccessListener(unused -> {
                        d.dismiss();
                        onSuccess.run();
                    })
                    .addOnFailureListener(err -> showMsg("*Senha incorreta: " + err.getMessage(), true));
        });
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
}
