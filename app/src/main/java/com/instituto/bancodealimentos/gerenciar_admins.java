package com.instituto.bancodealimentos;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class gerenciar_admins extends AppCompatActivity {

    private RecyclerView recycler;
    private AdminAdapter adapter;
    private FirebaseFirestore db;
    private ListenerRegistration listener;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowInsetsHelper.setupEdgeToEdge(this);
        setContentView(R.layout.activity_gerenciar_admins);

        // Aplicar insets
        WindowInsetsHelper.applyTopInsets(findViewById(R.id.header));
        WindowInsetsHelper.applyScrollInsets(findViewById(R.id.recyclerAdmins));

        ImageButton voltar = findViewById(R.id.btn_voltar);
        if (voltar != null) voltar.setOnClickListener(v -> onBackPressed());

        recycler = findViewById(R.id.recyclerAdmins);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        // Pega o ID do usuário atual
        FirebaseAuth auth = FirebaseAuth.getInstance();
        currentUserId = auth.getUid();

        adapter = new AdminAdapter(this::abrirBottomSheetRemover, currentUserId);
        recycler.setAdapter(adapter);

        View btnAdd = findViewById(R.id.btnAddAdmin);
        if (btnAdd != null) {
            btnAdd.setOnClickListener(v -> startActivity(new Intent(this, add_admin.class)));
            btnAdd.setOnLongClickListener(v -> {
                abrirDialogPromover(); // atalho DEV
                return true;
            });
        }

        db = FirebaseFirestore.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        listener = db.collection("admins")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Toast.makeText(gerenciar_admins.this, "Falha ao carregar administradores", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        final List<AdminUser> base = new ArrayList<>();
                        final List<String> faltantes = new ArrayList<>();

                        if (value != null) {
                            for (DocumentSnapshot d : value.getDocuments()) {
                                String uid = d.getId();
                                String nome = d.contains("nome") ? d.getString("nome") : "";
                                String email = d.contains("email") ? d.getString("email") : "";

                                if (nome == null) nome = "";
                                if (email == null) email = "";

                                if (nome.isEmpty() || email.isEmpty()) {
                                    faltantes.add(uid);
                                }
                                base.add(new AdminUser(uid, nome, email, true));
                            }
                        }

                        // Ordena: usuário atual primeiro, depois os outros
                        ordenarAdmins(base);

                        // Mostra imediatamente o que já temos
                        adapter.setItems(base);

                        // Completa nome/email a partir de USUARIOS para quem faltou
                        if (!faltantes.isEmpty()) {
                            completarDadosUsuarios(base, faltantes);
                        }
                    }
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (listener != null) {
            listener.remove();
            listener = null;
        }
    }

    private void ordenarAdmins(List<AdminUser> lista) {
        Collections.sort(lista, new Comparator<AdminUser>() {
            @Override
            public int compare(AdminUser a, AdminUser b) {
                // Usuário atual sempre vem primeiro
                if (a.getId().equals(currentUserId)) return -1;
                if (b.getId().equals(currentUserId)) return 1;

                // Outros ordenados por nome
                String nomeA = a.getNome() != null ? a.getNome() : "";
                String nomeB = b.getNome() != null ? b.getNome() : "";
                return nomeA.compareToIgnoreCase(nomeB);
            }
        });
    }

    // Busca usuarios/{uid} em lotes de 10 (limite do whereIn) e preenche nome/email na lista
    private void completarDadosUsuarios(List<AdminUser> base, List<String> uidsFaltantes) {
        List<List<String>> chunks = chunk(uidsFaltantes, 10);
        final Map<String, Integer> indexPorUid = new HashMap<>();
        for (int i = 0; i < base.size(); i++) {
            indexPorUid.put(base.get(i).getId(), i);
        }

        for (List<String> parte : chunks) {
            db.collection("usuarios")
                    .whereIn(FieldPath.documentId(), parte)
                    .get()
                    .addOnSuccessListener(snap -> {
                        if (snap != null) {
                            for (DocumentSnapshot d : snap.getDocuments()) {
                                String uid = d.getId();
                                String nome = d.contains("nome") ? d.getString("nome") : "";
                                String email = d.contains("email") ? d.getString("email") : "";
                                if (nome == null) nome = "";
                                if (email == null) email = "";

                                Integer idx = indexPorUid.get(uid);
                                if (idx != null) {
                                    AdminUser u = base.get(idx);
                                    // Só sobrescreve se estiver vazio
                                    if (u.getNome() == null || u.getNome().isEmpty()) {
                                        u.setNome(nome);
                                    }
                                    if (u.getEmail() == null || u.getEmail().isEmpty()) {
                                        u.setEmail(email);
                                    }
                                }
                            }
                            // Reordena e atualiza
                            ordenarAdmins(base);
                            adapter.setItems(new ArrayList<>(base));
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Falha ao completar dados: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private static <T> List<List<T>> chunk(List<T> list, int size) {
        List<List<T>> res = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            res.add(list.subList(i, Math.min(list.size(), i + size)));
        }
        return res;
    }

    private void abrirBottomSheetRemover(AdminUser user) {
        BottomSheetDialog bs = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottomsheet_remover_admin, null, false);
        bs.setContentView(view);

        MaterialButton btnCancelar = view.findViewById(R.id.btnCancelar);
        MaterialButton btnRemover = view.findViewById(R.id.btnRemover);

        btnCancelar.setOnClickListener(v -> bs.dismiss());

        btnRemover.setOnClickListener(v -> {
            String myUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
            if (user.getId().equals(myUid)) {
                Toast.makeText(this, "Você não pode remover a si mesmo.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Remove de fato: deleta admins/{uid} + desliga flag em usuarios/{uid}
            com.google.firebase.firestore.WriteBatch batch = db.batch();
            com.google.firebase.firestore.DocumentReference uRef =
                    db.collection("usuarios").document(user.getId());
            com.google.firebase.firestore.DocumentReference aRef =
                    db.collection("admins").document(user.getId());

            batch.update(uRef, "isAdmin", false);
            batch.delete(aRef);

            batch.commit()
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Permissão de administrador removida.", Toast.LENGTH_SHORT).show();
                        bs.dismiss();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Erro ao remover permissão: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        bs.show();
    }

    // === PROMOVER POR UID (cria/atualiza admins/{uid} e garante usuarios/{uid}.isAdmin=true) ===
    private void promoverAdminPorUid(String uid, String nome, String email) {
        com.google.firebase.firestore.WriteBatch batch = db.batch();

        com.google.firebase.firestore.DocumentReference uRef =
                db.collection("usuarios").document(uid);
        com.google.firebase.firestore.DocumentReference aRef =
                db.collection("admins").document(uid);

        java.util.Map<String, Object> uData = new java.util.HashMap<>();
        uData.put("nome", nome);
        uData.put("email", email);
        uData.put("isAdmin", true);
        batch.set(uRef, uData, com.google.firebase.firestore.SetOptions.merge());

        java.util.Map<String, Object> aData = new java.util.HashMap<>();
        aData.put("nome", nome);
        aData.put("email", email);
        aData.put("criadoEm", com.google.firebase.Timestamp.now());
        batch.set(aRef, aData);

        batch.commit()
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Administrador promovido.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Falha ao promover: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // === Diálogo simples (DEV) para promover digitando UID/Nome/Email ===
    private void abrirDialogPromover() {
        android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(this);
        b.setTitle("Promover administrador (DEV)");

        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        android.widget.EditText edUid = new android.widget.EditText(this);
        edUid.setHint("UID");
        edUid.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        root.addView(edUid, new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));

        android.widget.EditText edNome = new android.widget.EditText(this);
        edNome.setHint("Nome");
        edNome.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        root.addView(edNome, new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));

        android.widget.EditText edEmail = new android.widget.EditText(this);
        edEmail.setHint("Email");
        edEmail.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        root.addView(edEmail, new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));

        b.setView(root);

        b.setNegativeButton("Cancelar", (d, w) -> d.dismiss());
        b.setPositiveButton("Promover", (d, w) -> {
            String uid = edUid.getText().toString().trim();
            String nome = edNome.getText().toString().trim();
            String email = edEmail.getText().toString().trim();
            if (uid.isEmpty() || nome.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Preencha UID, nome e email.", Toast.LENGTH_SHORT).show();
                return;
            }
            promoverAdminPorUid(uid, nome, email);
        });

        b.show();
    }
}