package astarasikov.github.io.camandnettest.shaders;

/**
* Created by alexander on 02/03/15.
*/
public interface CameraShader {
    public String vertex();
    public String fragment();
    public void uploadShaderSpecificData(int program);
}
