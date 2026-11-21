package com.magisksu.manager.data

import com.magisksu.manager.root.RootShell

/** Loads Magisk daemon logs plus su requests captured by logcat. */
object LogRepository {

    fun load(): String {
        if (!RootShell.hasRoot()) {
            return "Root erişimi yok.\n\nLogları görmek için cihazınızda Magisk kurulu olmalı ve bu uygulamaya su izni verilmiş olmalı."
        }

        val magiskLog = RootShell.exec(
            "tail -n 1000 /data/adb/magisk.log 2>/dev/null || " +
                "tail -n 1000 /cache/magisk.log 2>/dev/null || echo __EMPTY__",
            timeoutMs = 15_000,
        )

        val suLog = RootShell.exec(
            "logcat -d -v time -s Magisk:V magisk:V 2>/dev/null | tail -n 400 || echo __EMPTY__",
            timeoutMs = 15_000,
        )

        val daemon = magiskLog.stdout
            .replace("__EMPTY__", "")
            .trim()
            .ifEmpty { "(magisk.log boş veya bulunamadı)" }

        val su = suLog.stdout
            .replace("__EMPTY__", "")
            .trim()
            .ifEmpty { "(su isteği kaydı yok)" }

        return buildString {
            appendLine("===== Magisk daemon logu =====")
            appendLine(daemon)
            appendLine()
            appendLine("===== Su istekleri (logcat) =====")
            appendLine(su)
        }
    }

    /** Truncates the on-device log files and clears logcat. */
    fun clear(): Boolean =
        RootShell.exec(
            "> /data/adb/magisk.log 2>/dev/null; " +
                "> /cache/magisk.log 2>/dev/null; " +
                "logcat -c 2>/dev/null; true",
        ).isSuccess
}
