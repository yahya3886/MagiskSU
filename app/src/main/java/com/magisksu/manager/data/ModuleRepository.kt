package com.magisksu.manager.data

import android.content.Context
import android.net.Uri
import com.magisksu.manager.root.RootShell
import java.io.File
import java.util.zip.ZipInputStream

/**
 * Reads and manages Magisk modules under /data/adb/modules.
 *
 * State flags follow Magisk's own convention:
 *   `disable` file -> module is turned off
 *   `remove`  file -> module is deleted on next reboot
 *   `update`  file -> staged update exists
 * Staged installs go to /data/adb/modules_update/<id>, which Magisk moves
 * into place during boot.
 */
object ModuleRepository {

    private const val MARKER = "###MAGISKSU_MODULE###"
    private const val MODULES_DIR = "/data/adb/modules"
    private const val MODULES_UPDATE_DIR = "/data/adb/modules_update"

    fun list(): List<ModuleInfo> {
        if (!RootShell.hasRoot()) return emptyList()

        val script = """
            for d in $MODULES_DIR/*/ ; do
              [ -f "${'$'}d/module.prop" ] || continue
              echo "$MARKER"
              echo "${'$'}d"
              cat "${'$'}d/module.prop"
              [ -f "${'$'}d/disable" ] && echo "flag:disable"
              [ -f "${'$'}d/remove" ] && echo "flag:remove"
              [ -f "${'$'}d/update" ] && echo "flag:update"
            done
            exit 0
        """.trimIndent()

        val result = RootShell.exec(script, timeoutMs = 30_000)
        if (!result.isSuccess) return emptyList()

        return result.stdout
            .split(MARKER)
            .drop(1)
            .mapNotNull { parseSection(it) }
            .sortedBy { it.name.lowercase() }
    }

    private fun parseSection(section: String): ModuleInfo? {
        val lines = section.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val path = lines.firstOrNull() ?: return null
        val id = path.trimEnd('/').substringAfterLast('/').ifEmpty { return null }

        val props = mutableMapOf<String, String>()
        var disabled = false
        var pendingRemove = false
        var pendingUpdate = false

        for (line in lines.drop(1)) {
            when {
                line == "flag:disable" -> disabled = true
                line == "flag:remove" -> pendingRemove = true
                line == "flag:update" -> pendingUpdate = true
                '=' in line -> {
                    val idx = line.indexOf('=')
                    props[line.substring(0, idx).trim()] = line.substring(idx + 1).trim()
                }
            }
        }

        return ModuleInfo(
            id = id,
            name = props["name"] ?: id,
            version = props["version"] ?: "-",
            versionCode = props["versionCode"] ?: "-",
            description = props["description"] ?: "",
            author = props["author"] ?: "-",
            enabled = !disabled,
            pendingRemove = pendingRemove,
            pendingUpdate = pendingUpdate,
        )
    }

    /** Enables or disables a module (creates/removes the `disable` flag file). */
    fun setEnabled(id: String, enabled: Boolean): Boolean {
        val safeId = sanitizeId(id) ?: return false
        val cmd = if (enabled) {
            "rm -f '$MODULES_DIR/$safeId/disable'"
        } else {
            "touch '$MODULES_DIR/$safeId/disable'"
        }
        return RootShell.exec(cmd).isSuccess
    }

    /** Schedules module removal on next reboot (Magisk `remove` flag file). */
    fun scheduleRemoval(id: String): Boolean {
        val safeId = sanitizeId(id) ?: return false
        return RootShell.exec("touch '$MODULES_DIR/$safeId/remove'").isSuccess
    }

    private fun sanitizeId(id: String): String? =
        id.takeIf { it.isNotEmpty() && it.all { c -> c.isLetterOrDigit() || c == '_' || c == '-' || c == '.' } }

    /**
     * Extracts a module zip (with Zip-Slip protection), validates module.prop
     * and stages it into /data/adb/modules_update/<id> for the next boot.
     */
    fun install(context: Context, uri: Uri): ModuleInstallResult {
        if (!RootShell.hasRoot()) {
            return ModuleInstallResult(false, "Root erişimi yok")
        }

        val stageDir = File(context.cacheDir, "module_stage")
        stageDir.deleteRecursively()
        stageDir.mkdirs()

        val extracted = try {
            extractZip(context, uri, stageDir)
        } catch (e: Exception) {
            stageDir.deleteRecursively()
            return ModuleInstallResult(false, "Zip açılamadı: ${e.message}")
        }
        if (!extracted) {
            stageDir.deleteRecursively()
            return ModuleInstallResult(false, "Zip açılamadı")
        }

        val propFile = findModuleProp(stageDir)
            ?: run { stageDir.deleteRecursively(); return ModuleInstallResult(false, "module.prop bulunamadı, bu bir Magisk modülü değil") }

        val props = parseProps(propFile)
        val id = sanitizeId(props["id"] ?: "")
            ?: run { stageDir.deleteRecursively(); return ModuleInstallResult(false, "Geçersiz modül kimliği (id)") }

        val sourceDir = propFile.parentFile
            ?: run { stageDir.deleteRecursively(); return ModuleInstallResult(false, "Geçersiz modül yapısı") }

        val target = "$MODULES_UPDATE_DIR/$id"
        val copy = RootShell.exec(
            "rm -rf '$target' && mkdir -p '$target' && cp -a '${sourceDir.absolutePath}/.' '$target/'",
            timeoutMs = 60_000,
        )

        stageDir.deleteRecursively()

        return if (copy.isSuccess) {
            ModuleInstallResult(
                success = true,
                message = "Kurulum tamamlandı. Değişikliklerin etkin olması için cihazı yeniden başlatın.",
                moduleName = props["name"] ?: id,
            )
        } else {
            ModuleInstallResult(false, "Kopyalama başarısız: ${copy.combined.ifEmpty { "bilinmeyen hata" }}")
        }
    }

    private fun extractZip(context: Context, uri: Uri, targetDir: File): Boolean {
        val input = context.contentResolver.openInputStream(uri) ?: return false
        val base = targetDir.canonicalPath + File.separator
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val outFile = File(targetDir, entry.name)
                // Zip-Slip protection: never write outside the staging dir.
                if (!outFile.canonicalPath.startsWith(base)) return false
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { out -> zip.copyTo(out) }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return true
    }

    private fun findModuleProp(dir: File, depth: Int = 0): File? {
        val direct = File(dir, "module.prop")
        if (direct.isFile) return direct
        if (depth >= 2) return null
        dir.listFiles()?.forEach { child ->
            if (child.isDirectory) {
                findModuleProp(child, depth + 1)?.let { return it }
            }
        }
        return null
    }

    private fun parseProps(file: File): Map<String, String> =
        runCatching {
            file.readLines()
                .mapNotNull { line ->
                    val idx = line.indexOf('=')
                    if (idx <= 0) null else line.substring(0, idx).trim() to line.substring(idx + 1).trim()
                }
                .toMap()
        }.getOrDefault(emptyMap())
}
