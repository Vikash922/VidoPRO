#include <jni.h>
#include <android/bitmap.h>
#include <android/log.h>
#include <vector>
#include <cstdint>
#include "AdjustmentProcessor.h"
#include "BlendEngine.h"
#include "AudioBeatDetector.h"
#include "KeyframeEngine.h"

#define TAG "VidoProNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_example_core_media_nativeengine_NativeVideoEngine_nativeApplyAdjustments(
    JNIEnv* env,
    jobject /* thiz */,
    jobject bitmap,
    jfloatArray paramsArray
) {
    if (!bitmap || !paramsArray) return JNI_FALSE;

    jsize paramLen = env->GetArrayLength(paramsArray);
    if (paramLen < 20) return JNI_FALSE;

    jfloat* p = env->GetFloatArrayElements(paramsArray, nullptr);
    if (!p) return JNI_FALSE;

    vidopro::AdjustmentParams params;
    params.brightness = p[0];
    params.contrast = p[1];
    params.exposure = p[2];
    params.highlights = p[3];
    params.shadows = p[4];
    params.whites = p[5];
    params.blacks = p[6];
    params.saturation = p[7];
    params.vibrance = p[8];
    params.temperature = p[9];
    params.tint = p[10];
    params.hue = p[11];
    params.redBalance = p[12];
    params.greenBalance = p[13];
    params.blueBalance = p[14];
    params.gamma = p[15];
    params.midtones = p[16];
    params.vignette = p[17];
    params.fade = p[18];
    params.opacity = p[19];

    env->ReleaseFloatArrayElements(paramsArray, p, JNI_ABORT);

    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) return JNI_FALSE;
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) return JNI_FALSE;

    void* pixelPtr = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixelPtr) < 0) return JNI_FALSE;

    vidopro::AdjustmentProcessor::processPixels(
        reinterpret_cast<uint32_t*>(pixelPtr),
        info.width,
        info.height,
        info.stride / 4,
        params
    );

    AndroidBitmap_unlockPixels(env, bitmap);
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_example_core_media_nativeengine_NativeVideoEngine_nativeApplyBlur(
    JNIEnv* env,
    jobject /* thiz */,
    jobject bitmap,
    jint radius
) {
    if (!bitmap || radius <= 0) return JNI_FALSE;

    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) return JNI_FALSE;
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) return JNI_FALSE;

    void* pixelPtr = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixelPtr) < 0) return JNI_FALSE;

    vidopro::AdjustmentProcessor::applyFastBlur(
        reinterpret_cast<uint32_t*>(pixelPtr),
        info.width,
        info.height,
        radius
    );

    AndroidBitmap_unlockPixels(env, bitmap);
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_example_core_media_nativeengine_NativeVideoEngine_nativeApplySharpen(
    JNIEnv* env,
    jobject /* thiz */,
    jobject bitmap,
    jfloat amount
) {
    if (!bitmap || amount <= 0.01f) return JNI_FALSE;

    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) return JNI_FALSE;
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) return JNI_FALSE;

    void* pixelPtr = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixelPtr) < 0) return JNI_FALSE;

    vidopro::AdjustmentProcessor::applySharpen(
        reinterpret_cast<uint32_t*>(pixelPtr),
        info.width,
        info.height,
        amount
    );

    AndroidBitmap_unlockPixels(env, bitmap);
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_example_core_media_nativeengine_NativeVideoEngine_nativeApplyGrain(
    JNIEnv* env,
    jobject /* thiz */,
    jobject bitmap,
    jfloat amount,
    jlong seed
) {
    if (!bitmap || amount <= 0.01f) return JNI_FALSE;

    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) return JNI_FALSE;
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) return JNI_FALSE;

    void* pixelPtr = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixelPtr) < 0) return JNI_FALSE;

    vidopro::AdjustmentProcessor::applyGrain(
        reinterpret_cast<uint32_t*>(pixelPtr),
        info.width,
        info.height,
        amount,
        static_cast<uint64_t>(seed)
    );

    AndroidBitmap_unlockPixels(env, bitmap);
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_example_core_media_nativeengine_NativeVideoEngine_nativeApplyLut(
    JNIEnv* env,
    jobject /* thiz */,
    jobject bitmap,
    jbyteArray lutDataArray,
    jint lutSize,
    jfloat intensity
) {
    if (!bitmap || !lutDataArray || lutSize < 2 || intensity <= 0.01f) return JNI_FALSE;

    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) return JNI_FALSE;
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) return JNI_FALSE;

    jbyte* bytes = env->GetByteArrayElements(lutDataArray, nullptr);
    if (!bytes) return JNI_FALSE;

    void* pixelPtr = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixelPtr) < 0) {
        env->ReleaseByteArrayElements(lutDataArray, bytes, JNI_ABORT);
        return JNI_FALSE;
    }

    vidopro::AdjustmentProcessor::applyLut3D(
        reinterpret_cast<uint32_t*>(pixelPtr),
        info.width,
        info.height,
        reinterpret_cast<const uint8_t*>(bytes),
        lutSize,
        intensity
    );

    AndroidBitmap_unlockPixels(env, bitmap);
    env->ReleaseByteArrayElements(lutDataArray, bytes, JNI_ABORT);
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_example_core_media_nativeengine_NativeVideoEngine_nativeBlendBitmaps(
    JNIEnv* env,
    jobject /* thiz */,
    jobject baseBitmap,
    jobject overlayBitmap,
    jint mode,
    jfloat opacity
) {
    if (!baseBitmap || !overlayBitmap || opacity <= 0.001f) return JNI_FALSE;

    AndroidBitmapInfo baseInfo, overlayInfo;
    if (AndroidBitmap_getInfo(env, baseBitmap, &baseInfo) < 0) return JNI_FALSE;
    if (AndroidBitmap_getInfo(env, overlayBitmap, &overlayInfo) < 0) return JNI_FALSE;

    if (baseInfo.width != overlayInfo.width || baseInfo.height != overlayInfo.height) {
        return JNI_FALSE;
    }

    void *basePtr = nullptr, *overlayPtr = nullptr;
    if (AndroidBitmap_lockPixels(env, baseBitmap, &basePtr) < 0) return JNI_FALSE;
    if (AndroidBitmap_lockPixels(env, overlayBitmap, &overlayPtr) < 0) {
        AndroidBitmap_unlockPixels(env, baseBitmap);
        return JNI_FALSE;
    }

    vidopro::BlendEngine::blendSurfaces(
        reinterpret_cast<uint32_t*>(basePtr),
        reinterpret_cast<const uint32_t*>(overlayPtr),
        baseInfo.width,
        baseInfo.height,
        static_cast<vidopro::BlendMode>(mode),
        opacity
    );

    AndroidBitmap_unlockPixels(env, overlayBitmap);
    AndroidBitmap_unlockPixels(env, baseBitmap);
    return JNI_TRUE;
}

