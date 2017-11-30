#version 330

#ifdef GL_ES
precision mediump float;
#endif

// Constants.
uniform mat4 u_worldTransform;
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
in vec2 v_position_screen;
in vec3 v_position_world;
in vec4 v_color;
in vec3 v_normal;
in vec2 v_textureCoordinate;

void main() {
	//v_textureCoordinate = a_texCoord;
	//gl_Position = u_cameraTransform * u_worldTransform * vec4(a_position.x, a_position.y, a_position.z, 1.0);

	// We do this calc in vertex instead.
	//vec3 v_position_world = (u_worldTransform*vec4(a_position, 1.0)).xyz;
	vec3 worldNormal = normalize(u_worldTransform * vec4(v_normal, 0.0)).xyz;
	float brightnessNormalModifier0 = 1.0; //dot(worldNormal, (u_worldTransform*vec4(u_light0_position, 1.0)).xyz);
	float brightnessNormalModifier1 = 1.0; //dot(worldNormal, (u_worldTransform*vec4(u_light1_position, 1.0)).xyz);
	float brightnessNormalModifier2 = 1.0; //dot(worldNormal, (u_worldTransform*vec4(u_light2_position, 1.0)).xyz);
	vec3 finalColor = vec3(0f, 0f, 0f);
	finalColor += vec3((brightnessNormalModifier0 * u_light0_intensity * v_color.rgb * u_light0_color.rgb) / pow(distance(v_position_world, u_light0_position), 1));
	finalColor += vec3((brightnessNormalModifier1 * u_light1_intensity * v_color.rgb * u_light1_color.rgb) / pow(distance(v_position_world, u_light1_position), 1));
	finalColor += vec3((brightnessNormalModifier2 * u_light2_intensity * v_color.rgb * u_light2_color.rgb) / pow(distance(v_position_world, u_light2_position), 1));
    gl_FragColor = vec4(finalColor, 1.0f); //vec4(v_textureCoordinate, 1.0, 1.0);
    //gl_FragColor = vec4(1.0f, 1.0f, 1.0f, 1.0f);
}
