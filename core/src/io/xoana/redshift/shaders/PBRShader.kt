package io.xoana.redshift.shaders

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g3d.Attribute
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.Shader
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.GdxRuntimeException

class PBRShader : Shader {
	val shaderProgram : ShaderProgram

	// If we use setAttribute/setUniform, etc. it does a look up based on the string.
	// Prefetching those names will save us the compute time.
	val u_worldTransform_index: Int
	val u_cameraTransform_index: Int
	val a_diffuse_index: Int
	val a_normal_index: Int

	init {
		shaderProgram = ShaderProgram(Gdx.files.internal("shaders/vertex.glsl"), Gdx.files.internal("shaders/fragment.glsl"))
		if(!shaderProgram.isCompiled) {
			throw GdxRuntimeException(shaderProgram.log)
		}

		u_worldTransform_index = shaderProgram.getUniformLocation("u_worldTransform")
		u_cameraTransform_index = shaderProgram.getUniformLocation("u_cameraTransform")
		a_diffuse_index = shaderProgram.getAttributeLocation("a_diffuse")
		a_normal_index = shaderProgram.getAttributeLocation("a_normal")
	}

	override fun init() {}

	override fun canRender(instance: Renderable): Boolean {
		return instance.material.has(ColorAttribute.Diffuse)
	}

	override fun compareTo(other: Shader?): Int = 0

	override fun begin(camera: Camera, context: RenderContext) {
		shaderProgram.begin()
		shaderProgram.setUniformMatrix(u_cameraTransform_index, camera.combined)
		context.setDepthTest(GL20.GL_LEQUAL)
		context.setCullFace(GL20.GL_BACK)
	}

	override fun render(renderable: Renderable) {
		shaderProgram.setUniformMatrix(u_worldTransform_index, renderable.worldTransform)
		val ca:ColorAttribute = renderable.material.get(ColorAttribute.Diffuse) as ColorAttribute
		//shaderProgram.setAttributef(a_normal_index, ca.color.r, ca.color.g, ca.color.b, ca.color.a)
		shaderProgram.setUniformf(a_normal_index, ca.color.r, ca.color.g, ca.color.b, ca.color.a)
		renderable.meshPart.render(shaderProgram)
	}

	override fun end() {
		shaderProgram.end()
	}

	override fun dispose() {
		shaderProgram.dispose()
	}
}