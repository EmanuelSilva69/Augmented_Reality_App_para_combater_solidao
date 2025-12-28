package com.google.ar.core.examples.java.AvatarAR;

import java.util.HashMap;

import com.google.ar.core.Anchor;
import com.google.ar.core.examples.java.AvatarAR.common.samplerender.Mesh; // Importa Mesh da sua pasta samplerender
import com.google.ar.core.examples.java.AvatarAR.common.samplerender.Shader; // Importa Shader da sua pasta samplerender
import com.google.ar.core.examples.java.AvatarAR.common.samplerender.Texture; // Importa Texture da sua pasta samplerender
import com.google.ar.core.examples.java.AvatarAR.common.samplerender.SampleRender; // Importa SampleRender

import java.io.IOException;

/**
 * Gerencia o modelo 3D (humanóide), sua posição, escala, e estado de animação
 * no ambiente de Realidade Aumentada.
 */
public class AvatarModel {

    // --- Enumeração para Customização de Animação ---
    public enum AnimationType {
        IDLE, TALKING, WAVING, LISTENING, CUSTOM_ACTION
    }

    // --- Variáveis de Renderização ---
    private Mesh avatarMesh;
    private Shader avatarShader;
    private Texture avatarTexture;

    private Anchor anchor;
    private float scaleFactor = 1.0f;
    private AnimationType currentAnimation = AnimationType.IDLE;
    private long lastAnimationTime = 0;
    private static final long ANIMATION_DURATION_MS = 1500;

    // --- Inicialização ---

    /**
     * Carrega o modelo 3D (OBJ) e sua textura.
     * @param render O contexto SampleRender necessário para carregar assets.
     * @param modelFileName O nome do arquivo OBJ (ex: "avatar.obj").
     * @param textureFileName O nome do arquivo de textura (ex: "avatar.png").
     */
    public void loadModel(SampleRender render, String modelFileName, String textureFileName) {
        try {
            avatarMesh = Mesh.createFromAsset(render, modelFileName);
            avatarTexture = Texture.createFromAsset(render, textureFileName, Texture.WrapMode.CLAMP_TO_EDGE, Texture.ColorFormat.SRGB);

            avatarShader = Shader.createFromAssets(
                            render,
                            "shaders/ar_unlit_object.vert",
                            "shaders/ar_unlit_object.frag",
                            null // Shaders unlit geralmente não precisam de defines
                    )
                    .setTexture("u_Texture", avatarTexture); // Verifique se no .frag está escrito u_Texture

        } catch (IOException e) {
            throw new RuntimeException("Erro nos assets: " + e.getMessage());
        }
    }

    // --- Renderização ---

    /**
     * Desenha o avatar na tela.
     */
    // Altere a assinatura do método para aceitar os dados de luz
    public void render(SampleRender render, float[] viewMatrix, float[] projectionMatrix) {
        if (anchor != null && avatarMesh != null && avatarShader != null) {
            float[] modelMatrix = new float[16];
            anchor.getPose().toMatrix(modelMatrix, 0);

            // ESCALA MUITO PEQUENA (0.01) para garantir que não cubra a tela toda
            android.opengl.Matrix.scaleM(modelMatrix, 0, 0.01f, 0.01f, 0.01f);

            float[] modelViewProjectionMatrix = new float[16];
            float[] modelViewMatrix = new float[16];
            android.opengl.Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);
            android.opengl.Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0);

            // Verifique se o seu shader .vert usa exatamente este nome "u_ModelViewProjection"
            avatarShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix);

            render.draw(avatarMesh, avatarShader);
        }
    }


    private void updateModelMatrixForRender(float[] modelMatrix) {
        // Lógica de animação de "fala" simples... (código omitido para brevidade)
        if (currentAnimation == AnimationType.TALKING) {
            // Lógica de animação
        } else {
            currentAnimation = AnimationType.IDLE;
        }
    }

    public void setAnchor(Anchor newAnchor) {
        this.anchor = newAnchor;
    }

    public void setAnimation(AnimationType type) {
        this.currentAnimation = type;
        this.lastAnimationTime = System.currentTimeMillis();
        android.util.Log.i("AvatarModel", "Animação definida como: " + type.name());
    }
}