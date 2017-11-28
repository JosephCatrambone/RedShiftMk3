package io.xoana.redshift

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import java.util.*

/**
 * Created by Jo on 2017-07-09.
 * 2017-11-26 : Added more triangulation and bug fixes.  Added normal to Triangle.  Added point-in-triangle.
 * 2017-11-24 : Added ray-triangle intersection.
 */
class Vec(var x:Float=0f, var y:Float=0f, var z:Float=0f, var w:Float=0f) {
	val EPSILON = 1e-6f

	// libGDX Interop section.
	constructor(v2: Vector2):this(v2.x, v2.y, 0f, 0f)
	constructor(v3: Vector3):this(v3.x, v3.y, v3.z, 0f)

	fun toGDXVector2(): Vector2 = Vector2(this.x, this.y)
	fun toGDXVector3(): Vector3 = Vector3(this.x, this.y, this.z)
	// End libGDX interop section

	val isZero:Boolean
		get():Boolean = x==0f && y==0f && z==0f && w==0f

	val squaredMagnitude:Float
		get():Float = this.dot(this)

	val magnitude:Float
		get():Float = Math.sqrt(this.squaredMagnitude.toDouble()).toFloat()

	var data:FloatArray
		get() = floatArrayOf(x, y, z, w)
		set(value:FloatArray) {
			this.x = value.getOrElse(0, {_ -> 0f})
			this.y = value.getOrElse(1, {_ -> 0f})
			this.z = value.getOrElse(2, {_ -> 0f})
			this.w = value.getOrElse(3, {_ -> 0f})
		}

	override fun toString(): String {
		return "<$x, $y, $z, $w>"
	}

	override fun equals(other: Any?): Boolean {
		if(other !is Vec) {
			return false
		}

		return distanceSquared(other) < EPSILON
	}

	operator fun plus(value:Float):Vec = Vec(this.x+value, this.y+value, this.z+value, this.w+value)
	operator fun minus(value:Float):Vec = Vec(this.x-value, this.y-value, this.z-value, this.w-value)
	operator fun times(value:Float):Vec = Vec(this.x*value, this.y*value, this.z*value, this.w*value)
	operator fun div(value:Float):Vec = Vec(this.x/value, this.y/value, this.z/value, this.w/value)

	operator fun plus(other:Vec):Vec = Vec(this.x+other.x, this.y+other.y, this.z+other.z, this.w+other.w)
	operator fun minus(other:Vec):Vec = Vec(this.x-other.x, this.y-other.y, this.z-other.z, this.w-other.w)
	operator fun times(other:Vec):Vec = Vec(this.x*other.x, this.y*other.y, this.z*other.z, this.w*other.w)
	operator fun div(other:Vec):Vec = Vec(this.x/other.x, this.y/other.y, this.z/other.z, this.w/other.w) // TODO: This probably shouldn't exist because of the default zeros.

	operator fun plusAssign(value:Float) { x += value; y += value; z += value; w += value; }

	fun sum():Float {
		return x+y+z+w
	}

	fun dot2(other:Vec):Float = this.x*other.x + this.y*other.y

	fun dot3(other:Vec):Float = this.x*other.x + this.y*other.y + this.z*other.z

	fun dot(other:Vec):Float {
		return (this * other).sum()
	}

	// Perform the cross product with this as a vector2
	fun cross2(other:Vec):Float = x*other.y - y*other.x

	fun cross3(other:Vec):Vec {
		return Vec(
				this.y*other.z - this.z*other.y,
				this.x*other.z - this.z*other.x,
				this.x*other.y - this.y*other.x
		)
		// (2,3,4) (x) (5,6,7) -> (-3, 6, -3)
	}

	fun normalized():Vec {
		// elements / sqrt(sum(elem ^2))
		val mag = this.magnitude
		// If we have no magnitude, just return a zero vec.
		if(mag == 0f) {
			TODO("Unhandled case: Normalizing zero-length vector")
		}

		return Vec(x/mag, y/mag, z/mag, w/mag)
	}

	fun normalize() {
		val mag = this.magnitude
		if(mag == 0f) { TODO() }
		x /= mag
		y /= mag
		z /= mag
		w /= mag
	}

