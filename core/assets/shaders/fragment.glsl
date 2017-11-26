#version 330

#ifdef GL_ES
precision mediump float;
#endif

// Constants.
uniform mat4 u_cameraTransform;

// Passed in and carried over.  Modifyable.
in vec3 v_position;
in vec4 v_color;
in vec2 v_textureCoordinate;

void main() {
    gl_FragColor = v_color; //vec4(v_textureCoordinate, 1.0, 1.0);
}