package io.xoana.redshift.levels

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import io.xoana.redshift.Line
import io.xoana.redshift.Vec

class Level(val sectors: List<Sector>) {
	private val model: Model
	val modelInstance: ModelInstance

	init {
		// Be sure our sectors have correctly identified their neighbors for path-checking.
		sectors.forEach { s -> s.updateNeighbors(sectors) }

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

	fun isValidMovement(movement: Line, maxFloorDelta:Float) {
		// Check this movement against the edges in this sector and the other sectors.
		// If it crosses a wall (not a gap), it's not allowed.
		// If it crosses a gap, return if the floor distance of the new sector is sufficiently similar.
	}

	fun dispose() {
		model.dispose()
	}
}