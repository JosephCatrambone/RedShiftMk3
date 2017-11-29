package io.xoana.redshift

class MinHeap<T>(size:Int, val comparator: Comparator<T>): Iterable<T> {

	private var nextInsertPosition = 0
	private val items:Array<Any?> = arrayOfNulls<Any?>(size)

	val capacity:Int
		get() = items.size

	val size:Int // TODO: Efficiency gains here.  Might be O(n)
		get() = items.filterNotNull().size

	val isEmpty:Boolean
		get() = nextInsertPosition == 0

	val isFull:Boolean
		get() = nextInsertPosition >= items.size

	override fun iterator(): Iterator<T> {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	private fun getParent(index:Int): Int = (index-1)/2
	private fun leftChild(index:Int): Int = (index)*2 + 1
	private fun rightChild(index:Int): Int = (index+1)*2

	override fun toString(): String {
		return items.joinToString(", ")
	}

	//         0
	//     1        2
	//  3   4     5     6
	// 7 8 9 10 11 12 13 14
	//
	// Left chain: 0 1 3 7 15
	// Right chain: 0 2 6 14

	fun verifyHeap() {
		items.forEachIndexed({i, value ->
			val leftPos = leftChild(i)
			val rightPos = rightChild(i)
			if(leftPos < items.size && items[leftPos] != null) {
				assert(comparator.compare(value as T, items[leftPos] as T) <= 0)
			}
			if(rightPos < items.size && items[rightPos] != null) {
				assert(comparator.compare(value as T, items[rightPos] as T) <= 0)
			}
		})
	}

	/*** push
	 * Push the item onto the heap, maintaining the heap constant.
	 * If the heap is full, drop the biggest.
	 */
	fun push(item:T) {
		if(nextInsertPosition > items.size) { // Next item will be outside of our heap.
			TODO()
		}
		// Push into the next position.
		var pos = nextInsertPosition
		nextInsertPosition++
		items[pos] = item
		while(pos != 0){
			val parentIndex = getParent(pos)
			if(comparator.compare(items[pos] as T, items[parentIndex] as T) < 0) {
				val tmp = items[parentIndex]
				items[parentIndex] = items[pos]
				items[pos] = tmp
			}
			pos = parentIndex
		}
	}

	fun pop(): T? {
		if(isEmpty) {
			return null
		}
		val ret = items[0] as T?
		// Step back the next insert and put it at the root.
		nextInsertPosition--
		items[0] = items[nextInsertPosition]
		items[nextInsertPosition] = null

		// Keep swapping this item with the smalles child until the it's smaller than all children.
		var pos = 0
		var leftPos = leftChild(pos)
		var rightPos = rightChild(pos)
		var swapHappened = true
		while(leftPos < items.size && rightPos < items.size && swapHappened) {
			swapHappened = false
			val currentValue = items[pos] as T?
			val leftValue = items[leftPos] as T?
			val rightValue = items[rightPos] as T?

			val leftSmallerThanCurrent = if(leftValue == null) { false } else { comparator.compare(leftValue, currentValue) < 0 }
			val rightSmallerThanCurrent = if(rightValue == null) { false } else { comparator.compare(rightValue, currentValue) < 0 }
			if(!leftSmallerThanCurrent && !rightSmallerThanCurrent) {
				break
			} else if(leftSmallerThanCurrent && rightSmallerThanCurrent) {
				// current value is greater than the left or greater than the right.  Swap with the lesser.
				val leftSmallerThanRight = comparator.compare(leftValue, rightValue) < 0
				if(leftSmallerThanRight) { // Left is smaller.
					items[pos] = leftValue
					items[leftPos] = currentValue
					pos = leftPos
					swapHappened = true
				} else { // Right is smaller.
					items[pos] = rightValue
					items[rightPos] = currentValue
					pos = rightPos
					swapHappened = true
				}
			} else if(leftSmallerThanCurrent) {
				items[pos] = leftValue
				items[leftPos] = currentValue
				pos = leftPos
				swapHappened = true
			} else if(rightSmallerThanCurrent) {
				items[pos] = rightValue
				items[rightPos] = currentValue
				pos = rightPos
				swapHappened = true
			}
			leftPos = leftChild(pos)
			rightPos = rightChild(pos)
		}

		return ret
	}

	fun peek(): T? {
		if(isEmpty) {
			return null
		} else {
			return items[0] as T
		}
	}

}