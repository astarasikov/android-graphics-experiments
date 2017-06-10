#include <jni.h>
#include <stdlib.h>

#include <android/native_window_jni.h>
#include <android/log.h>

#define  LOG_TAG    "android-pure-ndk"
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

static void process_buffer(ANativeWindow_Buffer *buffer) {
	unsigned pixel_size = 0;

    if (buffer->format == WINDOW_FORMAT_RGB_565) {
        pixel_size = 2;
    } else if (buffer->format == WINDOW_FORMAT_RGBA_8888 || buffer->format == WINDOW_FORMAT_RGBX_8888) {
        pixel_size = 4;
    } else {
        LOGE("Unsupported buffer format: %d", buffer->format);
        return;
    }

	//LOGE("buffer width=%d height=%d stride=%d\n", buffer->width, buffer->height, buffer->stride);

	const size_t height = buffer->height;
	const size_t stride = buffer->stride;
	size_t line_length = buffer->width * pixel_size;
	unsigned char *ptr = (unsigned char*)buffer->bits;

	for (size_t j = 0; j < height; j++) {
		for (size_t i = 0; i < line_length; i++) {
			ptr[i] = ~ptr[i];
		}
		ptr += stride * pixel_size;
	}

    /* clear the screen */
    //memset(buffer->bits, 0xff, buffer->stride*pixel_size*buffer->height / 8);
}

extern "C" {

JNIEXPORT void JNICALL Java_astarasikov_github_io_camandnettest_MyCameraView_ProcessFrame(JNIEnv *env, jobject deez, jobject surface);

JNIEXPORT void JNICALL Java_astarasikov_github_io_camandnettest_MyCameraView_ProcessFrame(JNIEnv *env, jobject deez, jobject surface)
{
	ANativeWindow *window = ANativeWindow_fromSurface(env, surface);;
	ANativeWindow_Buffer buffer;
	int rc = -1;
	rc = ANativeWindow_lock(window, &buffer, NULL);
	if (rc < 0) {
		LOGE("Failed to lock window buffer error=%d window=%p", rc, window);
		return;
	}
	//LOGE("Locked buffer successfully!");
	process_buffer(&buffer);
	ANativeWindow_unlockAndPost(window);
	ANativeWindow_release(window);
}

} //extern "C"

