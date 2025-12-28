package com.google.ar.core.examples.java.AvatarAR;

import android.util.Log;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;

public class GeminiManager {
    private final OkHttpClient client = new OkHttpClient();
    private String userApiKey = "AIzaSyBRq98wBAOZ-Klvknpwe3hcWN6X76J-ez8gemgemi";

    public interface GeminiCallback {
        void onResponse(String text);
        void onError(String error);
    }

    public void setApiKey(String key) {
        this.userApiKey = key;
    }

    public void perguntar(String pergunta, GeminiCallback callback) {
        if (userApiKey.isEmpty()) {
            callback.onError("Chave de API não configurada!");
            return;
        }

        // Endpoint para o modelo Gemini 1.5 Flash (mais rápido e barato/grátis)
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key=" + userApiKey;

        // Montando o JSON conforme a documentação do Google
        try {
            JSONObject jsonBody = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject parts = new JSONObject();
            parts.put("text", "Responda de forma curta (máximo 2 frases) para um chat de Realidade Aumentada: " + pergunta);

            JSONObject contentObj = new JSONObject();
            contentObj.put("parts", new JSONArray().put(parts));
            contents.put(contentObj);
            jsonBody.put("contents", contents);

            RequestBody body = RequestBody.create(
                    jsonBody.toString(),
                    MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder().url(url).post(body).build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError(e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            JSONObject resJson = new JSONObject(response.body().string());
                            String resposta = resJson.getJSONArray("candidates")
                                    .getJSONObject(0)
                                    .getJSONObject("content")
                                    .getJSONArray("parts")
                                    .getJSONObject(0)
                                    .getString("text");
                            callback.onResponse(resposta.trim());
                        } catch (Exception e) {
                            callback.onError("Erro ao processar resposta do Google");
                        }
                    } else {
                        callback.onError("Erro na API: " + response.code());
                    }
                }
            });
        } catch (Exception e) {
            callback.onError("Erro na requisição: " + e.getMessage());
        }
    }
}