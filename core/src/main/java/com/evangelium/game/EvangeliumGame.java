package com.evangelium.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class EvangeliumGame extends ApplicationAdapter {
    private static final int ESTADO_INTRO = 0;
    private static final int ESTADO_CALLE = 1;
    private int estadoActual = ESTADO_INTRO;

    private SpriteBatch batch;
    private FitViewport viewport;
    private OrthographicCamera camara;

    // Mapa
    private TiledMap mapa;
    private OrthogonalTiledMapRenderer mapaRenderer;
    private final int[] capasFondo = {0, 1, 2, 3, 4, 5}; 
    private final int[] capasFrente = {6, 7};

    // Intro
    private Texture texturaMonstruo; // <-- Pon aquí la imagen de tu monstruo
    private Music ambienteIntro;
    private BitmapFont font;
    private GlyphLayout layout;
    private final String textoCompleto = "El hombre no teme a la oscuridad...";
    private String textoAMostrar = "";
    private float timerTexto = 0;
    private int indiceCaracter = 0;
    private float alphaNegro = 0;
    private float esperaFinal = 0;
    private boolean textoTerminado = false;

    // Padre
    private Texture spriteSheetPadre;
    private Animation<TextureRegion> padreCaminar;
    private float stateTimePadre;
    private float padreX = 400;
    private float padreY = 200;
    private float velocidad = 200f;
    private boolean mirandoIzquierda = false; // Para saber hacia dónde voltearlo

    @Override
    public void create() {
        batch = new SpriteBatch();
        camara = new OrthographicCamera();
        viewport = new FitViewport(800, 600, camara);
        camara.position.set(400, 300, 0);

        layout = new GlyphLayout();
        font = new BitmapFont();

        // Cargar Música (asegúrate que ambiente.mp3 está en assets)
        ambienteIntro = Gdx.audio.newMusic(Gdx.files.internal("ambiente.mp3"));
        ambienteIntro.setLooping(true);
        ambienteIntro.play();

        // Cargar Monstruo (Reemplaza "monstruo.png" por el nombre de tu imagen)
        // texturaMonstruo = new Texture("monstruo.png");

        try {
            mapa = new TmxMapLoader().load("mapas/escena1.tmx");
            mapaRenderer = new OrthogonalTiledMapRenderer(mapa);
            System.out.println("[SISTEMA] Mapa cargado correctamente");
        } catch (Exception e) {
            System.out.println("[ERROR] El mapa no cargó: Revisa las rutas en el archivo .tmx");
        }

        spriteSheetPadre = new Texture("padre/Walk.png");
        TextureRegion[][] tmp = TextureRegion.split(spriteSheetPadre, spriteSheetPadre.getWidth() / 8, spriteSheetPadre.getHeight());
        padreCaminar = new Animation<>(0.1f, tmp[0]);
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        if (estadoActual == ESTADO_INTRO) {
            actualizarIntro(delta);
            dibujarIntro();
            if(Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                estadoActual = ESTADO_CALLE; // Saltar intro con ENTER
            }
        } else {
            actualizarCalle(delta);
            dibujarCalle();
        }
    }

    private void actualizarCalle(float delta) {
        boolean moviendose = false;

        // Movimiento WASD
        if (Gdx.input.isKeyPressed(Input.Keys.W)) { padreY += velocidad * delta; moviendose = true; }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) { padreY -= velocidad * delta; moviendose = true; }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) { 
            padreX -= velocidad * delta; 
            moviendose = true; 
            mirandoIzquierda = true; // Voltea a la izquierda
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) { 
            padreX += velocidad * delta; 
            moviendose = true; 
            mirandoIzquierda = false; // Voltea a la derecha
        }

        if (moviendose) {
            stateTimePadre += delta;
        } else {
            stateTimePadre = 0; 
        }

        camara.position.set(padreX, padreY, 0);
        camara.update();
    }

    private void dibujarCalle() {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.3f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (mapaRenderer != null) {
            mapaRenderer.setView(camara);
            mapaRenderer.render(capasFondo);
        }

        batch.setProjectionMatrix(camara.combined);
        batch.begin();
        TextureRegion frame = padreCaminar.getKeyFrame(stateTimePadre, true);
        
        // Truco para voltear el sprite dependiendo de hacia dónde camina
        float drawX = mirandoIzquierda ? padreX + 64 : padreX - 64;
        float drawWidth = mirandoIzquierda ? -128 : 128;
        
        batch.draw(frame, drawX, padreY - 64, drawWidth, 128);
        batch.end();

        if (mapaRenderer != null) {
            mapaRenderer.render(capasFrente);
        }
    }

    private void actualizarIntro(float delta) {
        timerTexto += delta;
        if (timerTexto > 0.06f && indiceCaracter < textoCompleto.length()) {
            textoAMostrar += textoCompleto.charAt(indiceCaracter);
            indiceCaracter++;
            timerTexto = 0;
        } else if (indiceCaracter >= textoCompleto.length()) {
            textoTerminado = true;
        }
        if (textoTerminado) {
            esperaFinal += delta;
            if (esperaFinal > 2.0f) {
                alphaNegro += delta * 0.4f;
                if (alphaNegro >= 1) estadoActual = ESTADO_CALLE;
            }
        }
    }

    private void dibujarIntro() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        
        // Descomenta y ajusta si tienes la imagen estática del monstruo
        // if(texturaMonstruo != null) {
        //     batch.draw(texturaMonstruo, 300, 250); 
        // }
        
        layout.setText(font, textoAMostrar);
        font.draw(batch, textoAMostrar, (800 - layout.width) / 2, 200);
        batch.end();
    }

    @Override
    public void resize(int width, int height) { viewport.update(width, height); }

    @Override
    public void dispose() {
        batch.dispose();
        if(mapa != null) mapa.dispose();
        spriteSheetPadre.dispose();
        if(ambienteIntro != null) ambienteIntro.dispose();
        // if(texturaMonstruo != null) texturaMonstruo.dispose();
    }
}