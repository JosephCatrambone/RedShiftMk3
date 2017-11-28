#version 330

#ifdef GL_ES
precision mediump float;
#endif

// Constants.
uniform mat4 u_cameraTransform;

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
in vec3 v_position_world;
in vec3 v_position;
in vec4 v_color;
in vec2 v_textureCoordinate;

void main() {
	//v_textureCoordinate = a_texCoord;
	//gl_Position = u_cameraTransform * u_worldTransform * vec4(a_position.x, a_position.y, a_position.z, 1.0);
	//v_position_world = (u_worldTransform*vec4(a_position, 1.0));
	vec3 finalColor = vec3(0f, 0f, 0f);
	finalColor += vec3((u_light0_intensity * v_color.rgb * u_light0_color.rgb) / pow(distance(v_position_world, u_light0_position), 1));
	finalColor += vec3((u_light1_intensity * v_color.rgb * u_light1_color.rgb) / pow(distance(v_position_world, u_light1_position), 1));
	finalColor += vec3((u_light2_intensity * v_color.rgb * u_light2_color.rgb) / pow(distance(v_position_world, u_light2_position), 1));
    gl_FragColor = vec4(finalColor, 1.0f); //vec4(v_textureCoordinate, 1.0, 1.0);
}
