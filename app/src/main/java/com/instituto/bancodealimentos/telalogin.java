package com.instituto.bancodealimentos;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class telalogin extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText edtEmail, edtSenha;
    private TextView tvLoginError;

    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_telalogin);

        // Ajusta padding para status/navigation bars (edge-to-edge)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Views
        edtEmail     = findViewById(R.id.edtEmail);
        edtSenha     = findViewById(R.id.edtSenha);
        tvLoginError = findViewById(R.id.tvLoginError);

        ImageButton btnBack = findViewById(R.id.btnBack);
        Button btnEntrar    = findViewById(R.id.btnEntrar);
        TextView tvForgot   = findViewById(R.id.txtForgot);
        TextView tvRegister = findViewById(R.id.txtRegister);
        Button btnGoogle    = findViewById(R.id.btnGoogle);

        // Navegação básica
        btnBack.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
        tvRegister.setOnClickListener(v -> startActivity(new Intent(this, telaregistro.class)));
        tvForgot.setOnClickListener(v -> startActivity(new Intent(this, EsqueciSenhaActivity.class)));

        // Limpa erro inline ao digitar
        TextWatcher clearErrorWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { hideInlineError(); }
            @Override public void afterTextChanged(Editable s) { }
        };
        edtEmail.addTextChangedListener(clearErrorWatcher);
        edtSenha.addTextChangedListener(clearErrorWatcher);

        // Login email/senha
        btnEntrar.setOnClickListener(v -> loginEmailSenha());

        // Google Sign-In (usa o Web Client ID em strings.xml -> default_web_client_id)
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        btnGoogle.setOnClickListener(v ->
                startActivityForResult(mGoogleSignInClient.getSignInIntent(), RC_SIGN_IN)
        );
    }

    private void loginEmailSenha() {
        hideInlineError();

        String email = edtEmail.getText() == null ? "" : edtEmail.getText().toString().trim();
        String senha = edtSenha.getText() == null ? "" : edtSenha.getText().toString().trim();

        if (email.isEmpty() || senha.isEmpty()) {
            toast("Preencha todos os campos!");
            return;
        }

        mAuth.signInWithEmailAndPassword(email, senha).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                showInlineError("*E-mail ou senha incorretos");
                return;
            }
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                toast("Erro inesperado: usuário nulo.");
                return;
            }
            ensureUserDocExistsThenNavigate(user);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (data == null) { toast("Falha no login com Google."); return; }
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    String idToken = account.getIdToken();
                    if (idToken == null || idToken.isEmpty()) {
                        toast("Token do Google ausente.");
                        return;
                    }
                    firebaseAuthWithGoogle(idToken);
                } else {
                    toast("Conta Google inválida.");
                }
            } catch (ApiException e) {
                // Mostra o status code (ex.: 10 = DEVELOPER_ERROR)
                toast("Falha no login com Google: " + e.getStatusCode());
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        hideInlineError();
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (!task.isSuccessful()) {
                String msg = task.getException() != null ? task.getException().getMessage() : "";
                toast("Falha na autenticação Firebase: " + msg);
                return;
            }
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) { toast("Erro inesperado: usuário nulo."); return; }
            ensureUserDocExistsThenNavigate(user);
        });
    }

    private void ensureUserDocExistsThenNavigate(FirebaseUser user) {
        String uid = user.getUid();

        Map<String, Object> usuario = new HashMap<>();
        usuario.put("nome",  user.getDisplayName() != null ? user.getDisplayName() : "");
        usuario.put("email", user.getEmail() != null ? user.getEmail() : "");
        usuario.put("lastLoginAt", Timestamp.now());

        db.collection("usuarios")
                .document(uid)
                .set(usuario, SetOptions.merge())
                .addOnSuccessListener(a -> checkAdminAndGo(uid))
                .addOnFailureListener(e -> checkAdminAndGo(uid)); // mesmo com falha, segue
    }

    private void checkAdminAndGo(String uid) {
        db.collection("admins").document(uid).get()
                .addOnSuccessListener((DocumentSnapshot snap) -> goToNextScreen(snap != null && snap.exists()))
                .addOnFailureListener(e -> goToNextScreen(false));
    }

    private void goToNextScreen(boolean isAdmin) {
        Intent it = new Intent(this, isAdmin ? menu_admin.class : menu.class);
        it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(it);
        finish();
    }

    private void showInlineError(String msg) {
        if (tvLoginError != null) {
            tvLoginError.setText(msg);
            tvLoginError.setVisibility(android.view.View.VISIBLE);
        }
    }

    private void hideInlineError() {
        if (tvLoginError != null && tvLoginError.getVisibility() == android.view.View.VISIBLE) {
            tvLoginError.setVisibility(android.view.View.GONE);
        }
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
