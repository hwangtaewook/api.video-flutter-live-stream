package video.api.flutter.livestream.manager

import android.Manifest
import android.util.Size
import android.view.Surface
import io.flutter.view.TextureRegistry
import io.github.thibaultbee.streampack.core.elements.encoders.AudioCodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig
import io.github.thibaultbee.streampack.core.streamers.single.SingleStreamer
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import io.github.thibaultbee.streampack.core.interfaces.startPreview
import io.github.thibaultbee.streampack.core.interfaces.stopPreview
import io.github.thibaultbee.streampack.core.interfaces.setCameraId
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.ICameraSource
import io.github.thibaultbee.streampack.core.interfaces.startStream

class LiveStreamViewManager(
    private val streamer: SingleStreamer,
    textureRegistry: TextureRegistry,
    private val permissionsManager: PermissionsManager,
    private val onConnectionSucceeded: () -> Unit,
    private val onDisconnected: () -> Unit,
    private val onConnectionFailed: (String) -> Unit,
    private val onGenericError: (Exception) -> Unit,
    private val onVideoSizeChanged: (Size) -> Unit,
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val flutterTexture = textureRegistry.createSurfaceTexture()
    val textureId: Long
        get() = flutterTexture.id()

    private var _isPreviewing = false
    private var _isStreaming = false
    val isStreaming: Boolean
        get() = _isStreaming

    private var _videoConfig: VideoCodecConfig? = null
    val videoConfig: VideoCodecConfig
        get() = _videoConfig!!

    fun setVideoConfig(
        videoConfig: VideoCodecConfig,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (isStreaming) {
            throw UnsupportedOperationException("You have to stop streaming first")
        }

        onVideoSizeChanged(videoConfig.resolution)

        val wasPreviewing = _isPreviewing
        if (wasPreviewing) {
            stopPreview()
        }
        runBlocking {
            streamer.setVideoConfig(videoConfig)
        }
        _videoConfig = videoConfig
        if (wasPreviewing) {
            startPreview(onSuccess, onError)
        } else {
            onSuccess()
        }
    }

    private var _audioConfig: AudioCodecConfig? = null
    val audioConfig: AudioCodecConfig
        get() = _audioConfig!!

    fun setAudioConfig(
        audioConfig: AudioCodecConfig,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (isStreaming) {
            throw UnsupportedOperationException("You have to stop streaming first")
        }

        permissionsManager.requestPermission(
            Manifest.permission.RECORD_AUDIO,
            onGranted = {
                try {
                    runBlocking {
                        streamer.setAudioConfig(audioConfig)
                    }
                    _audioConfig = audioConfig
                    onSuccess()
                } catch (e: Exception) {
                    onError(e)
                }
            },
            onShowPermissionRationale = { _ ->
                /**
                 * Require an AppCompat theme to use MaterialAlertDialogBuilder
                 *
                context.showDialog(
                R.string.permission_required,
                R.string.record_audio_permission_required_message,
                android.R.string.ok,
                onPositiveButtonClick = { onRequiredPermissionLastTime() }
                ) */
                onError(SecurityException("Missing permission Manifest.permission.RECORD_AUDIO"))
            },
            onDenied = {
                onError(SecurityException("Missing permission Manifest.permission.RECORD_AUDIO"))
            })
    }

    var isMuted: Boolean
        get() = streamer.audioInput?.isMuted ?: false
        set(value) {
            streamer.audioInput?.isMuted = value
        }

    val camera: String
        get() = (streamer.videoInput?.sourceFlow?.value as? ICameraSource)?.cameraId ?: ""

    fun setCamera(camera: String, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        permissionsManager.requestPermission(
            Manifest.permission.CAMERA,
            onGranted = {
                try {
                    runBlocking {
                        streamer.setCameraId(camera)
                    }
                    onSuccess()
                } catch (e: Exception) {
                    onError(e)
                }
            },
            onShowPermissionRationale = { _ ->
                /**
                 * Require an AppCompat theme to use MaterialAlertDialogBuilder
                 *
                 * context.showDialog(
                R.string.permission_required,
                R.string.camera_permission_required_message,
                android.R.string.ok,
                onPositiveButtonClick = { onRequiredPermissionLastTime() }
                )*/
                onError(SecurityException("Missing permission Manifest.permission.CAMERA"))
            },
            onDenied = {
                onError(SecurityException("Missing permission Manifest.permission.CAMERA"))
            })
    }

    init {
        scope.launch {
            streamer.throwableFlow.filterNotNull().collect { throwable ->
                _isStreaming = false
                onGenericError(Exception(throwable))
            }
        }
        scope.launch {
            streamer.isStreamingFlow.collect { isStreaming ->
                if (isStreaming) {
                    onConnectionSucceeded()
                } else {
                    onDisconnected()
                }
            }
        }
    }

    fun dispose() {
        stopStream()
        runBlocking {
            streamer.stopPreview()
        }
        flutterTexture.release()
    }

    fun startStream(url: String) {
        runBlocking {
            try {
                streamer.startStream(url)
                _isStreaming = true
            } catch (e: Exception) {
                onConnectionFailed("Failed to start stream: ${e.message}")
                throw e
            }
        }
    }

    fun stopStream() {
        val wasConnected = streamer.isStreamingFlow.value
        runBlocking {
            streamer.stopStream()
            if (wasConnected) {
                onDisconnected()
            }
            _isStreaming = false
        }
    }

    fun startPreview(onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        permissionsManager.requestPermission(
            Manifest.permission.CAMERA,
            onGranted = {
                if (_videoConfig == null) {
                    onError(IllegalStateException("Video has not been configured!"))
                } else {
                    try {
                        runBlocking {
                            streamer.startPreview(getSurface(videoConfig.resolution))
                        }
                        _isPreviewing = true
                        onSuccess()
                    } catch (e: Exception) {
                        onError(e)
                    }
                }
            },
            onShowPermissionRationale = { _ ->
                /**
                 * Require an AppCompat theme to use MaterialAlertDialogBuilder
                 *
                 * context.showDialog(
                R.string.permission_required,
                R.string.camera_permission_required_message,
                android.R.string.ok,
                onPositiveButtonClick = { onRequiredPermissionLastTime() }
                )*/
                onError(SecurityException("Missing permission Manifest.permission.CAMERA"))
            },
            onDenied = {
                onError(SecurityException("Missing permission Manifest.permission.CAMERA"))
            })
    }

    fun stopPreview() {
        runBlocking {
            streamer.stopPreview()
        }
        _isPreviewing = false
    }

    private fun getSurface(resolution: Size): Surface {
        val surfaceTexture = flutterTexture.surfaceTexture().apply {
            setDefaultBufferSize(
                resolution.width,
                resolution.height
            )
        }
        return Surface(surfaceTexture)
    }


}