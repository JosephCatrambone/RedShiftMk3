package io.xoana.redshift

import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder

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
		while(neighbors.size < walls.points.size-1) { // Inefficient
			neighbors.add(null)
		}

		// TODO: This is runtime of O(n^2) for n = #pts.  Needs optimization.
		// Given a list of the sectors, will update the neighbors.
		for(i in 0 until walls.points.size-1) {
			// For each of the points in this sector?
			val p0 = walls.points[i]
			val p1 = walls.points[(i+1)%walls.points.size]
			// We can't continue with a forEach,
			nextWall@for(j in 0 until others.size) {
				if(this == others[j]) {
					continue // We can't be neighbors with ourselv.
				}
				val nbrCandidate = others[j]
				// Does p0 match and points in any neighbors?
				val numNbrPoints = nbrCandidate.walls.points.size
				for (k in 0 until numNbrPoints) {
					val q0 = nbrCandidate.walls.points[k]
					// Do these match?
					if (p0.distanceSquared(q0) < epsilon) {
						// Two points match!  Either q1 matches or q-1 matches.
						// Our neighbor's points may be reversed.  Check both the next and prev.
						val q1 = nbrCandidate.walls.points[(k + 1) % numNbrPoints]
						val q2 = nbrCandidate.walls.points[(k + numNbrPoints - 1) % numNbrPoints]
						// Does this match?
						if (p1.distanceSquared(q1) < epsilon) {
							// This is a neighbor!
							neighbors[i] = nbrCandidate
							break@nextWall
						} else if(p1.distanceSquared(q2) < epsilon) {
							neighbors[i] = nbrCandidate
							break@nextWall
						}
					}
				}
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
		val floorIndices = walls.triangulate(Vec(0f, 0f, 1f), true).map { i -> i.toShort() }.toShortArray()
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
		val ceilingIndices = walls.triangulate(Vec(0f, 0f, -1f), true).map { i -> i.toShort() }.toShortArray()
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
		return walls.points.fold(Vec(), {acc, v -> acc+v}) / walls.points.size.toFloat()
	}
}
