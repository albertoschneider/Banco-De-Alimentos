package com.instituto.bancodealimentos;

import android.view.View;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Helper class para gerenciar WindowInsets (edge-to-edge)
 * ATUALIZADO: Mais espaçamento no topo (24dp) e proteção contra navigation bar
 */
public class WindowInsetsHelper {

    /**
     * Configura a activity para edge-to-edge
     */
    public static void setupEdgeToEdge(AppCompatActivity activity) {
        if (activity == null) return;

        View decorView = activity.getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
    }

    /**
     * Aplica insets no topo (para headers fixos)
     * ATUALIZADO: Adiciona 24dp de espaçamento extra para afastar MUITO do topo
     */
    public static void applyTopInsets(View view) {
        if (view == null) return;

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Calcula o espaçamento extra (24dp convertido para pixels)
            int extraSpacingPx = (int) (24 * view.getContext().getResources().getDisplayMetrics().density);

            // Aplica padding no topo com espaçamento extra
            v.setPadding(
                    v.getPaddingLeft(),
                    systemBars.top + extraSpacingPx, // ADICIONA 24dp de espaçamento
                    v.getPaddingRight(),
                    v.getPaddingBottom()
            );

            return insets;
        });
    }

    /**
     * Aplica insets em conteúdo scrollável (ScrollView, RecyclerView, etc)
     * ATUALIZADO: Adiciona padding inferior para evitar que navigation bar sobreponha conteúdo
     */
    public static void applyScrollInsets(View view) {
        if (view == null) return;

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // CRÍTICO: Adiciona padding inferior para evitar sobreposição da navigation bar
            int extraBottomPadding = (int) (16 * view.getContext().getResources().getDisplayMetrics().density);

            v.setPadding(
                    v.getPaddingLeft(),
                    v.getPaddingTop(),
                    v.getPaddingRight(),
                    systemBars.bottom + extraBottomPadding // PROTEÇÃO contra navigation bar
            );

            return insets;
        });
    }

    /**
     * NOVO: Aplica insets em views fixas no rodapé (footers, botões fixos)
     * Garante que navigation bar não sobreponha os botões
     */
    public static void applyBottomInsets(View view) {
        if (view == null) return;

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Adiciona padding extra no rodapé (24dp para segurança)
            int extraBottomPadding = (int) (24 * view.getContext().getResources().getDisplayMetrics().density);

            v.setPadding(
                    v.getPaddingLeft(),
                    v.getPaddingTop(),
                    v.getPaddingRight(),
                    systemBars.bottom + extraBottomPadding // PROTEÇÃO contra navigation bar
            );

            return insets;
        });
    }

    /**
     * NOVO: Aplica marginBottom em views para evitar sobreposição
     * Útil para botões fixos no rodapé
     */
    public static void applyBottomMargin(View view) {
        if (view == null) return;

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            if (v.getLayoutParams() instanceof android.view.ViewGroup.MarginLayoutParams) {
                android.view.ViewGroup.MarginLayoutParams params =
                        (android.view.ViewGroup.MarginLayoutParams) v.getLayoutParams();

                // Adiciona margin extra no rodapé (24dp)
                int extraBottomMargin = (int) (24 * view.getContext().getResources().getDisplayMetrics().density);

                params.bottomMargin = systemBars.bottom + extraBottomMargin;
                v.setLayoutParams(params);
            }

            return insets;
        });
    }
}