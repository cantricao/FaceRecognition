package com.example.detectionexample.genai

import com.google.mlkit.genai.schema.annotations.Generable
import com.google.mlkit.genai.schema.annotations.Guide

/**
 * Structured output model for on-device facial attribute inspection powered by Gemini Nano.
 */
@Generable(description = "Structured analysis of facial attributes and expressions")
data class FaceAttributeAnalysis(
    @param:Guide(description = "Primary facial expression, such as Smiling, Neutral, Frowning, or Surprised")
    var expression: String = "",

    @param:Guide(description = "Estimated age category, such as Child, Young Adult, Adult, or Senior")
    var ageGroup: String = "",

    @param:Guide(description = "Visible facial accessories, such as Glasses, Sunglasses, Hat, Mask, or None")
    var accessories: String = "",

    @param:Guide(description = "Lighting and image clarity evaluation, such as Clear, Dim, Overexposed, or Blurry")
    var imageQuality: String = "",

    @param:Guide(description = "Brief 1-sentence overall natural language summary of the face")
    var summary: String = ""
)
