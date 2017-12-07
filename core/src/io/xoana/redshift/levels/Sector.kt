package io.xoana.redshift.levels

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL30
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import io.xoana.redshift.Line
import io.xoana.redshift.Polygon
import io.xoana.redshift.Vec

class Sector(
		val id: Int,
		var walls: Polygon,
		var floorHeight: Float,
		var ceilingHeight: Float,
		var floorMaterial: Material,
		var ceilingMaterial: Material,
		var wallMaterial: Material
) {
	// On the one hand, if we copied the reference to the point from the neighbor, that would mean edits saved us the trouble.
	// Not sure how well we'd adapt to changes, though.
	val neighbors = mutableListOf<Sector?>() // element at zero references the wall made by points 0,1.

	fun updateNeighbors(others: List<Sector>, epsilon:Float=1e-6f) {
		// Clear the current nbr list.  First make sure the size matches, then set all the elements to null.
		neighbors.clear()
		while(neighbors.size < walls.points.size) { // Inefficient
			neighbors.add(null)
		}

		// TODO: Perf optimization.  This is runtime of O(n^2) for n = #pts.
		// Given a list of the sectors, will update the neighbors.
		getWallIterator().withIndex().forEach { witer ->
			val i = witer.index
			val w1 = witer.value
			// We can't continue with a forEach,
			nextWall@for(j in 0 until others.size) {
				if(this == others[j]) {
					continue // We can't be neighbors with ourselv.
				}
				val nbrCandidate = others[j]
				// We we have a common wall?
				nbrCandidate.getWallIterator().forEach({ w2 ->
					if(
						w1.start.distanceSquared(w2.start) < epsilon && w1.end.distanceSquared(w2.end) < epsilon ||
						w1.end.distanceSquared(w2.start) < epsilon && w1.start.distanceSquared(w2.end) < epsilon
							) {
						neighbors[i] = nbrCandidate
					}
				})
			}
		}
	}

	// Each Model has Mesh[], MeshPart[], and Material[].
	// Mesh has Vert[] and Indices[].
	// MeshPart has offset and size which points into mesh.
	// Already tried passing in a MeshPartBuilder, but we need more control over our model production and don't want one enormous model.
	fun buildModel(): Model {
		val mb = ModelBuilder()
		lateinit var mp:MeshPartBuilder
		mb.begin()

		//val modelAttributes = (VertexAttributes.Usage.Position or VertexAttributes.Usage.ColorUnpacked or VertexAttributes.Usage.Normal or VertexAttributes.Usage.TextureCoordinates).toLong()
		val modelAttributes = (VertexAttributes.Usage.Position).toLong()

		// Floor.
		mp = mb.part("sector${id}_floor", GL30.GL_TRIANGLES, modelAttributes, floorMaterial)
		val floorVerts = FloatArray(walls.points.size*3, {i ->
			when(i%3) {
				0 -> walls.points[i/3].x
				1 -> walls.points[i/3].y
				2 -> floorHeight
				else -> throw Exception("Impossible: $i%3 >= 3")
			}
		})
		val floorIndices = walls.triangulate(Vec(0f, 0f, 1f), false).map { i -> i.toShort() }.toShortArray()
		mp.addMesh(floorVerts, floorIndices)
		//mp.setUVRange(0f, 0f, 1f, 1f)

		// Ceiling.
		mp = mb.part("sector${id}_ceiling", GL30.GL_TRIANGLES, modelAttributes, ceilingMaterial)
		val ceilingVerts = FloatArray(walls.points.size*3, {i ->
			when(i%3) {
				0 -> walls.points[i/3].x
				1 -> walls.points[i/3].y
				2 -> ceilingHeight
				else -> throw Exception("Impossible: $i%3 >= 3")
			}
		})
		val ceilingIndices = walls.triangulate(Vec(0f, 0f, -1f), false).map{i -> i.toShort()}.toShortArray()
		mp.addMesh(ceilingVerts, ceilingIndices)

		// Walls.
		mp = mb.part("sector${id}_walls", GL30.GL_TRIANGLES, modelAttributes, wallMaterial)
		for(i in 0 until walls.points.size) {
			val p0 = walls.points[i]
			val p1 = walls.points[(i+1)%walls.points.size]
			val nbr = neighbors[i]
			// If this wall has a neighbor, we do things differently.
			if(nbr == null) {
				val pts = makeWall(p0, p1, floorHeight, ceilingHeight)
				val topLeft = pts[0]
				val bottomLeft = pts[1]
				val bottomRight = pts[2]
				val topRight = pts[3]

				// UV

				// Quad, CCW.
				mp.rect(topLeft, bottomLeft, bottomRight, topRight)
				//meshPartBuilder.triangle(bottomLeft, bottomRight, topLeft)
				//meshPartBuilder.triangle(topLeft, bottomRight, topRight)
			} else {
				// We DO need to handle this differently.
				// If the floor of the neighbor is higher than ours, we build the base that leads up.
				if(nbr.floorHeight > this.floorHeight) {
					val pts = makeWall(p0, p1, floorHeight, nbr.floorHeight)
					// Left triangle, CCW.
					mp.triangle(pts[0], pts[1], pts[2])
					// Right triangle, also CCW.
					mp.triangle(pts[0], pts[2], pts[3])
				}

				// We also need to build the ceiling.
				if(nbr.ceilingHeight < this.ceilingHeight) {
					val pts = makeWall(p0, p1, nbr.ceilingHeight, ceilingHeight)
					mp.triangle(pts[0], pts[1], pts[2])
					// Right triangle, also CCW.
					mp.triangle(pts[0], pts[2], pts[3])
				}
			}
		}

		return mb.end()
	}

	private fun makeWall(p0:Vec, p1:Vec, floor:Float, ceil:Float): Array<MeshPartBuilder.VertexInfo> {
		val topLeft = MeshPartBuilder.VertexInfo()
		val topRight = MeshPartBuilder.VertexInfo()
		val bottomLeft = MeshPartBuilder.VertexInfo()
		val bottomRight = MeshPartBuilder.VertexInfo()

		// Location
		topLeft.hasPosition = true
		topLeft.position.set(p0.x, p0.y, ceil)
		topRight.hasPosition = true
		topRight.position.set(p1.x, p1.y, ceil)
		bottomLeft.hasPosition = true
		bottomLeft.position.set(p0.x, p0.y, floor)
		bottomRight.hasPosition = true
		bottomRight.position.set(p1.x, p1.y, floor)

		// Calculate surface normal.
		val a = Vec(p0.x, p0.y, ceil)
		val b = Vec(p1.x, p1.y, ceil)
		val c = Vec(p0.x, p0.y, floor)
		val norm = (b-a).cross3(c-a).normalized()

		// Normal
		topLeft.hasNormal = true
		topLeft.normal.set(norm.toGDXVector3())
		topRight.hasNormal = true
		topRight.normal.set(topLeft.normal.cpy())
		bottomLeft.hasNormal = true
		bottomLeft.normal.set(topLeft.normal.cpy())
		bottomRight.hasNormal = true
		bottomRight.normal.set(topLeft.normal.cpy())

		return arrayOf<MeshPartBuilder.VertexInfo>(topLeft, bottomLeft, bottomRight, topRight)
	}

	fun calculateCenter(): Vec {
		return walls.points.fold(Vec(), { acc, v -> acc+v}) / walls.points.size.toFloat()
	}

	fun getWallIterator(): Iterator<Line> {
		return object : Iterator<Line> {
			private var currentPoint:Int = 0
			private val points = walls.points

			override fun hasNext(): Boolean = currentPoint < points.size

			override fun next(): Line {
				val p0 = points[currentPoint]
				val p1 = points[(currentPoint+1)%points.size]
				currentPoint++
				return Line(p0, p1)
			}

		}
	}
}
