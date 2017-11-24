package io.xoana.redshift.editors

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector3
import com.sun.org.apache.xpath.internal.operations.Or
import io.xoana.redshift.Polygon
import io.xoana.redshift.Vec
import io.xoana.redshift.screens.Screen
import io.xoana.redshift.shaders.PBRShader

/**
 * Created by jo on 2017-11-24.
 */

class LevelEditorScreen : Screen() {
	val GRID_COLOR = Color(0.2f, 0.2f, 0.2f, 0.5f)
	val WALL_COLOR = Color(0.9f, 0.9f, 0.9f, 1.0f)
	val VERT_SIZE = 3f
	val VERT_COLOR = Color(0.2f, 0.2f, 0.9f, 1.0f)
	val SECTOR_FILL_COLOR = Color(0.1f, 0.1f, 0.7f, 0.5f)
	val sectors = mutableListOf<Sector>()

	// For 2D rendering.
	var mouseWasDown = false
	val editCamera = OrthographicCamera()
	val shapeBatch = ShapeRenderer() // Maybe a sprite renderer?
	var gridSize = 10f

	// For rendering in 3D
	val walkCamera = PerspectiveCamera()
	val modelBatch = ModelBatch()
	val shader = PBRShader()

	var activeTool: EditorTool = DrawSectorTool(this)
	var walkMode = false // If we're in walk mode, render 3D, otherwise render in 2D.
	val mapModel:Model = Model()
	var modelNeedsRebuild = true // If we've made changes to the polygons or rooms, we need to update the geometry.

	override fun render() {
		// Two modes; 2D edit and 3D edit.
		if(walkMode) { // 3D
		} else {
			editCamera.update()
			shapeBatch.projectionMatrix = editCamera.combined
			shapeBatch.begin(ShapeRenderer.ShapeType.Filled)
			drawGrid(shapeBatch)
			// Draw all the sectors, then draw the active tool.
			sectors.forEach({drawSector(shapeBatch, it)})
			activeTool.draw(shapeBatch)
			shapeBatch.end()
		}
	}

	override fun update(deltaTime: Float) {
		// TODO: We should handle drag events.
		if(Gdx.input.isButtonPressed(0) || Gdx.input.isTouched(0)) {
			mouseWasDown = true
		} else {
			if(mouseWasDown) {
				mouseWasDown = false
				activeTool.onClick()
			}
		}

		activeTool.update(deltaTime)
	}

	override fun resize(width: Int, height: Int) {
		super.resize(width, height)
		//uiStage.viewport.update(width, height, true)
		editCamera.setToOrtho(false, width.toFloat(), height.toFloat())
		editCamera.update(true)
	}

	// Can we reuse for the ones that are being drawn?
	fun drawSector(shapeBatch: ShapeRenderer, sector:Sector) {
		// Assume shapeBatch is already started.
		// Zip points 0 -> (n-1) and points (1 -> n)
		sector.walls.points.dropLast(1).zip(sector.walls.points.drop(1)).forEach({ edge ->
			// Draw point.
			shapeBatch.color = WALL_COLOR
			shapeBatch.rect(edge.first.x - VERT_SIZE/2f, edge.first.y - VERT_SIZE/2f, VERT_SIZE, VERT_SIZE)
			// Draw line.
			shapeBatch.color = VERT_COLOR
			shapeBatch.line(edge.first.x, edge.first.y, edge.second.x, edge.second.y)
		})
		// Draw the last point.
		shapeBatch.color = WALL_COLOR
		shapeBatch.rect(sector.walls.points.last().x - VERT_SIZE/2f, sector.walls.points.last().y - VERT_SIZE/2f, VERT_SIZE, VERT_SIZE)
		shapeBatch.color = VERT_COLOR
		shapeBatch.line(sector.walls.points.last().x, sector.walls.points.last().y, sector.walls.points.first().x, sector.walls.points.first().y)
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
}

// We'll move this later.  It's here now just for convenience while we hack on it.
data class Sector(var walls: Polygon, var floorHeight: Float, var ceilingHeight: Float)

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
	override val editorRef: LevelEditorScreen = editor

	override fun onClick() {

	}

	override fun update(dt:Float) {

	}

	override fun draw(shapeRenderer: ShapeRenderer) {}
}

class DrawSectorTool(editor:LevelEditorScreen) : EditorTool {
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

		// Quick design:
		// What can happen?
		// A player can close a sector.
		// A player can continue editing a sector.
		// A player can start a new sector.
		// A player can SPLIT a sector.
		// Should split be a separate tool?  Both involve drawing.
		// Let's not worry about that for now.

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

	}

	override fun draw(shapeRenderer: ShapeRenderer) {
		super.draw(shapeRenderer)

		// Assume shape renderer has started already.
		if(newSector != null) {
			val sec = newSector!!
			for(i in 0 until sec.size-1) {
				// Draw the point.
				shapeRenderer.color = VERT_COLOR
				shapeRenderer.rect(sec[i].x-VERT_SIZE/2f, sec[i].y-VERT_SIZE/2f, VERT_SIZE, VERT_SIZE)
				// Draw the line.
				shapeRenderer.color = WALL_COLOR
				shapeRenderer.line(sec[i].x, sec[i].y, sec[i+1].x, sec[i+1].y)
			}
			// Always draw at least the last point.  May result in redundant draws.  That's okay.
			shapeRenderer.color = VERT_COLOR
			shapeRenderer.rect(sec.last().x-VERT_SIZE/2f, sec.last().y-VERT_SIZE/2f, VERT_SIZE, VERT_SIZE)
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