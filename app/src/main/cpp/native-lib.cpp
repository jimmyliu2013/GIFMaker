#include <jni.h>
#include <string>
#include <dirent.h>
#include <android/log.h>
#include <android/bitmap.h>


#define LOG_TAG "GIFMaker_NDK"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define ASSERT(cond, fmt, ...)                                \
  if (!(cond)) {                                              \
    __android_log_assert(#cond, LOG_TAG, fmt, ##__VA_ARGS__); \
  }

static unsigned char *new_image_buffer;
std::string *folder_name;

static unsigned char *gif_writer_buffer;
const std::string *gif_name;

int saveBufferToFile(unsigned char *buffer, std::string fileName, int size) {
    FILE *file = fopen(fileName.c_str(), "wb");
    int result = 1;

    if (file && buffer && size) {
        if (fwrite(buffer, 1, size, file) == size) {
            result = 0;
        }
        fclose(file);
    } else {
        if (file)
            fclose(file);
    }
    return result;
}


void cropImageBuffer(unsigned char *oldBuffer,
                     unsigned char *newBuffer,
                     int bytesPerPixel,
                     int startPointX,
                     int startPointY,
                     int newWidth,
                     int newHeight,
                     int oldPixelsPerRow) {
    for (int b = 0; b < newHeight; b++) {
        for (int a = 0; a < newWidth; a++) {
            //LOGE("processing point b:%d, a:%d, index:%d", b, a, (a + b * newWidth) * 4);
            for (int i = 0; i < bytesPerPixel; i++) {
                newBuffer[(a + b * newWidth) * bytesPerPixel + i] =
                        oldBuffer[(startPointX + a) * bytesPerPixel +
                                  (startPointY + b) * bytesPerPixel *
                                  oldPixelsPerRow + i];
            }
        }
    }
}


extern "C"
JNIEXPORT jint JNICALL
Java_com_lim_gifmaker_GIFController_processBuffer(JNIEnv *env,
                                                  jobject thiz,
                                                  jobject bb,
                                                  jint index,
                                                  jint x_frame,
                                                  jint y_frame,
                                                  jint width_frame,
                                                  jint height_frame,
                                                  jint old_width) {
    int new_size = width_frame * height_frame * 4;
    unsigned char *buffer = (unsigned char *) env->GetDirectBufferAddress(bb);

    cropImageBuffer(buffer,
                    new_image_buffer,
                    4,
                    x_frame,
                    y_frame,
                    width_frame,
                    height_frame,
                    old_width);

    std::string fileName =
            *folder_name + std::string("/") + std::to_string(index) + ".data";
    return saveBufferToFile(new_image_buffer, fileName, new_size);
}


extern "C"
JNIEXPORT void JNICALL
Java_com_lim_gifmaker_GIFController_allocateNativeBuffer(JNIEnv *env, jobject thiz, jint size) {
    new_image_buffer = new unsigned char[size];
}

extern "C"
JNIEXPORT void JNICALL
Java_com_lim_gifmaker_GIFController_setFolderName(JNIEnv *env, jobject thiz, jstring folder) {
    const char *str = env->GetStringUTFChars(folder, nullptr);
    folder_name = new std::string(str);
    env->ReleaseStringUTFChars(folder, str);
}


extern "C"
JNIEXPORT void JNICALL
Java_com_lim_gifmaker_GIFController_clearNative(JNIEnv *env, jobject thiz) {
    delete new_image_buffer;
    delete folder_name;
    new_image_buffer = nullptr;
    folder_name = nullptr;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_lim_gifmaker_GIFController_initGIFWriter(JNIEnv *env, jobject thiz, jint size) {

}


extern "C"
JNIEXPORT void JNICALL
Java_com_lim_gifmaker_GIFController_setGIFName(JNIEnv *env, jobject thiz, jstring name) {

}

void readBufferFromFile(std::string fileName, int size) {
    FILE *file = fopen(fileName.c_str(), "rb");

    if (file && gif_writer_buffer && size) {
        fread(gif_writer_buffer, size, 1, file);
        fclose(file);
    } else {
        if (file)
            fclose(file);
    }
}
