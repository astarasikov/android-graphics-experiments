package astarasikov.github.io.camandnettest.shaders;

public class ShadesOfGrayShader implements CameraShader {
    public String vertex() {
        return
            "attribute vec2 vPosition;\n" +
                    "attribute vec2 vTexcoord;\n" +
                    "varying vec2 texcoord;\n" +
                    "void main() {\n" +
                    "texcoord = vTexcoord;\n" +
                        "gl_Position = vec4(vPosition.x, vPosition.y, 0.0, 1.0);\n" +
                    "}";
    }

    public String fragment() {
        return
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "varying vec2 texcoord;\n" +
                    "void main() {\n" +
                        "float gray = dot(vec3(1.0 / 3.0), texture2D(sTexture, texcoord).rgb);\n"+
                        "float radius = max(texcoord.s, texcoord.t);\n" +
                        "float coeff = floor(radius * 50.0);\n" +
                        "float result = gray * pow(0.947, coeff);\n" +
                        "gl_FragColor = vec4(vec3(result), 0.0);\n" +
                    "}";
    }

    public void uploadShaderSpecificData(int mProgram) {
    }
}