	fun distanceSquared(other:Vec): Float {
		val delta = this - other
		return delta.x*delta.x + delta.y*delta.y + delta.z*delta.z + delta.w*delta.w
	}

	fun project(other:Vec): Vec {
		// Project this vector onto the other.
		// (A dot norm(b)) * norm(b)
		// or
		// (a dot b) / (b dot b) * b
		val bNorm = other.normalized()
		return bNorm * this.dot(bNorm)
	}
}

class Line(val start:Vec, val end:Vec) {
	/***
	 *
	 */
	fun intersection2D(other:Line, epsilon:Float = 1e-8f): Vec? {
		// Begin with line-line intersection.
		val determinant = ((start.x-end.x)*(other.start.y-other.end.y))-((start.y-end.y)*(other.start.x-other.end.x))
		if(Math.abs(determinant.toDouble()).toFloat() < epsilon) {
			return null;
		}

		val candidatePoint = Vec(
				((start.x*end.y - start.y*end.x)*(other.start.x-other.end.x))-((start.x-end.x)*(other.start.x*other.end.y - other.start.y*other.end.x)),
				((start.x*end.y - start.y*end.x)*(other.start.y-other.end.y))-((start.y-end.y)*(other.start.x*other.end.y - other.start.y*other.end.x))
		)/determinant

		// If the lines are infinite, we're done.  No more work.
		return candidatePoint
	}

	fun segmentIntersection2D(other:Line): Vec? {
		val a = this.start
		val b = this.end
		val c = other.start
		val d = other.end

		val r = b-a
		val s = d-c

		val rxs = r.cross2(s)
		val t:Float = (c-a).cross2(s)/rxs
		val u:Float = (c-a).cross2(r)/rxs

		if(t < 0 || t > 1 || u < 0 || u > 1) {
			return null;
		}
		return a + r*t
	}

	fun shortestConnectingSegment(other:Line, epsilon:Float=1e-6f): Line? {
		// Returns the smallest line segment between two lines in N-dimensional space.
		// This can be thought of as 'intersection' in some sense if len < epsilon.
		// Ported from Paul Bourke's algorithm.
		// The line from L12 to L34 is Lab.
		// Lab = Pa -> Pb
		// L12 = P1 -> P2
		// L34 = P3 -> P4
		// Pa = P1 + m(P2-P1) // Pa is on L12
		// Pb = P3 + n(P4-P3) // Pb is on L34
		// Want to minimize ||Pb - Pa||^2
		// Therefore: minimize ||P3 + n(P4-P3) - P1 + m(P2-P1)||^2
		// Side note: shortest line between the two has to be perpendicular to both, meaning (Pa - Pb) dot (P2 - P1) = 0 AND (Pa - Pb) dot (P4 - P3) = 0.
		val p1 = this.start
		val p2 = this.end
		val p3 = other.start
		val p4 = other.end
		val p13 = p1 - p3
		val p43 = p4 - p3
		val p21 = p2 - p1

		if(p43.squaredMagnitude < epsilon || p21.squaredMagnitude < epsilon) {
			return null
		}

		val d1343 = p13.dot(p43)
		val d4321 = p43.dot(p21)
		val d1321 = p13.dot(p21)
		val d4343 = p43.dot(p43)
		val d2121 = p21.dot(p21)

		val denominator = d2121*d4343 - d4321*d4321
		if(denominator < epsilon) {
			return null
		}

		val numerator = d1343 * d4321 - d1321 * d4343
		val m = numerator/denominator
		val n = (d1343 + (d4321 * m)) / d4343

		val pa = p1 + (p21*m)
		val pb = p3 + (p43*n)

		return Line(pa, pb)
	}

	fun pointOnLine(pt:Vec, epsilon:Float = 1e-6f):Boolean {
		// Is this point a solution to this line?
		if(epsilon == 0f) {
			// THIS IS A BAD IDEA!  NUMERICAL PRECISION IS A FACTOR!
			// p1 + t*(p2-p1) = pt?
			// Just solve for t, and if the value is between 0 and 1, it's on the line.
			// t*(p2-p1) = pt - p1
			// t = (pt - p1)/(p2-p1)
			// Unfortunately, we've gotta' do this component-wise.
			val tX = (pt.x - start.x) / (end.x - start.x)
			if (tX < 0 || tX > 1) {
				return false
			}
			val tY = (pt.y - start.y) / (end.y - start.y)
			if (tY < 0 || tY > 1) {
				return false
			}
			return true
		} else {
			TODO("Bugfix")
			return ((Math.abs(((end.y-start.y)*pt.x - (end.x-start.x)*pt.y + end.x*start.y - end.y*start.x).toDouble()))/Math.sqrt(((end.x-start.x)*(end.x-start.x) + (end.y-start.y)*(end.y-start.y)).toDouble()).toFloat()) < epsilon
		}
	}
}

