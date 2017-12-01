package io.xoana.redshift.editors.tools

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector3
import io.xoana.redshift.editors.LevelEditorScreen
import io.xoana.redshift.*
import io.xoana.redshift.levels.Sector

class SelectTool(editor: LevelEditorScreen) : EditorTool {
	val MAX_SELECTION_DISTANCE = 100f
	val SELECTED_COLOR = Color(0.9f, 0.1f, 0.0f, 0.9f)
	val DESELECT_KEY = Input.Keys.ESCAPE
	val DELETE_KEY = Input.Keys.X

	override val editorRef: LevelEditorScreen = editor
	var selected : Sector? = null
	var deleted: Sector? = null // If we removed one, it's stored here briefly.
	private val bounds: Vec = Vec() // Using the XYZW here.

	override fun onClick(button:Int) {
		if(editorRef.sectors.isEmpty()) {
			return
		}

		// Where did we click?
		val screenMouse = Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0.0f)
		val localMouse = Vec(editorRef.editCamera.unproject(screenMouse.cpy()))

		// Iterate through the sectors of the map and select the nearest one based on the vertices.
		// If there are two verts that overlap, select the sector in which the mouse is located.
		var minDist = Float.MAX_VALUE
		var sector = editorRef.sectors.first()
		editorRef.sectors.forEach { candidateSector ->
			// Check each of the points in the sector.
			candidateSector.walls.points.forEach({ p ->
				// Get the distance to the mouse.
				val dist = p.distanceSquared(localMouse)
				if(dist <= minDist) {
					// Is it equal?  Then check the sector.
					if(Math.abs(dist - minDist) < 1e-6) {
						// TODO: The sector in which we clicked rather than the nearest sector.
						val distToCurrentCenter = sector.calculateCenter().distanceSquared(localMouse)
						val distToNewCenter = candidateSector.calculateCenter().distanceSquared(localMouse)
						if(distToNewCenter < distToCurrentCenter) { // New one is closer.  Reassign.
							// minDist = dist // Equal.
							sector = candidateSector
						}
					} else {
						minDist = dist
						sector = candidateSector
					}
				}
			})
		}

		if(minDist < MAX_SELECTION_DISTANCE*editorRef.cameraZoom) {
			selected = sector
		} else {
			selected = null
		}
	}

	override fun update(dt:Float) {
		// Refresh the bounds.
		if(GDXMain.frameCount % 10 == 0L) {
			if(selected != null) {
				var left = Float.MAX_VALUE
				var right = Float.MIN_VALUE
				var top = Float.MIN_VALUE
				var bottom = Float.MAX_VALUE

				selected!!.walls.points.forEach({ p ->
					left = minOf(p.x, left)
					right = maxOf(p.x, right)
					top = maxOf(p.y, top)
					bottom = minOf(p.y, bottom)
				})

				bounds.x = left
				bounds.y = bottom
				bounds.z = right - left
				bounds.w = top - bottom
			} else {
				bounds.x = 0f
				bounds.y = 0f
				bounds.z = 0f
				bounds.w = 0f
			}
		}

		// Check if the user hit delete and all that.
		if(Gdx.input.isKeyJustPressed(DESELECT_KEY)) {
			selected = null
		}

		if(Gdx.input.isKeyJustPressed(DELETE_KEY) && selected != null) {
			editorRef.sectors.remove(selected!!)
			deleted = selected!!
			selected = null
			editorRef.notifySectorUpdate()
			editorRef.pushMessage("Removed sector $deleted")
		}

		if(Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) && Gdx.input.isKeyJustPressed(Input.Keys.Z)) {
			if(deleted != null) {
				selected = deleted
				editorRef.sectors.add(deleted!!)
				deleted = null
				editorRef.notifySectorUpdate()
				editorRef.pushMessage("UNDO Delete")
			}
		}
	}

	override fun draw(shapeRenderer: ShapeRenderer) {
		// Draw a selection bound around our sector.
		shapeRenderer.color = SELECTED_COLOR
		shapeRenderer.rect(bounds.x, bounds.y, bounds.z, bounds.w)
	}
}