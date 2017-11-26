package io.xoana.redshift;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;
import io.xoana.redshift.editors.LevelEditorScreen;
import io.xoana.redshift.screens.MainGameScreen;
import io.xoana.redshift.screens.Screen;

import java.util.Stack;

public class GDXMain extends ApplicationAdapter {
	public static Stack<Screen> screenStack = new Stack<Screen>();
	public static AssetManager assetManager;
	public static Long frameCount = 0L;

	public float timeToNextCapture = 0f;
	public int lastFrameCaptureCount = 0;

	@Override
	public void create () {
		assetManager = new AssetManager();
		//screenStack.push(new MainGameScreen());
		screenStack.push(new LevelEditorScreen());

		// We should add a post-init to set up the input listeners and such.
		screenStack.peek().resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
	}

	@Override
	public void render () {
		//Gdx.gl.glClearColor(1, 0, 0, 1);
		//Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		screenStack.peek().render();
		float dt = Gdx.graphics.getDeltaTime();
		TweenManager.update(dt);
		screenStack.peek().update(dt);

		frameCount++; // Don't worry about rollaround.  At 1000 FPS it will still take 292000 years to roll over.

		// FPS Counter:
		lastFrameCaptureCount++;
		if(timeToNextCapture <= 0f) {
			timeToNextCapture = 1f;
			System.out.println("FPS: " + lastFrameCaptureCount);
			lastFrameCaptureCount = 0;
		}
		timeToNextCapture -= dt;
	}
	
	@Override
	public void dispose () {

	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		screenStack.peek().resize(width, height);
	}
}
