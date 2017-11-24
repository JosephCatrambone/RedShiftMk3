package io.xoana.redshift.scenes

abstract class Screen {
	abstract fun render()
	abstract fun update(deltaTime: Float)
	open fun restore() {}
	open fun dispose() {}
}