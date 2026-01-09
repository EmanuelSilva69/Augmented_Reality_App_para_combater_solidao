package com.google.ar.core.examples.java.AvatarAR;
// Usando o https://github.com/SceneView/sceneview-android !! (melhor q o arcore puro em minha opinião)
// Animações do https://www.mixamo.com/#/
// O resto fiz no Blender (rigging das animações com o avatar e tal. Mas é meio difícil ent só fiz com 1)

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

// Lógica de Negócio e Sessão
import com.google.ar.core.Session;
import com.google.ar.core.examples.java.AvatarAR.common.helpers.CameraPermissionHelper;
import com.google.ar.core.examples.java.AvatarAR.common.helpers.DepthSettings;
import com.google.ar.core.examples.java.AvatarAR.common.helpers.InstantPlacementSettings;

// SceneView Imports
import io.github.sceneview.ar.ArSceneView;
import io.github.sceneview.ar.node.ArModelNode;
import io.github.sceneview.node.Node;
import io.github.sceneview.node.ModelNode;
// Imports Matemáticos (para substituir o Position e Scale)
import dev.romainguy.kotlin.math.Float3;
import java.util.List;
import java.util.ArrayList;

public class AvatarArActivity extends AppCompatActivity {

  private EditText editChatMessage;
  private Button btnSendChat;
  private OllamaManager ollamaManager;
  private TTSManager ttsManager;
  private Session session;
  private ArSceneView sceneView;

  // --- CORREÇÃO: Mapa de Cache e Nó Atual ---
  // Mapa para guardar todos os modelos carregados na memória
  private java.util.Map<String, ArModelNode> avatarCache = new java.util.HashMap<>();
  private java.util.Map<ModelNode, Float3> posicoesCenario = new java.util.HashMap<>();
  // Referência para saber qual boneco está visível no momento
  private ArModelNode avatarNodeAtual = null;
  // Lista para controlar os objetos do palco
  private List<ModelNode> objetosCenario = new ArrayList<>();
  private android.os.Handler idleHandler = new android.os.Handler();
  private Runnable idleRunnable;
  private final DepthSettings depthSettings = new DepthSettings();
  private Node cameraRoot;
  private final InstantPlacementSettings instantPlacementSettings = new InstantPlacementSettings();
  private GeminiManager geminiManager;
  private boolean usarGemini = false;

  // --- Listas de Arquivos ---
  private String[] modelosDancaIdle = {
          "animations/avatar1/dancing.glb",
          "animations/avatar1/dancinghiphop.glb",
          "animations/avatar1/dancinghiphop2.glb",
          "animations/avatar1/dancingjazz.glb",
          "animations/avatar1/dancingsalsa.glb",
          "animations/avatar1/dancingsamba.glb"
  };
  private int ultimaDancaIndex = -1;

  private String[] modelosTalking = {
          "animations/avatar1/talking.glb",
          "animations/avatar1/talkingwalking.glb"
  };

  private String modeloMorte = "animations/avatar1/morte.glb";

  // --- MÉTODO DE TROCA DE ESTADO (CACHE) ---
  private void atualizarEstadoAvatar(String path) {
    ArModelNode novoNode = avatarCache.get(path);
    if (novoNode == null) return;

    if (novoNode == avatarNodeAtual) return;

    // Esconde anterior
    if (avatarNodeAtual != null) {
      avatarNodeAtual.setVisible(false);
    }

    avatarNodeAtual = novoNode;

    // 🔥 PASSO CRÍTICO:
    // Remove de QUALQUER lugar
    avatarNodeAtual.setParent(null);

    // 🔥 GARANTE que está na mesma gaiola do palco
    if (cameraRoot != null) {
      avatarNodeAtual.detachAnchor();
      avatarNodeAtual.setPlacementMode(
              io.github.sceneview.ar.node.PlacementMode.DISABLED
      );
      cameraRoot.addChild(avatarNodeAtual);
    }

    // Reset local
    avatarNodeAtual.setPosition(new Float3(0f, 0f, 0f));
    avatarNodeAtual.setRotation(new Float3(0f, 0f, 0f));

    avatarNodeAtual.setVisible(true);
  }

