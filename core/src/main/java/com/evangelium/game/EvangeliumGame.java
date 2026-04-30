package com.evangelium.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
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

    private float alphaNegro = 1; 
    private boolean fundiendoACalle = false;

    // --- EL PADRE (CONFIGURACIÓN REALISTA) ---
    private Texture padreTexture;
    private TextureRegion padreRegion;
    private float padreX = 150; 
    private float padreY = 95; 
    private float velocidad = 120f; 
    private boolean mirandoIzquierda = false;
    
    // Medidas para evitar el estiramiento
    private float anchoProporcional;
    private float altoDeseado = 95f; // Ajusta esto para cambiar el tamaño general

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
        camara.zoom = 0.7f; 

        layout = new GlyphLayout();
        font = new BitmapFont(); 
        font.getData().setScale(1.1f);

        muros = new Array<>();
        // Rectángulo de colisión (más pequeño que el sprite para realismo)
        rectPadre = new Rectangle(padreX, padreY, 25, 15);

        cargarAssets();
    }

    private void cargarAssets() {
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

            try {
                sonidoLluvia = Gdx.audio.newMusic(Gdx.files.internal("lluvia.mp3"));
                sonidoLluvia.setLooping(true);
                sonidoTrueno = Gdx.audio.newSound(Gdx.files.internal("trueno.wav"));
            } catch (Exception e) {}

            // --- CARGA Y CÁLCULO DE PROPORCIÓN ---
            if (Gdx.files.internal("padre/IDLE1.png").exists()) {
                padreTexture = getTransparentTexture("padre/IDLE1.png");
                padreRegion = new TextureRegion(padreTexture);
                
                // Calculamos el ancho basándonos en la imagen real para que no se alargue
                float aspect = (float) padreRegion.getRegionWidth() / padreRegion.getRegionHeight();
                anchoProporcional = altoDeseado * aspect;
                
            } else {
                System.out.println("ERROR: No se encontró IDLE1.png");
                // Fallback: Textura roja si no existe el archivo
                Pixmap p = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
                p.setColor(Color.RED); p.fill();
                padreTexture = new Texture(p);
                padreRegion = new TextureRegion(padreTexture);
                anchoProporcional = altoDeseado;
                p.dispose();
            }

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
            System.out.println("Error cargando assets: " + e.getMessage());
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
        if (!fundiendoACalle && alphaNegro > 0) alphaNegro -= delta * 0.5f;
        
        timerTexto += delta;
        if (timerTexto > 0.06f && indiceCaracter < textoCompleto.length()) {
            textoAMostrar += textoCompleto.charAt(indiceCaracter);
            indiceCaracter++;
            timerTexto = 0;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) fundiendoACalle = true;

        if (fundiendoACalle) {
            alphaNegro += delta * 1.5f;
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
        if (alphaNegro > 0) alphaNegro -= delta * 1.0f;
        if (alphaNegro < 0) alphaNegro = 0;

        timerNiebla += delta;
        proximoTrueno -= delta;
        if (proximoTrueno <= 0) {
            if (sonidoTrueno != null) sonidoTrueno.play();
            proximoTrueno = 8 + (float)Math.random() * 10;
        }

        float viejaX = padreX;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) { padreX -= velocidad * delta; mirandoIzquierda = true; }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) { padreX += velocidad * delta; mirandoIzquierda = false; }

        // Actualizamos rectángulo de colisión centrado en los pies
        rectPadre.setPosition(padreX - (rectPadre.width/2), padreY);
        for (Rectangle muro : muros) {
            if (rectPadre.overlaps(muro)) {
                padreX = viejaX;
                break;
            }
        }

        camara.position.set(padreX, padreY + 150, 0); 
        camara.update();
    }

    private void dibujarCalle() {
        if (mapaRenderer != null) {
            mapaRenderer.setView(camara);
            mapaRenderer.render(); 
        }

        batch.setProjectionMatrix(camara.combined);
        batch.begin();
        
        if (padreRegion != null) {
            // Calculamos X según la dirección para que no "salte" al girar
            float drawX = mirandoIzquierda ? padreX + (anchoProporcional / 2) : padreX - (anchoProporcional / 2);
            float drawWidth = mirandoIzquierda ? -anchoProporcional : anchoProporcional;
            
            batch.draw(padreRegion, drawX, padreY, drawWidth, altoDeseado);
        }
        
        batch.end();

        // Niebla
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(camara.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        float osc = (float)Math.sin(timerNiebla * 0.3f) * 0.03f;
        shapeRenderer.setColor(0.5f, 0.5f, 0.6f, 0.07f + osc);
        shapeRenderer.rect(padreX - 400, padreY - 50, 800, 350);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void dibujarFundido() {
        if (alphaNegro > 0) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0, 0, 0, alphaNegro);
            shapeRenderer.rect(0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
    }

    @Override public void resize(int w, int h) { viewport.update(w, h); }
    
    @Override public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        if (mapa != null) mapa.dispose();
        if (padreTexture != null) padreTexture.dispose();
    }

    private Texture getTransparentTexture(String path) {
        try {
            Pixmap pixmap = new Pixmap(Gdx.files.internal(path));
            Pixmap trans = new Pixmap(pixmap.getWidth(), pixmap.getHeight(), Pixmap.Format.RGBA8888);
            float tol = 0.15f; 
            for (int y = 0; y < pixmap.getHeight(); y++) {
                for (int x = 0; x < pixmap.getWidth(); x++) {
                    int color = pixmap.getPixel(x, y);
                    Color c = new Color(color);
                    if (c.r > (1f - tol) && c.g < tol && c.b > (1f - tol)) { 
                        trans.drawPixel(x, y, 0x00000000); 
                    } else {
                        trans.drawPixel(x, y, color); 
                    }
                }
            }
            Texture texture = new Texture(trans);
            pixmap.dispose(); trans.dispose();
            return texture;
        } catch (Exception e) {
            return new Texture(Gdx.files.internal(path));
        }
    }
}