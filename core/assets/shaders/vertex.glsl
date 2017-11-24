#version 120

// Passed in, but not carried over.
attribute vec3 a_position;
attribute vec4 a_diffuse;
attribute vec3 a_normal;
attribute vec2 a_textureCoordinate;

// Constants.
uniform mat4 u_worldTransform;
uniform mat4 u_cameraTransform;

// Passed in and carried over.  Modifyable.
varying vec4 v_color;
varying vec2 v_textureCoordinate;

void main() {
	v_textureCoordinate = a_textureCoordinate;
	v_color = a_diffuse;
	gl_Position = u_cameraTransform * u_worldTransform * vec4(a_position, 1.0);
}
