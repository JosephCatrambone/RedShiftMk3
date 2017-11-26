#version 120

// Passed in, but not carried over.
attribute vec3 a_position;
attribute vec4 a_diffuse;
attribute vec3 a_normal;
attribute vec2 a_textureCoordinate;

attribute vec3 a_light0_position;
attribute vec4 a_light0_color;
attribute float a_light0_size;

// Constants.
uniform mat4 u_worldTransform;
uniform mat4 u_cameraTransform;

// Passed in and carried over.  Modifyable.
varying vec3 v_light0_position;
varying vec4 v_light0_color;
varying float v_light0_size;
varying vec4 v_color;
varying vec2 v_textureCoordinate;

float saturate(in float v) {
	return clamp(v, 0.0f, 1.0f);
}

void main() {
	v_light0_position = a_light0_position;
	v_light0_color = a_light0_color;
	v_light0_size = a_light0_size;

	v_textureCoordinate = a_textureCoordinate;
	v_color = (a_diffuse * v_light0_color) / distance(a_position, a_light0_position);
	gl_Position = u_cameraTransform * u_worldTransform * vec4(a_position, 1.0);
}