class AABB(val x:Float, val y:Float, val w:Float, val h:Float) {

	val center = Vec(x+(w/2), y+(h/2))

	fun overlaps(other:AABB):Boolean {
		// This Right < Other Left
		if(x+w < other.x) {
			return false
		}
		// thisTop < otherBottom
		if(y+h < other.y) {
			return false
		}
		// thisLeft > otherRight
		if(x > other.x+other.w) {
			return false
		}
		// thisBottom > otherTop
		if(y > other.y+other.h) {
			return false
		}
		return true
	}

	fun pointInside(otherX:Float, otherY:Float):Boolean {
		return otherX > this.x && otherX < (this.x+this.w) && otherY > this.y && otherY < (this.y+this.h)
	}

	// Returns the smallest vector required to push the other AABB out of this one.
	// NOTE: Assumes that these AABBs overlap.
	fun getPushForce(other:AABB): Vec {
		// For each axis, calculate the overlap and keep the smallest one.
		val otherCenter = other.center
		val thisCenter = this.center
		val zeroOverlapX = this.w/2 + other.w/2 // Sum of half-widths.
		val dx = otherCenter.x - thisCenter.x
		val forceX = zeroOverlapX - Math.abs(dx) // If we apply forceX to the other object, it will bring it to this one's surface.
		val zeroOverlapY = this.h/2 + other.h/2
		val dy = otherCenter.y - thisCenter.y
		val forceY = zeroOverlapY - Math.abs(dy)
		if(Math.abs(forceX) < Math.abs(forceY)) {
			return Vec(Math.copySign(forceX, dx))
		} else {
			return Vec(0f, Math.copySign(forceY, dy))
		}
	}
}

class Triangle(val a:Vec, val b:Vec, val c:Vec) {
	val normal: Vec
		get() = (b-a).cross3(c-a)

	// Returns +1 if it's a left 0.  0 if abc are collinear.  -1 if right turn.
	fun getTurn(): Int {
		// Det |1 ax ay|
		//     |1 bx bt| > 0 -> Making a left hand turn.
		//     |1 cx cy|
		val det = 1f*(b.x*c.y - b.y*c.x) + -a.x*(1*c.y - b.y*1) + a.y*(1*c.x - b.x*1)
		if(det < 0) { return -1 }
		else if(det == 0f) { return 0 }
		else if(det > 0) { return 1 }
		else {
			throw Exception("Invalid state: determinant is NaN.  How can this happen?")
		}
	}

	// Returns true if the triangle ABC is making a left turn.
	fun leftTurn(): Boolean {
		return getTurn() > 0f
	}

	fun intersection(line:Line, lineSegment:Boolean=false, planeIntersection:Boolean=false, epsilon:Float=1e-6f): Vec? {
		/*
		If the line doesn't intersect the triangle, returns null.
		If planeIntersection is true, will return where the line intersects the plane instead of just the triangle.
		If lineSegment is true, will return where the line intersects the plane or, if the point is not on the line, returns null.
		Let N be the normal of the triangle as determined by the ab (cross) ac.
		If then a point Q on the plane satisfies the equation N dot (Q-a) = 0. (?)
		Substitute: N dot (P1 + u(P2-P1)) = N dot P3
		u = N dot (a - P1) / N dot (P2 - P1)
		*/
		val r = b-a
		val s = c-a
		val normal = r.cross3(s)
		val numerator = normal.dot3(a - line.start)
		val denominator = normal.dot3(line.end - line.start)

		if(Math.abs(denominator) < epsilon) {
			return null
		}

		val t = numerator / denominator
		if(lineSegment && (t < 0.0f || t > 1.0f)) {
			return null // There is an intersection, but it's not on the line.
		}

		// Get the point.
		val p = line.start + (line.end-line.start)*t

		// If we care only about the plane, can return here.
		if(planeIntersection) {
			return p
		}

		if(pointInTriangle3D(p, epsilon)) {
			return p
		} else {
			return null
		}
	}

