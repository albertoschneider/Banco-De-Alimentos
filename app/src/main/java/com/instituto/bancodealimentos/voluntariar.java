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
        setContentView(R.layout.activity_voluntariar); // seu layout

        // Header com padding do status bar (edge-to-edge)
        View header = findViewById(R.id.header);
        if (header != null) {
            ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
                Insets sb = insets.getInsets(WindowInsetsCompat.Type.statusBars());
                v.setPadding(v.getPaddingLeft(), sb.top, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
            ViewCompat.requestApplyInsets(header);
        }

        viewPager = findViewById(R.id.viewPager);
        ImageButton btnNext = findViewById(R.id.btn_next);
        ImageButton btnPrev = findViewById(R.id.btn_prev);
        ImageButton imgBtnBack = findViewById(R.id.btn_voltar);
        LinearLayout btnWhatsapp = findViewById(R.id.btn_whatsapp);

        // Imagens locais (troque pelos seus drawables)
        imageList = Arrays.asList(
                R.drawable.voluntario_imagem1,
                R.drawable.voluntario_imagem2,
                R.drawable.voluntario_imagem3,
                R.drawable.voluntario_imagem4,
                R.drawable.voluntario_imagem5
        );

        // Adapter
        // onde estava: new ImageAdapter(voluntariar.this, imageList);
        adapter = new ImageAdapter(imageList);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(1);

        // Remover qualquer margem/decoração/efeito que gere "faixa branca"
        viewPager.setPadding(0, 0, 0, 0);
        while (viewPager.getItemDecorationCount() > 0) {
            viewPager.removeItemDecorationAt(0);
        }
        viewPager.setPageTransformer(null);
        // Remove o glow de overscroll que pode dar impressão de borda
        View vpChild = viewPager.getChildAt(0);
        if (vpChild != null) vpChild.setOverScrollMode(View.OVER_SCROLL_NEVER);

        // Setas: navegação simples
        btnNext.setOnClickListener(v -> {
            int next = viewPager.getCurrentItem() + 1;
            if (next < imageList.size()) {
                viewPager.setCurrentItem(next, true);
            }
        });

        btnPrev.setOnClickListener(v -> {
            int prev = viewPager.getCurrentItem() - 1;
            if (prev >= 0) {
                viewPager.setCurrentItem(prev, true);
            }
        });

        // Botão WhatsApp
        btnWhatsapp.setOnClickListener(v -> {
            // COLOQUE O SEU LINK COMPLETO AQUI (já está pronto):
            String url = "https://wa.me/555192481830?text=Ol%C3%A1%21%20Tenho%20interesse%20em%20me%20tornar%20volunt%C3%A1rio%20no%20Banco%20de%20Alimentos%20e%20gostaria%20de%20saber%20como%20posso%20ajudar.\n";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        });

        // Voltar (se quiser apenas fechar a tela, troque por finish(); )
        imgBtnBack.setOnClickListener(v -> {
            Intent intent = new Intent(voluntariar.this, menu.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });
    }
}
