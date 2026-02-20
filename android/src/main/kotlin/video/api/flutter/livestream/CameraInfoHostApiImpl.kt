package video.api.flutter.livestream

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import android.util.Range
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.cameraManager
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.isBackCamera
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.isExternalCamera
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.isFrontCamera
import video.api.flutter.livestream.generated.CameraInfoHostApi
import video.api.flutter.livestream.generated.NativeCameraLensDirection

class CameraInfoHostApiImpl(
    var context: Context
) : CameraInfoHostApi {
    override fun getSensorRotationDegrees(cameraId: String): Long {
        val manager = context.cameraManager
        val characteristics = manager.getCameraCharacteristics(cameraId)
        return (characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0).toLong()
    }

    override fun getLensDirection(cameraId: String): NativeCameraLensDirection {
        val manager = context.cameraManager
        return when {
            manager.isFrontCamera(cameraId) -> NativeCameraLensDirection.FRONT
            manager.isBackCamera(cameraId) -> NativeCameraLensDirection.BACK
            manager.isExternalCamera(cameraId) -> NativeCameraLensDirection.OTHER
            else -> throw IllegalArgumentException("Invalid camera position for camera $cameraId")
        }
    }

    private fun getZoomRange(cameraId: String): Range<Float> {
        val manager = context.cameraManager
        val characteristics = manager.getCameraCharacteristics(cameraId)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            characteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE) ?: Range(1.0f, 1.0f)
        } else {
            val maxZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f
            Range(1.0f, maxZoom)
        }
    }

    override fun getMinZoomRatio(cameraId: String) = getZoomRange(cameraId).lower.toDouble()

    override fun getMaxZoomRatio(cameraId: String) =
        getZoomRange(cameraId).upper.toDouble()
}
