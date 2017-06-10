package astarasikov.github.io.camandnettest.shaders;

import android.opengl.GLES20;

import astarasikov.github.io.camandnettest.shaders.CameraShader;

/**
* Created by alexander on 02/03/15.
*/
public class ConvolutionShader implements CameraShader {
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
                        "uniform vec2 texsize;\n" +
                        "uniform float kernel[9];\n" +
                        "void main() {\n" +
                        "float dx = 1.0 / texsize.x;\n" +
                        "float dy = 1.0 / texsize.y;\n" +
                        "vec4 tmp_FragColor = vec4(0.0);\n" +
                        "tmp_FragColor += kernel[0] * texture2D(sTexture, texcoord + vec2(-dx, -dy));\n" +
                        "tmp_FragColor += kernel[1] * texture2D(sTexture, texcoord + vec2(0.0, -dy));\n" +
                        "tmp_FragColor += kernel[2] * texture2D(sTexture, texcoord + vec2(dx, -dy));\n" +
                        "tmp_FragColor += kernel[3] * texture2D(sTexture, texcoord + vec2(-dx, 0.0));\n" +
                        "tmp_FragColor += kernel[4] * texture2D(sTexture, texcoord);\n" +
                        "tmp_FragColor += kernel[5] * texture2D(sTexture, texcoord + vec2(dx, 0.0));\n" +
                        "tmp_FragColor += kernel[6] * texture2D(sTexture, texcoord + vec2(-dx, dy));\n" +
                        "tmp_FragColor += kernel[7] * texture2D(sTexture, texcoord + vec2(0.0, dy));\n" +
                        "tmp_FragColor += kernel[8] * texture2D(sTexture, texcoord + vec2(dx, dy));\n" +
                        "float ksum = 0.0;\n" +
                        "for (int i = 0; i < 9; i++) { ksum += kernel[i]; }\n" +
                        "if (ksum <= 0.0) { ksum = 1.0; }\n" +
                        "gl_FragColor = vec4((tmp_FragColor / ksum).rgb, 0.0);\n" +
                        "}";
    }

    private final float kernelEdgeDetection[] = {
            -2, -1, 0,
            -1, 0, 1,
            0, 1, 2
    };

    public void uploadShaderSpecificData(int mProgram) {
        int u_kernel = GLES20.glGetUniformLocation(mProgram, "kernel");
        GLES20.glUniform1fv(u_kernel, kernelEdgeDetection.length, kernelEdgeDetection, 0);
    }
}
