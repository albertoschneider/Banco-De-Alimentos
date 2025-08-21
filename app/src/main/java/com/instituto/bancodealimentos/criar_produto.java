package com.instituto.bancodealimentos;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

public class criar_produto extends AppCompatActivity {

    private static final String CLOUD_NAME = "dobs6lmfz";
    private static final String UPLOAD_PRESET = "imagensBARC";

    private ImageView imgPreview;
    private TextInputEditText etNome, etPreco;
    private MaterialButton btnCriar;

    private Uri imgUri;
    private String imagemUrl;

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    imgUri = uri;
                    Glide.with(this).load(uri).into(imgPreview);
                    imgPreview.setVisibility(android.view.View.VISIBLE);
                    if (findViewById(R.id.tvAddHint) != null) {
                        findViewById(R.id.tvAddHint).setVisibility(android.view.View.GONE);
                    }
                }
            });

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_criar_produto);

        View header = findViewById(R.id.header); // o ConstraintLayout do topo
        ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop() + sb.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
        ViewCompat.requestApplyInsets(header);

        ImageButton back = findViewById(R.id.btn_voltar);
        if (back != null) back.setOnClickListener(v -> onBackPressed());

        imgPreview = findViewById(R.id.imgPreview);
        etNome = findViewById(R.id.etNome);
        etPreco = findViewById(R.id.etPreco);
        btnCriar = findViewById(R.id.btnCriar);

        findViewById(R.id.btn_pick_image).setOnClickListener(v -> pickImage.launch("image/*"));

        btnCriar.setOnClickListener(v -> {
            String nome = text(etNome);
            double preco = parsePreco(text(etPreco));

            if (nome.isEmpty()) { etNome.setError("Informe o nome"); return; }
            if (preco <= 0) { etPreco.setError("Informe um preço válido"); return; }

            btnCriar.setEnabled(false);

            if (imgUri != null && (imagemUrl == null || imagemUrl.isEmpty())) {
                CloudinaryUploader.upload(this, imgUri, CLOUD_NAME, UPLOAD_PRESET,
                        new CloudinaryUploader.Callback() {
                            @Override public void onSuccess(String secureUrl) {
                                imagemUrl = secureUrl;
                                salvar(nome, preco, imagemUrl);
                            }
                            @Override public void onError(String message) {
                                btnCriar.setEnabled(true);
                                Toast.makeText(criar_produto.this,
                                        "Falha no upload: " + message, Toast.LENGTH_LONG).show();
                            }
                        });
            } else {
                salvar(nome, preco, imagemUrl); // imagemUrl pode ser null (produto sem imagem)
            }
        });
    }

    private void salvar(String nome, double preco, String imagemUrl) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String,Object> doc = new HashMap<>();
        doc.put("nome", nome);
        doc.put("preco", preco);
        if (imagemUrl != null && !imagemUrl.isEmpty()) doc.put("imagemUrl", imagemUrl);

        db.collection("produtos").add(doc).addOnSuccessListener(r -> {
            Toast.makeText(this, "Produto criado!", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            btnCriar.setEnabled(true);
            Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private String text(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    // Aceita "R$ 5,00" / "5,00" / "5.00"
    private double parsePreco(String s) {
        if (s == null) return 0.0;
        s = s.replace("R$", "").replace(" ", "").replace(".", "").replace(",", ".");
        try { return Double.parseDouble(s); } catch (Exception e) { return 0.0; }
    }
}