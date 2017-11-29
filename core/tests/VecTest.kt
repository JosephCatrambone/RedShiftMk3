import io.xoana.redshift.*
import org.junit.Test
import org.junit.Assert.*

class VecTest {
	@Test
	fun dotProducts() {
		val a = Vec(1f, 0f, 0f)
		val b = Vec(0f, 1f, 0f)
		assertEquals(a.dot(b), 0f)
		assertEquals(a.dot(a), 1f)
	}

	@Test
	fun crossProducts() {
		val a = Vec(1f, 0f, 0f)
		val b = Vec(0f, 1f, 0f)
		val c = Vec(0f, 0f, 1f)
		assertEquals(a.cross3(b), c)
	}
}
