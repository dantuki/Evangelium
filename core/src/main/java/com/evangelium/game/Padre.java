package com.evangelium.game;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Padre {
    private Animation<TextureRegion> caminata;
    private float tiempo;
    private Texture hojaSprites;
    private TextureRegion frameActual;

    public Padre() {
        // Cargamos la tira de 8 frames
        hojaSprites = new Texture("assets/padre/Walk.png"); 
        
        // Dividimos la imagen: 1 fila, 8 columnas
        TextureRegion[][] tmp = TextureRegion.split(hojaSprites, 
                hojaSprites.getWidth() / 8, 
                hojaSprites.getHeight());

        // Pasamos los frames a la animación (0.1f es la velocidad)
        TextureRegion[] framesCaminata = new TextureRegion[8];
        for (int i = 0; i < 8; i++) {
            framesCaminata[i] = tmp[0][i];
        }

        caminata = new Animation<>(0.1f, framesCaminata);
        tiempo = 0f;
    }

    public void dibujar(SpriteBatch batch, float x, float y, float delta) {
        tiempo += delta;
        frameActual = caminata.getKeyFrame(tiempo, true);
        batch.draw(frameActual, x, y);
    }
}