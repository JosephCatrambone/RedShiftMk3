#version 330

// Passed in, but not carried over.
in vec3 a_position; // We use a_position instead of gl_Vertex because we may want to translate with bones or morph in app.
in vec4 a_diffuse;
in vec3 a_normal;
in vec2 a_textureCoordinate;

in float a_light0_intensity;
in vec3 a_light0_position;
in vec4 a_light0_color;
in float a_light0_size;

// Constants.
uniform mat4 u_worldTransform;
uniform mat4 u_cameraTransform;

// Passed in and carried over.  Modifyable.
out float v_light0_intensity;
out vec3 v_light0_position;
out vec4 v_light0_color;
out float v_light0_size;
out vec3 v_position;
out vec4 v_color;
out vec2 v_textureCoordinate;

float saturate(in float v) {
	return clamp(v, 0.0f, 1.0f);
}

void main() {
	v_light0_intensity = a_light0_intensity;
	v_light0_position = a_light0_position;
	v_light0_color = a_light0_color;
	v_light0_size = a_light0_size;

	v_textureCoordinate = a_textureCoordinate;
	v_color = vec4((v_light0_intensity * a_diffuse.rgb * v_light0_color.rgb) / pow(distance(a_position, v_light0_position), 0), 1.0f);
	gl_Position = u_cameraTransform * u_worldTransform * vec4(a_position, 1.0);
}
