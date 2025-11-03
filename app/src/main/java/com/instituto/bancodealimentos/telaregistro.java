package com.instituto.bancodealimentos;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.activity.EdgeToEdge;
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
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class telaregistro extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextInputEditText edtNome, edtEmail, edtSenha, edtConfirmarSenha;
    private TextView tvAuthErrorRegister;

    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    private Button btnRegistrar, btnGoogle, btnBackToLogin;

    // URL para deep link de sucesso (abre o app após verificar)
    private static final String URL_SUCESSO_EMAIL_VERIFICADO = "https://albertoschneider.github.io/success/email-verificado/";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowInsetsHelper.setupEdgeToEdge(this);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_telaregistro);


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        edtNome = findViewById(R.id.et_username);
        edtEmail = findViewById(R.id.et_email);
        edtSenha = findViewById(R.id.et_password);
        edtConfirmarSenha = findViewById(R.id.et_confirm_password);
        tvAuthErrorRegister = findViewById(R.id.tvAuthErrorRegister);

        ImageButton btnBack = findViewById(R.id.btn_back);
        TextView tvLoginHere = findViewById(R.id.tv_login_here);
        btnRegistrar = findViewById(R.id.btn_login);
        btnGoogle = findViewById(R.id.btn_google);

        btnBack.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
        tvLoginHere.setOnClickListener(v -> startActivity(new Intent(this, telalogin.class)));

        btnRegistrar.setOnClickListener(v -> {
            hideMsg();
            registrarUsuarioEmailSenha();
        });

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        btnGoogle.setOnClickListener(v -> startActivityForResult(mGoogleSignInClient.getSignInIntent(), RC_SIGN_IN));
    }

    private void registrarUsuarioEmailSenha() {
        String nome = val(edtNome);
        String email = val(edtEmail);
        String senha = val(edtSenha);
        String confirmar = val(edtConfirmarSenha);

        if (nome.isEmpty() || email.isEmpty() || senha.isEmpty() || confirmar.isEmpty()) { showError("*Preencha todos os campos."); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { showError("*E-mail inválido."); return; }
        if (senha.length() < 6) { showError("*Senha muito curta (mínimo 6)."); return; }
        if (!senha.equals(confirmar)) { showError("*As senhas não coincidem."); return; }

        setButtonsEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, senha).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                setButtonsEnabled(true);
                Throwable ex = task.getException();
                if (ex instanceof FirebaseAuthUserCollisionException) { showError("*Este e-mail já está em uso. Tente fazer login."); return; }
                if (ex instanceof FirebaseAuthException) {
                    String code = ((FirebaseAuthException) ex).getErrorCode();
                    switch (code) {
                        case "ERROR_EMAIL_ALREADY_IN_USE": showError("*Este e-mail já está em uso. Tente fazer login."); return;
                        case "ERROR_INVALID_EMAIL": showError("*E-mail inválido."); return;
                        case "ERROR_WEAK_PASSWORD": showError("*Senha muito curta (mínimo 6)."); return;
                        default: showError("*Erro no cadastro. Tente novamente."); return;
                    }
                }
                showError("*Erro no cadastro. Tente novamente.");
                return;
            }

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                setButtonsEnabled(true);
                showError("*Erro inesperado: usuário nulo.");
                return;
            }

            user.updateProfile(new UserProfileChangeRequest.Builder()
                    .setDisplayName(nome).build());

            String uid = user.getUid();
            Map<String,Object> usuario = new HashMap<>();
            usuario.put("nome", nome);
            usuario.put("email", email);
            usuario.put("createdAt", com.google.firebase.Timestamp.now());

            db.collection("usuarios").document(uid).set(usuario, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(a -> enviarVerificacao(user, email))
                    .addOnFailureListener(e -> {
                        // Mesmo que o Firestore falhe, ainda enviamos verificação
                        enviarVerificacao(user, email);
                    });
        });
    }

    private void enviarVerificacao(FirebaseUser user, String email) {
        ActionCodeSettings settings = ActionCodeSettings.newBuilder()
                .setUrl(URL_SUCESSO_EMAIL_VERIFICADO)
                .setHandleCodeInApp(true)
                .setAndroidPackageName("com.instituto.bancodealimentos", true, null)
                .build();

        user.sendEmailVerification(settings)
                .addOnSuccessListener(v -> {
                    // NÃO redireciona; mostra mensagem clara e mantém na tela
                    showInfo("Enviamos um link de verificação para " + email + ". Verifique seu e-mail para concluir o cadastro.");
                    setButtonsEnabled(true);
                    // Desloga preventivamente pra evitar uso sem verificação
                    FirebaseAuth.getInstance().signOut();
                })
                .addOnFailureListener(e -> {
                    showError("*Falha ao enviar verificação: " + e.getMessage());
                    setButtonsEnabled(true);
                    FirebaseAuth.getInstance().signOut();
                });
    }

    // ===== Google Sign-In =====
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) firebaseAuthWithGoogle(account.getIdToken());
                else toast("Conta Google inválida.");
            } catch (ApiException e) { toast("Falha no login com Google: " + e.getMessage()); }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        mAuth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        showError("*Falha na autenticação com Google.");
                        return;
                    }
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user == null) { showError("*Erro inesperado: usuário nulo."); return; }

                    String uid = user.getUid();
                    String nome = user.getDisplayName() != null ? user.getDisplayName() : "";
                    String email = user.getEmail() != null ? user.getEmail() : "";

                    Map<String,Object> usuario = new HashMap<>();
                    usuario.put("nome", nome);
                    usuario.put("email", email);
                    usuario.put("createdAt", com.google.firebase.Timestamp.now());

                    db.collection("usuarios").document(uid).set(usuario, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener(a -> checarAdminENavegar(uid))
                            .addOnFailureListener(e -> { toast("Erro ao salvar no banco: " + e.getMessage()); checarAdminENavegar(uid); });
                });
    }

    private void checarAdminENavegar(String uid) {
        db.collection("admins").document(uid).get()
                .addOnSuccessListener((DocumentSnapshot snap) -> goToNextScreen(snap != null && snap.exists()))
                .addOnFailureListener(e -> goToNextScreen(false));
    }

    private void goToNextScreen(boolean isAdmin) {
        Intent it = new Intent(this, isAdmin ? menu_admin.class : menu.class);
        startActivity(it);
        finish();
    }

    // ===== helpers de UI =====
    private void setButtonsEnabled(boolean enabled) {
        if (btnRegistrar != null) { btnRegistrar.setEnabled(enabled); btnRegistrar.setAlpha(enabled ? 1f : 0.6f); }
        if (btnGoogle != null)    { btnGoogle.setEnabled(enabled);   btnGoogle.setAlpha(enabled ? 1f : 0.6f); }
    }

    private void hideMsg() {
        if (tvAuthErrorRegister != null) tvAuthErrorRegister.setVisibility(View.GONE);
    }

    private void showError(String msg) {
        if (tvAuthErrorRegister != null) {
            tvAuthErrorRegister.setText(msg);
            tvAuthErrorRegister.setTextColor(0xFFDC2626); // vermelho
            tvAuthErrorRegister.setVisibility(View.VISIBLE);
        } else {
            toast(msg);
        }
    }

    private void showInfo(String msg) {
        if (tvAuthErrorRegister != null) {
            tvAuthErrorRegister.setText(msg);
            tvAuthErrorRegister.setTextColor(0xFF10B981); // verde CLARO (mais legível)
            tvAuthErrorRegister.setVisibility(View.VISIBLE);
        } else {
            toast(msg);
        }
    }

    private String val(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}