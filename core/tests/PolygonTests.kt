import io.xoana.redshift.*
import java.util.*
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.*

class PolygonTest {
	val random = Random()

	fun toRadians(degrees:Float): Float {
		return (degrees*PI/180f).toFloat()
	}

	@Test
	fun pointInCircumcircle() {
		//    1
		//    /\
		//  6/__\2
		//  /\  /\
		// /__\/__\
		// 5   4   3
		val triforce = Polygon(listOf<Vec>(
			Vec(2f, 2f),
			Vec(3f, 1f),
			Vec(4f, 0f),
			Vec(2f, 0f),
			Vec(0f, 0f),
			Vec(1f, 1f)
		))
		val triangles = triforce.triangulate(up=Vec(0f, 0f, 1f))
	}
}
