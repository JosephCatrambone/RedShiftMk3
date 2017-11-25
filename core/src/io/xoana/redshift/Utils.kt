package io.xoana.redshift

import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

// Parallel execution of collection operations.
fun <T, R> Iterable<T>.pmap(
		numThreads: Int = Runtime.getRuntime().availableProcessors() - 2,
		exec: ExecutorService = Executors.newFixedThreadPool(numThreads),
		transform: (T) -> R
	): List<R> {

	// default size is just an inlined version of kotlin.collections.collectionSizeOrDefault
	val defaultSize = if (this is Collection<*>) this.size else 10
	val destination = Collections.synchronizedList(ArrayList<R>(defaultSize))

	for (item in this) {
		exec.submit { destination.add(transform(item)) }
	}

	exec.shutdown()
	exec.awaitTermination(1, TimeUnit.DAYS)

	return ArrayList<R>(destination)
}

/*** Tween
 * Parent class of the others.  Generally unused.
 */
open class Tween(val time:Float, val updateFunction: (Float) -> Unit) {
	protected var accumulator = 0f

	open val dead:Boolean
		get() = accumulator >= time

	open fun update(dt:Float) {
		this.accumulator += dt
	}
}

/*** DelayTween
 * Wait for [time] seconds and then execute the onFinish method.
 */
class DelayTween(time:Float, onFinish: (Float) -> Unit) : Tween(time, onFinish) {
	private var hasRun = false

	override val dead: Boolean
		get() = this.hasRun

	override fun update(dt: Float) {
		super.update(dt)
		if(!dead && this.accumulator > this.time) {
			this.updateFunction(this.accumulator)
			hasRun = true
		}
	}
}

/*** BasicTween
 * @param time The time, in seconds, over which this tween is run.
 * @param startValue the value from which we should begin interpolating.
 * @param stopValue the value at which we'll end up.
 * @param updateFunction the function to call with the lerp'd value.
 */
class BasicTween(time:Float, val startValue:Float, val stopValue:Float, updateFunction: (Float) -> Unit) : Tween(time, updateFunction) {
	override fun update(dt:Float) {
		super.update(dt)
		if(!dead) {
			// Interpolate from 'from' to 'to'.
			val totalTimeRatio = this.accumulator / time
			// Calculate the stop in which we find ourselves.
			updateFunction(totalTimeRatio*stopValue + (1.0f-totalTimeRatio)*startValue)
		}
	}
}

/*** SequentialTween
 * Execute a number of tweens in order.
 */
class SequentialTween(vararg tweens:Tween): Tween(tweens.fold(0f, {acc, t -> acc + t.time}), { f -> Unit }) {
	val pendingTweens:MutableList<Tween> = mutableListOf(*tweens)

	override val dead:Boolean
		get() = this.pendingTweens.isEmpty()

	override fun update(dt: Float) {
		super.update(dt)
		// If we're not dead...
		if(pendingTweens.size > 0) {
			// First, update the time on this item.
			pendingTweens[0].update(dt)
			if(pendingTweens[0].dead) {
				pendingTweens.removeAt(0)
			}
		}
	}
}

/*** EaseTween
 * @param ease The exponent for the time function.  Less than one introduces ease out.  Greater than one is ease in.
 */
class EaseTween(time:Float, val startValue:Float, val stopValue:Float, val ease:Float, updateFunction: (Float) -> Unit) : Tween(time, updateFunction) {
	override fun update(dt: Float) {
		super.update(dt)
		if(!dead) {
			// Interpolate from 'from' to 'to'.
			val totalTimeRatio = Math.pow((this.accumulator / time).toDouble(), ease.toDouble()).toFloat()
			// Calculate the stop in which we find ourselves.
			updateFunction(totalTimeRatio*stopValue + (1.0f-totalTimeRatio)*startValue)
		}
	}
}

class MultiStopTween(time:Float, val stops:FloatArray, updateFunction: (Float) -> Unit) : Tween(time, updateFunction) {
	private val stopSize = time/stops.size.toFloat()

	override fun update(dt:Float) {
		super.update(dt)
		if(!dead) {
			// Interpolate from 'from' to 'to'.
			val totalTimeRatio = this.accumulator / time
			// Calculate the stop in which we find ourselves.
			val stopIndex:Int = (totalTimeRatio*stops.size).toInt()
			if(stopIndex < stops.size-1) {
				// Find the interval of THIS ratio.
				val intervalTimeRatio = (this.accumulator - (stopIndex * stopSize)) / stopSize
				val amount = intervalTimeRatio * stops[stopIndex + 1] + (1.0f - intervalTimeRatio) * stops[stopIndex]
				updateFunction(amount)
			} else {
				updateFunction(stops.last())
			}
		}
	}
}

object TweenManager {
	val activeTweens = mutableListOf<Tween>()

	@JvmStatic
	fun add(t:Tween) {
		TweenManager.activeTweens.add(t)
	}

	@JvmStatic
	fun update(dt: Float) {
		TweenManager.activeTweens.forEach { t:Tween ->
			t.update(dt)
		}

		TweenManager.activeTweens.removeIf({ t:Tween -> t.dead })
	}
}
