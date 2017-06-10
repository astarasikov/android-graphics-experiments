package astarasikov.github.io.camandnettest;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.Surface;
import android.view.View;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import astarasikov.github.io.camandnettest.shaders.CameraShader;
import astarasikov.github.io.camandnettest.shaders.IdentityShader;

class MyCameraView extends GLSurfaceView {
    private final static String LOG_TAG = "MyCameraView";
    
	static {
        Log.i(LOG_TAG, "loading jni_proc");
        System.loadLibrary("jni_proc");
    }
    
	public native void ProcessFrame(Object surface);

    class MyGlRenderer implements Renderer, SurfaceTexture.OnFrameAvailableListener {
        /**
         * Some ugly FPS counter
         */
        long mBaseTimeStamp = 0;
        long mLastTimeStamp = 0;
        int frameCount = 0;

        void measureFPS() {
            if (0 == mBaseTimeStamp) {
                mBaseTimeStamp = System.currentTimeMillis();
            }
            mLastTimeStamp = System.currentTimeMillis();
            long dt = mLastTimeStamp - mBaseTimeStamp;
            ++frameCount;

            if (dt != 0) {
                double fps = (double)frameCount / (dt / 1e3);
                Log.e("FPSFPS", new Double(fps).toString());
            }
        }

		/**
		 * You can choose the shader by uncommenting one of the lines below
		 * which initialize the mCameraShader variable
		 */

        private CameraShader mCameraShader = new IdentityShader();
        //private CameraShader mCameraShader = new ShadesOfGrayShader();

        int compileShader(String txt, int type) {
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, txt);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (0 == compiled[0]) {
                Log.e(LOG_TAG, "Failed to compile shader: " + GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = -1;
            }
            return shader;
        }

        private Camera mCamera;
        private SurfaceTexture mSurfaceTextureCameraPreview;
        private volatile boolean mSurfaceTextureCameraPreviewDirty = true;

        private int[] mTextureHandles;
        private int mProgram;
        private FloatBuffer mVertexBuffer;
        private FloatBuffer mTexCoordBuffer;

        private SurfaceTexture mSurfaceTextureCPUMapped;
        private Surface mSurfaceCPUMapped;
        private int[] mFrameBuffer = new int[1];

        private int mWidth = 1920;
        private int mHeight = 1080;

        MyGlRenderer(View view) {
            mVertexBuffer = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            mTexCoordBuffer = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();

            mVertexBuffer.put(new float[]{
                    1, 1,
                    1, -1,
                    -1, 1,
                    -1, -1,
            });
            mVertexBuffer.position(0);

            mTexCoordBuffer.put(new float[]{
				1, 1,
				0, 1,
				1, 0,
				0, 0
			});
            mTexCoordBuffer.position(0);
        }

        private void cleanup() {
            mSurfaceTextureCameraPreview.release();
            if (mCamera != null) {
                mCamera.release();
                mCamera = null;
            }
            GLES20.glDeleteProgram(mProgram);
        }

