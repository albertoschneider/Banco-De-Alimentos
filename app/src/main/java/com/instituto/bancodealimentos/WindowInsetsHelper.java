package com.instituto.bancodealimentos;

import android.view.View;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.app.AppCompatActivity;

public class WindowInsetsHelper {

    /**
     * Configura edge-to-edge na Activity
     * CHAMAR no onCreate ANTES de setContentView
     */
    public static void setupEdgeToEdge(AppCompatActivity activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
    }

    /**
     * Aplica padding no topo para evitar que seja cortado pela status bar
     * USAR em headers/toolbars
     */
    public static void applyTopInsets(View view) {
        if (view == null) return;

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars());

            // Mantém padding existente + adiciona o da status bar
            v.setPadding(
                    v.getPaddingLeft(),
                    v.getPaddingTop() + statusBar.top,  // Adiciona padding do topo
                    v.getPaddingRight(),
                    v.getPaddingBottom()
            );

            return insets;
        });

        ViewCompat.requestApplyInsets(view);
    }

    /**
     * Aplica padding embaixo para evitar que seja cortado pela navigation bar
     * USAR em footers/botões no final da tela
     */
    public static void applyBottomInsets(View view) {
        if (view == null) return;

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars());

            // Mantém padding existente + adiciona o da navigation bar
            v.setPadding(
                    v.getPaddingLeft(),
                    v.getPaddingTop(),
                    v.getPaddingRight(),
                    v.getPaddingBottom() + navBar.bottom  // Adiciona padding embaixo
            );

            return insets;
        });

        ViewCompat.requestApplyInsets(view);
    }

    /**
     * Aplica padding no topo E embaixo
     * USAR quando precisar dos dois (raro)
     */
    public static void applyTopAndBottomInsets(View view) {
        if (view == null) return;

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            Insets navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars());

            v.setPadding(
                    v.getPaddingLeft(),
                    v.getPaddingTop() + statusBar.top,
                    v.getPaddingRight(),
                    v.getPaddingBottom() + navBar.bottom
            );

            return insets;
        });

        ViewCompat.requestApplyInsets(view);
    }

    /**
     * Aplica padding em ScrollViews/RecyclerViews
     * Usa clipToPadding=false para permitir scroll completo
     */
    public static void applyScrollInsets(View scrollView) {
        if (scrollView == null) return;

        ViewCompat.setOnApplyWindowInsetsListener(scrollView, (v, insets) -> {
            Insets systemBars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
            );

            v.setPadding(
                    v.getPaddingLeft(),
                    0,  // Não adiciona no topo (header já tem)
                    v.getPaddingRight(),
                    systemBars.bottom  // Adiciona embaixo
            );

            return insets;
        });

        ViewCompat.requestApplyInsets(scrollView);
    }
}