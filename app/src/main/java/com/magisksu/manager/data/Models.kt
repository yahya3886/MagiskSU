package com.magisksu.manager.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.graphics.drawable.toBitmap

/** An installed app paired with its Magisk su policy. */
data class SuperuserApp(
    val uid: Int,
    val packageName: String,
    val label: String,
    val policy: Int,
    val logging: Boolean,
    val notification: Boolean,
    val isSystemApp: Boolean,
) {
    val granted: Boolean get() = com.magisksu.manager.root.SuPolicyValue.isGranted(policy)

    val policyLabel: String
        get() = when (policy) {
            com.magisksu.manager.root.SuPolicyValue.ALLOW -> "İzin verildi"
            com.magisksu.manager.root.SuPolicyValue.RESTRICT -> "Kısıtlı"
            com.magisksu.manager.root.SuPolicyValue.DENY -> "Reddedildi"
            else -> "Sorgulanacak"
        }
}

/** A Magisk module as installed under /data/adb/modules. */
data class ModuleInfo(
    val id: String,
    val name: String,
    val version: String,
    val versionCode: String,
    val description: String,
    val author: String,
    val enabled: Boolean,
    val pendingRemove: Boolean,
    val pendingUpdate: Boolean,
) {
    val statusLabel: String
        get() = when {
            pendingRemove -> "Kaldırılacak"
            !enabled -> "Devre dışı"
            pendingUpdate -> "Güncellemeye hazır"
            else -> "Etkin"
        }
}

/** Result of a module zip installation. */
data class ModuleInstallResult(
    val success: Boolean,
    val message: String,
    val moduleName: String = "",
)

/** Helpers for reading app icons without Coil/remote loading. */
object AppIcons {

    fun iconOf(context: Context, packageName: String): Bitmap? = try {
        val pm = context.packageManager
        @Suppress("DEPRECATION")
        val ai = pm.getApplicationIcon(packageName)
        drawableToBitmap(ai)
    } catch (e: Exception) {
        null
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 96
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 96
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                drawable.toBitmap(width, height, Bitmap.Config.ARGB_8888)
            } else {
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            }
        } catch (e: Exception) {
            null
        }
    }
}
