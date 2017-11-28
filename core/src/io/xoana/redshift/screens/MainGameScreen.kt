package io.xoana.redshift.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT
import com.badlogic.gdx.graphics.GL20.GL_DEPTH_BUFFER_BIT
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.PointLight
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Stage
import io.xoana.redshift.gameobjects.GameObject
import io.xoana.redshift.levels.Sector
import io.xoana.redshift.shaders.PBRShader

class MainGameScreen : Screen() {

	val renderContext:RenderContext;
	val camera:Camera
	val environment:Environment
	val modelBatch:ModelBatch
	val shader:Shader

	val uiStage: Stage
	val font: BitmapFont

	val debugModel : Model
	val debugModelInstance : ModelInstance
	var debugTimeAccumulator: Float = 0f

	// In theory we could push this list down into the sector.  Would make for faster access, but I don't think sector cares.
	// In this way, MainGame has the sole responsibility of keeping track of the objects as they move.
	val gameObjectSectorMap = mutableMapOf<GameObject, Sector>()
	val sectorGameObjectsMap = mutableMapOf<Sector, MutableSet<GameObject>>()

	init {
		renderContext = RenderContext(DefaultTextureBinder(DefaultTextureBinder.WEIGHTED, 1))
		camera = PerspectiveCamera(80f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
		environment = Environment()

		uiStage = Stage()
		font = BitmapFont()

		shader = PBRShader()
		shader.init()

		environment.add(PointLight().set(Color(1.0f, 1.0f, 1.0f, 1.0f), 10.0f, 0.0f, 0.0f, 10.0f))

		camera.position.set(10f, 10f, 10f)
		camera.lookAt(0f, 0f, 0f)
		camera.update(true)

		modelBatch = ModelBatch(renderContext)

		val mb = ModelBuilder()
		debugModel = mb.createBox(10f, 10f, 10f, Material(ColorAttribute.createDiffuse(1f, 0f, 1f, 1.0f)), (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong())
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
		if(isVisible(debugModelInstance)) {
			modelBatch.render(debugModelInstance, environment, shader)
		}
		modelBatch.end()
	}

	protected fun isVisible(instance: ModelInstance, cam: Camera=camera):Boolean {
		// By default, use our internal camera.
		val pos = Vector3()
		instance.transform.getTranslation(pos)
		return cam.frustum.pointInFrustum(pos)
	}

	override fun update(deltaTime: Float) {
		debugTimeAccumulator += deltaTime
		//debugModelInstance.transform.trn(1f*Math.cos(0.5*Gdx.graphics.rawDeltaTime.toDouble()).toFloat(), 1f*Math.sin(Gdx.graphics.rawDeltaTime.toDouble()).toFloat(), 0.0f)
		debugModelInstance.transform.setTranslation(10f*Math.cos(debugTimeAccumulator.toDouble()).toFloat(), 10f*Math.sin(debugTimeAccumulator.toDouble()).toFloat(), 0.0f)
	}

	override fun dispose() {
		super.dispose()
		shader.dispose()
	}

	override fun resize(width: Int, height: Int) {
		super.resize(width, height)
		uiStage.viewport.update(width, height, true)
	}

}