  private void lidarComErroIA(String error) {
    runOnUiThread(() -> {
      Log.e("AvatarAR_Error", "Erro na IA: " + error);
      idleHandler.removeCallbacks(idleRunnable);
      atualizarEstadoAvatar(modeloMorte);
      Toast.makeText(AvatarArActivity.this, "Erro: " + error, Toast.LENGTH_LONG).show();
      idleHandler.postDelayed(idleRunnable, 8000);
    });
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // Inicialização UI e Managers
    editChatMessage = findViewById(R.id.edit_chat_message);
    btnSendChat = findViewById(R.id.btn_send_chat);
    ollamaManager = new OllamaManager();
    geminiManager = new GeminiManager();
    ttsManager = new TTSManager(this); // Inicializa aqui para garantir

    // Configuração do SceneView
    sceneView = findViewById(R.id.sceneView);
    sceneView.getPlaneRenderer().setEnabled(false); // Some com os pontos brancos

    // -----------------------------------------------------------------------
    //  A "Gaiola" (cameraRoot) deve nascer AQUI
    // -----------------------------------------------------------------------
    // Se você chamar montarPalco antes disso, o app CRASHA porque cameraRoot é null.

    cameraRoot = new io.github.sceneview.node.Node(sceneView.getEngine());

    // Posição: 4 metros na frente (-4.0f), um pouco abaixo (-0.8f)
    cameraRoot.setPosition(new dev.romainguy.kotlin.math.Float3(0.0f, -0.8f, -4.0f));
    cameraRoot.setScale(new dev.romainguy.kotlin.math.Float3(1.4f, 1.4f, 1.4f));

    // Prende na câmera imediatamente
    sceneView.getCameraNode().addChild(cameraRoot);

    // Configurações visuais do SceneView (Remove bolinhas e mãozinha)
    sceneView.getPlaneRenderer().setEnabled(false);



    // -----------------------------------------------------------------------
    // 2. AGORA SIM: Montar o Palco (Porque a gaiola já existe)
    // -----------------------------------------------------------------------
    montarPalco();
    // LISTA MESTRA DE TODOS OS ARQUIVOS
    String[] todosModelos = {
            //"animations/avatar1/dancing.glb",
            "animations/avatar1/dancinghiphop.glb",
            "animations/avatar1/dancinghiphop2.glb",
            "animations/avatar1/dancingjazz.glb",
            "animations/avatar1/dancingsalsa.glb",
            "animations/avatar1/dancingsamba.glb",
            "animations/avatar1/talking.glb",
            //"animations/avatar1/talkingwalking.glb",
            //"animations/avatar1/morte.glb"
    };

    Toast.makeText(this, "Carregando avatares...", Toast.LENGTH_SHORT).show();

    // --- LOOP DE PRÉ-CARREGAMENTO CORRIGIDO ---
    for (String path : todosModelos) {
      // 1. Criamos um nó temporário
            ArModelNode node = new ArModelNode(sceneView.getEngine());


      // O modo BEST_AVAILABLE coloca o objeto imediatamente, sem esperar detectar o chão perfeito.
            node.setPlacementMode(io.github.sceneview.ar.node.PlacementMode.DISABLED);


      node.setEditable(true);
      // 3. Carregamos o modelo neste nó
      node.loadModelGlbAsync(
              path,
              true, // autoAnimate
              0.7f, // scale
              null, // center position
              null,
              instance -> {
                // Assim que carregar, esconde
                node.setVisible(false);
                return kotlin.Unit.INSTANCE;
              }
      );

      // 4. Adicionamos à cena e ao Cache
      sceneView.addChild(node);
      avatarCache.put(path, node); // Agora sim guardamos no Map
    }


    // --- INICIALIZAÇÃO DO CICLO DE DANÇA ---
    idleRunnable = new Runnable() {
      @Override
      public void run() {
        int novoIndex;
        // Evita repetir a mesma dança
        do {
          novoIndex = new Random().nextInt(modelosDancaIdle.length);
        } while (novoIndex == ultimaDancaIndex && modelosDancaIdle.length > 1);

        ultimaDancaIndex = novoIndex;

        Log.d("AvatarAR", "Sorteado: " + novoIndex);
        atualizarEstadoAvatar(modelosDancaIdle[novoIndex]); // Usa o método de cache

        // Agenda próxima troca (10 a 20 segundos)
        int proximoTempo = new Random().nextInt(10001) + 10000;
        idleHandler.postDelayed(this, proximoTempo);
      }
    };

    // Inicia o ciclo após 2.5 segundos (Dando tempo para o loop acima carregar os arquivos)
    // --- START AUTOMÁTICO DO SHOW (SUBSTITUA O SEU BLOCO DE 2.5s POR ESTE) ---
    // --- POSICIONAMENTO FIXO (Sem Âncoras / Sem Chão) ---
    // --- MODO ESTÚDIO: VISÍVEL E DISTANTE ---
    // --- MODO ESTÚDIO (FIXO NA TELA) ---
    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {

      Toast.makeText(this, "Modo Estúdio (4 Metros)...", Toast.LENGTH_SHORT).show();

      try {
        // =========================================================
        // 1. CRIAR A GAIOLA (CAMERA ROOT) - PRIMEIRO DE TUDO!
        // =========================================================
       /* if (cameraRoot != null && cameraRoot.getParent() != null) {
          cameraRoot.getParent().removeChild(cameraRoot);
        }

        // Cria o nó
        cameraRoot = new io.github.sceneview.node.Node(sceneView.getEngine());

        // --- AJUSTE DE DISTÂNCIA (ANTI-CLIPPING) ---
        // Z = -4.0f (4 metros longe. Resolve o problema de estar "dentro" do usuário)
        // Y = -1.0f (Mais baixo para compensar a distância)
        cameraRoot.setPosition(new dev.romainguy.kotlin.math.Float3(0.0f, -1.0f, -4.0f));

        // Escala do Mundo (0.8x tamanho real)
        cameraRoot.setScale(new dev.romainguy.kotlin.math.Float3(1f, 1f, 1f));

        // Prende na Câmera
        sceneView.getCameraNode().addChild(cameraRoot);
        */ //fiz lá em cima, removi aqui( Meti o louco pra fazer isso funfar)

        // =========================================================
        // 2. AGORA SIM: MOSTRAR O AVATAR
        // =========================================================
        // Como o cameraRoot já existe, esse método vai funcionar e adicionar o boneco nele


// =========================================================
        // 3. MOVER O PALCO (GARANTIA FINAL)
        // =========================================================
        // Altere o tipo para 'io.github.sceneview.node.Node' para ser genérico
        for (io.github.sceneview.node.Node obj : objetosCenario) {

          // Checagem de segurança
          if (obj == null) continue;

          // Garante que está na gaiola (caso tenha escapado)
          if (obj.getParent() != cameraRoot) {
            obj.setParent(null);
            cameraRoot.addChild(obj);
          }

          // Reaplica posição salva (só para garantir que não moveu)
          dev.romainguy.kotlin.math.Float3 offset = posicoesCenario.get(obj);
          if (offset != null) {
            obj.setPosition(offset);
          }

          // Força visibilidade
          obj.setVisible(true);
        }

        Log.d("AvatarAR", "Cenário verificado.");

        Log.d("AvatarAR", "Cenário montado em Z=-4.0m");

      } catch (Exception e) {
        Log.e("AvatarAR", "Erro crítico: " + e.getMessage());
        e.printStackTrace();
      }

      // Inicia loop de dança
      idleHandler.postDelayed(idleRunnable, 10000);

    }, 1000);// Executa isso 2.5 segundos depois de abrir o app


