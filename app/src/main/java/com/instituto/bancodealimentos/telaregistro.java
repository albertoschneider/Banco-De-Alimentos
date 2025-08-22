package com.instituto.bancodealimentos;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.firebase.auth.AuthCredential;
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
    private EditText edtNome, edtEmail, edtSenha, edtConfirmarSenha;
    private TextView tvAuthErrorRegister;

    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        Button btnRegistrar = findViewById(R.id.btn_login);
        Button btnGoogle = findViewById(R.id.btn_google);

        btnBack.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
        tvLoginHere.setOnClickListener(v -> startActivity(new Intent(this, telalogin.class)));

        btnRegistrar.setOnClickListener(v -> {
            // Esconde o aviso antes de tentar
            if (tvAuthErrorRegister != null) tvAuthErrorRegister.setVisibility(View.GONE);
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
        String nome = edtNome.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String senha = edtSenha.getText().toString().trim();
        String confirmar = edtConfirmarSenha.getText().toString().trim();

        if (nome.isEmpty() || email.isEmpty() || senha.isEmpty() || confirmar.isEmpty()) { toast("Preencha todos os campos!"); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { toast("E-mail inválido."); return; }
        if (!senha.equals(confirmar)) { toast("As senhas não coincidem!"); return; }

        mAuth.createUserWithEmailAndPassword(email, senha).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                // E-mail já em uso? → mostra o aviso em vermelho abaixo do confirmar senha
                Throwable ex = task.getException();
                if (ex instanceof FirebaseAuthUserCollisionException) {
                    showEmailEmUso();
                    return;
                }
                if (ex instanceof FirebaseAuthException) {
                    String code = ((FirebaseAuthException) ex).getErrorCode();
                    if ("ERROR_EMAIL_ALREADY_IN_USE".equalsIgnoreCase(code)) {
                        showEmailEmUso();
                        return;
                    }
                }
                // Outros erros → mantém via Toast
                toast("Erro no cadastro: " + (ex != null ? ex.getMessage() : ""));
                return;
            }

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) { toast("Erro inesperado: usuário nulo."); return; }

            user.updateProfile(new UserProfileChangeRequest.Builder()
                    .setDisplayName(nome)
                    .build()
            );

            String uid = user.getUid();
            Map<String,Object> usuario = new HashMap<>();
            usuario.put("nome", nome);
            usuario.put("email", email);
            usuario.put("createdAt", com.google.firebase.Timestamp.now());

            db.collection("usuarios").document(uid).set(usuario, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(a -> checarAdminENavegar(uid))
                    .addOnFailureListener(e -> { toast("Erro ao salvar no banco: " + e.getMessage()); checarAdminENavegar(uid); });
        });
    }

    private void showEmailEmUso() {
        if (tvAuthErrorRegister != null) {
            tvAuthErrorRegister.setText("*Este E-mail ja está sendo usado. Tente Fazer Login");
            tvAuthErrorRegister.setVisibility(View.VISIBLE);
        }
    }

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
                        toast("Falha na autenticação Firebase: " + (task.getException()!=null?task.getException().getMessage():""));
                        return;
                    }
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user == null) { toast("Erro inesperado: usuário nulo."); return; }

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

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}