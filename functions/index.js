const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

const db = admin.firestore();

/**
 * HTTPS Function chamada pelo app para CRIAR uma cobrança.
 * Retorna: { pagamentoId, txid, qrCodePayload, expiresAt }
 *
 * Aqui você chama o gateway (Pix/MP/Stripe) e pega:
 *  - txid/chargeId
 *  - payload/qr_code
 * Depois grava em Firestore.
 */
exports.criarCobranca = functions.https.onCall(async (data, context) => {
  // Autenticação obrigatória
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Login necessário.");
  }

  const userId = context.auth.uid;
  const valor = Number(data.valor || 0);
  const orderId = data.orderId || null;
  if (!valor || valor <= 0) {
    throw new functions.https.HttpsError("invalid-argument", "Valor inválido.");
  }

  // TODO: INTEGRE COM SEU GATEWAY AQUI (Pix/MP/Stripe...)
  // const { txid, qrCodePayload } = await gateway.criarCobranca(valor, ...);

  // MOCK provisório para teste local:
  const txid = "tx_" + Date.now();
  const qrCodePayload = "0002012636...MOCK..." + txid;

  const now = Date.now();
  const expiresAt = admin.firestore.Timestamp.fromDate(new Date(now + 10 * 60 * 1000));

  const doc = {
    userId,
    orderId,
    valor,
    status: "PENDENTE",
    txid,
    qrCodePayload,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    expiresAt,
  };

  const ref = await db.collection("pagamentos").add(doc);
  return {
    pagamentoId: ref.id,
    txid,
    qrCodePayload,
    expiresAt: expiresAt.toDate().toISOString()
  };
});

/**
 * WEBHOOK do provedor de pagamento.
 * PASSO QUE VOCÊ PRECISA FAZER: no painel do provedor,
 * cadastre a URL pública desta função (após o deploy).
 *
 * Ex.: https://us-central1-SEU-PROJETO.cloudfunctions.net/webhookPagamento
 *
 * TODO: Valide a assinatura do provedor (depende do gateway).
 */
exports.webhookPagamento = functions.https.onRequest(async (req, res) => {
  try {
    // TODO: validar assinatura! (depende do seu gateway)
    // Exemplo: const sig = req.headers['x-gateway-signature'];

    // TODO: mapeie o evento recebido
    // Exemplo: const txid = req.body?.data?.txid;
    // Simulando para teste (use o campo correto do seu gateway):
    const txid = req.body?.txid || req.query.txid; // só para teste

    if (!txid) {
      console.warn("Webhook sem txid.");
      return res.status(200).send("ok");
    }

    // Localiza o pagamento pelo txid
    const snap = await db.collection("pagamentos").where("txid", "==", txid).limit(1).get();
    if (snap.empty) return res.status(200).send("ok");

    const ref = snap.docs[0].ref;
    await ref.update({
      status: "PAGO",
      paidAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    return res.status(200).send("ok");
  } catch (e) {
    console.error(e);
    return res.status(400).send("error");
  }
});

/**
 * (Opcional) Tarefa agendada para marcar pagamentos como EXPIRADO
 * quando passar de 10 minutos sem pagar.
 */
exports.expirarPagamentos = functions.pubsub.schedule("every 5 minutes").onRun(async () => {
  const agora = Date.now();
  const qs = await db.collection("pagamentos")
    .where("status", "==", "PENDENTE")
    .where("expiresAt", "<=", admin.firestore.Timestamp.fromDate(new Date(agora)))
    .get();

  const batch = db.batch();
  qs.docs.forEach(doc => {
    batch.update(doc.ref, {
      status: "EXPIRADO",
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });
  });
  if (!qs.empty) await batch.commit();
  return null;
});
