package io.xoana.redshift.levels

import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import io.xoana.redshift.Line
import io.xoana.redshift.Polygon
import io.xoana.redshift.Vec

class Sector(
		var walls: Polygon,
		var floorHeight: Float,
		var ceilingHeight: Float
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
	// We handle this construction by passing a MeshBuilder into our method.
	fun buildMesh(meshPartBuilder: MeshPartBuilder) {
		// Make the floor verts.
		val floorVerts = FloatArray(walls.points.size*3, {i ->
			when(i%3) {
				0 -> walls.points[i/3].x
				1 -> walls.points[i/3].y
				2 -> floorHeight
				else -> throw Exception("Impossible: $i%3 >= 3")
			}
		})
		val floorIndices = walls.triangulate(Vec(0f, 0f, 1f), clockwise = false).map { i -> i.toShort() }.toShortArray()
		meshPartBuilder.addMesh(floorVerts, floorIndices)

		// Build the ceiling.
		val ceilingVerts = FloatArray(walls.points.size*3, {i ->
			when(i%3) {
				0 -> walls.points[i/3].x
				1 -> walls.points[i/3].y
				2 -> ceilingHeight
				else -> throw Exception("Impossible: $i%3 >= 3")
			}
		})
		val ceilingIndices = walls.triangulate(Vec(0f, 0f, -1f), clockwise = false).map { i -> i.toShort() }.toShortArray()
		meshPartBuilder.addMesh(ceilingVerts, ceilingIndices)

		// Make the walls.
		// GL_CCW is front-facing.
		/*
		for(i in 0 until walls.points.size) {
			val p0 = walls.points[i]
			val p1 = walls.points[(i+1)%walls.points.size]
			// Left triangle, CCW.
			meshPartBuilder.triangle(
				Vector3(p0.x, p0.y, floorHeight),
				Vector3(p1.x, p1.y, floorHeight),
				Vector3(p0.x, p0.y, ceilingHeight)
			)
			// Right triangle, also CCW.
			meshPartBuilder.triangle(
				Vector3(p0.x, p0.y, ceilingHeight),
				Vector3(p1.x, p1.y, floorHeight),
				Vector3(p1.x, p1.y, ceilingHeight)
			)
		}
		*/
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
