package io.xoana.redshift.shaders

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.Shader
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.PointLightsAttribute
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.GdxRuntimeException

class PBRShader : Shader {
	val MAX_LIGHTS = 1
	val shaderProgram : ShaderProgram

	// If we use setAttribute/setUniform, etc. it does a look up based on the string.
	// Prefetching those names will save us the compute time.
	val worldTransformUniformIndex: Int
	val cameraTransformUniformIndex: Int

	val diffuseAttributeIndex: Int
	val normalAttributeIndex: Int

	val lightIntensityAttributeIndices = IntArray(MAX_LIGHTS)
	val lightColorAttributeIndices = IntArray(MAX_LIGHTS)
	val lightPositionAttributeIndices = IntArray(MAX_LIGHTS)
	val lightSizeAttributeIndices = IntArray(MAX_LIGHTS)

	init {
		shaderProgram = ShaderProgram(Gdx.files.internal("shaders/vertex.glsl"), Gdx.files.internal("shaders/fragment.glsl"))
		if(!shaderProgram.isCompiled) {
			throw GdxRuntimeException(shaderProgram.log)
		}

		worldTransformUniformIndex = shaderProgram.getUniformLocation("u_worldTransform")
		cameraTransformUniformIndex = shaderProgram.getUniformLocation("u_cameraTransform")
		diffuseAttributeIndex = shaderProgram.getAttributeLocation("a_diffuse")
		normalAttributeIndex = shaderProgram.getAttributeLocation("a_normal")

		for(i in 0 until MAX_LIGHTS) {
			lightIntensityAttributeIndices[i] = shaderProgram.getAttributeLocation("a_light${i}_intensity")
			lightColorAttributeIndices[i] = shaderProgram.getAttributeLocation("a_light${i}_color")
			lightPositionAttributeIndices[i] = shaderProgram.getAttributeLocation("a_light${i}_position")
			lightSizeAttributeIndices[i] = shaderProgram.getAttributeLocation("a_light${i}_size")
		}
	}

	override fun init() {}

	override fun canRender(instance: Renderable): Boolean {
		return instance.material.has(ColorAttribute.Diffuse)
	}

	override fun compareTo(other: Shader?): Int = 0

	override fun begin(camera: Camera, context: RenderContext) {
		shaderProgram.begin()
		shaderProgram.setUniformMatrix(cameraTransformUniformIndex, camera.combined)
		context.setDepthTest(GL20.GL_LEQUAL)
		context.setCullFace(GL20.GL_BACK)
	}

	override fun render(renderable: Renderable) {
		// Grab the lights from the environment and assign them based on proximity.
		val pointLights = renderable.environment.get(PointLightsAttribute.Type) as PointLightsAttribute
		pointLights.lights.forEachIndexed({ i, light ->
			Gdx.gl20.glVertexAttrib1f(lightIntensityAttributeIndices[i], light.intensity);
			Gdx.gl20.glVertexAttrib4f(lightColorAttributeIndices[i], light.color.r, light.color.g, light.color.b, light.color.a);
			Gdx.gl20.glVertexAttrib3f(lightPositionAttributeIndices[i], light.position.x, light.position.y, light.position.z);
		})

		// Set transform.
		shaderProgram.setUniformMatrix(worldTransformUniformIndex, renderable.worldTransform)

		// Set color data.
		val ca:ColorAttribute = renderable.material.get(ColorAttribute.Diffuse) as ColorAttribute
		//shaderProgram.setAttributef(normalAttributeIndex, ca.color.r, ca.color.g, ca.color.b, ca.color.a)
		shaderProgram.setUniformf(diffuseAttributeIndex, ca.color.r, ca.color.g, ca.color.b, ca.color.a)

		renderable.meshPart.render(shaderProgram)
	}

	override fun end() {
		shaderProgram.end()
	}

	override fun dispose() {
		shaderProgram.dispose()
	}
}