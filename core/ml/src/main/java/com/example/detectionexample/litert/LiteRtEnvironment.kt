package com.example.detectionexample.litert

import com.google.ai.edge.litert.Environment

object LiteRtEnvironment {
    val environment: Environment by lazy { Environment.create() }
}
