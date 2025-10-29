package com.instituto.bancodealimentos;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MAIN_ACTIVITY";
    private Button btnRegistrarse;
    private TextView textViewClick;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowInsetsHelper.setupEdgeToEdge(this);
        Log.w(TAG, "onCreate()");
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        btnRegistrarse = findViewById(R.id.btnRegister);
        textViewClick  = findViewById(R.id.txtLogin);

        btnRegistrarse.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, telaregistro.class));
            }
        });

        textViewClick.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, telalogin.class));
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });
    }

    // *** REMOVIDO O onStart() QUE CAUSAVA O PROBLEMA ***
    // A SplashActivity já faz o roteamento inicial correto.
    // Este onStart() estava interferindo quando voltando de outras Activities,
    // causando a destruição prematura da pilha durante o fluxo do Google Sign-In.
}