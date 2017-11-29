import io.xoana.redshift.*
import org.junit.Test
import org.junit.Assert.*

class LineTest {
	@Test
	fun lineIntersection() {
		val a = Line(Vec(1f, 101f), Vec(101f, 1f))
		val b = Line(Vec(1f, 1f), Vec(10f, 10f)) // Up and to the right, but not through A.
		val c = Vec(51f, 51f)

		val intersection = a.intersection2D(b)
		val noIntersection = a.segmentIntersection2D(b)
		assertNotNull(intersection)
		assertNull(noIntersection)
		assertEquals(intersection, c)

		val d = Line(Vec(0f, 0f), Vec(49f, 49f))
		assertNull(a.segmentIntersection2D(d))
	}
}
