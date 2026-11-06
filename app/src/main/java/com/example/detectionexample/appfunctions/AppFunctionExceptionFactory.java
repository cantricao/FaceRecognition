package com.example.detectionexample.appfunctions;

import android.os.Bundle;
import androidx.appfunctions.AppFunctionElementNotFoundException;
import androidx.appfunctions.AppFunctionInvalidArgumentException;

/**
 * Factory creating AppFunction exceptions with a valid non-null Bundle
 * to ensure compatibility across both Android runtime and host JVM unit tests.
 */
public final class AppFunctionExceptionFactory {
    private AppFunctionExceptionFactory() {}

    public static AppFunctionInvalidArgumentException createInvalidArgument(String message) {
        return new AppFunctionInvalidArgumentException(message, new Bundle());
    }

    public static AppFunctionElementNotFoundException createElementNotFound(String message) {
        return new AppFunctionElementNotFoundException(message, new Bundle());
    }
}
