package com.instituto.bancodealimentos;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

/**
 * Activity "ponte" que faz APENAS o fluxo de Google Sign-In.
 */
public class AuthBridgeActivity extends AppCompatActivity {

    public static final String TAG = "AUTH_BRIDGE";
    public static final String EXTRA_STATUS = "status_code";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_UID     = "firebase_uid";

    private GoogleSignInClient gClient;
    private boolean isProcessingResult = false;

    // Activity Result (nova API)
    private final ActivityResultLauncher<Intent> launcher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Log.w(TAG, "========================================");
                Log.w(TAG, "=== RETORNO do Google Sign-In Chooser ===");
                Log.w(TAG, "ResultCode: " + result.getResultCode());

                isProcessingResult = true; // Marca que estamos processando

                Intent data = result.getData();
                if (data == null) {
                    Log.e(TAG, "❌ Intent data é NULL (usuário cancelou ou erro inesperado)");
                    fail(12400, "Usuário cancelou o login");
                    return;
                }

                try {
                    Log.w(TAG, "Tentando obter GoogleSignInAccount do Intent...");
                    GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);

                    if (account == null) {
                        Log.e(TAG, "❌ GoogleSignInAccount é NULL");
                        fail(10, "Conta nula após sign-in");
                        return;
                    }

                    Log.w(TAG, "✓ GoogleSignInAccount obtida:");
                    Log.w(TAG, "  - Email: " + account.getEmail());
                    Log.w(TAG, "  - DisplayName: " + account.getDisplayName());

                    String idToken = account.getIdToken();
                    if (idToken == null) {
                        Log.e(TAG, "❌ idToken é NULL!");
                        Log.e(TAG, "Possíveis causas:");
                        Log.e(TAG, "  1. default_web_client_id incorreto");
                        Log.e(TAG, "  2. SHA-1 não configurado no Firebase");
                        fail(10, "idToken nulo - verifique SHA-1 e client_id");
                        return;
                    }

                    Log.w(TAG, "✓ idToken obtido (primeiros 20 chars): " + idToken.substring(0, Math.min(20, idToken.length())) + "...");
                    Log.w(TAG, "Criando credencial Firebase e autenticando...");

                    AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

                    FirebaseAuth.getInstance()
                            .signInWithCredential(credential)
                            .addOnCompleteListener(this, task -> {
                                if (!task.isSuccessful()) {
                                    Exception ex = task.getException();
                                    String errorMsg = ex != null ? ex.getMessage() : "erro desconhecido";
                                    Log.e(TAG, "❌ Firebase auth falhou: " + errorMsg);
                                    if (ex != null) ex.printStackTrace();
                                    fail(12500, "Firebase auth falhou: " + errorMsg);
                                    return;
                                }

                                String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                                        ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
                                String email = FirebaseAuth.getInstance().getCurrentUser() != null
                                        ? FirebaseAuth.getInstance().getCurrentUser().getEmail() : "";

                                Log.w(TAG, "✓✓✓ SUCESSO TOTAL! Firebase autenticado!");
                                Log.w(TAG, "  - UID: " + uid);
                                Log.w(TAG, "  - Email: " + email);
                                Log.w(TAG, "Retornando RESULT_OK para telalogin...");

                                Intent ok = new Intent();
                                ok.putExtra(EXTRA_STATUS, 0);
                                ok.putExtra(EXTRA_UID, uid);
                                setResult(Activity.RESULT_OK, ok);
                                finish();
                                Log.w(TAG, "finish() chamado.");
                            });
                } catch (ApiException e) {
                    int code = e.getStatusCode();
                    Log.e(TAG, "❌ ApiException capturada!");
                    Log.e(TAG, "Status Code: " + code);
                    Log.e(TAG, "Mensagem: " + e.getMessage());
                    Log.e(TAG, "Códigos comuns:");
                    Log.e(TAG, "  7 = NETWORK_ERROR");
                    Log.e(TAG, "  10 = DEVELOPER_ERROR (SHA-1 ou client_id)");
                    Log.e(TAG, "  12500 = SIGN_IN_FAILED");
                    Log.e(TAG, "  12501 = SIGN_IN_CANCELLED");
                    fail(code, "ApiException: " + e.getMessage());
                } catch (Throwable t) {
                    Log.e(TAG, "❌ Exceção INESPERADA: " + t.getMessage());
                    t.printStackTrace();
                    fail(-1, "Exceção inesperada: " + t.getMessage());
                }
                Log.w(TAG, "========================================");
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.w(TAG, "onCreate()");

        int gms = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        Log.w(TAG, "GMS status = " + gms + " (0=SUCCESS)");
        if (gms != ConnectionResult.SUCCESS) {
            fail(gms, "Google Play Services indisponível");
            return;
        }

        String clientId = getString(R.string.default_web_client_id);
        Log.w(TAG, "clientId=" + clientId);

        gClient = GoogleSignIn.getClient(
                this,
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(clientId)
                        .requestEmail()
                        .build()
        );

        Log.w(TAG, "Forçando signOut() antes do chooser…");
        gClient.signOut().addOnCompleteListener(x -> {
            Log.w(TAG, "signOut() completo. Abrindo intent do Google Sign-In…");
            launcher.launch(gClient.getSignInIntent());
            Log.w(TAG, "launcher.launch() chamado. Aguardando usuário escolher conta...");
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.w(TAG, "onDestroy() - isProcessingResult=" + isProcessingResult);

        // Se foi destruída ANTES de processar resultado, algo está errado
        if (!isProcessingResult) {
            Log.e(TAG, "❌ ALERTA: Activity destruída ANTES de receber resultado do Google!");
            Log.e(TAG, "Isso geralmente indica que outra Activity forçou a destruição da pilha.");
        }
    }

    private void fail(int status, String msg) {
        Log.e(TAG, "========================================");
        Log.e(TAG, "=== FAIL ===");
        Log.e(TAG, "Status: " + status);
        Log.e(TAG, "Mensagem: " + msg);
        Intent i = new Intent();
        i.putExtra(EXTRA_STATUS, status);
        i.putExtra(EXTRA_MESSAGE, msg);
        setResult(Activity.RESULT_CANCELED, i);
        finish();
        Log.e(TAG, "========================================");
    }
}