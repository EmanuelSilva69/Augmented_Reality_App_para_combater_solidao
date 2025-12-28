package com.google.ar.core.examples.java.AvatarAR;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.speech.tts.TextToSpeech;
import java.util.Locale;



// Lógica de Negócio e Sessão
import com.google.ar.core.Session;
import com.google.ar.core.examples.java.AvatarAR.common.helpers.CameraPermissionHelper;
import com.google.ar.core.examples.java.AvatarAR.common.helpers.DepthSettings;
import com.google.ar.core.examples.java.AvatarAR.common.helpers.InstantPlacementSettings;

// SceneView e Autoanimate
import io.github.sceneview.ar.ArSceneView;
import io.github.sceneview.ar.node.ArModelNode;
import io.github.sceneview.ar.arcore.ArFrame;

import java.util.Random;

public class AvatarArActivity extends AppCompatActivity {

  private EditText editChatMessage; // [cite: 14]
  private Button btnSendChat; // [cite: 15]
  private OllamaManager ollamaManager; // [cite: 15]
  private TTSManager ttsManager;
  private Session session; // [cite: 22]
  private ArSceneView sceneView;
  private ArModelNode avatarNode;
  private android.os.Handler idleHandler = new android.os.Handler();
  private Runnable idleRunnable;
  private final DepthSettings depthSettings = new DepthSettings(); // [cite: 24]
  private final InstantPlacementSettings instantPlacementSettings = new InstantPlacementSettings(); // [cite: 25]
  private TextToSpeech tts;
  private GeminiManager geminiManager;
  private boolean usarGemini = false; // Toggle para o usuário escolher

  // Lista de animações de fala (Talking)
// Estas animações de dança serão o teu novo "Idle" (o que ele faz enquanto espera)
  private String[] modelosDancaIdle = {
          "animations/dancing.glb",
          "animations/dancinghiphop.glb",
          "animations/dancinghiphop2.glb",
          "animations/dancingjazz.glb",
          "animations/dancingsalsa.glb",
          "animations/dancingsamba.glb"
  };
  private int ultimaDancaIndex = -1;
  // Estas são as animações para quando o Ollama responder (Talking)
  private String[] modelosTalking = {
          "animations/talking.glb",
          "animations/talkingwalking.glb"
  };

  // Animação especial para erros ou finalização
  private String modeloMorte = "animations/morte.glb";
  private void atualizarEstadoAvatar(String path) {
    if (avatarNode != null) {
      avatarNode.loadModelGlbAsync(
              path, true, 0.7f, null, null,
              instance -> {
                Log.d("AvatarAR", "Estado atualizado: " + path);
                return kotlin.Unit.INSTANCE;
              }
      );
    }
  }
  private void lidarComErroIA(String error) {
    runOnUiThread(() -> {
      // 1. Log do erro para depuração no Android Studio
      Log.e("AvatarAR_Error", "Erro na IA: " + error);

      // 2. Para qualquer agendamento de dança ativo para focar no erro
      idleHandler.removeCallbacks(idleRunnable);

      // 3. Feedback visual: Carrega o modelo de "morte" ou erro
      // Este é o ficheiro "animations/morrendo.glb" que definiu anteriormente
      atualizarEstadoAvatar(modeloMorte);

      // 4. Feedback textual para o utilizador
      Toast.makeText(AvatarArActivity.this, "Erro: " + error, Toast.LENGTH_LONG).show();

      // 5. Recuperação Automática:
      // Após 8 segundos, tenta voltar a dançar (estado Idle) para o app não ficar "morto"
      idleHandler.postDelayed(idleRunnable, 8000);

      Log.d("AvatarAR", "Avatar em estado de erro. Recuperação agendada para 8s.");
    });
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main); // [cite: 42]

    // Inicialização do Chat e Ollama
    editChatMessage = findViewById(R.id.edit_chat_message);
    btnSendChat = findViewById(R.id.btn_send_chat);
    ollamaManager = new OllamaManager();
    geminiManager = new GeminiManager();

// 2. Configuração do SceneView e Node
    sceneView = findViewById(R.id.sceneView);
    avatarNode = new ArModelNode(sceneView.getEngine());
    avatarNode.setEditable(true);
    sceneView.addChild(avatarNode);
    // Inicia carregando o primeiro modelo de dança (ou um aleatório da lista de animações)
    idleRunnable = new Runnable() {
      @Override
      public void run() {
        int novoIndex = new Random().nextInt(modelosDancaIdle.length);;
        Log.d("AvatarAR", "Sorteado: " + novoIndex + " Arquivo: " + modelosDancaIdle[novoIndex]);
        // Garante que a próxima dança seja diferente da atual
        ultimaDancaIndex = novoIndex;
        atualizarEstadoAvatar(modelosDancaIdle[novoIndex]);


        // Agenda a próxima troca (15-30 segundos)
        int proximoTempo = new Random().nextInt(10001) + 10000;
        idleHandler.postDelayed(this, proximoTempo);
      }
    };
    idleHandler.post(idleRunnable);
    // 3. TERCEIRO: Configuramos os botões usando o Runnable centralizado
    com.google.android.material.floatingactionbutton.FloatingActionButton fabAnim =
            findViewById(R.id.fab_change_animation);

