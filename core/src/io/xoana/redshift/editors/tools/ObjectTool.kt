package io.xoana.redshift.editors.tools

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.environment.PointLight
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector3
import io.xoana.redshift.editors.LevelEditorScreen
import io.xoana.redshift.*
import io.xoana.redshift.gameobjects.GameObject
import io.xoana.redshift.gameobjects.LightObject

class ObjectTool(editor: LevelEditorScreen) : EditorTool {
	val MAX_SELECTION_DISTANCE = 100f
	val DESELECT_KEY = Input.Keys.ESCAPE
	val DELETE_KEY = Input.Keys.X

	override val editorRef: LevelEditorScreen = editor
	var selected : GameObject? = null
	var deleted: GameObject? = null // If we removed one, it's stored here briefly.

	override fun onClick(button:Int) {
		// Where did we click?
		val screenMouse = Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0.0f)
		val localMouse = Vec(editorRef.editCamera.unproject(screenMouse.cpy()))

		if(button == 0) {
			placeObject(localMouse)
		} else if(button == 1) { // Right click.
			select(localMouse)
		}
	}

	fun placeObject(localMouse: Vec) {
		val light = LightObject(Color.WHITE, 10f)
		light.position.x = localMouse.x
		light.position.y = localMouse.y
		// TODO: Get the sector Z and move light.
		light.position.z = 1.0f
		editorRef.gameObjects.add(light)

		val pointLight = PointLight().set(light.color, light.position.toGDXVector3(), light.intensity)
		editorRef.environment.add(pointLight)
	}

	fun select(localMouse:Vec) {
		if(editorRef.gameObjects.isEmpty()) {
			return
		}

		// Iterate through the sectors of the map and select the nearest one based on the vertices.
		// If there are two verts that overlap, select the sector in which the mouse is located.
		var minDist = Float.MAX_VALUE
		var candidate = editorRef.gameObjects.first()
		editorRef.gameObjects.forEach { gob ->
			// Get the distance to the mouse.
			val dist = gob.position.distanceSquared(localMouse)
			if(dist <= minDist) {
				candidate = gob
				minDist = dist
			}
		}

		if(minDist < MAX_SELECTION_DISTANCE*editorRef.cameraZoom) {
			selected = candidate
		} else {
			selected = null
		}
	}

	override fun update(dt:Float) {
		// Check if the user hit delete and all that.
		if(Gdx.input.isKeyJustPressed(DESELECT_KEY)) {
			selected = null
		}

		if(Gdx.input.isKeyJustPressed(DELETE_KEY) && selected != null) {
			editorRef.gameObjects.remove(selected!!)
			editorRef.pushMessage("Removed object ${selected!!.name}")
			deleted = selected!!
			selected = null
		}

		if(Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) && Gdx.input.isKeyJustPressed(Input.Keys.Z)) {
			if(deleted != null) {
				selected = deleted
				editorRef.gameObjects.add(deleted!!)
				deleted = null
				editorRef.notifySectorUpdate()
				editorRef.pushMessage("UNDO Delete")
			}
		}
	}

	override fun draw(shapeRenderer: ShapeRenderer) {

	}
}