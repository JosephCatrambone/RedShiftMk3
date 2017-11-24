#version 120

#ifdef GL_ES
precision mediump float;
#endif

// Constants.
uniform mat4 u_cameraTransform;

// Passed in and carried over.  Modifyable.
varying vec4 v_color;
varying vec2 v_textureCoordinate;

void main() {
    gl_FragColor = v_color; //vec4(v_textureCoordinate, 1.0, 1.0);
}