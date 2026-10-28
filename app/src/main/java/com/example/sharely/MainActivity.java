package com.example.sharely;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/** Launcher welcome screen with a lightweight, non-blocking brand entrance animation. */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        View root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets safe = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);

        TextView login = findViewById(R.id.buttonLogin);
        TextView register = findViewById(R.id.buttonRegister);
        login.setOnClickListener(v -> startActivity(
                new Intent(this, LoginActivity.class)));
        register.setOnClickListener(v -> startActivity(
                new Intent(this, RegisterActivity.class)));

        if (savedInstanceState == null) {
            View mark = findViewById(R.id.welcomeLogo);
            View wordmark = findViewById(R.id.welcomeWordmark);
            // Uses system animation scale, and does not delay authentication.
            mark.setAlpha(0f);
            mark.setScaleX(0.88f);
            mark.setScaleY(0.88f);
            mark.setTranslationY(18f * getResources().getDisplayMetrics().density);
            mark.animate().alpha(1f).scaleX(1f).scaleY(1f)
                    .translationY(0f).setDuration(600).start();

            wordmark.setAlpha(0f);
            wordmark.setTranslationY(9f * getResources().getDisplayMetrics().density);
            wordmark.animate().alpha(1f).translationY(0f)
                    .setStartDelay(160).setDuration(560).start();
        }
    }
}