    fabAnim.setOnClickListener(v -> {
      idleHandler.removeCallbacks(idleRunnable); // Reseta o timer
      idleRunnable.run(); // Executa a lógica de sorteio centralizada
      Toast.makeText(this, "Mudando o ritmo...", Toast.LENGTH_SHORT).show();
    });
    btnSendChat.setOnClickListener(v -> {
      String msg = editChatMessage.getText().toString().trim();
      if (!msg.isEmpty()) {
        idleHandler.removeCallbacks(idleRunnable);
        editChatMessage.setText(""); //
        enviarPergunta(msg); //
      }
    });

    // Posicionamento via toque
    sceneView.setOnClickListener(v -> {
      // No ArSceneView, o anchor() sem parâmetros tenta fixar o nó
      // no centro da visão ou no último plano detectado [cite: 13, 148]
      if (avatarNode != null) {
        avatarNode.anchor();
        Toast.makeText(this, "Avatar fixado no ambiente!", Toast.LENGTH_SHORT).show(); // [cite: 44]
      }
    });

    // Configurações e Menus
    depthSettings.onCreate(this);
    instantPlacementSettings.onCreate(this);

    ImageButton settingsButton = findViewById(R.id.settings_button);
    settingsButton.setOnClickListener(v -> {
      PopupMenu popup = new PopupMenu(this, v);
      popup.setOnMenuItemClickListener(this::settingsMenuClick);
      popup.inflate(R.menu.settings_menu);
      popup.show();
    });


// Inicia o ciclo de dança pela primeira vez

    ttsManager = new TTSManager(this);

  }



  // Lógica de Ollama
  private void enviarPergunta(String pergunta) {
    idleHandler.removeCallbacks(idleRunnable);

    if (usarGemini) {
      // Lógica com Google Gemini
      geminiManager.perguntar(pergunta, new GeminiManager.GeminiCallback() {
        @Override
        public void onResponse(String text) {
          processarRespostaIA(text);
        }

        @Override
        public void onError(String error) {
          lidarComErroIA(error);
        }
      });
    } else {
      // Lógica antiga com Ollama
      ollamaManager.askAvatar(pergunta, new OllamaManager.OllamaCallback() {
        @Override
        public void onResponse(String text) {
          processarRespostaIA(text);
        }

        @Override
        public void onError(String error) {
          lidarComErroIA(error);
        }
      });
    }
  }

  // Método auxiliar para não repetir código de animação
  private void processarRespostaIA(String text) {
    runOnUiThread(() -> {
      Toast.makeText(AvatarArActivity.this, "IA: " + text, Toast.LENGTH_LONG).show();
      int index = new Random().nextInt(modelosTalking.length);
      atualizarEstadoAvatar(modelosTalking[index]);
      ttsManager.falar(text);
      int delayVoltaDanca = Math.max(13000, text.length() * 100);
      idleHandler.postDelayed(idleRunnable, delayVoltaDanca);
    });
  }

  // Permissões e Sessão
  @Override
  protected void onResume() {
    super.onResume();
    if (!CameraPermissionHelper.hasCameraPermission(this)) {
      CameraPermissionHelper.requestCameraPermission(this); //
      return;
    }
  }

  @Override
  protected void onDestroy() {
    if (session != null) { session.close(); }
    if (ttsManager != null) {
      ttsManager.parar();
    }
    super.onDestroy();
  }

  protected boolean settingsMenuClick(MenuItem item) {
    if (item.getItemId() == R.id.config_gemini) {
      // Criar um AlertDialog com um EditText para o usuário colar a chave
      android.widget.EditText input = new android.widget.EditText(this);
      new android.app.AlertDialog.Builder(this)
              .setTitle("Configurar Gemini")
              .setMessage("Cole sua chave de API do Google AI Studio:")
              .setView(input)
              .setPositiveButton("Salvar", (dialog, which) -> {
                String key = input.getText().toString();
                geminiManager.setApiKey(key);
                usarGemini = true; // Ativa o Gemini automaticamente ao salvar a chave
                Toast.makeText(this, "Gemini Ativado!", Toast.LENGTH_SHORT).show();
              })
              .setNegativeButton("Cancelar", null)
              .show();
      return true;
    }
    if (item.getItemId() == R.id.avatar_choice_1) {
      idleHandler.removeCallbacks(idleRunnable);
      idleRunnable.run(); // Usa a lógica centralizada
      return true;
    }
    return false;
  }




}