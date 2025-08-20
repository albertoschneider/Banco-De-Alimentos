package com.instituto.bancodealimentos;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Upload unsigned para Cloudinary (sem OkHttp).
 * Uso:
 * CloudinaryUploader.upload(ctx, uri, "dobs6lmfz", "imagensBARC", new CloudinaryUploader.Callback() { ... });
 */
public class CloudinaryUploader {

    public interface Callback {
        void onSuccess(String secureUrl);
        void onError(String message);
    }

    public static void upload(Context ctx, Uri uri, String cloudName, String uploadPreset, Callback cb) {
        new Thread(() -> {
            Handler main = new Handler(Looper.getMainLooper());
            HttpURLConnection conn = null;
            DataOutputStream out = null;
            InputStream in = null;
            try {
                // Lê os bytes do arquivo
                byte[] fileBytes = readAll(ctx.getContentResolver().openInputStream(uri));
                String fileName = "upload.jpg";
                String mime = ctx.getContentResolver().getType(uri);
                if (mime == null || mime.trim().isEmpty()) mime = "image/jpeg";

                // Endpoint
                URL url = new URL("https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload");
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);

                // Multipart
                String boundary = "----AndroidFormBoundary" + System.currentTimeMillis();
                String lineEnd = "\r\n";
                String twoHyphens = "--";

                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                out = new DataOutputStream(conn.getOutputStream());

                // Campo upload_preset
                out.writeBytes(twoHyphens + boundary + lineEnd);
                out.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"" + lineEnd);
                out.writeBytes(lineEnd);
                out.writeBytes(uploadPreset + lineEnd);

                // Campo file (binário)
                out.writeBytes(twoHyphens + boundary + lineEnd);
                out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"" + lineEnd);
                out.writeBytes("Content-Type: " + mime + lineEnd);
                out.writeBytes(lineEnd);
                out.write(fileBytes);
                out.writeBytes(lineEnd);

                // Fechar multipart
                out.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                out.flush();

                int code = conn.getResponseCode();
                in = (code >= 200 && code < 300)
                        ? new BufferedInputStream(conn.getInputStream())
                        : new BufferedInputStream(conn.getErrorStream());

                String resp = readString(in);
                if (code >= 200 && code < 300) {
                    JSONObject obj = new JSONObject(resp);
                    String secure = obj.getString("secure_url");
                    main.post(() -> cb.onSuccess(secure));
                } else {
                    main.post(() -> cb.onError("HTTP " + code + ": " + resp));
                }

            } catch (Exception e) {
                main.post(() -> cb.onError(e.getMessage()));
            } finally {
                try { if (out != null) out.close(); } catch (Exception ignored) {}
                try { if (in != null) in.close(); } catch (Exception ignored) {}
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    // ===== Helpers =====

    private static byte[] readAll(InputStream is) throws Exception {
        if (is == null) throw new IllegalArgumentException("InputStream nulo");
        try (InputStream in = new BufferedInputStream(is);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) bos.write(buf, 0, len);
            return bos.toByteArray();
        }
    }

    private static String readString(InputStream is) throws Exception {
        byte[] bytes = readAll(is);
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }
}