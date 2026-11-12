#version 330 core
out vec4 FragColor;

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoords;
in vec4 FragPosLightSpace;

uniform vec3 lightPos;
uniform vec3 viewPos;
uniform vec3 lightColor;
uniform vec3 objectColor;
uniform sampler2D textureSampler;   // texture unit 0
uniform sampler2D shadowMap;        // texture unit 1
uniform bool useTexture;
uniform bool useBlinnPhong;
uniform bool useShadow;

// Shadow Mapping: bandingkan depth fragment (sudut pandang cahaya) dengan Shadow Map.
// Return 0.0 (terang) .. 1.0 (bayangan penuh), memakai PCF 3x3 agar tepi halus.
float ShadowCalculation(vec4 fragPosLightSpace, vec3 norm, vec3 lightDir) {
    vec3 proj = fragPosLightSpace.xyz / fragPosLightSpace.w; // perspective divide
    proj = proj * 0.5 + 0.5;                                  // NDC [-1,1] -> [0,1]
    if (proj.z > 1.0) return 0.0;                             // di luar far plane cahaya

    // Dynamic slope-scaled bias (anti Shadow Acne). Konstanta disesuaikan dengan
    // rentang depth ortografik cahaya (near=1, far=16) agar tidak Peter Panning.
    float bias = max(0.005 * (1.0 - dot(norm, lightDir)), 0.0005);

    float shadow = 0.0;
    vec2 texelSize = 1.0 / vec2(textureSize(shadowMap, 0));
    for (int x = -1; x <= 1; ++x) {
        for (int y = -1; y <= 1; ++y) {
            float closestDepth = texture(shadowMap, proj.xy + vec2(x, y) * texelSize).r;
            shadow += (proj.z - bias > closestDepth) ? 1.0 : 0.0;
        }
    }
    return shadow / 9.0;
}

void main() {
    // 1. Ambient Lighting
    float ambientStrength = 0.2;
    vec3 ambient = ambientStrength * lightColor;

    // 2. Diffuse Lighting (Lambertian)
    vec3 norm = normalize(Normal);
    vec3 lightDir = normalize(lightPos - FragPos);
    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = diff * lightColor;

    // 3. Specular Lighting (Phong vs Blinn-Phong)
    float specularStrength = 0.8;
    vec3 viewDir = normalize(viewPos - FragPos);
    float spec = 0.0;
    if (useBlinnPhong) {
        vec3 halfwayDir = normalize(lightDir + viewDir);
        spec = pow(max(dot(norm, halfwayDir), 0.0), 32.0);  // Blinn-Phong
    } else {
        vec3 reflectDir = reflect(-lightDir, norm);
        spec = pow(max(dot(viewDir, reflectDir), 0.0), 16.0); // Standard Phong
    }
    vec3 specular = specularStrength * spec * lightColor;

    // 4. Shadow factor: 1.0 = terkena cahaya, 0.2 = dalam bayangan
    float visibility = 1.0;
    if (useShadow) {
        float shadow = ShadowCalculation(FragPosLightSpace, norm, lightDir);
        visibility = 1.0 - 0.8 * shadow;
    }

    // Combine Texture & Lighting
    vec3 baseColor = useTexture ? texture(textureSampler, TexCoords).rgb : objectColor;
    vec3 result = (ambient + visibility * (diffuse + specular)) * baseColor;
    FragColor = vec4(result, 1.0);
}
