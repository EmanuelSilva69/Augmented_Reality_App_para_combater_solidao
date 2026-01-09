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

O aplicativo evoluiu para um **Estúdio de Realidade Aumentada Portátil**. Diferente de apps AR comuns que dependem de chão perfeito, o Avatar AR utiliza um sistema de "Gaiola Virtual" (`Camera Root`), mantendo os personagens fixos e visíveis à frente do usuário, ideal para uso em movimento ou ambientes complexos.

### Funcionalidades Principais:
* **Dual AI Engine (Híbrido):** Suporte a **Google Gemini** (Nuvem) e **Ollama** (Local).
* **Multi-Avatar System:** Troca instantânea entre 3 personagens distintos (Ex: Mulher, Robô, Rapaz), cada um com seu próprio set de animações e personalidade.
* **Modo Estúdio (Studio Mode):** O avatar e o palco são fixados a 4 metros da câmera, garantindo que nunca "sumam" ou atravessem paredes, criando uma experiência de palco portátil.
* **DJ Virtual:** Sistema de música integrado com playlist cíclica (Faixa 1 -> Faixa 2 -> Off).
* **Carregamento Inteligente (Anti-ANR):** Sistema de *Staggered Loading* que carrega os modelos 3D em fila (com atraso milimétrico) para não travar o celular na inicialização.
* **Máquina de Estados:** Alterna fluidamente entre *Idle* (Danças aleatórias sem repetição) e *Talking* (Fala sincronizada).

---

## 🎭 Gestão de Modelos e Animações 3D

O projeto agora organiza os assets em subpastas para facilitar a gestão de múltiplos personagens. Os arquivos estão em `src/main/assets/animations/`.

### Estrutura de Pastas e Arquivos:

**📂 avatar1/ (Personagem Principal)**
* `dancinghiphop.glb`, `dancingjazz.glb`, `dancingsamba.glb` (e variações)
* `talking.glb` (Fala padrão)
* `morte.glb` (Fallback de erro)

**📂 avatar2/ (Personagem Secundário)**
* `macarena.glb`, `salsa.glb`
* `TALKING.glb`

**📂 avatar3/ (Personagem Terciário)**
* `rapping.glb`, `salsa.glb`
* `talking.glb`

**📂 pistadanca/ (Cenário)**
* `animated_dance_floor_neon_lights.glb`
* `sound_box.glb`, `disco_ball.glb`, `cube.glb` (Luzes)

> **Nota Técnica:** O sistema utiliza a técnica de "Holofote" para gestão de memória. Todos os modelos são carregados no início e mantidos na cena (`cameraRoot`), alternando apenas a visibilidade (`setVisible`). Isso evita lags, recarregamentos e o desaparecimento de modelos durante a troca.
## 🛠️ Arquitetura Técnica

O projeto segue o padrão MVVM simplificado para Android Java, focado em Managers para isolar responsabilidades.

---

## 🔧 Solução de Problemas

| Problema | Causa Provável | Solução |
| :--- | :--- | :--- |
| **App trava na abertura (ANR)** | Carregar muitos GLBs pesados simultaneamente. | O código já implementa um *delay* de 200ms-500ms entre cada carregamento (*Staggered Loading*). Se adicionar mais avatares, mantenha essa lógica. |
| **Música não toca** | Arquivo ausente ou nome errado. | Verifique se os arquivos `.mp3` estão na pasta `res/raw` e se o nome é todo minúsculo (ex: `musica1.mp3`). |
| **Avatar sumiu após troca** | Erro no path do cache. | O sistema atual monta a lista de carregamento copiando as variáveis de definição. Certifique-se de que o arquivo existe na pasta `assets` com o nome exato. |
| **Erro 404 na IA** | API Key inválida. | Verifique espaços em branco na chave do Gemini no menu de configurações. |

---

#### 4. Verificação dos Assets (Importante!)
* **Modelos 3D:** Confirma em `src > main > assets > animations`.
* **Música:** Confirma se tens a pasta `src > main > res > raw` com os ficheiros `musica1.mp3` e `musica2.mp3`. Se não tiveres, cria a pasta e adiciona qualquer ficheiro de áudio para evitar erros de compilação (`R.raw.musica...`).

## 🎭 Gestão de Modelos e Animações 3D

O aplicativo utiliza o formato **.GLB** (glTF Binary) por ser leve e otimizado para mobile. Os arquivos estão localizados em `src/main/assets/animations/`.

### Lista de Animações Atuais (Por Personagem):

O projeto agora suporta múltiplos personagens, cada um com seu próprio conjunto de animações organizado em subpastas (`assets/animations/`).

**1. Avatar 1 (Principal):**
* **Idle (Danças):** `dancinghiphop.glb`, `dancinghiphop2.glb`, `dancingjazz.glb`, `dancingsalsa.glb`, `dancingsamba.glb`
* **Interação:** `talking.glb`
* **Erro:** `morte.glb` (Executado se a API falhar)

**2. Avatar 2 (Secundário):**
* **Idle (Danças):** `macarena.glb`, `salsa.glb`
* **Interação:** `TALKING.glb`

**3. Avatar 3 (Terciário):**
* **Idle (Danças):** `rapping.glb`, `salsa.glb`
* **Interação:** `talking.glb`

> **Nota:** O sistema de "Sorteio Inteligente" garante que, dentro do conjunto de cada avatar, as danças variem aleatoriamente sem repetições consecutivas.

## ➕ Guia de Personalização

Você pode adicionar novas danças aos personagens existentes ou criar novos avatares facilmente.

### Passo 1: Preparar o Arquivo (Mixamo/Blender)
1.  Baixe a animação em formato **.fbx** ou **.glb**.
2.  **IMPORTANTE:** Se for uma animação de loop (como dança), marque a opção **"In Place"** (No Lugar) para evitar que o avatar saia andando pela sala e atravesse a "gaiola" do modo estúdio.
3.  Converta para `.glb` se necessário.
4.  Renomeie o arquivo usando **apenas letras minúsculas** (ex: `novadanca.glb`). O Android tem problemas com maiúsculas em assets.

### Passo 2: Adicionar ao Projeto
Coloque o arquivo na subpasta do personagem correto em:
`app/src/main/assets/animations/`

* Se for para o **Avatar 1**, coloque em: `animations/avatar1/`
* Se for para o **Avatar 2**, coloque em: `animations/avatar2/`

### Passo 3: Registrar no Código
Abra `AvatarArActivity.java` e adicione o caminho do arquivo na lista correspondente ao personagem (localizadas no topo da classe):

```java
// Exemplo: Adicionando uma dança ao Avatar 2
private String[] dancasAvatar2 = {
    "animations/avatar2/macarena.glb",
    "animations/avatar2/salsa.glb",
    "animations/avatar2/novadanca.glb" // <--- ADICIONE AQUI (Use o caminho completo)
};
```
atualize no On Create também, para ser gerado no cache.

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

## 🔒 Política de Privacidade

**Última atualização:** 09 de  Janeiro de 2025

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
