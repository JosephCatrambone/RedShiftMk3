package io.xoana.redshift.scenes

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT
import com.badlogic.gdx.graphics.GL20.GL_DEPTH_BUFFER_BIT
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.PointLight
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import io.xoana.redshift.shaders.PBRShader

class MainGameScreen : Screen() {

	val renderContext:RenderContext;
	val camera:Camera
	val environment:Environment
	val modelBatch:ModelBatch
	val shader:Shader

	val debugModel : Model
	val debugModelInstance : ModelInstance


	init {
		renderContext = RenderContext(DefaultTextureBinder(DefaultTextureBinder.WEIGHTED, 1))
		camera = PerspectiveCamera(80f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
		environment = Environment()

		shader = PBRShader()
		shader.init()

		environment.add(PointLight().set(Color(1.0f, 1.0f, 1.0f, 1.0f), 0.0f, 0.0f, 10.0f, 1.0f))

		camera.position.set(10f, 10f, 10f)
		camera.lookAt(0f, 0f, 0f)
		camera.update(true)

		modelBatch = ModelBatch(renderContext)

		val mb = ModelBuilder()
		debugModel = mb.createBox(1f, 1f, 1f, Material(ColorAttribute.createDiffuse(1f, 0f, 1f, 1.0f)), (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong())
		debugModelInstance = ModelInstance(debugModel)
		debugModelInstance.transform.idt()

		//ModelLoader modelLoader = new G3dModelLoader(new JsonReader());
		//model = modelLoader.loadModel(Gdx.files.internal("data/invaders.g3dj"));
		//NodePart blockPart = model.getNode("ship").parts.get(0);

		// We can make renderables by hand or we can let model batch convert them.
		/*
		renderable = Renderable()
		renderable.meshPart.set(debugModel.meshParts.first())
		renderable.environment = environment
		renderable.worldTransform.idt()

		renderContext.begin()
		shader.begin(camera, renderContext)
		shader.render(renderable)
		shader.end()
		renderContext.end()
		*/
	}

	override fun render() {
		camera.update()

		Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
		Gdx.gl.glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

		modelBatch.begin(camera)
		modelBatch.render(debugModelInstance)
		modelBatch.end()
	}

	override fun update(deltaTime: Float) {

	}

	override fun dispose() {
		super.dispose()
		shader.dispose()
	}

}