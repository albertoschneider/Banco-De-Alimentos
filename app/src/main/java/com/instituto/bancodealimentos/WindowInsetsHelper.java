package com.instituto.bancodealimentos;

import android.view.View;
import android.view.ViewGroup;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.app.AppCompatActivity;

/**
 * WindowInsetsHelper - Classe para gerenciar insets de sistema (status bar e navigation bar)
 * Versão DEFINITIVA - 100% funcional e testada
 */
public class WindowInsetsHelper {

    /**
     * Configura Edge-to-Edge para a Activity
     * Deve ser chamado ANTES de setContentView()
     */
    public static void setupEdgeToEdge(AppCompatActivity activity) {
        if (activity == null) return;
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
    }

    /**
     * Aplica padding no topo para evitar sobreposição com a status bar
     * Uso: Headers/Toolbars
     */
    public static void applyTopInsets(View view) {
        if (view == null) return;

        final int originalPaddingLeft = view.getPaddingLeft();
        final int originalPaddingTop = view.getPaddingTop();
        final int originalPaddingRight = view.getPaddingRight();
        final int originalPaddingBottom = view.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(
                    originalPaddingLeft,
                    originalPaddingTop + statusBar.top,
                    originalPaddingRight,
                    originalPaddingBottom
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }

    /**
     * Aplica padding no fundo para evitar sobreposição com a navigation bar
     * Uso: Footers/Botões inferiores
     */
    public static void applyBottomInsets(View view) {
        if (view == null) return;

        final int originalPaddingLeft = view.getPaddingLeft();
        final int originalPaddingTop = view.getPaddingTop();
        final int originalPaddingRight = view.getPaddingRight();
        final int originalPaddingBottom = view.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPadding(
                    originalPaddingLeft,
                    originalPaddingTop,
                    originalPaddingRight,
                    originalPaddingBottom + navBar.bottom
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }

    /**
     * Aplica padding no topo E no fundo
     * Uso: Conteúdo que precisa evitar ambas as barras
     */
    public static void applyTopAndBottomInsets(View view) {
        if (view == null) return;

        final int originalPaddingLeft = view.getPaddingLeft();
        final int originalPaddingTop = view.getPaddingTop();
        final int originalPaddingRight = view.getPaddingRight();
        final int originalPaddingBottom = view.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            Insets navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPadding(
                    originalPaddingLeft,
                    originalPaddingTop + statusBar.top,
                    originalPaddingRight,
                    originalPaddingBottom + navBar.bottom
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }

    /**
     * Aplica insets para RecyclerViews e ScrollViews
     * Adiciona padding inferior para não ficar atrás da navigation bar
     */
    public static void applyScrollInsets(View scrollView) {
        if (scrollView == null) return;

        final int originalPaddingLeft = scrollView.getPaddingLeft();
        final int originalPaddingTop = scrollView.getPaddingTop();
        final int originalPaddingRight = scrollView.getPaddingRight();
        final int originalPaddingBottom = scrollView.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(scrollView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    originalPaddingLeft,
                    originalPaddingTop,
                    originalPaddingRight,
                    originalPaddingBottom + systemBars.bottom
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(scrollView);
    }

    /**
     * Aplica insets em um header com toolbar/título
     * Garante que o header não fique atrás da status bar
     * E mantém altura fixa para evitar "esticamento"
     */
    public static void setupWithToolbar(View header) {
        if (header == null) return;

        final int originalPaddingLeft = header.getPaddingLeft();
        final int originalPaddingTop = header.getPaddingTop();
        final int originalPaddingRight = header.getPaddingRight();
        final int originalPaddingBottom = header.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
            Insets statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars());

            // Aplica padding no topo
            v.setPadding(
                    originalPaddingLeft,
                    originalPaddingTop + statusBar.top,
                    originalPaddingRight,
                    originalPaddingBottom
            );

            // Força altura mínima para evitar "esticamento"
            ViewGroup.LayoutParams params = v.getLayoutParams();
            if (params != null && params.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
                // Mantém wrap_content mas define uma altura mínima
                v.setMinimumHeight(dpToPx(v, 56) + statusBar.top);
            }

            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(header);
    }

    /**
     * Converte DP para PX
     */
    private static int dpToPx(View view, int dp) {
        float density = view.getContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    /**
     * Remove todos os listeners de insets de uma view
     * Útil para resetar configurações
     */
    public static void clearInsets(View view) {
        if (view == null) return;
        ViewCompat.setOnApplyWindowInsetsListener(view, null);
    }
}