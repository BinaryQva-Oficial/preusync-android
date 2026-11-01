/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase: DonateActivity.java
 * Versión: v2.0.0
 * Descripción: Pantalla de donaciones completas (Zaps Bio, Transfermóvil BPA, USDT, SOL, TON).
 * Autor: JiroxDEV
 * Licensed under the GNU Affero General Public License v3
 * ============================================================================
 */

package binaryqva.educative.preusync.ui.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;

import com.google.android.material.button.MaterialButton;

import binaryqva.educative.preusync.R;
import binaryqva.educative.preusync.utils.theme.ThemeManager;

/**
 * Pantalla informativa y operativa con canales oficiales de apoyo económico.
 */
public class DonateActivity extends BaseActivity {

    private ImageButton backButton;
    private MaterialButton btnZapsBio, btnCopyBpa, btnCopyBep20, btnCopyTrc20, btnCopySol, btnCopyTon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_donate);

        backButton = findViewById(R.id.backButton);
        btnZapsBio = findViewById(R.id.btnZapsBio);
        btnCopyBpa = findViewById(R.id.btnCopyBpa);
        btnCopyBep20 = findViewById(R.id.btnCopyBep20);
        btnCopyTrc20 = findViewById(R.id.btnCopyTrc20);
        btnCopySol = findViewById(R.id.btnCopySol);
        btnCopyTon = findViewById(R.id.btnCopyTon);

        initialize(savedInstanceState);
        initializeLogic();
    }

    private void initialize(Bundle savedInstanceState) {
        if (backButton != null) backButton.setOnClickListener(v -> finish());

        if (btnZapsBio != null) {
            btnZapsBio.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://zaps.bio/binaryqva"));
                startActivity(intent);
            });
        }

        if (btnCopyBpa != null) btnCopyBpa.setOnClickListener(v -> copyToClipboard("Tarjeta BPA", "9205129976701558"));
        if (btnCopyBep20 != null) btnCopyBep20.setOnClickListener(v -> copyToClipboard("Dirección BEP20/ERC20", "0x042bF7f30aF43b7A69f18F5Fd28e29179CbA8446"));
        if (btnCopyTrc20 != null) btnCopyTrc20.setOnClickListener(v -> copyToClipboard("Dirección TRC20", "TEphHAaFE3ogamr4pTZUSSzXXHR8heFHzf"));
        if (btnCopySol != null) btnCopySol.setOnClickListener(v -> copyToClipboard("Dirección Solana", "H8J5tCkC38hXQoNC6K6Yv8P8k5irGFA5XQiGVxyw25iR"));
        if (btnCopyTon != null) btnCopyTon.setOnClickListener(v -> copyToClipboard("Dirección Toncoin", "UQD31gwYfU9T8sSQ_V76htxPRm2RUVNWzb4QwEnmYmHDUSsk"));
    }

    private void copyToClipboard(String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText(label, text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "¡Copiado al portapapeles!: " + text, Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeLogic() {
        int colorBackground = ThemeManager.getThemeColor(this, R.attr.colorBackground);
        getWindow().setNavigationBarColor(colorBackground);
    }
}
