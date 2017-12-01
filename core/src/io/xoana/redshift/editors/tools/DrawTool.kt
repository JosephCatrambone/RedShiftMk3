package io.xoana.redshift.editors.tools

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector3
import io.xoana.redshift.*
import io.xoana.redshift.editors.LevelEditorScreen
import io.xoana.redshift.levels.Sector

class DrawTool(editor: LevelEditorScreen) : EditorTool {
	val UNDO_BUTTON = Input.Keys.BACKSPACE
	val RAISE_FLOOR = Input.Keys.PAGE_UP
	val DROP_FLOOR = Input.Keys.PAGE_DOWN
	val RAISE_CEILING = Input.Keys.HOME
	val DROP_CEILING = Input.Keys.END

	val WALL_COLOR = Color(0.9f, 0.9f, 0.9f, 1.0f)
	val VERT_SIZE = 3.0f
	val VERT_COLOR = Color(0.0f, 1.0f, 0.0f, 1.0f)

	override val editorRef = editor

	var newSector: MutableList<Vec>? = null // Null until we start drawing it.
	var ceilHeight = 1f
	var floorHeight = 0f

	override fun onClick(button:Int) {
		// Grab and unwrap the sector coordinates.
		val screenMouse = Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0.0f) // 0.0f for camera-near.
		val localMouse = Vec(editorRef.editCamera.unproject(screenMouse.cpy())) // Have to copy because GDX mangles this.
		val snappedPoint = editorRef.snapToGrid(localMouse)

		// We have clicked.  Do we have an active sector?
		if(newSector == null) { // No sector.  Start building a new one.
			newSector = mutableListOf<Vec>(snappedPoint) // Place our first point at the mouse coordinates.
		} else {
			// Check to see if we're closing the loop.  If not, just add it to the list.
			if(snappedPoint.distanceSquared(newSector!!.first()) < editorRef.gridSize) {
				// Close off the loop.
				finalizeSector()
			} else {
				// Haven't clicked near the start.  Just add the point.
				newSector!!.add(snappedPoint)
			}
		}
	}

	override fun update(dt:Float) {
		if(Gdx.input.isKeyJustPressed(UNDO_BUTTON) || (Gdx.input.isKeyJustPressed(Input.Keys.Z) && Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT))) {
			// Undo that last thing.
			if(newSector != null) {
				if(newSector!!.size == 1) {
					// Removing only point.  Get rid of the whole sector.
					newSector = null
				} else {
					// Remove that last point.
					newSector!!.removeAt(newSector!!.size-1)
				}
			}
		}

		if(Gdx.input.isKeyJustPressed(RAISE_CEILING)) {
			ceilHeight += 1f
			editorRef.pushMessage("Ceil height: $ceilHeight")
		}
		if(Gdx.input.isKeyJustPressed(DROP_CEILING)) {
			ceilHeight -= 1f
			editorRef.pushMessage("Ceil height: $ceilHeight")
		}
		if(Gdx.input.isKeyJustPressed(RAISE_FLOOR)) {
			floorHeight += 1f
			editorRef.pushMessage("Floor height: $floorHeight")
		}
		if(Gdx.input.isKeyJustPressed(DROP_FLOOR)) {
			floorHeight -= 1f
			editorRef.pushMessage("Floor height: $floorHeight")
		}
	}

	override fun draw(shapeBatch: ShapeRenderer) {
		super.draw(shapeBatch)

		// Assume shape renderer has started already.
		if(newSector != null) {
			// Assume shapeBatch is already started.
			// Zip points 0 -> (n-1) and points (1 -> n)
			newSector!!.dropLast(1).zip(newSector!!.drop(1)).forEachIndexed({ i, edge ->
				// Draw line.
				shapeBatch.color = WALL_COLOR
				shapeBatch.line(edge.first.x, edge.first.y, edge.second.x, edge.second.y)
				// Draw point.
				shapeBatch.color = VERT_COLOR
				shapeBatch.rect(edge.first.x - VERT_SIZE/2f, edge.first.y - VERT_SIZE/2f, VERT_SIZE, VERT_SIZE)
			})
			// Draw the last point no matter what.
			shapeBatch.color = VERT_COLOR
			shapeBatch.rect(newSector!!.last().x - VERT_SIZE / 2f, newSector!!.last().y - VERT_SIZE / 2f, VERT_SIZE, VERT_SIZE)
		}
	}

	fun finalizeSector() {
		// Make our candidate sector into a real one.
		var poly = Polygon(newSector!!)
		val ccw = poly.isCounterClockwise()
		println("New polygon CCW: ${poly.isCounterClockwise()}")
		if(ccw) {
			println("Reversing.")
			poly = poly.getReversedWinding()
		}

		val s = Sector(poly, floorHeight, ceilHeight)
		editorRef.sectors.add(s)
		editorRef.notifySectorUpdate()
		// Clear the sector so we can handle the next one.
		newSector = null
		println("Finished sector.")
	}
}