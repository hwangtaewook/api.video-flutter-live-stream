package video.api.flutter.livestream.utils

import android.media.AudioFormat
import android.util.Size
import io.github.thibaultbee.streampack.core.elements.encoders.AudioCodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig
import video.api.flutter.livestream.generated.NativeChannel
import video.api.flutter.livestream.generated.NativeAudioConfig
import video.api.flutter.livestream.generated.NativeResolution
import video.api.flutter.livestream.generated.NativeVideoConfig

fun NativeResolution.toSize() = Size(width.toInt(), height.toInt())

fun Size.toNativeResolution() = NativeResolution(width.toLong(), height.toLong())

fun NativeVideoConfig.toVideoConfig() = VideoCodecConfig(
    mimeType = android.media.MediaFormat.MIMETYPE_VIDEO_AVC,
    startBitrate = bitrate.toInt(),
    resolution = resolution.toSize(),
    fps = fps.toInt(),
    gopDurationInS = gopDurationInS.toFloat()
)

fun NativeChannel.toChannelConfig() = when (this) {
    NativeChannel.MONO -> AudioFormat.CHANNEL_IN_MONO
    NativeChannel.STEREO -> AudioFormat.CHANNEL_IN_STEREO
}

fun NativeAudioConfig.toAudioConfig() = AudioCodecConfig(
    mimeType = android.media.MediaFormat.MIMETYPE_AUDIO_AAC,
    startBitrate = bitrate.toInt(),
    sampleRate = sampleRate.toInt(),
    channelConfig = channel.toChannelConfig(),
    byteFormat = android.media.AudioFormat.ENCODING_PCM_16BIT
)