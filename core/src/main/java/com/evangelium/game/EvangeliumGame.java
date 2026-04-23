package com.evangelium.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class EvangeliumGame extends ApplicationAdapter {
    private SpriteBatch batch;
    private OrthographicCamera camera;

    @Override
    public void create() {
        batch = new SpriteBatch();
        
        // La cámara nos permite ver el mundo. 800x480 es una buena resolución base.
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 480);
    }

    @Override
    public void render() {
        // Establecemos el color de fondo: Azul muy oscuro (R, G, B, Alpha)
        Gdx.gl.glClearColor(0.02f, 0.02f, 0.1f, 1); 
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        // Aquí es donde dibujaremos todo en el futuro
        batch.end();
    }
    
    @Override
    public void dispose() {
        batch.dispose();
    }
}