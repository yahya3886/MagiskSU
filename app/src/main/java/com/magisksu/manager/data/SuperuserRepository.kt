package com.magisksu.manager.data

import android.content.Context
import android.content.pm.ApplicationInfo
import com.magisksu.manager.root.MagiskCore
import com.magisksu.manager.root.RootShell

/**
 * Bridges Magisk's `policies` table with PackageManager so the UI can show
 * a human readable superuser list.
 */
object SuperuserRepository {

    data class LoadResult(
        val rootAvailable: Boolean,
        val apps: List<SuperuserApp>,
    )

    fun load(context: Context): LoadResult {
        if (!RootShell.hasRoot()) return LoadResult(false, emptyList())

        val policies = MagiskCore.listPolicies()
        if (policies.isEmpty()) return LoadResult(true, emptyList())

        val pm = context.packageManager
        val apps = policies.mapNotNull { policy ->
            val packages = pm.getPackagesForUid(policy.uid)
            if (packages.isNullOrEmpty()) return@mapNotNull null
            val pkg = packages.first()
            val info = try {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(pkg, 0)
            } catch (e: Exception) {
                return@mapNotNull null
            }
            SuperuserApp(
                uid = policy.uid,
                packageName = pkg,
                label = pm.getApplicationLabel(info).toString(),
                policy = policy.policy,
                logging = policy.logging,
                notification = policy.notification,
                isSystemApp = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
            )
        }.sortedBy { it.label.lowercase() }

        return LoadResult(true, apps)
    }

    /** Removes the stored decision so the app is asked again on next request. */
    fun revoke(uid: Int): Boolean = MagiskCore.deletePolicy(uid)

    /** Grants su access immediately (writes ALLOW into the policies table). */
    fun grant(uid: Int): Boolean = MagiskCore.grantPolicy(uid)
}
