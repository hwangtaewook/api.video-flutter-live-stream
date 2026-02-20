package video.api.flutter.livestream

import io.github.thibaultbee.streampack.core.elements.sources.video.camera.CameraSettings
import io.github.thibaultbee.streampack.core.interfaces.IWithVideoSource
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.ICameraSource
import video.api.flutter.livestream.generated.CameraSettingsHostApi
import video.api.flutter.livestream.manager.InstanceManager
import kotlinx.coroutines.runBlocking

class CameraSettingsHostApiImpl(
    private val instanceManager: InstanceManager
) :
    CameraSettingsHostApi {
    private val settings: CameraSettings
        get() {
            val streamer = instanceManager.getInstance()
            val videoSource = (streamer as? IWithVideoSource)?.videoInput?.sourceFlow?.value
            return (videoSource as? ICameraSource)?.settings ?: throw IllegalStateException("Not a camera source")
        }

    override fun setZoomRatio(zoomRatio: Double) {
        runBlocking {
            settings.zoom.setZoomRatio(zoomRatio.toFloat())
        }
    }

    override fun getZoomRatio(): Double {
        return runBlocking { settings.zoom.getZoomRatio() }.toDouble()
    }
}
