package com.instituto.bancodealimentos;

import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

public class add_admin extends AppCompatActivity {

    private static final String TAG = "ADD_ADMIN";
    private TextInputEditText edtEmail;
    private MaterialButton btnAdicionar;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowInsetsHelper.setupEdgeToEdge(this);
        setContentView(R.layout.activity_add_admin);

        // Aplicar insets
        WindowInsetsHelper.applyTopInsets(findViewById(R.id.header));

        ImageButton voltar = findViewById(R.id.btn_voltar);
        if (voltar != null) voltar.setOnClickListener(v -> onBackPressed());

        edtEmail = findViewById(R.id.edtEmail);
        btnAdicionar = findViewById(R.id.btnAdicionar);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        btnAdicionar.setOnClickListener(v -> {
            String email = edtEmail.getText() != null ? edtEmail.getText().toString().trim() : "";
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Digite um e-mail válido.", Toast.LENGTH_SHORT).show();
                return;
            }
            abrirBottomSheetConfirmar(email);
        });
    }

    private void abrirBottomSheetConfirmar(String email) {
        BottomSheetDialog bs = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottomsheet_add_admin, null, false);
        bs.setContentView(view);

        MaterialButton btnCancelar = view.findViewById(R.id.btnCancelar);
        MaterialButton btnConfirmar = view.findViewById(R.id.btnAdicionar);

        btnCancelar.setOnClickListener(v -> bs.dismiss());
        btnConfirmar.setOnClickListener(v -> {
            bs.dismiss();
            promoverPorEmail(email);
        });

        bs.show();
    }

    /**
     * Busca o UID pelo e-mail em 'usuarios'
     * CORRIGIDO: Busca com whereEqualTo em 'email' (funciona para Google e email/senha)
     */
    private void promoverPorEmail(String email) {
        travarBotao(true);

        Log.d(TAG, "Buscando usuário com email: " + email);

        // Busca por email (funciona para ambos: login normal e Google)
        db.collection("usuarios")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap == null || snap.isEmpty()) {
                        Log.w(TAG, "Nenhum usuário encontrado com email: " + email);
                        travarBotao(false);
                        Toast.makeText(this, "Nenhum usuário encontrado com este e-mail.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    DocumentSnapshot d = snap.getDocuments().get(0);
                    String uid = d.getId();
                    String nome = d.contains("nome") ? d.getString("nome") : "";
                    String emailDoc = d.contains("email") ? d.getString("email") : "";

                    Log.d(TAG, "Usuário encontrado! UID: " + uid + ", Nome: " + nome + ", Email: " + emailDoc);

                    promoverUsuario(uid, nome != null ? nome : "", emailDoc != null ? emailDoc : email);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao buscar usuário: " + e.getMessage(), e);
                    travarBotao(false);
                    Toast.makeText(this, "Erro ao buscar usuário: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Promove o usuário a admin:
     * - Atualiza usuarios/{uid}.isAdmin = true
     * - Cria admins/{uid} com nome, email e criadoEm
     */
    private void promoverUsuario(String uid, String nome, String email) {
        WriteBatch batch = db.batch();

        // usuarios/{uid} - atualiza isAdmin
        Map<String, Object> uData = new HashMap<>();
        uData.put("isAdmin", true);
        if (nome != null && !nome.isEmpty()) uData.put("nome", nome);
        if (email != null && !email.isEmpty()) uData.put("email", email);
        batch.set(db.collection("usuarios").document(uid), uData, SetOptions.merge());

        // admins/{uid} - cria documento
        Map<String, Object> aData = new HashMap<>();
        aData.put("nome", nome != null ? nome : "");
        aData.put("email", email != null ? email : "");
        aData.put("criadoEm", Timestamp.now());
        batch.set(db.collection("admins").document(uid), aData);

        Log.d(TAG, "Promovendo usuário " + uid + " a administrador...");

        batch.commit()
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Usuário promovido com sucesso!");
                    travarBotao(false);
                    Toast.makeText(this, "Administrador adicionado com sucesso.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Falha ao promover admin: " + e.getMessage(), e);
                    travarBotao(false);
                    Toast.makeText(this, "Falha ao adicionar admin: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void travarBotao(boolean travar) {
        btnAdicionar.setEnabled(!travar);
        btnAdicionar.setAlpha(travar ? 0.6f : 1f);
    }
}