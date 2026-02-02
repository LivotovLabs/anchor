package io.anchorkmp.core

import android.content.Context
import java.lang.ref.WeakReference

object AnchorContext {
    private var contextRef: WeakReference<Context>? = null

    internal fun setContext(context: Context) {
        contextRef = WeakReference(context.applicationContext)
    }

    internal fun getContext(): Context {
        return contextRef?.get() ?: throw IllegalStateException("AnchorContext not initialized. Ensure AnchorInitProvider is registered in AndroidManifest or call AnchorContext.setContext manually.")
    }
}