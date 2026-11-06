package com.example.detectionexample.cameraeffect;

import android.opengl.EGLSurface;
import androidx.annotation.NonNull;

final class AutoValue_OpenGlRenderer_OutputSurface extends OpenGlRenderer.OutputSurface {
    private final EGLSurface eglSurface;
    private final int width;
    private final int height;

    AutoValue_OpenGlRenderer_OutputSurface(EGLSurface eglSurface, int width, int height) {
        if (eglSurface == null) {
            throw new NullPointerException("Null eglSurface");
        } else {
            this.eglSurface = eglSurface;
            this.width = width;
            this.height = height;
        }
    }

    @NonNull
    EGLSurface getEglSurface() {
        return this.eglSurface;
    }

    int getWidth() {
        return this.width;
    }

    int getHeight() {
        return this.height;
    }

    @NonNull
    public String toString() {
        return "OutputSurface{eglSurface=" + this.eglSurface + ", width=" + this.width + ", height=" + this.height + "}";
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof OpenGlRenderer.OutputSurface that)) {
            return false;
        } else {
            return this.eglSurface.equals(that.getEglSurface()) && this.width == that.getWidth() && this.height == that.getHeight();
        }
    }

    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= this.eglSurface.hashCode();
        h$ *= 1000003;
        h$ ^= this.width;
        h$ *= 1000003;
        h$ ^= this.height;
        return h$;
    }
}

