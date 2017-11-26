package io.xoana.redshift.levels

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder

class Level(val sectors: List<Sector>) {
	val model: Model
	val modelInstance: ModelInstance

	init {
		// Build map.
		val modelBuilder = ModelBuilder()
		modelBuilder.begin()
		// For each sector, build a new node.
		sectors.forEachIndexed({i, s ->
			val mat = Material()
			mat.set(ColorAttribute.createDiffuse(1.0f, 1.0f, 1.0f, 1.0f))
			val node = modelBuilder.node()
			node.id = "sector_$i"
			val meshBuilder: MeshPartBuilder = modelBuilder.part(
					"sector_$i",
					GL20.GL_TRIANGLES,
					//(VertexAttributes.Usage.Position or VertexAttributes.Usage.ColorUnpacked or VertexAttributes.Usage.Normal or VertexAttributes.Usage.TextureCoordinates).toLong(),
					VertexAttributes.Usage.Position.toLong(), // We will only be providing location for now.  Soon UV, etc.
					mat
			)
			//node.translation.set(10f, 0f, 0f)
			//meshBuilder.cone(5f, 5f, 5f, 10)
			s.buildMesh(meshBuilder)
		})

		model = modelBuilder.end()
		modelInstance = ModelInstance(model)
	}

	fun dispose() {
		model.dispose()
	}
}