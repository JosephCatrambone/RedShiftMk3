package io.xoana.redshift.shaders

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.Shader
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.PointLightsAttribute
import com.badlogic.gdx.graphics.g3d.environment.PointLight
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.GdxRuntimeException
import io.xoana.redshift.MinHeap

class PBRShader : Shader {
	var camera: Camera? = null // Set when rendering begins.  Cleared at end.

	val MAX_LIGHTS = 4
	val shaderProgram : ShaderProgram

	// If we use setAttribute/setUniform, etc. it does a look up based on the string.
	// Prefetching those names will save us the compute time.
	val worldTransformUniformIndex: Int
	val cameraTransformUniformIndex: Int

	// From ShaderProgram.
	//val positionAttributeIndex: Int

	val lightIntensityUniformIndices = IntArray(MAX_LIGHTS)
	val lightColorUniformIndices = IntArray(MAX_LIGHTS)
	val lightPositionUniformIndices = IntArray(MAX_LIGHTS)
	val lightSizeUniformIndices = IntArray(MAX_LIGHTS)

	init {
		shaderProgram = ShaderProgram(Gdx.files.internal("shaders/vertex.glsl"), Gdx.files.internal("shaders/fragment.glsl"))
		if(!shaderProgram.isCompiled) {
			throw GdxRuntimeException(shaderProgram.log)
		}

		worldTransformUniformIndex = shaderProgram.getUniformLocation("u_worldTransform")
		cameraTransformUniformIndex = shaderProgram.getUniformLocation("u_cameraTransform")
		// Position, etc, are defined in the VertexAttributes and ShaderProgram.
		// We're using those instead of this because MeshBuilder will automatically populate them.
		//positionAttributeIndex = shaderProgram.getAttributeLocation("a_position")
		//positionAttributeIndex = VertexAttributes.Usage.Position

		for(i in 0 until MAX_LIGHTS) {
			lightIntensityUniformIndices[i] = shaderProgram.getUniformLocation("u_light${i}_intensity")
			lightColorUniformIndices[i] = shaderProgram.getUniformLocation("u_light${i}_color")
			lightPositionUniformIndices[i] = shaderProgram.getUniformLocation("u_light${i}_position")
			lightSizeUniformIndices[i] = shaderProgram.getUniformLocation("u_light${i}_size")
		}

		// TODO: We should assert if any values are -1 since that means we didn't find the variable.
	}

	override fun init() {}

	override fun canRender(instance: Renderable): Boolean {
		return instance.material.has(ColorAttribute.Diffuse)
	}

	override fun compareTo(other: Shader?): Int = 0

	override fun begin(camera: Camera, context: RenderContext) {
		this.camera = camera
		shaderProgram.begin()
		shaderProgram.setUniformMatrix(cameraTransformUniformIndex, camera.combined)
		context.setDepthTest(GL20.GL_LEQUAL)
		context.setCullFace(GL20.GL_BACK)
	}

	override fun render(renderable: Renderable) {
		// Grab the lights from the environment and assign them based on proximity.
		val pointLights = renderable.environment.get(PointLightsAttribute.Type) as? PointLightsAttribute
		val lightSorter = MinHeap<PointLight>(pointLights?.lights?.size ?: 0, Comparator({ p1, p2 ->
			// We actually reverse this.  When we pop something, we want it to have the greatest distance.
			p1.position.dst2(camera!!.position).compareTo(p2.position.dst2(camera!!.position))
		}))
		if(pointLights != null) {
			pointLights.lights.forEach({ light ->
				lightSorter.push(light)
			})
		}

		// Add the lights, sorted by distance, to the object, stopping at the limit.
		for(i in 0 until minOf(lightSorter.size, MAX_LIGHTS)) {
			val light = lightSorter.pop()!!
			shaderProgram.setUniformf(lightIntensityUniformIndices[i], light.intensity)
			shaderProgram.setUniformf(lightPositionUniformIndices[i], light.position)
			shaderProgram.setUniformf(lightColorUniformIndices[i], light.color)
			//Gdx.gl30.glVertexAttrib1f(lightIntensityUniformIndices[i], light.intensity);
			//Gdx.gl30.glVertexAttrib4f(lightColorUniformIndices[i], light.color.r, light.color.g, light.color.b, light.color.a);
			//Gdx.gl30.glVertexAttrib3f(lightPositionUniformIndices[i], light.position.x, light.position.y, light.position.z);
		}

		// Set transform.
		shaderProgram.setUniformMatrix(worldTransformUniformIndex, renderable.worldTransform)

		// Set color data.
		val ca:ColorAttribute = renderable.material.get(ColorAttribute.Diffuse) as ColorAttribute
		//shaderProgram.setAttributef(normalAttributeIndex, ca.color.r, ca.color.g, ca.color.b, ca.color.a)
		shaderProgram.setAttributef(ShaderProgram.COLOR_ATTRIBUTE, ca.color.r, ca.color.g, ca.color.b, ca.color.a)

		renderable.meshPart.render(shaderProgram)
	}

	override fun end() {
		shaderProgram.end()
		this.camera = null
	}

	override fun dispose() {
		shaderProgram.dispose()
	}
}