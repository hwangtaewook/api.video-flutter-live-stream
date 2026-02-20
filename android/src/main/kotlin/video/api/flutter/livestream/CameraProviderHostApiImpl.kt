package video.api.flutter.livestream

import android.content.Context
import android.hardware.camera2.CameraManager
import video.api.flutter.livestream.generated.CameraProviderHostApi

class CameraProviderHostApiImpl(
    var context: Context
) : CameraProviderHostApi {
    override fun getAvailableCameraIds(): List<String> {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        return manager.cameraIdList.toList()
    }
}
