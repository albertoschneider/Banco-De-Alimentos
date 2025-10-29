package com.instituto.bancodealimentos;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

public class editar_produto extends AppCompatActivity {

    private static final String CLOUD_NAME = "dobs6lmfz";
    private static final String UPLOAD_PRESET = "imagensBARC";

    private ImageView imgProduto;
    private TextInputEditText etNome, etPreco;
    private MaterialButton btnSalvar, btnExcluir;

    private String produtoId;
    private String imagemUrlAtual;
    private Uri novaImagemUri;

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    novaImagemUri = uri;
                    Glide.with(this).load(uri).into(imgProduto);
                }
            });

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowInsetsHelper.setupEdgeToEdge(this);
        setContentView(R.layout.activity_editar_produto);

        // Aplicar insets
        WindowInsetsHelper.applyTopInsets(findViewById(R.id.header));
        WindowInsetsHelper.applyScrollInsets(findViewById(R.id.scroll));


        ImageButton back = findViewById(R.id.btn_voltar);
        if (back != null) back.setOnClickListener(v -> onBackPressed());

        imgProduto = findViewById(R.id.imgProduto);
        etNome = findViewById(R.id.etNome);
        etPreco = findViewById(R.id.etPreco);
        btnSalvar = findViewById(R.id.btnSalvar);
        btnExcluir = findViewById(R.id.btnExcluir);

        findViewById(R.id.btn_editar_imagem).setOnClickListener(v -> pickImage.launch("image/*"));

        produtoId = getIntent().getStringExtra("produtoId");
        if (produtoId == null || produtoId.isEmpty()) {
            Toast.makeText(this, "ID do produto ausente", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        carregar();

        btnSalvar.setOnClickListener(v -> salvar());

        btnExcluir.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Excluir produto")
                    .setMessage("Tem certeza que deseja excluir este produto?")
                    .setPositiveButton("Excluir", (d, w) -> excluirProduto())
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
    }

    private void carregar() {
        FirebaseFirestore.getInstance().collection("produtos").document(produtoId)
                .get().addOnSuccessListener(this::preencher)
                .addOnFailureListener(e -> Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void preencher(DocumentSnapshot d) {
        String nome = d.getString("nome");
        Double preco = d.getDouble("preco");
        imagemUrlAtual = d.getString("imagemUrl");

        if (nome != null) etNome.setText(nome);
        if (preco != null) etPreco.setText(formatBR(preco));
        if (imagemUrlAtual != null && !imagemUrlAtual.isEmpty())
            Glide.with(this).load(imagemUrlAtual).into(imgProduto);
        else
            imgProduto.setImageResource(android.R.color.darker_gray);
    }

    private void salvar() {
        String nome = text(etNome);
        double preco = parsePrecoBR(text(etPreco));

        if (nome.isEmpty()) { etNome.setError("Informe o nome"); return; }
        if (preco <= 0) { etPreco.setError("Informe um preço válido"); return; }

        btnSalvar.setEnabled(false);

        if (novaImagemUri != null) {
            CloudinaryUploader.upload(this, novaImagemUri, CLOUD_NAME, UPLOAD_PRESET, new CloudinaryUploader.Callback() {
                @Override public void onSuccess(String secureUrl) {
                    imagemUrlAtual = secureUrl;
                    updateFirestore(nome, preco, imagemUrlAtual);
                }
                @Override public void onError(String message) {
                    btnSalvar.setEnabled(true);
                    Toast.makeText(editar_produto.this, "Falha no upload: " + message, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            updateFirestore(nome, preco, imagemUrlAtual);
        }
    }

    private void excluirProduto() {
        btnExcluir.setEnabled(false);
        FirebaseFirestore.getInstance().collection("produtos").document(produtoId)
                .delete()
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Produto excluído!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnExcluir.setEnabled(true);
                    Toast.makeText(this, "Erro ao excluir: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateFirestore(String nome, double preco, String imagemUrl) {
        Map<String,Object> up = new HashMap<>();
        up.put("nome", nome);
        up.put("preco", preco);
        if (imagemUrl != null && !imagemUrl.isEmpty()) up.put("imagemUrl", imagemUrl);
        up.put("atualizadoEm", FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance().collection("produtos").document(produtoId)
                .update(up)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Produto atualizado!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSalvar.setEnabled(true);
                    Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String text(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    // "R$ 5,00" / "5,00" / "5.00" -> double
    private double parsePrecoBR(String s) {
        if (s == null) return 0.0;
        s = s.replace("R$", "").replace(" ", "").replace(".", "").replace(",", ".");
        try { return Double.parseDouble(s); } catch (Exception e) { return 0.0; }
    }

    private String formatBR(double v) {
        return String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.2f", v).replace(".", ",");
    }
}