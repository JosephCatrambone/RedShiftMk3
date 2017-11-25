package io.xoana.redshift.editors

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector3
import io.xoana.redshift.*
import io.xoana.redshift.screens.Screen
import io.xoana.redshift.shaders.PBRShader

/**
 * Created by jo on 2017-11-24.
 * Keyboard shortcuts:
 * - / = : Change grid size.
 * Ctrl+ - / = : Change grid size in 1-step increments.
 * Ctrl+Z or Backspace : Undo placement of last sector vert.
 * Ctrl+Middle Mouse : Zoom Camera
 * Middle Mouse or Space : Drag Camera
 * F12 : Build 3D map.
 */

class LevelEditorScreen : Screen() {
	val CAMERA_BUTTON = 2
	val CAMERA_DRAG_KEY = Input.Keys.SPACE
	val CAMERA_ZOOM_KEY = Input.Keys.CONTROL_LEFT
	val GRID_SCALE_INCREASE_KEY = Input.Keys.EQUALS
	val GRID_SCALE_DECREASE_KEY = Input.Keys.MINUS
	val BUILD_MAP = Input.Keys.F12
	val SELECT_TOOL = Input.Keys.Q
	val DRAW_TOOL = Input.Keys.W

	val MESSAGE_FADE_TIME = 5f // Five seconds might be too fast.

	val MIN_EDIT_ZOOM = 0.0f
	val MAX_EDIT_ZOOM = 10f

	val GRID_COLOR = Color(0.2f, 0.2f, 0.2f, 0.5f)
	val WALL_COLOR = Color(0.6f, 0.6f, 0.8f, 1.0f)
	val VERT_SIZE = 3f
	val VERT_COLOR = Color(0.8f, 0.8f, 0.9f, 1.0f)
	val SECTOR_FILL_COLOR = Color(0.1f, 0.1f, 0.7f, 0.5f)

	// UI
	val font = BitmapFont()
	val messageStack = mutableListOf<String>()

