package com.example.fpsres

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

object ShizukuHelper {

    const val REQUEST_CODE = 1001

    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    fun hasPermission(): Boolean {
        if (!isShizukuAvailable()) return false
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    fun requestPermission() {
        if (isShizukuAvailable() && !hasPermission()) {
            Shizuku.requestPermission(REQUEST_CODE)
        }
    }

    fun runCommand(command: String): String {
        if (!hasPermission()) return "Chưa có quyền Shizuku"

        return try {
            val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))

            val output = reader.readText()
            val error = errorReader.readText()
            process.waitFor()

            if (error.isNotBlank()) error else output.ifBlank { "OK" }
        } catch (e: Exception) {
            "Lỗi: ${e.message}"
        }
    }

    fun setResolution(width: Int, height: Int): String {
        return runCommand("wm size ${width}x${height}")
    }

    fun resetResolution(): String {
        return runCommand("wm size reset")
    }

    fun setDensity(dpi: Int): String {
        return runCommand("wm density $dpi")
    }

    fun resetDensity(): String {
        return runCommand("wm density reset")
    }

    fun getCurrentSize(): String {
        return runCommand("wm size")
    }

    fun getCurrentDensity(): String {
        return runCommand("wm density")
    }
}
