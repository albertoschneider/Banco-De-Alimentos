const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

const db = admin.firestore();

/**
 * HTTPS Function chamada pelo app para CRIAR uma cobrança.
 * (mantida como você enviou)
 */
exports.criarCobranca = functions.https.onCall(async (data, context) => {
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
 * WEBHOOK do provedor.
 * (pequeno extra: também marca o pedido como PAGO se houver userId/orderId)
 */
exports.webhookPagamento = functions.https.onRequest(async (req, res) => {
  try {
    // TODO: validar assinatura do provedor
    const txid = req.body?.txid || req.query.txid; // TESTE
    if (!txid) return res.status(200).send("ok");

    const snap = await db.collection("pagamentos").where("txid", "==", txid).limit(1).get();
    if (snap.empty) return res.status(200).send("ok");

    const ref = snap.docs[0].ref;
    const pagamento = snap.docs[0].data();

    // marca pagamento como PAGO
    await ref.update({
      status: "PAGO",
      paidAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    // EXTRA: marca pedido como PAGO se existir vínculo
    const userId = pagamento.userId;
    const orderId = pagamento.orderId;
    if (userId && orderId) {
      await db.collection("users").doc(userId)
        .collection("orders").doc(orderId)
        .set({ status: "PAGO", paidAt: admin.firestore.FieldValue.serverTimestamp() }, { merge: true });
    }

    return res.status(200).send("ok");
  } catch (e) {
    console.error(e);
    return res.status(400).send("error");
  }
});

/**
 * (Opcional) agendado — se não tiver billing, o Scheduler não roda.
 * Pode deixar aqui; não quebra nada em projeto free.
 */
exports.expirarPagamentos = functions.pubsub.schedule("every 5 minutes").onRun(async () => {
  const agora = admin.firestore.Timestamp.now();
  const qs = await db.collection("pagamentos")
    .where("status", "==", "PENDENTE")
    .where("expiresAt", "<=", agora)
    .get();

  if (qs.empty) return null;
  const batch = db.batch();
  qs.docs.forEach(doc => {
    batch.update(doc.ref, {
      status: "EXPIRADO",
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });
  });
  await batch.commit();
  return null;
});

/* --------- ADIÇÃO (grátis): garantir expiresAt no pedido recém-criado --------- */
exports.onOrderCreateEnsureExpiry = functions.firestore
  .document("users/{uid}/orders/{orderId}")
  .onCreate(async (snap, ctx) => {
    const data = snap.data() || {};
    if (!data.expiresAt) {
      const expiresAt = admin.firestore.Timestamp.fromMillis(Date.now() + 10 * 60 * 1000);
      await snap.ref.update({ expiresAt });
    }
  });
