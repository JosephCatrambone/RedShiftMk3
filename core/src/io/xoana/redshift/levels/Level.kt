package io.xoana.redshift.levels

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.utils.Array as GDXArray
import io.xoana.redshift.Line
import io.xoana.redshift.Vec

class Level(val sectors: List<Sector>) {
	private val models : List<Model>
	val visibleSectors : GDXArray<ModelInstance>

	init {
		// Be sure our sectors have correctly identified their neighbors for path-checking.
		sectors.forEach { s -> s.updateNeighbors(sectors) }

		// Rebuild geometry.
		models = List<Model>(sectors.size, { i -> sectors[i].buildModel() })

		// For now, always draw all sectors.
		visibleSectors = GDXArray<ModelInstance>(sectors.size)
		// Fucking GDXArray:
		//models.forEachIndexed({ index, model -> visibleSectors[index] = ModelInstance(model) })
		for(i in 0 until sectors.size) {
			visibleSectors.add(ModelInstance(models[i]))
		}
	}

	fun isValidMovement(movement: Line, maxFloorDelta:Float) {
		// Check this movement against the edges in this sector and the other sectors.
		// If it crosses a wall (not a gap), it's not allowed.
		// If it crosses a gap, return if the floor distance of the new sector is sufficiently similar.
	}

	fun dispose() {
		visibleSectors.clear()
		models.forEach { m -> m.dispose() }
	}
}