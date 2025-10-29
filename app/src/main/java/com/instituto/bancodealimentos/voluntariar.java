package com.instituto.bancodealimentos;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import java.util.Arrays;
import java.util.List;

public class voluntariar extends AppCompatActivity {

    private ViewPager2 viewPager;
    private ImageAdapter adapter;
    private List<Integer> imageList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowInsetsHelper.setupEdgeToEdge(this);
        setContentView(R.layout.activity_voluntariar);

        // Aplicar insets
        WindowInsetsHelper.applyTopInsets(findViewById(R.id.header));
        WindowInsetsHelper.applyScrollInsets(findViewById(R.id.scroll));

        // Header edge-to-edge

        viewPager = findViewById(R.id.viewPager);
        ImageButton btnNext = findViewById(R.id.btn_next);
        ImageButton btnPrev = findViewById(R.id.btn_prev);
        ImageButton imgBtnBack = findViewById(R.id.btn_voltar);
        LinearLayout btnWhatsapp = findViewById(R.id.btn_whatsapp);

        // Imagens do carrossel
        imageList = Arrays.asList(
                R.drawable.voluntario_imagem1,
                R.drawable.voluntario_imagem2,
                R.drawable.voluntario_imagem3,
                R.drawable.voluntario_imagem4,
                R.drawable.voluntario_imagem5
        );

        adapter = new ImageAdapter(imageList); // seu adapter com looping
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(1);
        viewPager.setPadding(0, 0, 0, 0);
        while (viewPager.getItemDecorationCount() > 0) viewPager.removeItemDecorationAt(0);
        viewPager.setPageTransformer(null);
        View vpChild = viewPager.getChildAt(0);
        if (vpChild != null) vpChild.setOverScrollMode(View.OVER_SCROLL_NEVER);

        // Setas com loop
        btnNext.setOnClickListener(v -> {
            int i = viewPager.getCurrentItem();
            int last = imageList.size() - 1;
            viewPager.setCurrentItem(i == last ? 0 : i + 1, true);
        });
        btnPrev.setOnClickListener(v -> {
            int i = viewPager.getCurrentItem();
            int last = imageList.size() - 1;
            viewPager.setCurrentItem(i == 0 ? last : i - 1, true);
        });

        // --- WhatsApp: usa a MESMA mensagem do seu link original ---
        final String mensagem =
                "Olá! Tenho interesse em me tornar voluntário no Banco de Alimentos e gostaria de saber como posso ajudar.";

        btnWhatsapp.setOnClickListener(v -> {
            // SettingsRepository monta o wa.me com o número salvo (ou vazio) + URL-encode da mensagem
            Uri uri = SettingsRepository.buildWhatsUrl(this, mensagem);
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        });
        // ------------------------------------------------------------

        // Voltar
        imgBtnBack.setOnClickListener(v -> finish());
    }
}