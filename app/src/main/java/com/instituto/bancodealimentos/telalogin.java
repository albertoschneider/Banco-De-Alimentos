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

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
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
    private GoogleSignInClient mGoogleSignInClient;

    private static final String URL_SUCESSO_EMAIL_VERIFICADO =
            "https://albertoschneider.github.io/success/email-verificado/";

    private ActivityResultLauncher<Intent> googleLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        if (txtRegister != null) txtRegister.setOnClickListener(v ->
                startActivity(new Intent(this, telaregistro.class)));
        if (txtForgot != null) txtForgot.setOnClickListener(v ->
                startActivity(new Intent(this, EsqueciSenhaActivity.class)));

        btnEntrar.setOnClickListener(v -> tentarLogin());

        // 1) LOGA o default_web_client_id real que está no seu google-services.json
        String cid = getString(R.string.default_web_client_id);
        Log.w(TAG, "default_web_client_id: " + cid);

        // 2) Config do Google Sign-In com o *WEB CLIENT ID* acima
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(cid) // importante: usar o default_web_client_id (web client)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // 3) Activity Result API p/ capturar o erro real
        googleLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Intent data = result.getData();
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);

                        if (account == null || account.getIdToken() == null) {
                            showError("*Falha ao obter idToken. Verifique o default_web_client_id / google-services.json.");
                            Log.e(TAG, "account==null ou idToken==null");
                            setButtonsEnabled(true);
                            return;
                        }

                        mAuth.signInWithCredential(
                                com.google.firebase.auth.GoogleAuthProvider.getCredential(account.getIdToken(), null)
                        ).addOnCompleteListener(this, t -> {
                            if (!t.isSuccessful()) {
                                Exception ex = t.getException();
                                showError("*Falha no login com Google.");
                                Log.e(TAG, "FirebaseAuth.signInWithCredential falhou", ex);
                                setButtonsEnabled(true);
                                return;
                            }
                            FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
                            if (u == null) { showError("*Erro inesperado: usuário nulo."); setButtonsEnabled(true); return; }
                            checarAdminENavegar(u.getUid());
                        });

                    } catch (ApiException e) {
                        // *** AQUI sai o motivo (status code) do "volta pra tela" ***
                        // 12500 (DEVELOPER_ERROR), 12501 (CANCELED), 10 (DEVELOPER_ERROR), etc.
                        int code = e.getStatusCode();
                        String msg = "GoogleSignIn ApiException statusCode=" + code;
                        Log.e(TAG, msg, e);
                        showError("*Google Sign-In falhou (código " + code + "). Veja Logcat para detalhes.");
                        setButtonsEnabled(true);
                    } catch (Exception e) {
                        Log.e(TAG, "Falha inesperada no Google Sign-In", e);
                        showError("*Falha inesperada no Google Sign-In.");
                        setButtonsEnabled(true);
                    }
                }
        );

        if (btnGoogle != null) {
            btnGoogle.setOnClickListener(v -> {
                setButtonsEnabled(false);
                // força escolher conta sempre (evita estado "pegajoso" bugado)
                mGoogleSignInClient.signOut().addOnCompleteListener(xx ->
                        googleLauncher.launch(mGoogleSignInClient.getSignInIntent())
                );
            });
        }
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
        db.collection("admins").document(uid).get()
                .addOnSuccessListener((DocumentSnapshot snap) -> {
                    Intent it = new Intent(this, (snap != null && snap.exists()) ? menu_admin.class : menu.class);
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(it);
                    finish();
                })
                .addOnFailureListener(e -> {
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
