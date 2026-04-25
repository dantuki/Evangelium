package com.evangelium.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class EvangeliumGame extends ApplicationAdapter {
    private static final int ESTADO_INTRO = 0;
    private static final int ESTADO_CALLE = 1;
    private int estadoActual = ESTADO_INTRO;

    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private FitViewport viewport;
    private OrthographicCamera camara;

    // --- MAPA Y COLISIONES ---
    private TiledMap mapa;
    private OrthogonalTiledMapRenderer mapaRenderer;
    private Array<Rectangle> muros;
    private Rectangle rectPadre;

    // --- INTRO ---
    private Animation<TextureRegion> monstruoAnim;
    private float stateTimeMonstruo;
    private Music ambienteIntro;
    private BitmapFont font;
    private GlyphLayout layout;
    private final String textoCompleto = "El hombre no teme a la oscuridad, teme a lo que el poder \nle permite hacer en ella. En la locura por ser Dios, \nolvidamos que solo somos barro...";
    private String textoAMostrar = "";
    private float timerTexto = 0;
    private int indiceCaracter = 0;

    // --- TRANSICIÓN ---
    private float alphaNegro = 1; 
    private boolean fundiendoACalle = false;

    // --- EL PADRE (NUEVO ESTILO COHERENTE) ---
    private Texture spriteSheetPadre;
    private Animation<TextureRegion> padreCaminar;
    private float stateTimePadre;
    private float padreX = 150; 
    private float padreY = 95; // Ajustado para que pise el suelo
    private float velocidad = 120f; // Un poco más lento para mejor sensación
    private boolean mirandoIzquierda = false;

    // --- CLIMA Y AMBIENTE ---
    private Music sonidoLluvia;
    private Sound sonidoTrueno;
    private float timerNiebla = 0;
    private float proximoTrueno = 5f;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        camara = new OrthographicCamera();
        viewport = new FitViewport(800, 600, camara);
        camara.zoom = 0.7f; // Un zoom que permite ver el mapa y al personaje

        layout = new GlyphLayout();
        font = new BitmapFont(); 
        font.getData().setScale(1.1f);

        muros = new Array<>();
        // El rectángulo de colisión debe ser más pequeño, acorde al nuevo tamaño
        rectPadre = new Rectangle(padreX, padreY, 20, 10);

        cargarAssets();
    }

    private void cargarAssets() {
        System.out.println("--- CARGANDO MUNDO COHERENTE ---");
        try {
            // Intro
            monstruoAnim = new Animation<>(0.15f, 
                new TextureRegion(new Texture("1.png")), 
                new TextureRegion(new Texture("2.png")), 
                new TextureRegion(new Texture("3.png")), 
                new TextureRegion(new Texture("4.png")));
            
            ambienteIntro = Gdx.audio.newMusic(Gdx.files.internal("ambiente.mp3")); 
            ambienteIntro.setLooping(true);
            ambienteIntro.play();

            // Sonidos de Clima (asegúrate de tener lluvia.mp3 y trueno.wav)
            try {
                sonidoLluvia = Gdx.audio.newMusic(Gdx.files.internal("lluvia.mp3"));
                sonidoLluvia.setLooping(true);
                sonidoLluvia.setVolume(0.4f);
                sonidoTrueno = Gdx.audio.newSound(Gdx.files.internal("trueno.wav"));
            } catch (Exception e) { System.out.println("No se encontraron sonidos de clima."); }

            // Padre (NUEVO SPRITESHEET COHERENTE de 8 frames, 32x64 cada uno)
            spriteSheetPadre = new Texture("padre/walk.png");
            // Dividimos por 8 frames horizontales
            int anchoFrame = spriteSheetPadre.getWidth() / 8;
            // Tomamos el alto total de la imagen para el frame
            int altoFrame = spriteSheetPadre.getHeight();
            
            // Hacemos el split con las medidas exactas
            TextureRegion[][] tmp = TextureRegion.split(spriteSheetPadre, anchoFrame, altoFrame);
            // Creamos la animación con los frames correctos
            padreCaminar = new Animation<>(0.12f, tmp[0]); 

            // Mapa
            mapa = new TmxMapLoader().load("mapas/escena1.tmx");
            mapaRenderer = new OrthogonalTiledMapRenderer(mapa);

            if (mapa.getLayers().get("colisiones") != null) {
                for (MapObject objeto : mapa.getLayers().get("colisiones").getObjects()) {
                    if (objeto instanceof RectangleMapObject) {
                        muros.add(((RectangleMapObject) objeto).getRectangle());
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Error al cargar assets, revisa los archivos: " + e.getMessage());
        }
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
        if (!fundiendoACalle && alphaNegro > 0) {
            alphaNegro -= delta * 0.5f;
            if (alphaNegro < 0) alphaNegro = 0;
        }

        timerTexto += delta;
        if (timerTexto > 0.06f && indiceCaracter < textoCompleto.length()) {
            textoAMostrar += textoCompleto.charAt(indiceCaracter);
            indiceCaracter++;
            timerTexto = 0;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) fundiendoACalle = true;

        if (fundiendoACalle) {
            alphaNegro += delta * 1.2f;
            if (alphaNegro >= 1) {
                alphaNegro = 1;
                if (ambienteIntro != null) ambienteIntro.stop();
                if (sonidoLluvia != null) sonidoLluvia.play();
                estadoActual = ESTADO_CALLE;
                fundiendoACalle = false; 
            }
        }
    }

    private void dibujarIntro() {
        camara.position.set(400, 300, 0);
        camara.update();
        batch.setProjectionMatrix(camara.combined);
        batch.begin();
        if (monstruoAnim != null) batch.draw(monstruoAnim.getKeyFrame(stateTimeMonstruo, true), 300, 250, 200, 200);
        layout.setText(font, textoAMostrar);
        font.draw(batch, textoAMostrar, (800 - layout.width) / 2, 200);
        batch.end();
    }

    private void actualizarCalle(float delta) {
        if (alphaNegro > 0) {
            alphaNegro -= delta * 0.8f;
            if (alphaNegro < 0) alphaNegro = 0;
        }

        timerNiebla += delta;
        
        // Lógica de truenos aleatorios
        proximoTrueno -= delta;
        if (proximoTrueno <= 0) {
            if (sonidoTrueno != null) sonidoTrueno.play();
            // Truenos cada 8-18 segundos
            proximoTrueno = 8 + (float)Math.random() * 10;
        }

        float viejaX = padreX;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) { padreX -= velocidad * delta; mirandoIzquierda = true; }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) { padreX += velocidad * delta; mirandoIzquierda = false; }

        // Actualizamos posición de colisión (ajustada al nuevo tamaño)
        rectPadre.setPosition(padreX - 10, padreY);
        for (Rectangle muro : muros) {
            if (rectPadre.overlaps(muro)) {
                padreX = viejaX;
                break;
            }
        }

        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.D)) stateTimePadre += delta;
        else stateTimePadre = 0;

        camara.position.set(padreX, padreY + 150, 0); 
        camara.update();
    }

    private void dibujarCalle() {
        Gdx.gl.glClearColor(0.01f, 0.01f, 0.02f, 1);
        if (mapaRenderer != null) {
            mapaRenderer.setView(camara);
            mapaRenderer.render(); 
        }

        batch.setProjectionMatrix(camara.combined);
        batch.begin();
        if (padreCaminar != null) {
            TextureRegion frame = padreCaminar.getKeyFrame(stateTimePadre, true);
            
            // --- AJUSTE DE TAMAÑO COHERENTE (Escala 2x del original 32x64) ---
            float anchoFinal = 64; 
            float altoFinal = 128;
            
            // Calculamos drawX para que gire sobre su eje
            float drawX = mirandoIzquierda ? padreX + (anchoFinal/2) : padreX - (anchoFinal/2);
            float drawWidth = mirandoIzquierda ? -anchoFinal : anchoFinal;
            
            batch.draw(frame, drawX, padreY, drawWidth, altoFinal);
        }
        batch.end();

        // Efecto de Niebla sutil
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(camara.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        float oscNiebla = (float)Math.sin(timerNiebla * 0.3f) * 0.03f;
        shapeRenderer.setColor(0.5f, 0.5f, 0.6f, 0.07f + oscNiebla);
        shapeRenderer.rect(padreX - 400, padreY - 50, 800, 350);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void dibujarFundido() {
        if (alphaNegro > 0) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA); 
            shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0, 0, 0, alphaNegro);
            shapeRenderer.rect(0, 0, 800, 600);
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
    }

    @Override public void resize(int w, int h) { viewport.update(w, h); }
    @Override public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        if (mapa != null) mapa.dispose();
        if (spriteSheetPadre != null) spriteSheetPadre.dispose();
        if (sonidoLluvia != null) sonidoLluvia.dispose();
    }
}