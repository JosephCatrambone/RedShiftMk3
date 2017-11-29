import io.xoana.redshift.*
import java.util.*
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.*

class TriangleTest {
	val random = Random()

	fun toRadians(degrees:Float): Float {
		return (degrees*PI/180f).toFloat()
	}

	@Test
	fun rightTurnTriangle() {
		val tri1 = Triangle(
			Vec(0f, 0f),
			Vec(0f, 1f),
			Vec(1f, 1f)
		)
		assertTrue(!tri1.leftTurn())

		val tri2 = Triangle(
			Vec(0f, 0f),
			Vec(1f, 0f),
			Vec(1f, 1f)
		)
		assertTrue(tri2.leftTurn())
	}

	@Test
	fun pointsInTriangle() {
		val tri = Triangle(
			Vec(10f, 10f),
			Vec(100f, 10f),
			Vec(10f, 100f)
		)
		assertTrue(tri.pointInTriangle2D(Vec(20f, 20f)))
		assertFalse(tri.pointInTriangle2D(Vec(0f, 0f)))
	}

	@Test
	fun pointInCircumcircle() {
		//val center = Vec(0f, 0f)
		val radius = 100f
		val step = toRadians(360f/3)
		val a = Vec(cos(step*0), sin(step*0))*radius
		val b = Vec(cos(step*1), sin(step*1))*radius
		val c = Vec(cos(step*2), sin(step*2))*radius
		val tri = Triangle(a, b, c)
		
		for(i in 0 until 1000) {
			val p = Vec(random.nextFloat()*radius*2, random.nextFloat()*radius*2)
			assertTrue((p.magnitude < radius) == tri.pointInCircumcircle2D(p))
		}
	}
}
