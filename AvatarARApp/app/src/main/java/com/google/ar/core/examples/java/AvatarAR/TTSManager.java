package com.google.ar.core.examples.java.AvatarAR;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import java.util.Locale;

public class TTSManager {
    private TextToSpeech tts;
    private boolean isReady = false;

    public TTSManager(Context context) {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                // Configura o idioma para Português Brasil
                // Ajusta a velocidade e o tom para soar mais humano
                tts.setPitch(1.0f);    // 1.0 é o normal. Mais baixo (0.8) fica mais grave.
                tts.setSpeechRate(1.1f); // Um pouco mais rápido soa menos robótico
                int result = tts.setLanguage(new Locale("pt", "BR"));
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS_DEBUG", "Idioma não suportado ou faltando dados");
                } else {
                    isReady = true;
                }
            } else {
                Log.e("TTS_DEBUG", "Falha na inicialização do TTS");
            }
        });
    }

    public void falar(String texto) {
        if (isReady && tts != null) {
            // QUEUE_FLUSH interrompe a fala anterior para começar a nova
            tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "AvatarTalkID");
        }
    }

    public void parar() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}