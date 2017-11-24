package io.xoana.redshift.gameobjects

import com.badlogic.gdx.math.collision.BoundingBox
import io.xoana.redshift.Vec

// A Game Object is any object which exists in the game world.
// It may not be visible, but it has a position, a (possibly zero) size, and an update method.
open class GameObject {
	val position: Vec = Vec()
	var radius:Float = 0f // For frustum culling and early-out collision detection.
	val boundingBox:BoundingBox = BoundingBox()

	open fun update(deltaTime:Float) {}
}