        void initializeTextures() {
            mTextureHandles = new int[2];
            GLES20.glGenTextures(2, mTextureHandles, 0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureHandles[0]);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

            mSurfaceTextureCameraPreview = new SurfaceTexture(mTextureHandles[0]);
            mSurfaceTextureCameraPreview.setOnFrameAvailableListener(this);

            int cpuMappedTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
            GLES20.glBindTexture(cpuMappedTextureTarget, mTextureHandles[1]);
            GLES20.glTexParameteri(cpuMappedTextureTarget, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(cpuMappedTextureTarget, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(cpuMappedTextureTarget, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(cpuMappedTextureTarget, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
            Log.e(LOG_TAG, "before allocating SurfaceTexture");

            mSurfaceTextureCPUMapped = new SurfaceTexture(mTextureHandles[1], false);
            mSurfaceTextureCPUMapped.setDefaultBufferSize(mHeight, mWidth);
            /**
             * keeping a reference to this Surface is probably UB
             * on MSM899X it allows us to read back the FB memory
             * on MSM8939 we never see GPU-side updates
             * or MSM8939 driver is broken but I don't have other phones to test
             */

            mSurfaceCPUMapped = new Surface(mSurfaceTextureCPUMapped);
            ProcessFrame(mSurfaceCPUMapped);
            mSurfaceTextureCPUMapped.updateTexImage();

            GLES20.glGenFramebuffers(1, mFrameBuffer, 0);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffer[0]);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,
                    GLES20.GL_COLOR_ATTACHMENT0,
                    cpuMappedTextureTarget,
                    mTextureHandles[1], 0);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        }

        void initializeCamera()
        {
            mCamera = Camera.open();

            /**
             * You can try different resolutions, but not all of them
             * can be supported by all devices.
             * comment out the "setParameters" call to use the default one.
             */

            Camera.Parameters params = mCamera.getParameters();
            //params.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            params.setPreviewSize(mWidth, mHeight);
            mCamera.setParameters(params);

            try {
                mCamera.setPreviewTexture(mSurfaceTextureCameraPreview);
            } catch (IOException e) {
                Log.e(LOG_TAG, e.getMessage());
            }
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

            initializeTextures();
            initializeCamera();
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            Log.e(LOG_TAG, "Frame");

            GLES20.glViewport(0, 0, width, height);
            if (null == mCamera) {
                return;
            }
            Camera.Parameters params = mCamera.getParameters();
            mCamera.setParameters(params);
            mCamera.startPreview();
        }

        void drawSurfaceTexture(int srcTexture)
        {
            //GLES20.glClearColor(0, 0, 0, 0);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glUseProgram(mProgram);

            int a_pos = GLES20.glGetAttribLocation(mProgram, "vPosition");
            int a_tc = GLES20.glGetAttribLocation(mProgram, "vTexcoord");
            int u_ts = GLES20.glGetUniformLocation(mProgram, "sTexture");
            int a_ts = GLES20.glGetUniformLocation(mProgram, "texsize");

            mCameraShader.uploadShaderSpecificData(mProgram);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, srcTexture);
            GLES20.glUniform1i(u_ts, 0);

            GLES20.glUniform2f(a_ts, mHeight, mWidth);

            GLES20.glVertexAttribPointer(a_pos, 2, GLES20.GL_FLOAT, false, 4 * 2, mVertexBuffer);
            GLES20.glVertexAttribPointer(a_tc, 2, GLES20.GL_FLOAT, false, 4 * 2, mTexCoordBuffer);
            GLES20.glEnableVertexAttribArray(a_pos);
            GLES20.glEnableVertexAttribArray(a_tc);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        }

        ByteBuffer mOnePixelBuffer = ByteBuffer.allocate(4);

        @Override
        public synchronized void onDrawFrame(GL10 gl) {
            measureFPS();

            {
                if (mSurfaceTextureCameraPreviewDirty) {
                    mSurfaceTextureCameraPreview.updateTexImage();
                    mSurfaceTextureCameraPreviewDirty = false;
                }
            }

            /**
             * First pass: render camera SurfaceTexture into the FrameBuffer bound to our Surface Texture
             *
             */
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffer[0]);
            if (mCamera != null) {
                drawSurfaceTexture(mTextureHandles[0]);
            }
            else {
                /**
                 * test mode for when the call to {@ref initializeCamera} is commented out
                 */
                GLES20.glClearColor(1, 0, 0, 1);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            }

            /**
             * such fence, much barrier, very cache flush.
             * On MSM899X glFinish seems to have no effect, but glReadPixels does wait for GPU
             * to finish rendering and seems to resolve the cache issues.
             * According to profiling results, the overhead of calling glReadPixels to read
             * a single pixel is fixed and does not depend on the surface resolution.
             * It is comparable to eglSwapBuffers
             */
            GLES20.glReadPixels(0, 0, 1, 1, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mOnePixelBuffer);

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

            /**
             * Map GPU texture to CPU address space and process it on the CPU side
             */
            mSurfaceTextureCPUMapped.releaseTexImage();
            ProcessFrame(mSurfaceCPUMapped);
            mSurfaceTextureCPUMapped.updateTexImage();

            /**
             * Second pass: draw our SurfaceTexture (mSurfaceTextureCPUMapped) to the screen
             */
            drawSurfaceTexture(mTextureHandles[1]);

            /**
             * If we're in the test mode without camera input, wait a while
             * to give the user the time to inspect the image for artifacts
             */

            if (mCamera != null) {
                return;
            }

            try {
                Thread.sleep(500);
            }
            catch (InterruptedException e) {
                Log.e("E", e.toString());
            }
        }

        @Override
        public synchronized void onFrameAvailable(SurfaceTexture surfaceTexture) {
            mSurfaceTextureCameraPreviewDirty = true;
        }
    }

    MyGlRenderer mRenderer;

    public MyCameraView(Context context) {
        super(context);
        mRenderer = new MyGlRenderer(this);
        setEGLContextClientVersion(2);
        setRenderer(mRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }
}
