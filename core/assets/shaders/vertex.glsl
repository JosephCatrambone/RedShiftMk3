#version 330

const float WORLD_SCALE = 10f;

// Passed in, but not carried over.
// DO NOT CHANGE THESE NAMES.
in vec3 a_position; // We use a_position instead of gl_Vertex because we may want to translate with bones or morph in app.
in vec4 a_color;
in vec3 a_normal;
in vec2 a_texCoord;
in vec3 a_tangent;

// Constants.
// THESE NAMES YOU CAN CHANGE IF YOU UPDATE PBRSHADER!
uniform mat4 u_worldTransform;
uniform mat4 u_cameraTransform;

// Making lights a uniform because they're the same for all points in this model.
uniform float u_light0_intensity;
uniform vec3 u_light0_position;
uniform vec4 u_light0_color;
uniform float u_light0_size;

uniform float u_light1_intensity;
uniform vec3 u_light1_position;
uniform vec4 u_light1_color;
uniform float u_light1_size;

uniform float u_light2_intensity;
uniform vec3 u_light2_position;
uniform vec4 u_light2_color;
uniform float u_light2_size;

// Passed in and carried over.  Modifyable.
out vec3 v_position_world;
out vec4 v_position_screen;
out vec4 v_color;
out vec2 v_textureCoordinate;

float saturate(in float v) {
	return clamp(v, 0.0f, 1.0f);
}

void main() {
	v_textureCoordinate = a_texCoord;
	v_position_world = (u_worldTransform*vec4(a_position, 1.0)).xyz;
	v_position_screen = u_cameraTransform * u_worldTransform * vec4(a_position, 1.0);
	v_color = a_color; 
	gl_Position = v_position_screen;
}
