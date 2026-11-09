#version 330

uniform sampler2D InSampler;

layout(std140) uniform ColorGradeConfig {
    float Saturation;
    float Brightness;
    float Contrast;
    float Hue;
    vec4 Tint;
    vec4 Extra;
};

in vec2 texCoord;

out vec4 fragColor;

vec3 shiftHue(vec3 c, float a) {
    const vec3 k = vec3(0.57735);
    float ca = cos(a);
    return c * ca + cross(k, c) * sin(a) + k * dot(k, c) * (1.0 - ca);
}

void main() {
    vec3 c = texture(InSampler, texCoord).rgb;
    c = shiftHue(c, Hue);
    c *= Brightness;
    c = (c - 0.5) * Contrast + 0.5;
    float l = dot(c, vec3(0.2126, 0.7152, 0.0722));
    c = mix(vec3(l), c, Saturation);
    c = mix(c, l * Tint.rgb * 1.35, Tint.a);
    c = pow(max(c, 0.0), vec3(Extra.x));
    vec2 d = texCoord - 0.5;
    c *= 1.0 - Extra.y * smoothstep(0.25, 0.75, length(d) * 1.2);
    fragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
