#version 150

uniform sampler2D Sampler0;
uniform float Time;

in vec2 texCoord;
in vec4 vertexColor;

out vec4 fragColor;

mat3 m = mat3(0.00, 0.80, 0.60, -0.80, 0.36, -0.48, -0.60, -0.48, 0.64);

float hash(float n) {
    return fract(sin(n) * 43758.5453);
}

float noise(in vec3 x) {
    vec3 p = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    
    float n = p.x + p.y * 57.0 + 113.0 * p.z;
    
    float res = mix(
        mix(mix(hash(n + 0.0), hash(n + 1.0), f.x),
            mix(hash(n + 57.0), hash(n + 58.0), f.x), f.y),
        mix(mix(hash(n + 113.0), hash(n + 114.0), f.x),
            mix(hash(n + 170.0), hash(n + 171.0), f.x), f.y),
        f.z);
    
    return res;
}

float fbm(vec3 p) {
    float f;
    f = 0.5000 * noise(p);
    p = m * p * 2.02;
    f += 0.2500 * noise(p);
    p = m * p * 2.03;
    f += 0.1250 * noise(p);
    return f;
}

void main() {
    vec2 uv = texCoord;
    
    vec3 pos = vec3(uv * 3.0, Time * 0.3);
    
    float density = fbm(pos);
    
    float wave = sin(uv.x * 10.0 + Time) * cos(uv.y * 10.0 + Time * 0.7) * 0.5 + 0.5;
    density = mix(density, wave, 0.3);
    
    vec3 cloudColor = mix(vertexColor.rgb * 0.6, vertexColor.rgb * 1.2, density);
    
    float edge = 1.0 - abs(uv.x - 0.5) * 2.0;
    edge *= 1.0 - abs(uv.y - 0.5) * 2.0;
    edge = pow(edge, 0.5);
    
    cloudColor += vertexColor.rgb * edge * 0.3;
    
    float alpha = (density * 0.5 + 0.3) * vertexColor.a;
    
    fragColor = vec4(cloudColor, alpha);
}
