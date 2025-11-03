package com.instituto.bancodealimentos;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class gerenciar_admins extends AppCompatActivity {

    private static final String TAG = "GERENCIAR_ADMINS";
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
                            Log.e(TAG, "Erro ao carregar admins", error);
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
                        Log.e(TAG, "Falha ao completar dados", e);
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
            String myUid = FirebaseAuth.getInstance().getUid();
            if (user.getId().equals(myUid)) {
                Toast.makeText(this, "Você não pode remover a si mesmo.", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "Removendo admin: " + user.getId());

            // CORRIGIDO: Remove de fato usando batch
            WriteBatch batch = db.batch();

            DocumentReference uRef = db.collection("usuarios").document(user.getId());
            DocumentReference aRef = db.collection("admins").document(user.getId());

            // Atualiza usuarios/{uid}.isAdmin = false
            batch.update(uRef, "isAdmin", false);

            // Deleta admins/{uid}
            batch.delete(aRef);

            batch.commit()
                    .addOnSuccessListener(unused -> {
                        Log.d(TAG, "Admin removido com sucesso!");
                        Toast.makeText(this, "Permissão de administrador removida.", Toast.LENGTH_SHORT).show();
                        bs.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Erro ao remover admin: " + e.getMessage(), e);
                        Toast.makeText(this, "Erro ao remover permissão: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        bs.show();
    }
}