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
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class EvangeliumGame extends ApplicationAdapter {
    private static final int ESTADO_INTRO = 0;
    private static final int ESTADO_CALLE = 1;
    private int estadoActual = ESTADO_INTRO;

    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private FitViewport viewport;
    private OrthographicCamera camara;

    // --- MAPA (TILED) ---
    private TiledMap mapa;
    private OrthogonalTiledMapRenderer mapaRenderer;
    
    // Basado en tu imagen de Tiled (Capas de abajo hacia arriba):
    // 0:suelo, 1:decoracion, 2:pasto, 3:casas_altas, 4:casas_bajas, 5:chimenea
    private final int[] capasFondo = {0, 1, 2, 3, 4, 5}; 
    // 6:techos, 7:lamparas (estas tapan al padre)
    private final int[] capasFrente = {6, 7};

    // --- INTRO Y MONSTRUO ---
    private Animation<TextureRegion> monstruoAnim;
    private float stateTimeMonstruo;
    private Music ambienteIntro;
    private BitmapFont font;
    private GlyphLayout layout;
    private final String textoCompleto = "El hombre no teme a la oscuridad, teme a lo que el poder \nle permite hacer en ella. En la locura por ser Dios, \nolvidamos que solo somos barro...";
    private String textoAMostrar = "";
    private float timerTexto = 0;
    private int indiceCaracter = 0;

    // --- TRANSICIÓN (FUNDIDO) ---
    private float alphaNegro = 1; 
    private boolean fundiendoACalle = false;

    // --- EL PADRE ---
    private Texture spriteSheetPadre;
    private Animation<TextureRegion> padreCaminar;
    private float stateTimePadre;
    private float padreX = 400; // Ajusta según donde esté el suelo en tu mapa
    private float padreY = 150; 
    private float velocidad = 200f;
    private boolean mirandoIzquierda = false;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        camara = new OrthographicCamera();
        viewport = new FitViewport(800, 600, camara);

        layout = new GlyphLayout();
        font = new BitmapFont(); 
        font.getData().setScale(1.2f);

        // Carga Mapa - Asegúrate que la ruta sea assets/mapas/escena1.tmx
        try {
            mapa = new TmxMapLoader().load("mapas/escena1.tmx");
            mapaRenderer = new OrthogonalTiledMapRenderer(mapa);
        } catch (Exception e) {
            Gdx.app.error("TMX", "No se pudo cargar el mapa. Revisa nombres de archivos .png");
        }

        // Carga Monstruo
        monstruoAnim = new Animation<>(0.15f, 
            new TextureRegion(new Texture("1.png")), 
            new TextureRegion(new Texture("2.png")), 
            new TextureRegion(new Texture("3.png")), 
            new TextureRegion(new Texture("4.png")));

        // Carga Audio
        ambienteIntro = Gdx.audio.newMusic(Gdx.files.internal("ambiente.mp3")); 
        ambienteIntro.setLooping(true);
        ambienteIntro.play();

        // Carga Padre
        spriteSheetPadre = new Texture("padre/Walk.png");
        int anchoFrame = spriteSheetPadre.getWidth() / 8;
        TextureRegion[][] tmp = TextureRegion.split(spriteSheetPadre, anchoFrame, spriteSheetPadre.getHeight());
        padreCaminar = new Animation<>(0.1f, tmp[0]); 
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (estadoActual == ESTADO_INTRO) {
            actualizarIntro(delta);
            dibujarIntro();
        } else {
            actualizarCalle(delta);
            dibujarCalle();
        }
        dibujarFundido();
    }

    private void actualizarIntro(float delta) {
        stateTimeMonstruo += delta;
        if (!fundiendoACalle && alphaNegro > 0) alphaNegro -= delta * 0.5f;

        timerTexto += delta;
        if (timerTexto > 0.06f && indiceCaracter < textoCompleto.length()) {
            textoAMostrar += textoCompleto.charAt(indiceCaracter);
            indiceCaracter++;
            timerTexto = 0;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) fundiendoACalle = true;

        if (fundiendoACalle) {
            alphaNegro += delta * 0.8f;
            if (alphaNegro >= 1) {
                ambienteIntro.stop();
                estadoActual = ESTADO_CALLE;
                fundiendoACalle = false;
            }
        }
    }

    private void dibujarIntro() {
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        batch.draw(monstruoAnim.getKeyFrame(stateTimeMonstruo, true), 400 - 100, 300 - 50, 200, 200);
        layout.setText(font, textoAMostrar);
        font.draw(batch, textoAMostrar, (800 - layout.width) / 2, 200);
        batch.end();
    }

    private void actualizarCalle(float delta) {
        if (alphaNegro > 0) alphaNegro -= delta * 0.5f;

        boolean moviendose = false;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) { padreX -= velocidad * delta; moviendose = true; mirandoIzquierda = true; }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) { padreX += velocidad * delta; moviendose = true; mirandoIzquierda = false; }

        if (moviendose) stateTimePadre += delta;
        else stateTimePadre = 0;

        camara.position.set(padreX, padreY + 100, 0); // La cámara sigue al padre
        camara.update();
    }

    private void dibujarCalle() {
        if (mapaRenderer != null) {
            mapaRenderer.setView(camara);
            mapaRenderer.render(capasFondo); // Suelo y casas
        }

        batch.setProjectionMatrix(camara.combined);
        batch.begin();
        TextureRegion frame = padreCaminar.getKeyFrame(stateTimePadre, true);
        float drawX = mirandoIzquierda ? padreX + 56 : padreX - 56;
        float drawWidth = mirandoIzquierda ? -112 : 112;
        batch.draw(frame, drawX, padreY, drawWidth, 128);
        batch.end();

        if (mapaRenderer != null) {
            mapaRenderer.render(capasFrente); // Techos y lámparas tapan al padre
        }
    }

    private void dibujarFundido() {
        if (alphaNegro > 0) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0, 0, 0, alphaNegro);
            shapeRenderer.rect(0, 0, 800, 600);
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        font.dispose();
        ambienteIntro.dispose();
        spriteSheetPadre.dispose();
        if (mapa != null) mapa.dispose();
    }
}