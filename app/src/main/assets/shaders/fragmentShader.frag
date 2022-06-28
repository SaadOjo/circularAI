precision mediump float;

varying vec2 vTexCoords;
uniform sampler2D uTexture;
uniform vec4 vColor;
void main() {
  //gl_FragColor = vColor;
  gl_FragColor = texture2D(uTexture, vTexCoords);
}