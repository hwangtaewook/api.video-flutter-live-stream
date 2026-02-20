package video.api.flutter.livestream.manager

import android.content.Context
import io.github.thibaultbee.streampack.core.streamers.single.SingleStreamer

class InstanceManager(var context: Context? = null) {
    private var instance: SingleStreamer? = null

    fun getInstance(): SingleStreamer {
        if (instance == null) {
            instance = SingleStreamer(context!!)
        }
        return instance!!
    }

    fun dispose() {
        instance = null
    }
}