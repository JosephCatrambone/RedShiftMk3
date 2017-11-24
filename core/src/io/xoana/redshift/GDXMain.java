package io.xoana.redshift;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.xoana.redshift.scenes.MainGameScreen;
import io.xoana.redshift.scenes.Screen;

import java.util.Stack;

public class GDXMain extends ApplicationAdapter {
	public static Stack<Screen> screenStack = new Stack<Screen>();

	@Override
	public void create () {
		screenStack.push(new MainGameScreen());
	}

	@Override
	public void render () {
		//Gdx.gl.glClearColor(1, 0, 0, 1);
		//Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		screenStack.peek().render();
		screenStack.peek().update(Gdx.graphics.getDeltaTime());
	}
	
	@Override
	public void dispose () {

	}
}
