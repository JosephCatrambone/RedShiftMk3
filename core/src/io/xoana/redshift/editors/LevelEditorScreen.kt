package io.xoana.redshift.editors

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.PointLight
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector3
import io.xoana.redshift.*
import io.xoana.redshift.screens.Screen
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Quaternion
import io.xoana.redshift.editors.tools.*


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
	val SHARED_WALL_COLOR = Color(0.9f, 0.3f, 0.3f, 0.9f)
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
	var activeTool: EditorTool = DrawTool(this)
	var walkMode = false // If we're in walk mode, render 3D, otherwise render in 2D.

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
			sectors.forEach({drawSector(shapeBatch, it)})

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
			activeTool = SelectTool(this)
			pushMessage("Selector Tool")
		} else if(Gdx.input.isKeyJustPressed(DRAW_TOOL)) {
			activeTool = DrawTool(this)
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
			/*
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
			*/

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
	fun drawSector(shapeBatch: ShapeRenderer, sector: Sector) {
		// Assume shapeBatch is already started.
		sector.getWallIterator().withIndex().forEach { iv ->
			val i = iv.index
			val v = iv.value
			val p1 = v.start
			val p2 = v.end

			// Draw line.
			shapeBatch.color = when(sector.neighbors[i]) {
				null -> WALL_COLOR
				else -> SHARED_WALL_COLOR
			}
			shapeBatch.line(p1.x, p1.y, p2.x, p2.y)
			// Draw point.
			shapeBatch.color = VERT_COLOR
			shapeBatch.rect(p1.x - VERT_SIZE / 2f, p1.y - VERT_SIZE / 2f, VERT_SIZE, VERT_SIZE)
		}
		// Draw the last point no matter what.
		shapeBatch.color = VERT_COLOR
		shapeBatch.rect(sector.walls.points.last().x - VERT_SIZE / 2f, sector.walls.points.last().y - VERT_SIZE / 2f, VERT_SIZE, VERT_SIZE)
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

	fun notifySectorUpdate() {
		// Called when our sectors have changed.  Do the things we need to do to recompute them.
		// This may in the future indicate we need to rebuild the map or lighting.
		// Would be nice if we could avoid iterating over all the sectors, since the method also iterates over all the sectors.
		// TODO: Performance optimization.
		this.sectors.forEach { it.updateNeighbors(this.sectors) }
	}
}
