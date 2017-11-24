package io.xoana.redshift.scenes

abstract class Screen {
	abstract fun render()
	abstract fun update(deltaTime: Float)
	open fun resize(width:Int, height:Int) {}
	open fun restore() {}
	open fun dispose() {}
}