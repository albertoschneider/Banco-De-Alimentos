package com.instituto.bancodealimentos;

import java.nio.charset.StandardCharsets;

public class PixPayloadBuilder {
    // Campos fixos do padrão EMV/PIX
    private static final String ID_PAYLOAD_FORMAT_INDICATOR = "00";
    private static final String ID_POINT_OF_INIT_METHOD     = "01";
    private static final String ID_MERCHANT_ACCOUNT_INFO    = "26";
    private static final String ID_MERCHANT_ACCOUNT_GUI     = "00";
    private static final String ID_MERCHANT_ACCOUNT_KEY     = "01";
    private static final String ID_MERCHANT_ACCOUNT_DESC    = "02"; // descrição opcional
    private static final String ID_MERCHANT_CATEGORY_CODE   = "52";
    private static final String ID_TRANSACTION_CURRENCY     = "53";
    private static final String ID_TRANSACTION_AMOUNT       = "54";
    private static final String ID_COUNTRY_CODE             = "58";
    private static final String ID_MERCHANT_NAME            = "59";
    private static final String ID_MERCHANT_CITY            = "60";
    private static final String ID_ADDITIONAL_DATA_FIELD    = "62";
    private static final String ID_ADDITIONAL_TXID          = "05";
    private static final String ID_CRC                      = "63";

    private String chavePix;       // e-mail, telefone, aleatória, CNPJ/CPF
    private String descricao;      // opcional
    private String nomeRecebedor;  // ex.: BANCO DE ALIMENTOS
    private String cidadeRecebedor;// ex.: SAO PAULO
    private String txid;           // até 25 chars
    private String valor;          // usar com ponto (ex "20.50"), ou null para sem valor fixo

    public PixPayloadBuilder setChavePix(String chavePix) { this.chavePix = chavePix; return this; }
    public PixPayloadBuilder setDescricao(String descricao) { this.descricao = descricao; return this; }
    public PixPayloadBuilder setNomeRecebedor(String nomeRecebedor) { this.nomeRecebedor = nomeRecebedor; return this; }
    public PixPayloadBuilder setCidadeRecebedor(String cidadeRecebedor) { this.cidadeRecebedor = cidadeRecebedor; return this; }
    public PixPayloadBuilder setTxid(String txid) { this.txid = txid; return this; }
    public PixPayloadBuilder setValor(String valor) { this.valor = valor; return this; }

    public String build() {
        // PF Indicator
        String payload = montaTLV(ID_PAYLOAD_FORMAT_INDICATOR, "01");

        // Static (01 = estático). Se não quiser, comente essa linha.
        payload += montaTLV(ID_POINT_OF_INIT_METHOD, "12");

        // Merchant Account Info (26)
        String mai = montaTLV(ID_MERCHANT_ACCOUNT_GUI, "br.gov.bcb.pix")
                + montaTLV(ID_MERCHANT_ACCOUNT_KEY, chavePix);
        if (descricao != null && !descricao.isEmpty()) {
            mai += montaTLV(ID_MERCHANT_ACCOUNT_DESC, descricao);
        }
        payload += montaTLV(ID_MERCHANT_ACCOUNT_INFO, mai);

        // MCC 0000 (não especificado)
        payload += montaTLV(ID_MERCHANT_CATEGORY_CODE, "0000");

        // Moeda 986 (BRL)
        payload += montaTLV(ID_TRANSACTION_CURRENCY, "986");

        // Valor (opcional)
        if (valor != null && !valor.isEmpty()) {
            payload += montaTLV(ID_TRANSACTION_AMOUNT, valor);
        }

        // País / Nome / Cidade
        payload += montaTLV(ID_COUNTRY_CODE, "BR");
        payload += montaTLV(ID_MERCHANT_NAME, limita(nomeRecebedor, 25));
        payload += montaTLV(ID_MERCHANT_CITY, limita(cidadeRecebedor, 15));

        // Additional Data Field (62): TXID obrigatório (até 25)
        String addData = montaTLV(ID_ADDITIONAL_TXID, limita(txid, 25));
        payload += montaTLV(ID_ADDITIONAL_DATA_FIELD, addData);

        // CRC (63) - calcular em cima do payload + "6304"
        String semCRC = payload + ID_CRC + "04";
        String crc = calculaCRC16(semCRC.getBytes(StandardCharsets.UTF_8));
        payload = semCRC + crc;

        return payload;
    }

    private static String montaTLV(String id, String valor) {
        String len = String.format("%02d", valor.length());
        return id + len + valor;
    }

    private static String limita(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }

    // CRC16/CCITT-FALSE (poly 0x1021, init 0xFFFF)
    private static String calculaCRC16(byte[] bytes) {
        int crc = 0xFFFF;
        for (byte b : bytes) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000) != 0) crc = (crc << 1) ^ 0x1021;
                else crc <<= 1;
                crc &= 0xFFFF;
            }
        }
        return String.format("%04X", crc);
    }
}
