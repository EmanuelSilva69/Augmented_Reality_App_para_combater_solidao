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
  private android.media.MediaPlayer mediaPlayer;
  // Lista com os IDs das músicas
  private int[] playlist = {
          R.raw.musica1, // Índice 0
          R.raw.musica2  // Índice 1
  };

  // Controle: -1 = Desligado, 0 = Música 1, 1 = Música 2
  private int indiceMusicaAtual = -1;
  // --- Listas de Arquivos ---
  // AVATAR 1 (A Mulher Atual)
  private String[] dancasAvatar1 = {
         // "animations/avatar1/dancing.glb",
          "animations/avatar1/dancinghiphop.glb",
          "animations/avatar1/dancinghiphop2.glb",
          "animations/avatar1/dancingjazz.glb",
          "animations/avatar1/dancingsalsa.glb",
          "animations/avatar1/dancingsamba.glb",
  };
  private String talkingAvatar1 = "animations/avatar1/talking.glb";

  // AVATAR 2 (Ex: Um Robô ou Homem - coloque seus arquivos aqui)
  private String[] dancasAvatar2 = {
          "animations/avatar2/macarena.glb",
          "animations/avatar2/salsa.glb"

  };
  private String talkingAvatar2 = "animations/avatar2/TALKING.glb";
  private String[] dancasAvatar3 = {
          "animations/avatar3/rapping.glb",
          "animations/avatar3/salsa.glb",

  };
  private String talkingAvatar3 = "animations/avatar3/talking.glb";
  // --- ESTADO ATUAL ---
  // Essa lista aponta para qual avatar estamos usando agora
  private String[] dancasAtuais = dancasAvatar1;
  private String talkingAtual = talkingAvatar1;

  // Controle de qual ID está ativo (1 ou 2)
  // ==============================================================================
  // 2. VARIÁVEIS ADAPTADAS (O "PONTEIRO" ATUAL)
  // ==============================================================================

  // AQUI ESTÁ O TRUQUE: Mantemos os nomes antigos!
  // Elas começam apontando para o Avatar 1, mas vão mudar quando clicarmos no botão.

  private String[] modelosDancaIdle = dancasAvatar1;

  // Transformamos a String única em Array para manter compatibilidade com seu código antigo
  private String[] modelosTalking = { talkingAvatar1 };

  // Controle interno
  private int idAvatarAtual = 1;
  private int ultimaDancaIndex = -1;

  private String modeloMorte = "animations/avatar1/morte.glb";

  // --- MÉTODO DE TROCA DE ESTADO (CACHE) ---
  private void atualizarEstadoAvatar(String path) {
    // 1. Pega o novo modelo do cache
    ArModelNode novoNode = avatarCache.get(path);

    if (novoNode == null) {
      Log.e("AvatarAR", "Modelo não encontrado no cache: " + path);
      return;
    }

    // Se já é o mesmo que está tocando, não faz nada (economiza processamento)
    if (novoNode == avatarNodeAtual) return;

    // 2. ESCONDE TODOS OS OUTROS (Técnica do Holofote)
    // Isso garante que não sobre nenhum "fantasma" visível
    for (ArModelNode node : avatarCache.values()) {
      node.setVisible(false);
      // Opcional: Pausar animação dos invisíveis para poupar bateria
      // node.pauseAnimation();
    }

    // 3. PREPARA O NOVO
    avatarNodeAtual = novoNode;

    // Garante que ele está preso na gaiola (caso tenha se soltado por algum bug)
    if (cameraRoot != null && avatarNodeAtual.getParent() != cameraRoot) {
      avatarNodeAtual.detachAnchor();
      avatarNodeAtual.setParent(null);
      cameraRoot.addChild(avatarNodeAtual);
    }

    // Reseta posição (garantia)
    avatarNodeAtual.setPosition(new dev.romainguy.kotlin.math.Float3(0.0f, 0.0f, 0.0f));
    avatarNodeAtual.setRotation(new dev.romainguy.kotlin.math.Float3(0.0f, 0.0f, 0.0f));

    // 4. MOSTRA O NOVO
    avatarNodeAtual.setVisible(true);

    // Se a animação tiver parado, dá o play de novo
    // (Opcional, o SceneView geralmente faz auto-play)
    // avatarNodeAtual.playAnimation(0, true);

    Log.d("AvatarAR", "Troca visual realizada para: " + path);
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
            "animations/avatar2/macarena.glb",
            "animations/avatar2/salsa.glb",
            "animations/avatar2/TALKING.glb",
            "animations/avatar3/rapping.glb",
            "animations/avatar3/salsa.glb",
            "animations/avatar3/talking.glb",

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
              0.9f, // scale
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

      // --- CORREÇÃO AQUI ---
      // 1. Removemos o Random (pois talkingAtual é um arquivo único, não uma lista)
      // 2. Usamos 'talkingAtual', que contém o caminho correto do avatar selecionado no momento

      if (talkingAtual != null) {
        atualizarEstadoAvatar(talkingAtual);
      } else {
        Log.e("AvatarAR", "talkingAtual está nulo!");
      }

      // Faz o celular falar
      if (ttsManager != null) ttsManager.falar(text);

      // Calcula o tempo para voltar a dançar
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
  protected void onPause() {
    super.onPause();
    // Pausa a música se sair do app
    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
      mediaPlayer.pause();
    }
  }
  @Override
  protected void onDestroy() {
    if (session != null) { session.close(); }
    if (ttsManager != null) { ttsManager.parar(); }
    // LIBERA A MÚSICA DA MEMÓRIA
    if (mediaPlayer != null) {
      mediaPlayer.stop();
      mediaPlayer.release();
      mediaPlayer = null;
    }
    super.onDestroy();
  }

  protected boolean settingsMenuClick(MenuItem item) {
    int id = item.getItemId();
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
      trocarPersonagem(1);
      idleHandler.removeCallbacks(idleRunnable);
      idleRunnable.run();
      return true;
    }
    if (item.getItemId() == R.id.avatar_choice_2) {
      trocarPersonagem(2);
      idleHandler.removeCallbacks(idleRunnable);
      idleRunnable.run();
      return true;
    }
    if (item.getItemId() == R.id.avatar_choice_3) {
      trocarPersonagem(3);
      idleHandler.removeCallbacks(idleRunnable);
      idleRunnable.run();
      return true;
    }
    if (id == R.id.action_music) {
      alternarMusica();
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
  private void trocarPersonagem(int id) {
    if (idAvatarAtual == id) return;

    idAvatarAtual = id;

    if (id == 1) {
      // Atualiza as variáveis NOVAS (para o Loop e a IA funcionarem)
      dancasAtuais = dancasAvatar1;
      talkingAtual = talkingAvatar1;

      // Atualiza as VELHAS (para garantir compatibilidade se sobrou código antigo)
      modelosDancaIdle = dancasAvatar1;
      modelosTalking = new String[]{ talkingAvatar1 };

      Toast.makeText(this, "Avatar 1: Mulher", Toast.LENGTH_SHORT).show();
    }
    else if (id == 2) {
      dancasAtuais = dancasAvatar2;
      talkingAtual = talkingAvatar2; // <--- ISSO FALTAVA!

      modelosDancaIdle = dancasAvatar2;
      modelosTalking = new String[]{ talkingAvatar2 };

      Toast.makeText(this, "Avatar 2: Robô", Toast.LENGTH_SHORT).show();
    }
    else if (id == 3) {
      dancasAtuais = dancasAvatar3;
      talkingAtual = talkingAvatar3; // <--- ISSO FALTAVA!

      modelosDancaIdle = dancasAvatar3;
      modelosTalking = new String[]{ talkingAvatar3 };

      Toast.makeText(this, "Avatar 3: Rapaz", Toast.LENGTH_SHORT).show();
    }

    // Reinicia a dança imediatamente
    idleHandler.removeCallbacks(idleRunnable);

    // Usa dancasAtuais para garantir
    if (dancasAtuais.length > 0) {
      atualizarEstadoAvatar(dancasAtuais[0]);
    }

    idleHandler.postDelayed(idleRunnable, 10000);
  }
  // --- CONTROLE DE MÚSICA ---
  // --- CONTROLE DE MÚSICA (CICLO) ---
  private void alternarMusica() {
    // 1. Limpeza: Se já tiver algo tocando, para e libera memória
    if (mediaPlayer != null) {
      if (mediaPlayer.isPlaying()) {
        mediaPlayer.stop();
      }
      mediaPlayer.release();
      mediaPlayer = null;
    }

    // 2. Avança para a próxima música
    indiceMusicaAtual++;

    // 3. Verifica se acabou a lista (Ciclo: 0 -> 1 -> Desligado)
    if (indiceMusicaAtual >= playlist.length) {
      indiceMusicaAtual = -1; // Volta para o estado "Desligado"
      Toast.makeText(this, "Música Desligada", Toast.LENGTH_SHORT).show();
      return; // Sai do método, não toca nada
    }

    // 4. Toca a nova música selecionada
    int musicaEscolhida = playlist[indiceMusicaAtual];

    mediaPlayer = android.media.MediaPlayer.create(this, musicaEscolhida);
    mediaPlayer.setLooping(true);
    mediaPlayer.setVolume(0.5f, 0.5f);
    mediaPlayer.start();

    Toast.makeText(this, "Tocando: Faixa " + (indiceMusicaAtual + 1), Toast.LENGTH_SHORT).show();
  }
}