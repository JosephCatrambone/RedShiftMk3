package io.xoana.redshift.editors.tools

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import io.xoana.redshift.editors.LevelEditorScreen

interface EditorTool {
	val editorRef: LevelEditorScreen

	// Do we want this to be on mouse down or mouse up?
	// For now we're going to be lazy and pass a reference to the Editor which has all the data.
	// The class itself can read Gdx input.  Maybe we'll refactor later.
	fun onClick(button:Int=0) {}
	fun update(dt:Float) {} // A generic method to handle all kinds of keystroke stuff.  Only called when active.
	fun draw(shapeBatch: ShapeRenderer) {}
}