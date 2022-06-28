uniform mat4 uMVPMatrix;
attribute vec4 vPosition;
attribute vec2 aTexCoords;
varying vec2 vTexCoords;
void main() {
  vTexCoords = vec2(aTexCoords.x, (1.0 -  aTexCoords.y));
  gl_Position = uMVPMatrix * vPosition;
}