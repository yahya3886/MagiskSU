package com.magisksu.manager.root

/**
 * Su (root) policy values as stored in Magisk's `policies` table.
 * Source of truth: topjohnwu/Magisk `SuPolicy.kt`.
 */
object SuPolicyValue {
    const val QUERY = 0
    const val DENY = 1
    const val ALLOW = 2
    const val RESTRICT = 3

    fun isGranted(policy: Int): Boolean = policy == ALLOW || policy == RESTRICT
}

/** A row of Magisk's `policies` table. */
data class SuPolicyRecord(
    val uid: Int,
    val policy: Int,
    val until: Long,
    val logging: Boolean,
    val notification: Boolean,
) {
    val granted: Boolean get() = SuPolicyValue.isGranted(policy)
}

/** Aggregated status of Magisk + root on this device. */
data class MagiskStatus(
    val hasRoot: Boolean,
    val magiskFound: Boolean,
    val magiskVersion: String,
    val magiskVersionCode: Int,
    val magiskPath: String,
    val kernelVersion: String,
    val androidVersion: String,
    val apiLevel: Int,
    val deviceModel: String,
    val zygiskEnabled: Boolean,
    val denylistEnabled: Boolean,
) {
    companion object {
        fun empty() = MagiskStatus(
            hasRoot = false,
            magiskFound = false,
            magiskVersion = "Bulunamadı",
            magiskVersionCode = 0,
            magiskPath = "-",
            kernelVersion = "-",
            androidVersion = "-",
            apiLevel = 0,
            deviceModel = "-",
            zygiskEnabled = false,
            denylistEnabled = false,
        )
    }
}

/**
 * All direct talks with the Magisk daemon/database.
 *
 * Magisk exposes a stable CLI for its own database:
 *   magisk --sqlite "<SQL>"     -> executes SQL against /data/adb/magisk.db
 *   magisk -v / -V / --path     -> daemon version / version code / tmp path
 * Schema (native/src/core/sqlite.cpp):
 *   policies(uid INT, policy INT, until INT, logging INT, notification INT, PRIMARY KEY(uid))
 *   settings(key TEXT, value INT, PRIMARY KEY(key))
 *   denylist(package_name TEXT, process TEXT, PRIMARY KEY(package_name, process))
 */
object MagiskCore {

    /** Runs a magisk CLI invocation, falling back to known install paths. */
    private fun magisk(args: String): ShellResult =
        RootShell.exec("magisk $args 2>/dev/null || /data/adb/magisk $args 2>/dev/null || /sbin/magisk $args")

    /** Executes SQL against Magisk's database. Returns rows split on `|`. */
    fun query(sql: String): List<List<String>> {
        val safeSql = sanitizeSql(sql)
        val result = magisk("--sqlite \"$safeSql\"")
        if (!result.isSuccess) return emptyList()
        return result.stdout
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { line -> line.split('|').map { it.trim() } }
            .toList()
    }

    /** Executes a write SQL statement. Returns true on success. */
    fun execute(sql: String): Boolean {
        val safeSql = sanitizeSql(sql)
        val result = magisk("--sqlite \"$safeSql\"")
        // magisk --sqlite returns an empty result set (exit 0) for writes.
        return result.isSuccess && !result.stderr.contains("sqlite", ignoreCase = true)
    }

    private fun sanitizeSql(sql: String): String =
        sql.filter { it.code in 32..126 && it != '"' && it != '\\' && it != '`' }

    /** Collects device + Magisk information with a minimal number of shell calls. */
    fun status(): MagiskStatus {
        if (!RootShell.hasRoot()) return MagiskStatus.empty()

        val device = RootShell.exec(
            "uname -r; getprop ro.build.version.release; " +
                "getprop ro.build.version.sdk; getprop ro.product.model",
        ).stdout.lines().map { it.trim() }

        val version = magisk("-v").stdout.trim()
        val versionCode = magisk("-V").stdout.trim().toIntOrNull() ?: 0
        val path = magisk("--path").stdout.trim()
        val settings = settings()

        return MagiskStatus(
            hasRoot = true,
            magiskFound = version.isNotEmpty() || versionCode > 0 || path.isNotEmpty(),
            magiskVersion = version.ifEmpty { "Bulunamadı" },
            magiskVersionCode = versionCode,
            magiskPath = path.ifEmpty { "-" },
            kernelVersion = device.getOrNull(0)?.ifEmpty { "-" } ?: "-",
            androidVersion = device.getOrNull(1)?.ifEmpty { "-" } ?: "-",
            apiLevel = device.getOrNull(2)?.toIntOrNull() ?: 0,
            deviceModel = device.getOrNull(3)?.ifEmpty { "-" } ?: "-",
            zygiskEnabled = settings["zygisk"] == 1,
            denylistEnabled = settings["denylist"] == 1,
        )
    }

    /** Reads the whole `settings` table as key -> value. */
    fun settings(): Map<String, Int> =
        query("SELECT key, value FROM settings")
            .mapNotNull { row ->
                val key = row.getOrNull(0) ?: return@mapNotNull null
                val value = row.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
                key to value
            }
            .toMap()

    /** Reads every su policy row. */
    fun listPolicies(): List<SuPolicyRecord> =
        query("SELECT uid, policy, until, logging, notification FROM policies").mapNotNull { row ->
            val uid = row.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
            SuPolicyRecord(
                uid = uid,
                policy = row.getOrNull(1)?.toIntOrNull() ?: SuPolicyValue.DENY,
                until = row.getOrNull(2)?.toLongOrNull() ?: 0L,
                logging = row.getOrNull(3)?.toIntOrNull() != 0,
                notification = row.getOrNull(4)?.toIntOrNull() != 0,
            )
        }

    /** Removes the su decision for [uid] so the app has to ask again. */
    fun deletePolicy(uid: Int): Boolean = execute("DELETE FROM policies WHERE uid=$uid")

    /** Grants su to [uid] without prompting (writes ALLOW = 2). */
    fun grantPolicy(uid: Int): Boolean = execute(
        "DELETE FROM policies WHERE uid=$uid;" +
            "INSERT INTO policies (uid, policy, until, logging, notification) " +
            "VALUES ($uid, ${SuPolicyValue.ALLOW}, 0, 1, 1)",
    )

    /** Persists a Magisk `settings` row (e.g. zygisk / denylist toggles). */
    fun putSetting(key: String, value: Int): Boolean =
        execute("REPLACE INTO settings (key, value) VALUES ('$key', $value)")

    /** Reboots the device into a normal Android restart. */
    fun reboot(): Boolean =
        RootShell.exec("reboot || svc power reboot", timeoutMs = 8_000).isSuccess
}
