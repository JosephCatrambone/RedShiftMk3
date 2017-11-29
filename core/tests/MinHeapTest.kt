import org.junit.Test
import org.junit.Assert.*
import io.xoana.redshift.*
import java.util.*

class MinHeapTest {
	val random = Random()

	@Test
	fun checkMinHeapConstraint() {
		val ordering = mutableListOf<Int>()
		var prev = -1
		val size = 100
		val mh = MinHeap<Int>(size, kotlin.Comparator<Int>({o1, o2 ->
			//println("Comparing $o1 and $o2")
			o1.compareTo(o2)
		}))
		while(!mh.isFull) {
			val v = random.nextInt(100)
			ordering.add(v)
			mh.push(v)
			mh.verifyHeap()
		}
		prev = mh.pop()!! // Can't be null.  We just filled it.
		while(!mh.isEmpty) {
			val v = mh.pop()
			mh.verifyHeap()
			assertNotNull("Not null check failure. Ordering $ordering", v)
			assertTrue("Condition check failed.  $prev > $v \n Ordering: $ordering", prev <= v!!)
			prev = v
		}
	}

	fun checkOldFailureCase(ordering: List<Int>) {
		var iterations = 0
		val mh = MinHeap<Int>(ordering.size, kotlin.Comparator({ o1, o2 -> o1.compareTo(o2) }))
		ordering.forEach { i ->
			mh.push(i)
			mh.verifyHeap()
		}
		var prev = mh.pop()!!
		while(!mh.isEmpty) {
			val v = mh.pop()
			mh.verifyHeap()
			assertNotNull("Not null check failure after $iterations steps.  ${mh.size} elements remained.", v)
			assertTrue("Condition check failed.  $prev > $v after $iterations steps. ${mh.size} elements remained.", prev <= v!!)
			prev = v
			iterations++
		}
	}

	@Test
	fun testOldFailureCase1() {
		checkOldFailureCase(listOf<Int>(61, 0, 54, 73, 18, 52, 78, 38, 76, 90, 60, 68, 6, 49, 19, 24, 8, 95, 98, 45, 75, 6, 8, 1, 70, 83, 3, 15, 2, 2, 19, 75, 13, 74, 58, 93, 64, 94, 37, 43, 3, 31, 58, 76, 11, 29, 66, 78, 40, 77, 78, 15, 21, 45, 54, 48, 0, 12, 30, 43, 91, 88, 13, 71, 33, 38, 76, 6, 79, 49, 15, 99, 46, 68, 83, 98, 69, 2, 60, 41, 80, 68, 63, 24, 5, 62, 85, 29, 48, 74, 90, 63, 71, 78, 0, 58, 7, 5, 93, 64))
	}
}
