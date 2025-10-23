package com.instituto.bancodealimentos;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class telalogin extends AppCompatActivity {

    private static final String TAG = "TELA_LOGIN";

    private TextInputEditText edtEmail, edtSenha;
    private TextView tvLoginError, txtForgot, txtRegister;
    private MaterialButton btnEntrar, btnGoogle;
    private ImageButton btnBack;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private static final String URL_SUCESSO_EMAIL_VERIFICADO =
            "https://albertoschneider.github.io/success/email-verificado/";

    // Lança a AuthBridgeActivity e trata RESULT_OK / RESULT_CANCELED
    private final ActivityResultLauncher<Intent> authBridgeLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), res -> {
                Log.w(TAG, "========================================");
                Log.w(TAG, "=== RETORNO DO AuthBridge ===");
                Log.w(TAG, "ResultCode recebido: " + res.getResultCode());
                Log.w(TAG, "RESULT_OK = " + RESULT_OK + " | RESULT_CANCELED = " + RESULT_CANCELED);

                if (res.getResultCode() == RESULT_OK) {
                    Log.w(TAG, "✓✓✓ RESULT_OK - Login bem-sucedido!");
                    FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
                    if (u == null) {
                        Log.e(TAG, "❌ ERRO: usuário Firebase é NULL após RESULT_OK");
                        showError("*Erro inesperado: usuário nulo após Google.");
                        setButtonsEnabled(true);
                        return;
                    }
                    Log.w(TAG, "✓ Usuário Firebase encontrado: uid=" + u.getUid() + ", email=" + u.getEmail());
                    checarAdminENavegar(u.getUid());
                } else if (res.getResultCode() == RESULT_CANCELED) {
                    Log.e(TAG, "❌❌❌ RESULT_CANCELED - Login falhou ou foi cancelado");
                    Intent data = res.getData();
                    if (data != null) {
                        int status = data.getIntExtra(AuthBridgeActivity.EXTRA_STATUS, -999);
                        String msg = data.getStringExtra(AuthBridgeActivity.EXTRA_MESSAGE);
                        Log.e(TAG, "Status Code: " + status);
                        Log.e(TAG, "Mensagem: " + msg);
                        showError("*Google Sign-In falhou (código " + status + "): " + msg);
                    } else {
                        Log.e(TAG, "Intent data é NULL - usuário pode ter cancelado");
                        showError("*Login cancelado.");
                    }
                    setButtonsEnabled(true);
                } else {
                    Log.e(TAG, "❌ ResultCode INESPERADO: " + res.getResultCode());
                    showError("*Erro desconhecido no login.");
                    setButtonsEnabled(true);
                }
                Log.w(TAG, "========================================");
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.w(TAG, "onCreate() da telalogin");
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_telalogin);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnBack      = findViewById(R.id.btnBack);
        edtEmail     = findViewById(R.id.edtEmail);
        edtSenha     = findViewById(R.id.edtSenha);
        tvLoginError = findViewById(R.id.tvLoginError);
        btnEntrar    = findViewById(R.id.btnEntrar);
        btnGoogle    = findViewById(R.id.btnGoogle);
        txtForgot    = findViewById(R.id.txtForgot);
        txtRegister  = findViewById(R.id.txtRegister);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        if (txtRegister != null) txtRegister.setOnClickListener(v -> startActivity(new Intent(this, telaregistro.class)));
        if (txtForgot != null) txtForgot.setOnClickListener(v -> startActivity(new Intent(this, EsqueciSenhaActivity.class)));

        btnEntrar.setOnClickListener(v -> tentarLogin());

        if (btnGoogle != null) {
            btnGoogle.setOnClickListener(v -> {
                Log.w(TAG, ">>> Botão Google clicado! Iniciando AuthBridge...");
                clearMsg();
                setButtonsEnabled(false);
                Intent intent = new Intent(this, AuthBridgeActivity.class);
                Log.w(TAG, ">>> Lançando AuthBridgeActivity via launcher...");
                authBridgeLauncher.launch(intent);
                Log.w(TAG, ">>> authBridgeLauncher.launch() chamado. Aguardando retorno...");
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.w(TAG, "onResume() da telalogin");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.w(TAG, "onPause() da telalogin");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.w(TAG, "onDestroy() da telalogin");
    }

    private void tentarLogin() {
        clearMsg();
        String email = val(edtEmail);
        String senha = val(edtSenha);

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { showError("*E-mail inválido."); return; }
        if (senha.length() < 6) { showError("*Senha muito curta (mínimo 6)."); return; }

        setButtonsEnabled(false);

        mAuth.signInWithEmailAndPassword(email, senha)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        setButtonsEnabled(true);
                        showError("*E-mail ou senha incorretos.");
                        return;
                    }

                    FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
                    if (u == null) {
                        setButtonsEnabled(true);
                        showError("*Erro inesperado: usuário nulo.");
                        return;
                    }

                    if (!u.isEmailVerified()) {
                        ActionCodeSettings settings = ActionCodeSettings.newBuilder()
                                .setUrl(URL_SUCESSO_EMAIL_VERIFICADO)
                                .setHandleCodeInApp(true)
                                .setAndroidPackageName("com.instituto.bancodealimentos", true, null)
                                .build();

                        u.sendEmailVerification(settings)
                                .addOnSuccessListener(x -> showInfo("Reenviamos o link de verificação para " + email + "."))
                                .addOnFailureListener(e -> showError("*Falha ao reenviar verificação: " + e.getMessage()));

                        FirebaseAuth.getInstance().signOut();
                        setButtonsEnabled(true);
                        showError("*Você precisa verificar seu e-mail antes de entrar.");
                        return;
                    }

                    checarAdminENavegar(u.getUid());
                });
    }

    private void checarAdminENavegar(String uid) {
        Log.w(TAG, ">>> checarAdminENavegar() chamado para uid=" + uid);
        db.collection("admins").document(uid).get()
                .addOnSuccessListener((DocumentSnapshot snap) -> {
                    boolean isAdmin = snap != null && snap.exists();
                    Class<?> target = isAdmin ? menu_admin.class : menu.class;
                    Log.w(TAG, "✓ Admin check concluído. É admin? " + isAdmin + " -> Navegando para " + target.getSimpleName());
                    Intent it = new Intent(this, target);
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(it);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Falha ao checar admin, navegando para menu padrão. Erro: " + e.getMessage());
                    Intent it = new Intent(this, menu.class);
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(it);
                    finish();
                });
    }

    private void setButtonsEnabled(boolean enabled) {
        if (btnEntrar != null) { btnEntrar.setEnabled(enabled); btnEntrar.setAlpha(enabled ? 1f : 0.6f); }
        if (btnGoogle != null) { btnGoogle.setEnabled(enabled); btnGoogle.setAlpha(enabled ? 1f : 0.6f); }
    }

    private void showError(String msg) {
        if (tvLoginError != null) {
            tvLoginError.setText(msg);
            tvLoginError.setTextColor(0xFFDC2626);
            tvLoginError.setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        }
    }

    private void showInfo(String msg) {
        if (tvLoginError != null) {
            tvLoginError.setText(msg);
            tvLoginError.setTextColor(0xFF10B981);
            tvLoginError.setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        }
    }

    private void clearMsg() {
        if (tvLoginError != null) tvLoginError.setVisibility(View.GONE);
    }

    private String val(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }
}