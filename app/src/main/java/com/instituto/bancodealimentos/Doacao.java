package com.instituto.bancodealimentos;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

public class Doacao {
    @DocumentId
    private String id;

    private String uid;            // UID do usuário (dono do pedido)
    private String status;         // "pending" | "paid" | "expired"
    private Long amountCents;      // ex.: 2590 = R$ 25,90
    private Long orderNumber;      // 1, 2, 3, ...
    private String method;         // "pix"
    private String description;    // opcional
    private Timestamp createdAt;   // obrigatório
    private Timestamp expiresAt;   // opcional
    private String referenceId;    // opcional (id no gateway)

    // NOVOS (para reabrir o mesmo pagamento)
    private String pixCopiaCola;   // payload "copia e cola" original
    private String pixQrImageUrl;  // URL da imagem do QR (ou salve base64 em outro campo)

    public Doacao() {}

    public String getId() { return id; }
    public String getUid() { return uid; }
    public String getStatus() { return status == null ? "pending" : status; }
    public Long getAmountCents() { return amountCents == null ? 0L : amountCents; }
    public Long getOrderNumber() { return orderNumber == null ? 0L : orderNumber; }
    public String getMethod() { return method == null ? "pix" : method; }
    public String getDescription() { return description == null ? "" : description; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getExpiresAt() { return expiresAt; }
    public String getReferenceId() { return referenceId; }

    public String getPixCopiaCola() { return pixCopiaCola == null ? "" : pixCopiaCola; }
    public String getPixQrImageUrl() { return pixQrImageUrl == null ? "" : pixQrImageUrl; }
}
