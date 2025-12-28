package com.google.ar.core.examples.java.AvatarAR;

import android.util.Log;
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class OllamaManager {
    private static final String TAG = "OllamaManager";

    // IMPORTANTE: Substitua pelo IP do seu PC (visto no comando ipconfig do Windows)
    // Se estiver usando o emulador, tente 10.0.2.2 (que aponta para o localhost do PC)
    private static final String PC_IP = "10.0.2.2";
    private static final String OLLAMA_URL = "http://" + PC_IP + ":11434/api/generate";

    private final OkHttpClient client;

    public OllamaManager() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    public interface OllamaCallback {
        void onResponse(String text);
        void onError(String error);
    }


    /**
     * Envia a pergunta para o Ollama e recebe a resposta completa.
     */
    public void askAvatar(String prompt, OllamaCallback callback) {
        try {
            JSONObject json = new JSONObject();
            // CERTIFIQUE-SE: Digite 'ollama list' no terminal.
            // Se aparecer llama3:latest, mude para "llama3:latest" aqui.
            json.put("system", "Você é um avatar assistente em Realidade Aumentada. Responda de forma curta e amigável em português.");
            json.put("model", "qwen2.5:3b");
            json.put("prompt", prompt);
            json.put("stream", false);

            RequestBody body = RequestBody.create(
                    json.toString(), MediaType.get("application/json; charset=utf-8"));

            Request request = new Request.Builder()
                    .url(OLLAMA_URL)
                    .post(body)
                    .header("Content-Type", "application/json") // Adicione este header explicitamente
                    .build();

            Log.d(TAG, "Tentando conectar em: " + OLLAMA_URL);

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    // Se cair aqui, o Logcat vai mostrar o motivo exato
                    Log.e(TAG, "ERRO CRÍTICO NA CONEXÃO: " + e.getMessage(), e);
                    callback.onError(e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String bodyStr = response.body().string();
                    Log.d(TAG, "Resposta bruta do servidor: " + bodyStr);

                    if (!response.isSuccessful()) {
                        callback.onError("Erro Servidor: " + response.code());
                        return;
                    }

                    try {
                        JSONObject resJson = new JSONObject(bodyStr);
                        callback.onResponse(resJson.getString("response"));
                    } catch (Exception e) {
                        callback.onError("Erro no JSON");
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Erro ao montar JSON: " + e.getMessage());
        }
    }

}