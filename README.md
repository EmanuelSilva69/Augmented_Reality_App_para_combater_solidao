# 🕺 Avatar AR - Assistente Inteligente em Realidade Aumentada

**Avatar AR** é uma aplicação Android nativa que funde o mundo físico com a inteligência artificial. Utilizando o **ARCore** para rastreamento de ambiente e **LLMs (Large Language Models)** para conversação, o projeto projeta um avatar 3D que dança, conversa e interage com o usuário em tempo real.

---

## 📋 Índice
1. [Visão Geral e Funcionalidades](#-visão-geral-e-funcionalidades)
2. [Arquitetura Técnica](#-arquitetura-técnica)
3. [Gestão de Modelos e Animações 3D](#-gestão-de-modelos-e-animações-3d)
4. [Guia de Personalização (Adicionar Avatares)](#-guia-de-personalização)
5. [Instalação e Configuração](#-instalação-e-configuração)
6. [Solução de Problemas](#-solução-de-problemas)
7. [Política de Privacidade](#-política-de-privacidade)

---

## 🚀 Visão Geral e Funcionalidades

O aplicativo transforma o ambiente do usuário em um palco virtual. Ao abrir a câmera, o sistema detecta superfícies planas (chão/mesa) e posiciona um avatar humanóide.

### Funcionalidades Principais:
* **Dual AI Engine (Híbrido):**
    * **Google Gemini (Nuvem):** Integração nativa com a API Gemini 1.5 Flash para respostas rápidas e criativas.
    * **Ollama (Local):** Suporte para conexão com servidor local (PC) para privacidade total e uso offline (requer rede local).
* **Máquina de Estados de Animação:** O avatar possui "vida própria", alternando entre estados de *Idle* (Dança), *Thinking* (Processando) e *Talking* (Respondendo).
* **Sorteio Inteligente de Danças:** O sistema garante que as danças de espera (`Idle`) variem aleatoriamente sem repetições consecutivas.
* **Voz Neural (TTS):** As respostas da IA são lidas em voz alta utilizando o motor *Text-to-Speech* do Android em Português Brasileiro.
* **Interação Tátil:** O usuário pode "ancorar" (fixar) o avatar em um ponto específico tocando na tela.

---

## 🛠️ Arquitetura Técnica

O projeto segue o padrão MVVM simplificado para Android Java, focado em Managers para isolar responsabilidades.

### Estrutura de Classes:

| Classe | Responsabilidade |
| :--- | :--- |
| **`AvatarArActivity`** | **O Cérebro.** Gerencia o ciclo de vida da Activity, inicializa o ARSceneView, controla a UI e orquestra a máquina de estados (quando dançar, quando falar). |
| **`GeminiManager`** | Cliente HTTP (OkHttp) que conecta à Google AI Studio. Gerencia a API Key do usuário e trata os JSONs de resposta. |
| **`OllamaManager`** | Cliente HTTP para conexão com localhost. Permite usar Llama 3, Mistral, etc., rodando no PC do usuário. |
| **`TTSManager`** | Wrapper para a classe `TextToSpeech` do Android. Configura locale (pt-BR), velocidade e tom da voz. |
| **`AvatarModel`** | (Via SceneView) Abstração do nó 3D. Controla carregamento de GLB, escala e posicionamento no mundo AR. |

---

## 🎭 Gestão de Modelos e Animações 3D

O aplicativo utiliza o formato **.GLB** (glTF Binary) por ser leve e otimizado para mobile. Os arquivos estão localizados em `src/main/assets/animations/`.

### Lista de Animações Atuais:

**1. Estado Idle (Danças de Espera):**
O avatar escolhe aleatoriamente uma destas animações enquanto aguarda input do usuário:
* `dancing.glb` (Dança Padrão)
* `dancinghiphop.glb` (Hip Hop 1)
* `dancinghiphop2.glb` (Hip Hop 2)
* `dancingjazz.glb` (Jazz/Contemporâneo)
* `dancingsalsa.glb` (Salsa)
* `dancingsamba.glb` (Samba)

**2. Estado de Interação:**
* `talking.glb`: Executado enquanto o TTS está falando a resposta da IA.
* `talkingwalking.glb`: Variação onde o avatar caminha enquanto fala (Cuidado: requer espaço físico).

**3. Estado de Erro:**
* `morte.glb`: Executado se a API da IA falhar ou a internet cair. Serve como feedback visual imediato.

---

## ➕ Guia de Personalização

Você pode adicionar seus próprios avatares ou novas danças (do Mixamo, Blender, etc.) facilmente.

### Passo 1: Preparar o Arquivo
1.  Baixe a animação em formato **.fbx** ou **.glb**.
2.  **IMPORTANTE:** Se for uma animação de loop (como dança), marque a opção **"In Place"** (No Lugar) para evitar que o avatar saia andando pela sala e perca a âncora AR.
3.  Converta para `.glb` se necessário.
4.  Renomeie o arquivo usando **apenas letras minúsculas** (ex: `minhanovadanca.glb`). O Android não reconhece maiúsculas em assets facilmente.

### Passo 2: Adicionar ao Projeto
Coloque o arquivo na pasta:
`app/src/main/assets/animations/`

### Passo 3: Registrar no Código
Abra `AvatarArActivity.java` e adicione o nome do arquivo na lista `modelosDancaIdle`:

```java
private String[] modelosDancaIdle = {
    "animations/dancing.glb",
    "animations/minhanovadanca.glb", // <--- SEU NOVO ARQUIVO AQUI
    // ... outros arquivos
};
```

### 💿 Instalação e Configuração

### Requisitos
* Android Studio Jellyfish ou superior.
* Dispositivo Android com suporte a **ARCore** (Google Play Services for AR).
* Cabo USB para depuração.

### Compilando o APK
1.  Clone este repositório.
2.  Abra no Android Studio e aguarde a sincronização do Gradle.
3.  Conecte seu celular.
4.  Clique em **Run** (Play).

### Configurando a IA (No App)
1.  Abra o aplicativo.
2.  Toque no botão de **Engrenagem** (Configurações).
3.  Selecione **Configurar Gemini**.
4.  Cole sua API Key (obtenha gratuitamente em [aistudio.google.com](https://aistudio.google.com)).
5.  Toque em **Salvar**. O modo Gemini será ativado instantaneamente.

---

## 🔧 Solução de Problemas

| Problema | Causa Provável | Solução |
| :--- | :--- | :--- |
| **Crash ao abrir a câmera** | Permissões negadas. | Vá nas configurações do Android > Apps > AvatarAR e permita o uso da Câmera. |
| **Erro 404 na IA** | API Key ou Modelo incorreto. | Verifique se não há espaços em branco na chave colada. Confirme se o modelo no `GeminiManager` é `gemini-2.5-flash`. |
| **Avatar deslizando no chão** | Animação com Root Motion. | Use animações "In Place" ou ancore o avatar tocando na tela assim que ele aparecer. |
| **Flickering (Piscada)** | Troca de modelo pesado. | Normal na troca de arquivos GLB. Reduzir o tamanho dos arquivos (texture compression) ajuda. |

---

## 🔒 Política de Privacidade

**Última atualização:** 28 de Dezembro de 2024

A sua privacidade é importante para nós. Esta política descreve como o aplicativo **Avatar AR** coleta, usa e protege as suas informações.

### 1. Permissões de Câmera
O aplicativo utiliza a câmera do seu dispositivo estritamente para funcionalidades de **Realidade Aumentada (AR)**.
* **Uso:** A imagem da câmera é processada localmente pelo **Google Play Services for AR (ARCore)** para detectar superfícies planas e renderizar o avatar 3D.
* **Armazenamento:** Nenhuma imagem ou vídeo da câmera é enviado para nossos servidores, armazenado externamente ou compartilhado com terceiros pelo desenvolvedor.

### 2. Dados de Áudio e Voz
O aplicativo pode utilizar o microfone (se a funcionalidade de voz for ativada futuramente) ou o sistema de Text-to-Speech.
* As respostas de áudio são geradas localmente pelo motor TTS do Android.

### 3. Uso de APIs de Inteligência Artificial
* **Google Gemini:** Ao optar por usar o Gemini, o texto das suas conversas é enviado para os servidores da Google para processamento. Consulte a [Política de Privacidade da Google](https://policies.google.com/privacy) para mais detalhes.
* **Ollama:** Ao usar o modo local, nenhum dado sai da sua rede local.

### 4. Coleta de Dados
O aplicativo não coleta dados pessoais, localização ou identificadores de publicidade. Não há sistema de login ou rastreamento de usuário implementado pelo desenvolvedor.

### 5. Contato
Para dúvidas sobre esta política ou sobre o funcionamento do app, entre em contato com o desenvolvedor responsável.

---
## 📲 Guia Passo a Passo de Instalação

### Opção A: Para Desenvolvedores (Compilar o Código)

Este método é recomendado se desejas modificar as animações, ajustar a IA ou contribuir para o código.

#### 1. Preparação do Ambiente
Antes de começar, certifica-te de ter o seguinte:
* **Android Studio** (Versão Jellyfish ou superior recomendada).
* **Git** instalado.
* Um dispositivo Android físico (O emulador do Android Studio tem suporte limitado a AR e é muito lento para renderizar 3D em tempo real).
* **Cabo USB** para ligar o telemóvel ao PC.

#### 2. Clonar o Repositório
Abre o terminal (ou Git Bash) e executa o comando:
```bash
git clone [https://github.com/SEU_USUARIO/AvatarAR.git](https://github.com/SEU_USUARIO/AvatarAR.git)
```
Ou transfere o ficheiro `.zip` e extrai numa pasta da tua preferência.

#### 3. Abrir no Android Studio
1.  Abre o Android Studio.
2.  Seleciona **File > Open**.
3.  Navega até à pasta onde clonaste o projeto e seleciona-a.
4.  **Aguarda o Gradle Sync:** O Android Studio irá transferir automaticamente as bibliotecas necessárias (`SceneView`, `OkHttp`, `ARCore`). Isto pode demorar alguns minutos.

#### 4. Verificação dos Assets (Importante!)
Certifica-te de que os modelos 3D estão no local correto para evitar ecrãs pretos.
* Navega na aba "Project" à esquerda: `app > src > main > assets > animations`.
* Confirma se ficheiros como `dancing.glb` e `talking.glb` estão lá.

#### 5. Configurar o Telemóvel
Para instalar apps via cabo USB, precisas ativar o **Modo de Programador**:
1.  Vai a **Definições > Sobre o telefone**.
2.  Toca 7 vezes em **Número de Compilação** (Build Number).
3.  Volta, vai a **Sistema > Opções de Programador**.
4.  Ativa a **Depuração USB**.

#### 6. Compilar e Executar
1.  Liga o telemóvel via USB.
2.  No Android Studio, seleciona o teu dispositivo na barra superior.
3.  Clica no botão verde **Run (▶)**.

---

### Opção B: Instalação via APK (Teste Rápido)

Se já possuis o ficheiro `app-debug.apk` gerado:

1.  Envia o ficheiro `.apk` para o teu telemóvel (via Google Drive, WhatsApp ou USB).
2.  Toca no ficheiro para abrir.
3.  Se o Android bloquear, seleciona **Definições > Permitir desta fonte**.
4.  Clica em **Instalar**.

---

## ⚙️ Configuração Inicial (Primeira Utilização)

Assim que a aplicação abrir pela primeira vez, segue estes passos para ativar a inteligência do Avatar:

### 1. Permissões
O Android solicitará permissão para usar a **Câmara**.
* Clica em **"Durante a utilização da app"**. Sem isto, a Realidade Aumentada não funciona.

### 2. Ativar o Gemini (Cérebro da IA)
Para que o Avatar converse contigo, ele precisa de uma chave de acesso.

1.  No teu computador ou telemóvel, acede ao [Google AI Studio](https://aistudio.google.com/).
2.  Clica em **"Get API Key"** e depois em **"Create API Key"**. Copia o código gerado.
3.  Na app **Avatar AR**, toca no botão de **Engrenagem** (canto do ecrã).
4.  Escolhe a opção **Configurar Gemini**.
5.  Cola a chave que copiaste e toca em **Salvar**.

Pronto! Agora podes digitar no chat e ver o avatar dançar e responder às tuas perguntas em tempo real.
---
*Desenvolvido com ❤️, Java e Loucura. Só a animação foi 2 dias fazendo no blender*