    // --- CONFIGURAÇÃO DOS BOTÕES ---
    com.google.android.material.floatingactionbutton.FloatingActionButton fabAnim =
            findViewById(R.id.fab_change_animation);

    fabAnim.setOnClickListener(v -> {
      idleHandler.removeCallbacks(idleRunnable);
      idleRunnable.run();
      Toast.makeText(this, "Trocando...", Toast.LENGTH_SHORT).show();
    });

    btnSendChat.setOnClickListener(v -> {
      String msg = editChatMessage.getText().toString().trim();
      if (!msg.isEmpty()) {
        idleHandler.removeCallbacks(idleRunnable);
        editChatMessage.setText("");
        enviarPergunta(msg);
      }
    });

    // Toque para Ancorar (Usa a variável avatarNodeAtual)

    /*sceneView.setOnClickListener(v -> {

      Log.d("AvatarAR", "Clique detectado!");

      // 1. Verifica se o avatar já carregou
      if (avatarNodeAtual == null) {
        Toast.makeText(this, "Aguarde o avatar carregar...", Toast.LENGTH_SHORT).show();
        return;
      }

      // 2. Evita re-ancorar se já estiver travado
      if (avatarNodeAtual.isAnchored()) {
        Toast.makeText(this, "Já está travado! Reinicie o app para mover.", Toast.LENGTH_SHORT).show();
        return;
      }

      // 3. Tenta ancorar (CORREÇÃO AQUI: Não salvamos em boolean)
      try {
        avatarNodeAtual.anchor(); // Apenas executa o comando
      } catch (Exception e) {
        Toast.makeText(this, "Erro técnico ao ancorar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        return;
      }

      // 4. Verifica se funcionou pegando a âncora
      com.google.ar.core.Anchor ancoraMestra = avatarNodeAtual.getAnchor();

      if (ancoraMestra == null) {
        Toast.makeText(this, "Mire no chão (pontinhos) e tente de novo.", Toast.LENGTH_SHORT).show();
        return;
      }

      Toast.makeText(this, "Travou! Montando palco...", Toast.LENGTH_SHORT).show();

      // 5. Inicia o Palco com atraso de segurança (Handler)
      palcoHandler.postDelayed(() -> {
        // Se o app estiver fechando, cancela tudo
        if (isFinishing() || isDestroyed()) return;

        try {
          int qtd = 0;
          for (ArModelNode obj : objetosCenario) {
            if (obj != null) {
              // Traz o objeto para a mesma âncora do avatar
              obj.setAnchor(ancoraMestra);

              // Ajusta posição
              dev.romainguy.kotlin.math.Float3 pos = posicoesCenario.get(obj);
              if (pos != null) obj.setPosition(pos);

              // Mostra
              obj.setVisible(true);
              qtd++;
            }
          }
          Toast.makeText(this, "Palco Pronto! (" + qtd + " objetos)", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
          Log.e("AvatarAR", "Erro palco: " + e.getMessage());
        }
      }, 500); // 0.5 segundos de espera
    });*/