	// For 2D rendering.
	var mouseDown: Vec? = null // On null, mouse was not down.
	val editCamera = OrthographicCamera(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
	val shapeBatch = ShapeRenderer() // Maybe a sprite renderer?
	val spriteBatch = SpriteBatch()
	var cameraZoom = editCamera.zoom
	var gridSize = 10f

	// For rendering in 3D
	val walkCamera = PerspectiveCamera()
	val modelBatch = ModelBatch()
	val shader = PBRShader()

	// Editor bits
	val sectors = mutableListOf<Sector>()
	val mapModel:Model = Model()
	var activeTool: EditorTool = DrawSectorTool(this)
	var walkMode = false // If we're in walk mode, render 3D, otherwise render in 2D.
	var modelNeedsRebuild = true // If we've made changes to the polygons or rooms, we need to update the geometry.

	override fun dispose() {
		super.dispose()
		font.dispose()
		modelBatch.dispose()
		shader.dispose()
		mapModel.dispose()
	}

	override fun render() {
		Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		// Two modes; 2D edit and 3D edit.
		if(walkMode) { // 3D
		} else {
			editCamera.update(true)

			shapeBatch.projectionMatrix = editCamera.combined

			// Draw the line pieces.
			shapeBatch.begin(ShapeRenderer.ShapeType.Line)
			drawGrid(shapeBatch)

			// Draw all the sectors, then draw the active tool.
			sectors.forEach({drawSector(shapeBatch, it.walls.points)})

			// Draw the active tool.
			activeTool.draw(shapeBatch)
			shapeBatch.end()

			// Draw all the messages on top.
			spriteBatch.begin()
			messageStack.forEachIndexed({ ind, msg ->
				font.draw(spriteBatch, msg as CharSequence, 0f, ind*(font.lineHeight + 2f))
			})
			spriteBatch.end()
		}
	}

	override fun update(deltaTime: Float) {
		// TODO: We should handle drag events.
		if(Gdx.input.isButtonPressed(0)) {
			mouseDown = Vec(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
		} else {
			if(mouseDown != null) {
				mouseDown = null
				activeTool.onClick()
			}
		}

		// Handle tool switching.
		if(Gdx.input.isKeyJustPressed(SELECT_TOOL)) {
			activeTool = SelectorTool(this)
			pushMessage("Selector Tool")
		} else if(Gdx.input.isKeyJustPressed(DRAW_TOOL)) {
			activeTool = DrawSectorTool(this)
			pushMessage("Draw Tool")
		}

		// Zoom the camera if the Ctrl button is down and we're using middle mouse,
		// else scroll camera if middle mouse or spacebar (as long as the zoom key isn't held.
		if(Gdx.input.isKeyPressed(CAMERA_ZOOM_KEY) && Gdx.input.isButtonPressed(CAMERA_BUTTON)) {
			val dy = Gdx.input.deltaY.toFloat()
			if(dy != 0f) {
				val newZoom = cameraZoom+(dy*deltaTime)
				cameraZoom = maxOf(newZoom, MIN_EDIT_ZOOM)
				cameraZoom = minOf(cameraZoom, MAX_EDIT_ZOOM)
				editCamera.zoom = cameraZoom
				editCamera.update(false)
				println("Zoom! $cameraZoom")
			}
		} else if(Gdx.input.isKeyPressed(CAMERA_DRAG_KEY) || Gdx.input.isButtonPressed(CAMERA_BUTTON)) {
			editCamera.translate(-Gdx.input.deltaX.toFloat()*cameraZoom, Gdx.input.deltaY.toFloat()*cameraZoom)
		}

		if(Gdx.input.isKeyJustPressed(GRID_SCALE_INCREASE_KEY)) {
			if(Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
				gridSize += 1.0f
			} else {
				gridSize *= 2.0f
			}
			pushMessage("Grid size: $gridSize")
		}
		if(Gdx.input.isKeyJustPressed(GRID_SCALE_DECREASE_KEY)) {
			if(Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
				gridSize = maxOf(gridSize - 1.0f, 1.0f)
			} else {
				gridSize = maxOf(gridSize / 2f, 1.0f)
			}
			pushMessage("Grid size: $gridSize")
		}

		if(Gdx.input.isKeyPressed(BUILD_MAP)) {

		}

		activeTool.update(deltaTime)
	}

	override fun resize(width: Int, height: Int) {
		super.resize(width, height)
		// These two will update the area to which the camera is drawing, keeping the clicks in the right place on unproject.
		editCamera.setToOrtho(false, width.toFloat()*cameraZoom, height.toFloat()*cameraZoom)
		editCamera.update(true)
	}

	// Can we reuse for the ones that are being drawn?
	fun drawSector(shapeBatch: ShapeRenderer, points:List<Vec>, wallColor:Color=WALL_COLOR, vertColor:Color=VERT_COLOR, closeLoop:Boolean=true) {
		// Assume shapeBatch is already started.
		// Zip points 0 -> (n-1) and points (1 -> n)
		points.dropLast(1).zip(points.drop(1)).forEach({ edge ->
			// Draw line.
			shapeBatch.color = wallColor
			shapeBatch.line(edge.first.x, edge.first.y, edge.second.x, edge.second.y)
			// Draw point.
			shapeBatch.color = vertColor
			shapeBatch.rect(edge.first.x - VERT_SIZE/2f, edge.first.y - VERT_SIZE/2f, VERT_SIZE, VERT_SIZE)
		})
		// Draw the last line?
		if(closeLoop) {
			shapeBatch.color = wallColor
			shapeBatch.line(points.last().x, points.last().y, points.first().x, points.first().y)
		}
		// Draw the last point no matter what.
		shapeBatch.color = vertColor
		shapeBatch.rect(points.last().x - VERT_SIZE / 2f, points.last().y - VERT_SIZE / 2f, VERT_SIZE, VERT_SIZE)
	}

	fun drawGrid(shapeBatch: ShapeRenderer) {
		shapeBatch.color = GRID_COLOR
		// We don't much care about our camera's position, but we do want to avoid drawing a ton of lines.
		for(x in (editCamera.position.x-editCamera.viewportWidth).toInt() until (editCamera.position.x+editCamera.viewportWidth).toInt()) {
			shapeBatch.line(snapToGrid(x.toFloat()), 0f, snapToGrid(x.toFloat()), Gdx.graphics.height.toFloat())
		}
		for(y in (editCamera.position.y-editCamera.viewportHeight).toInt() until (editCamera.position.x+editCamera.viewportHeight).toInt()) {
			// Snap the vec to the grid.
			shapeBatch.line(0f, snapToGrid(y.toFloat()), Gdx.graphics.width.toFloat(), snapToGrid(y.toFloat()))
		}
	}

	fun snapToGrid(v:Float): Float {
		return Math.round(v/gridSize)*gridSize
	}

	fun snapToGrid(v:Vec): Vec {
		// This snap works, but it doesn't feel very good.  Instead, round to the nearest instead of using toInt()
		return Vec(snapToGrid(v.x), snapToGrid(v.y))
	}

	fun pushMessage(str:String) {
		messageStack.add(str)
		println("MSG LOG: $str")
		TweenManager.add(DelayTween(MESSAGE_FADE_TIME, { _ ->
			messageStack.remove(str)
		}))
	}

	fun buildMap() {
		pushMessage("Starting map build.")

	}
}

interface EditorTool {
	val editorRef: LevelEditorScreen

	// Do we want this to be on mouse down or mouse up?
	// For now we're going to be lazy and pass a reference to the Editor which has all the data.
	// The class itself can read Gdx input.  Maybe we'll refactor later.
	fun onClick() {}
	fun update(dt:Float) {} // A generic method to handle all kinds of keystroke stuff.  Only called when active.
	fun draw(shapeBatch: ShapeRenderer) {}
}

class SelectorTool(editor:LevelEditorScreen) : EditorTool {
	val MAX_SELECTION_DISTANCE = 100f
	val SELECTED_COLOR = Color(0.9f, 0.1f, 0.0f, 0.9f)
	val DESELECT_KEY = Input.Keys.ESCAPE
	val DELETE_KEY = Input.Keys.DEL

	override val editorRef: LevelEditorScreen = editor
	var selected : Sector? = null
	var deleted: Sector? = null // If we removed one, it's stored here briefly.
	private val bounds:Vec = Vec() // Using the XYZW here.

	override fun onClick() {
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
			editorRef.pushMessage("Removed sector $deleted")
		}

		if(Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) && Gdx.input.isKeyJustPressed(Input.Keys.Z)) {
			if(deleted != null) {
				selected = deleted
				editorRef.sectors.add(deleted!!)
				deleted = null
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

class DrawSectorTool(editor:LevelEditorScreen) : EditorTool {
	val UNDO_BUTTON = Input.Keys.BACKSPACE

	val WALL_COLOR = Color(0.9f, 0.9f, 0.9f, 1.0f)
	val VERT_SIZE = 3.0f
	val VERT_COLOR = Color(0.0f, 1.0f, 0.0f, 1.0f)

	override val editorRef = editor

	var newSector: MutableList<Vec>? = null // Null until we start drawing it.

	override fun onClick() {
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
	}

	override fun draw(shapeRenderer: ShapeRenderer) {
		super.draw(shapeRenderer)

		// Assume shape renderer has started already.
		if(newSector != null) {
			editorRef.drawSector(shapeRenderer, newSector!!, wallColor = WALL_COLOR, vertColor = VERT_COLOR, closeLoop = false)
		}
	}

	fun finalizeSector() {
		// Make our candidate sector into a real one.
		val s = Sector(Polygon(newSector!!), 0f, 0f)
		editorRef.sectors.add(s)
		newSector = null
		println("Finished sector.")
	}
}

// We'll move this later.  It's here now just for convenience while we hack on it.
class Sector(
	var walls: Polygon,
	var floorHeight: Float,
	var ceilingHeight: Float
) {
	fun calculateCenter(): Vec {
		return walls.points.fold(Vec(), {acc, v -> acc+v}) / walls.points.size.toFloat()
	}

}
