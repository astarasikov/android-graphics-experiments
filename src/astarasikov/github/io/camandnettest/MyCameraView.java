package astarasikov.github.io.camandnettest;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.View;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import astarasikov.github.io.camandnettest.shaders.CameraShader;
import astarasikov.github.io.camandnettest.shaders.ConvolutionShader;
import astarasikov.github.io.camandnettest.shaders.ShadesOfGrayShader;

class MyCameraView extends GLSurfaceView {
    private final static String LOG_TAG = "MyCameraView";

    class MyGlRenderer implements Renderer, SurfaceTexture.OnFrameAvailableListener {
		/**
		 * You can choose the shader by uncommenting one of the lines below
		 * which initialize the mCameraShader variable
		 */

        private CameraShader mCameraShader = new ConvolutionShader();
        //private CameraShader mCameraShader = new ShadesOfGrayShader();

        int compileShader(String txt, int type) {
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, txt);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (0 == compiled[0]) {
                Log.e("GL", "Failed to compile shader: " + GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = -1;
            }
            return shader;
        }

        private Camera mCamera;
        private SurfaceTexture mSurfaceTexture;
        private volatile boolean mSurfaceTextureIsDirty = true;

        private int[] mTextureHandle;
        private int mProgram;
        private FloatBuffer mVertexBuffer;
        private FloatBuffer mTexcoordBuffer;

        MyGlRenderer(View view) {
            mVertexBuffer = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            mTexcoordBuffer = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();

            /*
                Normally one would draw a quad this way,
                but in Android the camera is rotated -90 degrees relative
                to the screen, so rearrange the vertices accordingly
            mVertexBuffer.put(new float[]{
                    1, -1,
                    -1, -1,
                    1, 1,
                    -1, 1
            });*/
            mVertexBuffer.put(new float[]{
                    1, 1,
                    1, -1,
                    -1, 1,
                    -1, -1,
            });
            mVertexBuffer.position(0);

            mTexcoordBuffer.put(new float[]{
				1, 1,
				0, 1,
				1, 0,
				0, 0
			});
            mTexcoordBuffer.position(0);
        }

        private void cleanup() {
            mSurfaceTexture.release();
            mCamera.release();
            mCamera = null;
            GLES20.glDeleteProgram(mProgram);
        }

        private void initTextures() {
            mTextureHandle = new int[1];
            GLES20.glGenTextures(1, mTextureHandle, 0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureHandle[0]);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            Log.e(LOG_TAG, "Surface Created");
            GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            mProgram = GLES20.glCreateProgram();
            GLES20.glAttachShader(mProgram, compileShader(mCameraShader.vertex(), GLES20.GL_VERTEX_SHADER));
            GLES20.glAttachShader(mProgram, compileShader(mCameraShader.fragment(), GLES20.GL_FRAGMENT_SHADER));
            GLES20.glLinkProgram(mProgram);

            initTextures();
            mSurfaceTexture = new SurfaceTexture(mTextureHandle[0]);
            mSurfaceTexture.setOnFrameAvailableListener(this);
            mCamera = Camera.open();

			/**
			 * You can try different resolutions, but not all of them
			 * can be supported by all devices.
			 * comment out the "setParameters" call to use the default one.
			 */

			Camera.Parameters params = mCamera.getParameters();
			//params.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
			params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
			params.setPreviewSize(1920, 1080);
			mCamera.setParameters(params);

            try {
                mCamera.setPreviewTexture(mSurfaceTexture);
            } catch (IOException e) {
                Log.e(LOG_TAG, e.getMessage());
            }
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            Log.e(LOG_TAG, "Frame");

            GLES20.glViewport(0, 0, width, height);
            Camera.Parameters params = mCamera.getParameters();
            mCamera.setParameters(params);
            mCamera.startPreview();
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glUseProgram(mProgram);

            synchronized (this) {
                if (mSurfaceTextureIsDirty) {
                    mSurfaceTexture.updateTexImage();
                    mSurfaceTextureIsDirty = false;
                }
            }

            int a_pos = GLES20.glGetAttribLocation(mProgram, "vPosition");
            int a_tc = GLES20.glGetAttribLocation(mProgram, "vTexcoord");
            int u_ts = GLES20.glGetUniformLocation(mProgram, "sTexture");
            int a_ts = GLES20.glGetUniformLocation(mProgram, "texsize");

            mCameraShader.uploadShaderSpecificData(mProgram);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureHandle[0]);
            GLES20.glUniform1i(u_ts, 0);

            Camera.Size sz = mCamera.getParameters().getPreviewSize();
            //GLES20.glUniform2f(a_ts, sz.width, sz.height);
            GLES20.glUniform2f(a_ts, sz.height, sz.width);

            GLES20.glVertexAttribPointer(a_pos, 2, GLES20.GL_FLOAT, false, 4 * 2, mVertexBuffer);
            GLES20.glVertexAttribPointer(a_tc, 2, GLES20.GL_FLOAT, false, 4 * 2, mTexcoordBuffer);
            GLES20.glEnableVertexAttribArray(a_pos);
            GLES20.glEnableVertexAttribArray(a_tc);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        }

        @Override
        public synchronized void onFrameAvailable(SurfaceTexture surfaceTexture) {
            mSurfaceTextureIsDirty = true;
        }
    }

    MyGlRenderer mRenderer;

    public MyCameraView(Context context) {
        super(context);
        Log.e(LOG_TAG, "MyCameraView");
        mRenderer = new MyGlRenderer(this);
        setEGLContextClientVersion(2);
        setRenderer(mRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }
}