	fun pointInCircumcircle2D(d:Vec): Boolean {
		// If the det of the 4x4 matrix made by | ax ay ax^2+ay^2 1 | for a through d > 0, then d is inside the circumcircle if abc is a CCW polygon.
		// a-dx a-dy (a-dx)^2+(a-dy)^2
		val da = a-d
		val db = b-d
		val dc = c-d

		// q r s
		// t u v
		// w x y

		val q = da.x
		val r = da.y
		val s = da.x*da.x + da.y*da.y

		val t = db.x
		val u = db.y
		val v = db.x*db.x + db.y*db.y

		val w = dc.x
		val x = dc.y
		val y = dc.x*dc.x + dc.y*dc.y
		val det = q*(u*y-v*x) - r*(t*y - v*w) + s*(t*x-u*w)

		if(leftTurn()) {
			return det > 0
		} else {
			return det < 0
		}
	}

	fun pointInTriangle3D(p:Vec, epsilon:Float=1e-6f):Boolean {
		val r = b-a
		val s = c-a

		// Check to see if the point is inside the triangle.
		var alpha = r.dot3(r)
		var beta = r.dot3(s)
		var gamma = beta
		var delta = s.dot3(s)

		val invDeterminant = alpha*delta - gamma*beta
		if(Math.abs(invDeterminant) < epsilon) {
			return false
		}
		val determinant = 1.0f / invDeterminant
		alpha *= determinant
		beta *= determinant
		gamma *= determinant
		delta *= determinant

		// 2x3 matrix.
		val mRow0 = (r * delta) + (s * -beta)
		val mRow1 = (r * -gamma) + (s * alpha)

		val u = mRow0.dot(p - a)
		val v = mRow1.dot(p - a)

		return u >= 0 && v >= 0 && u <= 1 && v <= 1 && u+v <= 1
	}

	fun pointInTriangle2D(p:Vec):Boolean {
		/*
		// Any point p where [B-A] cross [p-A] does not point in the same direction as [B-A] cross [C-A] isn't inside the triangle.
		val pPrime = p-a
		val q = b-a
		val r = c-a
		if((q.cross2(pPrime) >= 0) != (q.cross2(r) >= 0)) {
			return false // Can't be
		}
		// One more check now.  Need to verify it's inside the other sides, too.
		val qPrime = p-b
		val s = a-b
		val t = c-b
		val qpos = s.cross2(qPrime)
		val dpos = s.cross2(t)
		return (qpos >= 0) == (dpos >= 0)
		*/
		// Barycentric coordinate version from Realtime Collision Detection.
		// Compute vectors
		val v0 = c-a
		val v1 = b-a
		val v2 = p-a

		// Compute dot products
		val dot00 = v0.dot2(v0)
		val dot01 = v0.dot2(v1)
		val dot02 = v0.dot2(v2)
		val dot11 = v1.dot2(v1)
		val dot12 = v1.dot2(v2)

		// Compute barycentric coordinates
		val denom = (dot00 * dot11 - dot01 * dot01)
		if(denom == 0f) { // degenerate.
			return false
		}
		val invDenom = 1 / denom
		val u = (dot11 * dot02 - dot01 * dot12) * invDenom
		val v = (dot00 * dot12 - dot01 * dot02) * invDenom

		// Check if point is in triangle
		return (u >= 0) && (v >= 0) && (u + v < 1)
	}
}

class Polygon(val points:List<Vec>) {
	// TODO: There's a bug here in the triangulation for big sectors.

	// Triangulate this polygon, returning a list of 3n integers for the indices.
	// O(n^3) runtime.
	fun triangulate(up:Vec, counterClockWise:Boolean=true): IntArray {
		// First, build a triangulation from these polygon points.
		val triangles = mutableListOf<Triangle>()
		val pointIndices = MutableList<Int>(this.points.size, {i -> i})
		val finalTriangleIndices = mutableListOf<Int>()

		// Randomly shuffle the points.
		val random = Random()


		return finalTriangleIndices.toIntArray()
	}

