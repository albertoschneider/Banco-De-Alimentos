package com.instituto.bancodealimentos;

import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

public class add_admin extends AppCompatActivity {

    private TextInputEditText edtEmail;
    private MaterialButton btnAdicionar;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowInsetsHelper.setupEdgeToEdge(this);
        setContentView(R.layout.activity_add_admin);

        // Ajuste de status bar no header (igual ao gerenciar_admins)
        View header = findViewById(R.id.header);
        if (header != null) {
            ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
                Insets sb = insets.getInsets(WindowInsetsCompat.Type.statusBars());
                v.setPadding(v.getPaddingLeft(), sb.top, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
            ViewCompat.requestApplyInsets(header);
        }

        ImageButton voltar = findViewById(R.id.btn_voltar);
        if (voltar != null) voltar.setOnClickListener(v -> onBackPressed());

        edtEmail = findViewById(R.id.edtEmail);
        btnAdicionar = findViewById(R.id.btnAdicionar);

        db = FirebaseFirestore.getInstance();

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
        BottomSheetDialog bs = new BottomSheetDialog(this, com.google.android.material.R.style.ThemeOverlay_Material3_BottomSheetDialog);
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
     * Busca o UID pelo e-mail em 'usuarios' e promove:
     *  - set usuarios/{uid}.isAdmin = true (merge)
     *  - set admins/{uid} com nome/email/criadoEm
     */
    private void promoverPorEmail(String email) {
        travarBotao(true);

        db.collection("usuarios")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(this::onUsuarioPorEmail)
                .addOnFailureListener(e -> {
                    travarBotao(false);
                    Toast.makeText(this, "Erro ao buscar usuário: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void onUsuarioPorEmail(QuerySnapshot snap) {
        if (snap == null || snap.isEmpty()) {
            travarBotao(false);
            Toast.makeText(this, "Nenhum usuário encontrado com este e-mail.", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentSnapshot d = snap.getDocuments().get(0);
        String uid = d.getId();
        String nome = d.contains("nome") ? d.getString("nome") : "";
        String email = d.contains("email") ? d.getString("email") : "";

        WriteBatch batch = db.batch();

        // usuarios/{uid}
        Map<String, Object> uData = new HashMap<>();
        uData.put("isAdmin", true);
        if (nome != null && !nome.isEmpty()) uData.put("nome", nome);
        if (email != null && !email.isEmpty()) uData.put("email", email);
        batch.set(db.collection("usuarios").document(uid), uData, SetOptions.merge());

        // admins/{uid}
        Map<String, Object> aData = new HashMap<>();
        aData.put("nome", nome != null ? nome : "");
        aData.put("email", email != null ? email : "");
        aData.put("criadoEm", Timestamp.now());
        batch.set(db.collection("admins").document(uid), aData);

        batch.commit()
                .addOnSuccessListener(unused -> {
                    travarBotao(false);
                    Toast.makeText(this, "Administrador adicionado.", Toast.LENGTH_SHORT).show();
                    finish(); // volta para a lista; ela atualiza sozinha
                })
                .addOnFailureListener(e -> {
                    travarBotao(false);
                    Toast.makeText(this, "Falha ao adicionar admin: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void travarBotao(boolean travar) {
        btnAdicionar.setEnabled(!travar);
        btnAdicionar.setAlpha(travar ? 0.6f : 1f);
    }
}