    // Configurações finais
    depthSettings.onCreate(this);
    instantPlacementSettings.onCreate(this);

    ImageButton settingsButton = findViewById(R.id.settings_button);
    settingsButton.setOnClickListener(v -> {
      PopupMenu popup = new PopupMenu(this, v);
      popup.setOnMenuItemClickListener(this::settingsMenuClick);
      popup.inflate(R.menu.settings_menu);
      popup.show();
    });
  }

  // --- MÉTODOS DE IA ---
  private void enviarPergunta(String pergunta) {
    idleHandler.removeCallbacks(idleRunnable);
    if (usarGemini) {
      geminiManager.perguntar(pergunta, new GeminiManager.GeminiCallback() {
        @Override
        public void onResponse(String text) { processarRespostaIA(text); }
        @Override
        public void onError(String error) { lidarComErroIA(error); }
      });
    } else {
      ollamaManager.askAvatar(pergunta, new OllamaManager.OllamaCallback() {
        @Override
        public void onResponse(String text) { processarRespostaIA(text); }
        @Override
        public void onError(String error) { lidarComErroIA(error); }
      });
    }
  }

  private void processarRespostaIA(String text) {
    runOnUiThread(() -> {
      Toast.makeText(AvatarArActivity.this, "IA: " + text, Toast.LENGTH_LONG).show();
      int index = new Random().nextInt(modelosTalking.length);

      atualizarEstadoAvatar(modelosTalking[index]); // Usa cache

      if (ttsManager != null) ttsManager.falar(text);

      int delayVoltaDanca = Math.max(13000, text.length() * 100);
      idleHandler.postDelayed(idleRunnable, delayVoltaDanca);
    });
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (!CameraPermissionHelper.hasCameraPermission(this)) {
      CameraPermissionHelper.requestCameraPermission(this);
    }
  }

  @Override
  protected void onDestroy() {
    if (session != null) { session.close(); }
    if (ttsManager != null) { ttsManager.parar(); }
    super.onDestroy();
  }

  protected boolean settingsMenuClick(MenuItem item) {
    if (item.getItemId() == R.id.config_gemini) {
      android.widget.EditText input = new android.widget.EditText(this);
      new android.app.AlertDialog.Builder(this)
              .setTitle("Configurar Gemini")
              .setMessage("Cole sua chave de API:")
              .setView(input)
              .setPositiveButton("Salvar", (dialog, which) -> {
                geminiManager.setApiKey(input.getText().toString());
                usarGemini = true;
                Toast.makeText(this, "Gemini Ativado!", Toast.LENGTH_SHORT).show();
              })
              .setNegativeButton("Cancelar", null)
              .show();
      return true;
    }
    if (item.getItemId() == R.id.avatar_choice_1) {
      idleHandler.removeCallbacks(idleRunnable);
      idleRunnable.run();
      return true;
    }
    return false;
  }
