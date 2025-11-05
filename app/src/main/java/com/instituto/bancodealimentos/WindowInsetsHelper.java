package com.instituto.bancodealimentos;

import android.view.View;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Helper class para gerenciar WindowInsets (edge-to-edge)
 * VERSÃO CORRIGIDA: Não mexe no topo (funcionava), corrige SÓ o rodapé
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
     * MANTIDO ORIGINAL - Não adiciona espaçamento extra, funciona como antes
     */
    public static void applyTopInsets(View view) {
        if (view == null) return;

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // SEM espaçamento extra - usa apenas o systemBars.top padrão
            v.setPadding(
                    v.getPaddingLeft(),
                    systemBars.top, // SEM ADICIONAR NADA
                    v.getPaddingRight(),
                    v.getPaddingBottom()
            );

            return insets;
        });
    }

    /**
     * Aplica insets em conteúdo scrollável (ScrollView, RecyclerView, etc)
     * CORRIGIDO: Adiciona padding inferior para navigation bar
     */
    public static void applyScrollInsets(View view) {
        if (view == null) return;

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            v.setPadding(
                    v.getPaddingLeft(),
                    v.getPaddingTop(),
                    v.getPaddingRight(),
                    systemBars.bottom // Protege contra navigation bar
            );

            return insets;
        });
    }

    /**
     * Aplica insets em views fixas no rodapé (footers, botões fixos)
     * Garante que navigation bar não sobreponha os botões
     */
    public static void applyBottomInsets(View view) {
        if (view == null) return;

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Adiciona padding no rodapé para navigation bar + margem de segurança
            int extraBottomPadding = (int) (16 * view.getContext().getResources().getDisplayMetrics().density);

            v.setPadding(
                    v.getPaddingLeft(),
                    v.getPaddingTop(),
                    v.getPaddingRight(),
                    systemBars.bottom + extraBottomPadding // Proteção navigation bar
            );

            return insets;
        });
    }

    /**
     * Aplica marginBottom em views para evitar sobreposição
     * Útil para botões fixos no rodapé
     */
    public static void applyBottomMargin(View view) {
        if (view == null) return;

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            if (v.getLayoutParams() instanceof android.view.ViewGroup.MarginLayoutParams) {
                android.view.ViewGroup.MarginLayoutParams params =
                        (android.view.ViewGroup.MarginLayoutParams) v.getLayoutParams();

                int extraBottomMargin = (int) (16 * view.getContext().getResources().getDisplayMetrics().density);

                params.bottomMargin = systemBars.bottom + extraBottomMargin;
                v.setLayoutParams(params);
            }

            return insets;
        });
    }
}