	fun pointInside(pt:Vec): Boolean {
		// Cast a ray past the right edge.  If it crosses an even number of lines, it's outside.  Odd number, inside.
		// First, find the max value of this polygon.
		val maxX = points.fold(-Float.MAX_VALUE, {acc, newVal -> maxOf(acc, newVal.x)})
		// Build a line that reaches past the right side.
		val line = Line(pt, pt+Vec(maxX+1f, 0f))
		// Count the intersections.
		var intersectionCount = 0
		for(i in 0 until points.size-1) {
			val p1 = points[i]
			val p2 = points[(i+1)%points.size]
			val otherLine = Line(p1, p2)
			if(line.segmentIntersection2D(otherLine) != null) {
				intersectionCount++
			}
		}
		return intersectionCount%2 == 0
	}

	fun splitAtPoints(p1:Vec, p2:Vec, vararg innerPoints:Vec): Pair<Polygon, Polygon> {
		// Split the polygon with a fracture starting from p1 and running to p2.  Two polygons will be returned.
		// If innerPoints are returned, they will be included in both polygons between the fracture points.
		// Both p1 and p2 will be present in both sections.

		// Go around the points and find the indices of the cut.
		var p1Index=-1
		var p1Distance = Float.MAX_VALUE
		var p2Index=-1
		var p2Distance = Float.MAX_VALUE
		for(i in 0 until points.size) {
			// Get the distance from this point to p1.
			val distToP1 = p1.distanceSquared(points[i])
			val distToP2 = p2.distanceSquared(points[i])
			if(distToP1 < p1Distance) {
				p1Index = i
				p1Distance = distToP1
			}
			if(distToP2 < p2Distance) {
				p2Index = i
				p2Distance = distToP2
			}
		}

		// Make sure that p1 comes first.
		if(p2Index < p1Index) {
			val temp = p1Index
			p1Index = p2Index
			p2Index = temp
		}

		// If we have no internal points, we can just return the two new poligons.
		if(innerPoints.isEmpty()) {
			val leftPoints = points.filterIndexed{ i,v -> i <= p1Index || i >= p2Index }
			val rightPoints = points.filterIndexed{ i,v -> i >= p1Index && i <= p2Index }
			return Pair(Polygon(leftPoints), Polygon(rightPoints))
		} else {
			// There are two ways to assemble the halfs.
			// If the first point in the innerPoint list is closer to the start p1 and the end point is closer to p2, keep straight.  Otherwise flip the inner list.
			// p1 ------------------------- p2
			//     innerpt1 -------- innerp2
			val p1ToSplitHeadDistance = points[p1Index].distanceSquared(innerPoints.first())
			val p1ToSplitTailDistance = points[p1Index].distanceSquared(innerPoints.last())
			val p2ToSplitHeadDistance = points[p2Index].distanceSquared(innerPoints.first())
			val p2ToSplitTailDistance = points[p2Index].distanceSquared(innerPoints.last())
			if(p1ToSplitHeadDistance < p2ToSplitHeadDistance && p2ToSplitTailDistance < p1ToSplitTailDistance) {
				// We should take the points in order.
				val leftPoints = points.filterIndexed{ i,v -> i <= p1Index }.plus(innerPoints).plus(points.filterIndexed{ i,v -> i >= p2Index })
				val rightPoints = innerPoints.reversed().plus(points.filterIndexed{i,v -> i >= p1Index && i <= p2Index})
				return Pair(Polygon(leftPoints), Polygon(rightPoints))
			} else if(p1ToSplitHeadDistance > p2ToSplitHeadDistance && p2ToSplitTailDistance > p1ToSplitTailDistance) {
				// We should take the points reversed.
				val leftPoints = points.filterIndexed{ i,v -> i <= p1Index }.plus(innerPoints.reversed()).plus(points.filterIndexed{ i,v -> i >= p2Index })
				val rightPoints = innerPoints.toList().plus(points.filterIndexed{i,v -> i >= p1Index && i <= p2Index})
				return Pair(Polygon(leftPoints), Polygon(rightPoints))
			} else {
				// It's not clear which way they should rotate.
				throw Exception("Ambiguous split direction.  Polygon can't figure out how to match up the divide.")
			}
		}
	}
}
