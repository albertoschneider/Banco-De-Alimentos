package com.instituto.bancodealimentos;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;

import java.lang.reflect.Type;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

public class pagamento extends AppCompatActivity {

    // Preferências usadas no carrinho
    private static final String PREFS = "MeuApp";
    private static final String KEY_CART = "carrinho";

    private TextView tvTotalValue;
    private EditText etPixKey;
    private ImageView ivQrCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pagamento);

        // Mantém seu ajuste de insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        tvTotalValue = findViewById(R.id.tvTotalValue);
        etPixKey     = findViewById(R.id.etPixKey);
        ivQrCode     = findViewById(R.id.ivQrCode);
        ImageButton btnCopyPix = findViewById(R.id.btnCopyPix);

        // 1) Total: Intent("total") ou soma do carrinho salvo
        double total = getTotal();
        NumberFormat br = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        tvTotalValue.setText(br.format(total));

        // 2) Dados do recebedor (strings.xml) com normalização exigida pelo padrão
        String chave  = getString(R.string.pix_key);
        String nome   = normalizeNameOrCity(getString(R.string.pix_nome), 25);
        String cidade = normalizeNameOrCity(getString(R.string.pix_cidade), 15);

        // 3) Monta payload “copia e cola” (valor com ponto)
        String valorStr = String.format(Locale.US, "%.2f", total);
        String txid = makeTxid(); // garante <=25 chars
        String payload = new PixPayloadBuilder()
                .setChavePix(chave)
                .setDescricao("Doacao BARC")
                .setNomeRecebedor(nome)
                .setCidadeRecebedor(cidade)
                .setTxid(txid)
                .setValor(valorStr)        // use null para QR sem valor fixo
                .build();

        // 4) Exibe payload e QR
        etPixKey.setText(payload);
        gerarQr(payload, dp(210));

        // 5) Copiar
        btnCopyPix.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("PIX", payload));
            Toast.makeText(this, "Chave PIX copiada!", Toast.LENGTH_SHORT).show();
        });

        if (findViewById(R.id.btn_voltar) != null) {
            findViewById(R.id.btn_voltar).setOnClickListener(v -> finish());
        }
        if (findViewById(R.id.btnVoltarMenu) != null) {
            findViewById(R.id.btnVoltarMenu).setOnClickListener(v -> finish());
        }
    }

    // ---------- Utilidades de negócio ----------

    private double getTotal() {
        // a) Via Intent (ex.: putExtra("total", total))
        double viaIntent = getIntent().getDoubleExtra("total", -1);
        if (viaIntent >= 0) return viaIntent;

        // b) Soma do carrinho salvo (SharedPreferences + Gson)
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        String json = sp.getString(KEY_CART, null);
        if (json == null) return 0.0;
        Type type = new TypeToken<ArrayList<Produto>>() {}.getType();
        ArrayList<Produto> itens = new Gson().fromJson(json, type);
        double total = 0.0;
        if (itens != null) {
            for (Produto p : itens) total += p.getPreco() * p.getQuantidade();
        }
        return total;
    }

    // Gera um TXID curto e único (<= 25 chars)
    private String makeTxid() {
        String base = "DOACAO" + System.currentTimeMillis(); // ~13-19 dígitos
        if (base.length() > 25) base = base.substring(0, 25);
        return base;
    }

    // Normaliza nome/cidade: remove acentos, UPPERCASE, corta no limite
    private static String normalizeNameOrCity(String input, int maxLen) {
        if (input == null) return "";
        String upper = input.toUpperCase(Locale.ROOT);
        String noAccents = removeAccents(upper);
        // Mantém letras, números, espaço e alguns pontuares básicos
        String cleaned = noAccents.replaceAll("[^A-Z0-9 \\-\\.]", " ");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        if (cleaned.length() > maxLen) cleaned = cleaned.substring(0, maxLen);
        return cleaned;
    }

    private static String removeAccents(String s) {
        String norm = Normalizer.normalize(s, Normalizer.Form.NFD);
        return Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(norm).replaceAll("");
    }

    // ---------- QR Code ----------

    private void gerarQr(String text, int sizePx) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            com.google.zxing.common.BitMatrix m =
                    writer.encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx);
            Bitmap bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < sizePx; x++) {
                for (int y = 0; y < sizePx; y++) {
                    bmp.setPixel(x, y, m.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            ivQrCode.setImageBitmap(bmp);
        } catch (WriterException e) {
            Toast.makeText(this, "Erro ao gerar QR", Toast.LENGTH_SHORT).show();
        }
    }

    private int dp(int v) {
        float d = getResources().getDisplayMetrics().density;
        return Math.round(v * d);
    }

    // ---------- Builder do payload PIX (EMV/BR Code) com CRC16 ----------

    private static class PixPayloadBuilder {
        private static final String ID_PFI = "00";
        private static final String ID_PIM = "01";
        private static final String ID_MAI = "26";
        private static final String ID_GUI = "00";
        private static final String ID_KEY = "01";
        private static final String ID_DESC= "02";
        private static final String ID_MCC = "52";
        private static final String ID_CUR = "53";
        private static final String ID_AMT = "54";
        private static final String ID_CTY = "58";
        private static final String ID_MNM = "59";
        private static final String ID_MCI = "60";
        private static final String ID_ADF = "62";
        private static final String ID_TXI = "05";
        private static final String ID_CRC = "63";

        private String chavePix, descricao, nomeRecebedor, cidadeRecebedor, txid, valor;

        PixPayloadBuilder setChavePix(String v){chavePix=v;return this;}
        PixPayloadBuilder setDescricao(String v){descricao=v;return this;}
        PixPayloadBuilder setNomeRecebedor(String v){nomeRecebedor=v;return this;}
        PixPayloadBuilder setCidadeRecebedor(String v){cidadeRecebedor=v;return this;}
        PixPayloadBuilder setTxid(String v){txid=v;return this;}
        PixPayloadBuilder setValor(String v){valor=v;return this;}

        String build() {
            String payload = tlv(ID_PFI, "01");
            payload += tlv(ID_PIM, "12"); // estático

            String mai = tlv(ID_GUI, "br.gov.bcb.pix")
                    + tlv(ID_KEY, nz(chavePix));
            if (!nz(descricao).isEmpty()) {
                mai += tlv(ID_DESC, descricao);
            }
            payload += tlv(ID_MAI, mai);

            payload += tlv(ID_MCC, "0000");
            payload += tlv(ID_CUR, "986");
            if (valor != null && !valor.isEmpty()) payload += tlv(ID_AMT, valor);
            payload += tlv(ID_CTY, "BR");
            payload += tlv(ID_MNM, limit(nz(nomeRecebedor), 25));
            payload += tlv(ID_MCI, limit(nz(cidadeRecebedor), 15));

            String adf = tlv(ID_TXI, limit(nz(txid), 25));
            payload += tlv(ID_ADF, adf);

            String pre = payload + ID_CRC + "04";
            String crc = crc16(pre.getBytes());
            return pre + crc;
        }

        private static String tlv(String id, String value){
            return id + String.format(Locale.US,"%02d", value.length()) + value;
        }
        private static String nz(String s){ return s==null? "" : s; }
        private static String limit(String s,int m){ return s.length()>m? s.substring(0,m):s; }

        // CRC16/CCITT-FALSE
        private static String crc16(byte[] bytes){
            int crc = 0xFFFF;
            for (byte b : bytes) {
                crc ^= (b & 0xFF) << 8;
                for (int i = 0; i < 8; i++) {
                    if ((crc & 0x8000) != 0) crc = (crc << 1) ^ 0x1021;
                    else crc <<= 1;
                    crc &= 0xFFFF;
                }
            }
            return String.format(Locale.US,"%04X", crc);
        }
    }
}
