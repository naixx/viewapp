package com.github.naixx.viewapp

import android.app.Activity
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Intent
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import dev.zacsweers.metrox.android.MetroAppComponentFactory

/**
 * [MetroAppComponentFactory] that still works when it did not create the [android.app.Application] itself:
 * Robolectric instantiates the Application on its own and then asks the factory for the receivers of merged library
 * manifests, which Metro's factory answers with "metroApplication has not been initialized". Until the Application
 * exists nothing of ours can be asked for, so the plain reflective construction is the right answer there.
 */
@RequiresApi(28)
@Keep
class ViewAppComponentFactory : MetroAppComponentFactory() {

    override fun instantiateActivityCompat(cl: ClassLoader, className: String, intent: Intent?): Activity =
        orDefault({ super.instantiateActivityCompat(cl, className, intent) }) { newInstance(cl, className) }

    override fun instantiateServiceCompat(cl: ClassLoader, className: String, intent: Intent?): Service =
        orDefault({ super.instantiateServiceCompat(cl, className, intent) }) { newInstance(cl, className) }

    override fun instantiateReceiverCompat(cl: ClassLoader, className: String, intent: Intent?): BroadcastReceiver =
        orDefault({ super.instantiateReceiverCompat(cl, className, intent) }) { newInstance(cl, className) }

    private inline fun <T> orDefault(metro: () -> T, default: () -> T): T = try {
        metro()
    } catch (e: UninitializedPropertyAccessException) {
        default()
    }

    private inline fun <reified T : Any> newInstance(cl: ClassLoader, className: String): T =
        Class.forName(className, false, cl).asSubclass(T::class.java).getDeclaredConstructor().newInstance()
}
