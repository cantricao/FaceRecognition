package com.example.detectionexample.config

import org.tensorflow.lite.support.model.Model

object ModelConfig {
    const val BLAZEFACE_LABEL_NAME = "face.txt"
    const val BLAZEFACE_MODEL_NAME = "face_detection_front.tflite"
    const val CASCADE_DIRNAME = "cascade"
    const val CASCADE_FILENAME = "lbpcascade_frontalface_improved.xml"
    const val OPENCV_CODENAME = "CascadeClassifier"
    const val MLKIT_CODENAME = "face_mlkit"

    const val COCO_LABEL_PATH = "coco.txt"
    const val MOBILE_FACENET_MODEL_NAME = "mobile_face_net.tflite"

    val EXTRACTOR_DEFAULT_DEVICE = Model.Device.NNAPI
    const val EXTRACTOR_DEFAULT_MODEL = MOBILE_FACENET_MODEL_NAME

    val DETECTOR_DEFAULT_DEVICE = Model.Device.NNAPI
    const val DETECTOR_DEFAULT_MODEL = BLAZEFACE_MODEL_NAME


}