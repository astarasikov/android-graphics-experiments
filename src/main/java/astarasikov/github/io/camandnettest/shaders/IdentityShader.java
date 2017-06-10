package astarasikov.github.io.camandnettest.shaders;

import android.opengl.GLES20;

/**
* Created by alexander on 02/03/15.
*/
public class IdentityShader implements CameraShader {
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
                        "void main() {\n" +
                        "gl_FragColor = texture2D(sTexture, texcoord);\n" +
                        "}";
    }

    public void uploadShaderSpecificData(int mProgram) {
    }
}
