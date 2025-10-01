package com.instituto.bancodealimentos;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;
import android.view.View;
import android.widget.ImageButton;
import android.net.Uri;
import android.widget.LinearLayout;

import java.util.Arrays;
import java.util.List;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

public class voluntariar extends AppCompatActivity {

    private ViewPager2 viewPager;
    private ImageAdapter adapter;
    private List<Integer> imageList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voluntariar); // substitua pelo nome do seu layout XML

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

        imageList = Arrays.asList(
                R.drawable.voluntario_imagem1,
                R.drawable.voluntario_imagem2,
                R.drawable.voluntario_imagem3,
                R.drawable.voluntario_imagem4,
                R.drawable.voluntario_imagem5
        );

        LinearLayout btnWhatsapp = findViewById(R.id.btn_whatsapp);

        btnWhatsapp.setOnClickListener(v -> {
            // COLOQUE O SEU LINK COMPLETO AQUI
            String url = "https://wa.me/555192481830?text=Ol%C3%A1%21%20Tenho%20interesse%20em%20me%20tornar%20volunt%C3%A1rio%20no%20Banco%20de%20Alimentos%20e%20gostaria%20de%20saber%20como%20posso%20ajudar.\n";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        });


        ImageButton imgBtnBack = findViewById(R.id.btn_voltar);

        imgBtnBack.setOnClickListener(v -> {
            Intent intent = new Intent(voluntariar.this, menu.class);
            startActivity(intent);
        });

        adapter = new ImageAdapter(this, imageList);
        viewPager.setAdapter(adapter);

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int next = viewPager.getCurrentItem() + 1;
                if (next < imageList.size()) {
                    viewPager.setCurrentItem(next);
                }
            }
        });


        btnPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int prev = viewPager.getCurrentItem() - 1;
                if (prev >= 0) {
                    viewPager.setCurrentItem(prev);
                }
            }
        });
    }
}