// Montagem de palco
  private void carregarObjetoOriginal(String glbPath, dev.romainguy.kotlin.math.Float3 posicao, dev.romainguy.kotlin.math.Float3 escala, dev.romainguy.kotlin.math.Float3 rotacao) {
    ModelNode node = new ModelNode(sceneView.getEngine());
    node.setEditable(false);
    node.setScale(escala);
    node.setRotation(rotacao);
    node.loadModelGlbAsync(glbPath, true, 1.0f, null, null, instance -> {

      // --- AQUI ESTÁ A CORREÇÃO ---
      // Se tiver animação, damos play direto pelo Animator do Filament
      // O 'instance' é o objeto carregado.
      if (instance instanceof com.google.android.filament.gltfio.FilamentInstance) {
        com.google.android.filament.gltfio.FilamentInstance filamentInstance = (com.google.android.filament.gltfio.FilamentInstance) instance;

        // O Animator controla as animações
        com.google.android.filament.gltfio.Animator animator = filamentInstance.getAnimator();

        // Se existir um animator e tiver pelo menos 1 animação...
        if (animator != null && animator.getAnimationCount() > 0) {
          // Aplica a animação de índice 0
          animator.applyAnimation(0, 0.0f); // Prepara
          animator.updateBoneMatrices();

          // NOTA: O SceneView geralmente toca sozinho se 'autoAnimate' for true no loadModelGlbAsync.
          // Mas se não tocar, o ArModelNode tem um método helper:
          node.playAnimation(0, true);
        }
      } else {
        // Tenta o método genérico do nó, se disponível
        try {
          node.playAnimation(0, true);
        } catch (Exception e) {
          // Se der erro, é porque não tem animação, ignora.
        }
      }

      return kotlin.Unit.INSTANCE;
    });
    node.setPosition(posicao);

    // Adiciona na gaiola
    cameraRoot.addChild(node);
    node.setVisible(false);
    posicoesCenario.put(node, posicao);
    //sceneView.addChild(node);
    objetosCenario.add(node);
  }

  // --- MÉTODO 2: APENAS PARA AS LUZES (Cubos que pintamos) ---
  private void criarLuzNeon(dev.romainguy.kotlin.math.Float3 posicao, float[] cor) {
    ModelNode node = new ModelNode(sceneView.getEngine());
    node.setEditable(false);

    // Escala fixa pequena para as luzes
    node.setScale(new dev.romainguy.kotlin.math.Float3(0.15f, 0.15f, 0.15f));

    // Usa o cubo simples
    node.loadModelGlbAsync("animations/pistadanca/cube.glb", true, 1.0f, null, null, instance -> {
      if (instance instanceof com.google.android.filament.gltfio.FilamentInstance) {
        com.google.android.filament.gltfio.FilamentInstance fi = (com.google.android.filament.gltfio.FilamentInstance) instance;
        for (int i = 0; i < fi.getMaterialInstances().length; i++) {
          com.google.android.filament.MaterialInstance mat = fi.getMaterialInstances()[i];
          // Pinta de Neon
          mat.setParameter("baseColorFactor", cor[0], cor[1], cor[2], cor[3]);
          mat.setParameter("emissiveFactor", cor[0]*5.0f, cor[1]*5.0f, cor[2]*5.0f);
        }
      }
      return kotlin.Unit.INSTANCE;
    });
    cameraRoot.addChild(node);
    node.setVisible(false);
    posicoesCenario.put(node, posicao);
    //sceneView.addChild(node);
    objetosCenario.add(node);
  }
  // Método auxiliar que cria o nó, define escala e guarda a posição futura
  private void montarPalco() {
    objetosCenario.clear();
    posicoesCenario.clear();

    // Cores
    float[] luzAmarela = {1.0f, 1.0f, 0.0f, 1.0f};
    float[] luzCiano   = {0.0f, 1.0f, 1.0f, 1.0f};
    float[] luzRoxa    = {0.8f, 0.0f, 1.0f, 1.0f};

    // --- 1. PISTA DE DANÇA ---
    // Rotação 0,0,0 (Padrão)
    carregarObjetoOriginal("animations/pistadanca/animated_dance_floor_neon_lights.glb",
            new dev.romainguy.kotlin.math.Float3(0.0f, -0.05f, 0.0f),
            new dev.romainguy.kotlin.math.Float3(1.8f, 1.8f, 1.8f),
            new dev.romainguy.kotlin.math.Float3(0.0f, 0.0f, 0.0f));

    // --- 2. CAIXAS DE SOM (PEQUENAS) ---
    // Aqui giramos 180 graus no Y (meio) para elas olharem para frente
    // Se ficarem de costas, tente 0.0f. Se ficarem de lado, tente 90.0f.
    carregarObjetoOriginal("animations/pistadanca/mini_sound_box.glb",
            new dev.romainguy.kotlin.math.Float3(-0.6f, 0.0f, 0.6f),
            new dev.romainguy.kotlin.math.Float3(0.1f, 0.1f, 0.1f),
            new dev.romainguy.kotlin.math.Float3(0.0f, 90.0f, 0.0f));

    carregarObjetoOriginal("animations/pistadanca/mini_sound_box.glb",
            new dev.romainguy.kotlin.math.Float3(0.6f, 0.05f, 0.6f),
            new dev.romainguy.kotlin.math.Float3(0.2f, 0.2f, 0.2f),
            new dev.romainguy.kotlin.math.Float3(0.0f, 90.0f, 0.0f));

    // --- 3. CAIXAS GRANDES (FUNDO) ---
    // Girando levemente para o centro (45 graus e -45 graus) para dar estilo
    carregarObjetoOriginal("animations/pistadanca/sound_box.glb",
            new dev.romainguy.kotlin.math.Float3(-0.5f, 0.0f, -0.3f),
            new dev.romainguy.kotlin.math.Float3(0.5f, 0.5f, 0.5f),
            new dev.romainguy.kotlin.math.Float3(0.0f, 0.0f, 0.0f));

    carregarObjetoOriginal("animations/pistadanca/lowpoly_audio_speaker.glb",
            new dev.romainguy.kotlin.math.Float3(0.5f, 0.0f, -0.3f),
            new dev.romainguy.kotlin.math.Float3(0.5f, 0.5f, 0.5f),
            new dev.romainguy.kotlin.math.Float3(0.0f, 0.0f, 0.0f));

    // --- 4. GLOBO ---
    // Rotação 0 (Globo é redondo, tanto faz)
    carregarObjetoOriginal("animations/pistadanca/free_realistic_disco_ball.glb",
            new dev.romainguy.kotlin.math.Float3(0.0f, 2f, 0.0f),
            new dev.romainguy.kotlin.math.Float3(0.8f, 0.8f, 0.8f),
            new dev.romainguy.kotlin.math.Float3(0.0f, 0.0f, 0.0f));

    // --- 5. LUZES ALEATÓRIAS (CHUVA DE NEON) ---
    Random random = new Random();

    // Vamos criar 10 luzes espalhadas
    for (int i = 0; i < 4; i++) {

      // Gera X entre -0.8 e 0.8
      float randomX, randomZ;
      // 1. Sorteia Posição no Chão (X e Z), fugindo do centro
      do {
        randomX = (random.nextFloat() * 1.6f) - 0.8f;
        randomZ = (random.nextFloat() * 1.6f) - 0.8f;
      } while (Math.abs(randomX) < 0.4f && Math.abs(randomZ) < 0.4f);

      // 2. Sorteia Altura (Y) - Efeito Flutuante
      // Gera um número entre 0.0 (chão) e 0.4 (altura do joelho)
      float randomY = random.nextFloat() * 0.4f;
      // Escolhe cor baseada no número (par, impar ou divisível por 3)
      float[] corEscolhida;
      if (i % 3 == 0) corEscolhida = luzAmarela;
      else if (i % 3 == 1) corEscolhida = luzCiano;
      else corEscolhida = luzRoxa;

      // Cria a luz na posição sorteada
      criarLuzNeon(new dev.romainguy.kotlin.math.Float3(randomX, 0.0f, randomZ), corEscolhida);
    }
  }
}