JNIEXPORT jlongArray JNICALL
Java_com_example_core_media_nativeengine_NativeVideoEngine_nativeDetectBeats(
    JNIEnv* env,
    jobject /* thiz */,
    jshortArray pcmArray,
    jint totalSamples,
    jint sampleRate,
    jint channels,
    jfloat sensitivity
) {
    if (!pcmArray || totalSamples <= 0 || sampleRate <= 0 || channels <= 0) {
        return env->NewLongArray(0);
    }

    jshort* samples = env->GetShortArrayElements(pcmArray, nullptr);
    if (!samples) return env->NewLongArray(0);

    std::vector<int64_t> beats = vidopro::AudioBeatDetector::detectBeats(
        samples,
        totalSamples,
        sampleRate,
        channels,
        sensitivity
    );

    env->ReleaseShortArrayElements(pcmArray, samples, JNI_ABORT);

    jsize len = static_cast<jsize>(beats.size());
    jlongArray result = env->NewLongArray(len);
    if (len > 0) {
        env->SetLongArrayRegion(result, 0, len, reinterpret_cast<const jlong*>(beats.data()));
    }
    return result;
}

JNIEXPORT jfloat JNICALL
Java_com_example_core_media_nativeengine_NativeVideoEngine_nativeEvaluateBezier(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jfloat x1,
    jfloat y1,
    jfloat x2,
    jfloat y2,
    jfloat time
) {
    return vidopro::KeyframeEngine::evaluateCubicBezier(x1, y1, x2, y2, time);
}

JNIEXPORT jfloat JNICALL
Java_com_example_core_media_nativeengine_NativeVideoEngine_nativeInterpolate(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jfloat t,
    jfloat startVal,
    jfloat endVal,
    jint type,
    jfloat x1,
    jfloat y1,
    jfloat x2,
    jfloat y2
) {
    return vidopro::KeyframeEngine::interpolate(t, startVal, endVal, type, x1, y1, x2, y2);
}

} // extern "C"
