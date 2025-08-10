package com.instituto.bancodealimentos;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;
import android.view.View;
import android.widget.ImageButton;

import java.util.Arrays;
import java.util.List;

public class voluntariar extends AppCompatActivity {

    private ViewPager2 viewPager;
    private ImageAdapter adapter;
    private List<Integer> imageList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voluntariar); // substitua pelo nome do seu layout XML

        viewPager = findViewById(R.id.viewPager);
        ImageButton btnNext = findViewById(R.id.btn_next);
        ImageButton btnPrev = findViewById(R.id.btn_prev);

        imageList = Arrays.asList(
                R.drawable.imagem1
        );

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
