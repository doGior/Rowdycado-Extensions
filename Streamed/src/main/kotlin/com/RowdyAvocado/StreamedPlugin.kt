package com.RowdyAvocado

import android.content.Context
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.lagradost.cloudstream3.MainActivity.Companion.afterPluginsLoadedEvent
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import com.lagradost.cloudstream3.plugins.PluginManager

@CloudstreamPlugin
class StreamedPlugin : Plugin() {
    private var activity: AppCompatActivity? = null

    companion object {
        inline fun Handler.postFunction(crossinline function: () -> Unit) {
            this.post { function() }
        }
    }

    override fun load(context: Context) {
        activity = context as AppCompatActivity
        registerMainAPI(Streamed(this))
    }

    fun reload(context: Context?) {
        val pluginData =
                PluginManager.getPluginsOnline().find { it.internalName.contains("Streamed") }
        if (pluginData == null) {
            PluginManager.hotReloadAllLocalPlugins(context as AppCompatActivity)
        } else {
            PluginManager.unloadPlugin(pluginData.filePath)
            PluginManager.loadAllOnlinePlugins(context!!)
            afterPluginsLoadedEvent.invoke(true)
        }
    }
}
