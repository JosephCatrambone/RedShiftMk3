package io.xoana.redshift.editors

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.PointLight
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector3
import io.xoana.redshift.*
import io.xoana.redshift.screens.Screen
import io.xoana.redshift.shaders.PBRShader
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Quaternion


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
	// 3D shortcuts and movement options
	val X_SENSITIVITY = 0.1f
	val Y_SENSITIVITY = 0.1f
	val WALK_SPEED = 100f
	val FORWARD_KEY = Input.Keys.W
	val BACKWARD_KEY = Input.Keys.S
	val LEFT_KEY = Input.Keys.A
	val RIGHT_KEY = Input.Keys.D

	// 2D Shortcuts and tool options
	val CAMERA_BUTTON = 2
	val CAMERA_DRAG_KEY = Input.Keys.SPACE
	val CAMERA_ZOOM_KEY = Input.Keys.CONTROL_LEFT
	val GRID_SCALE_INCREASE_KEY = Input.Keys.EQUALS
	val GRID_SCALE_DECREASE_KEY = Input.Keys.MINUS
	val BUILD_MAP = Input.Keys.F12
	val SWITCH_MODE = Input.Keys.F1
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
	val lightList = mutableListOf<PointLight>() // TODO: Why can't we fetch this from the environment?
	val environment = Environment()
	val walkCamera = PerspectiveCamera()
	val modelBatch = ModelBatch()
	//val shader = PBRShader()

	// Editor bits
	val sectors = mutableListOf<Sector>()
	var mapModel:Model = Model()
	var mapModelInstance:ModelInstance = ModelInstance(mapModel)
	var activeTool: EditorTool = DrawSectorTool(this)
	var walkMode = false // If we're in walk mode, render 3D, otherwise render in 2D.
	var modelNeedsRebuild = true // If we've made changes to the polygons or rooms, we need to update the geometry.

	init {
		environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0f, 0f, 0f, 0f));
		walkCamera.fieldOfView = 90f
		walkCamera.near = 0.01f
		walkCamera.far = 1000f
		walkCamera.rotate(Quaternion().idt())
		walkCamera.direction.set(1f, 0f, 0f)
		walkCamera.up.set(0f, 0f, 1f)
	}

	override fun dispose() {
		super.dispose()
		font.dispose()
		modelBatch.dispose()
		//shader.dispose()
		mapModel.dispose()
	}

	override fun render() {
		Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT);

		// Two modes; 2D edit and 3D edit.
		if(walkMode) {
			walkCamera.update(true)

			modelBatch.begin(walkCamera)
			modelBatch.render(mapModelInstance, environment)
			modelBatch.end()
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
				font.draw(spriteBatch, msg as CharSequence, 10f, (ind+1)*(font.lineHeight + 2f))
			})
			spriteBatch.end()
		}
	}

	override fun update(deltaTime: Float) {
		if(walkMode) {
			walkModeUpdate(deltaTime)
		} else {
			editModeUpdate(deltaTime)
		}

		if(Gdx.input.isKeyJustPressed(SWITCH_MODE)) {
			walkMode = !walkMode
			Gdx.input.isCursorCatched = walkMode // When in 3D, don't show cursor.
		}
	}

	fun walkModeUpdate(deltaTime: Float) {
		// Movement
		if(Gdx.input.isKeyPressed(FORWARD_KEY)) {
			walkCamera.translate(walkCamera.direction.cpy().scl(deltaTime*WALK_SPEED))
		} else if(Gdx.input.isKeyPressed(BACKWARD_KEY)) {
			walkCamera.translate(walkCamera.direction.cpy().scl(-deltaTime*WALK_SPEED))
		}
		if(Gdx.input.isKeyPressed(RIGHT_KEY)) {
			val left = walkCamera.up.cpy().crs(walkCamera.direction.cpy())
			walkCamera.translate(left.scl(deltaTime*-WALK_SPEED))
		} else if(Gdx.input.isKeyPressed(LEFT_KEY)) {
			val left = walkCamera.up.cpy().crs(walkCamera.direction.cpy())
			walkCamera.translate(left.scl(deltaTime*WALK_SPEED))
		}

		// Camera looking
		// Mouse delta -> rotation.
		walkCamera.rotate(walkCamera.up, -Gdx.input.deltaX.toFloat()*X_SENSITIVITY)
		walkCamera.rotate(walkCamera.up.cpy().crs(walkCamera.direction.cpy()), Gdx.input.deltaY.toFloat()*Y_SENSITIVITY)
	}

	fun editModeUpdate(deltaTime: Float) {
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

		if(Gdx.input.isKeyJustPressed(BUILD_MAP)) {
			pushMessage("Rebuilding map")
			buildMap()
			pushMessage("Map built")

			// Building lighting.
			pushMessage("Building lighting")
			lightList.forEach { environment.remove(it) }
			sectors.forEach { s ->
				val center = s.calculateCenter()
				val color = Color((center.x%255.0f)/255.0f, (center.y%255.0f)/255.0f, (center.z%255.0f)/255.0f, 1.0f)
				val light = PointLight().set(color ,center.toGDXVector3(), 1.0f)
				environment.add(light)
				lightList.add(light)
			}
			pushMessage("Built lighting")

			// Move the camera to the middle of sector 0.
			val center = sectors.first().calculateCenter()
			center.z += sectors.first().floorHeight
			center.z += sectors.first().ceilingHeight
			center.z /= 2.0f
			walkCamera.lookAt(center.toGDXVector3())
			println("Moving camera to ${center.x}, ${center.y}, ${center.z}")
		}

		activeTool.update(deltaTime)
	}

	override fun resize(width: Int, height: Int) {
		super.resize(width, height)
		// These two will update the area to which the camera is drawing, keeping the clicks in the right place on unproject.
		editCamera.setToOrtho(false, width.toFloat()*cameraZoom, height.toFloat()*cameraZoom)
		editCamera.update(true)
		walkCamera.viewportWidth = width.toFloat()
		walkCamera.viewportHeight = height.toFloat()
		walkCamera.update(true)
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
		val modelBuilder = ModelBuilder()
		modelBuilder.begin()
		// For each sector, build a new node.
		sectors.forEachIndexed({i, s ->
			val mat = Material()
			mat.set(ColorAttribute.createDiffuse(1.0f, 1.0f, 1.0f, 1.0f))
			val node = modelBuilder.node()
			var meshBuilder: MeshPartBuilder = modelBuilder.part(
				"sector_$i",
				GL20.GL_TRIANGLES,
				//(VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal or VertexAttributes.Usage.TextureCoordinates).toLong(),
				VertexAttributes.Usage.Position.toLong(),
				mat
			)
			//node.translation.set(10f, 0f, 0f)
			//meshBuilder.cone(5f, 5f, 5f, 10)
			s.buildMesh(meshBuilder)
		})

		val model = modelBuilder.end()
		mapModelInstance = ModelInstance(model) // Done here because model must outlive modelInstance.
		val oldModel = mapModel
		oldModel.dispose()
		mapModel = model
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
		val s = Sector(Polygon(newSector!!), 0f, 10f)
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
	// Each Model has Mesh[], MeshPart[], and Material[].
	// Mesh has Vert[] and Indices[].
	// MeshPart has offset and size which points into mesh.
	// We handle this construction by passing a MeshBuilder into our method.
	// TODO: Can't say it enough.  THIS NEEDS TO BE OPTIMIZED VERY BADLY!
	fun buildMesh(meshPartBuilder: MeshPartBuilder) {
		// TODO: This can be made WAAAAAY more efficient by using vert indices.
		// TODO: Also, the winding order on the polygons is wrong, so some won't be facing the right way.
		/*
		triangulate().forEach { t ->
			// Make the floor.
			val floorOffset = Vec(0f, 0f, floorHeight)
			meshPartBuilder.triangle(
				(t.a + floorOffset).toGDXVector3(),
				(t.b + floorOffset).toGDXVector3(),
				(t.c + floorOffset).toGDXVector3()
			)
			// Make the ceiling.
			val ceilingOffset = Vec(0f, 0f, ceilingHeight)
			meshPartBuilder.triangle(
				(t.a + ceilingOffset).toGDXVector3(),
				(t.b + ceilingOffset).toGDXVector3(),
				(t.c + ceilingOffset).toGDXVector3()
			)
		}
		*/
		// Make the walls.
		// GL_CCW is front-facing.
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
	}

	fun triangulate(): Array<Triangle> {
		val triangles = mutableListOf<Triangle>()
		// TODO: This looks like n^3 runtime PLUS the allocation overhead.
		// Make a copy of the verts.
		val vertList = MutableList<Vec>(this.walls.points.size, {i -> this.walls.points[i]})
		// While we have more than three points, we want to pull off one edge point and make it into a triangle.
		while(vertList.size >= 3) {
			// Pick one point at offset 0, remove it.
			val p0 = vertList.removeAt(0)
			// Sample p1 and p2 from the first and last.
			val p1 = vertList.first()
			val p2 = vertList.last()
			triangles.add(Triangle(p0, p1, p2))
		}
		return triangles.toTypedArray()
	}

	fun calculateCenter(): Vec {
		return walls.points.fold(Vec(), {acc, v -> acc+v}) / walls.points.size.toFloat()
	}

}
