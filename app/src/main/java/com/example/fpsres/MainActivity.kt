package com.example.fpsres

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fpsres.databinding.ActivityMainBinding
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val permissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, _ ->
            if (requestCode == ShizukuHelper.REQUEST_CODE) {
                updateShizukuStatus()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupFpsSpinner()
        setupButtons()

        Shizuku.addRequestPermissionResultListener(permissionListener)

        updateShizukuStatus()
    }

    private fun setupFpsSpinner() {
        val fpsList = arrayOf(
            "30 FPS",
            "40 FPS",
            "60 FPS",
            "90 FPS",
            "120 FPS"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            fpsList
        )

        binding.fps.adapter = adapter

        // Mặc định 60 FPS
        binding.fps.setSelection(2)
    }

    private fun setupButtons() {

        // Nhấn trạng thái Shizuku để cấp quyền
        binding.status.setOnClickListener {

            if (!ShizukuHelper.isAvailable()) {
                toast("Hãy mở Shizuku trước")
                return@setOnClickListener
            }

            if (!ShizukuHelper.hasPermission()) {
                ShizukuHelper.requestPermission()
            } else {
                toast("Shizuku đã được cấp quyền")
            }
        }

        // Áp dụng profile
        binding.apply.setOnClickListener {
            applyProfile()
        }

        // Performance
        binding.performance.setOnClickListener {
            setGameMode("performance")
        }

        // Standard
        binding.reset.setOnClickListener {
            setGameMode("standard")
        }

        // Overlay
        binding.overlay.setOnClickListener {

            if (!Settings.canDrawOverlays(this)) {

                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )

                startActivity(intent)

            } else {

                try {
                    val intent = Intent(
                        this,
                        FpsOverlayService::class.java
                    )

                    startService(intent)

                    toast("Đã bật overlay")

                } catch (e: Exception) {

                    toast(
                        "Không thể bật overlay: ${e.message}"
                    )
                }
            }
        }
    }

    private fun updateShizukuStatus() {

        binding.status.text = when {

            !ShizukuHelper.isAvailable() ->
                "Shizuku: CHƯA CHẠY"

            !ShizukuHelper.hasPermission() ->
                "Shizuku: CHƯA CẤP QUYỀN"

            else ->
                "Shizuku: ĐÃ KẾT NỐI ✓"
        }
    }

    private fun getPackageNameInput(): String? {

        val pkg = binding.packageName.text
            .toString()
            .trim()

        if (pkg.isEmpty()) {
            toast("Nhập package của game")
            return null
        }

        /*
         * Chỉ cho phép package Android hợp lệ.
         * Ví dụ:
         * com.dts.freefireth
         */
        val valid = Regex(
            "^[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+$"
        )

        if (!valid.matches(pkg)) {
            toast("Package không hợp lệ")
            return null
        }

        return pkg
    }

    private fun applyProfile() {

        val pkg = getPackageNameInput()
            ?: return

        if (!ShizukuHelper.isAvailable()) {
            toast("Hãy khởi động Shizuku trước")
            return
        }

        if (!ShizukuHelper.hasPermission()) {
            ShizukuHelper.requestPermission()
            return
        }

        val scaleText = binding.downscale.text
            .toString()
            .trim()

        val scale = scaleText.toFloatOrNull()

        if (scale == null) {
            toast("Độ phân giải không hợp lệ")
            return
        }

        if (scale < 0.70f || scale > 1.0f) {
            toast("Downscale phải từ 0.70 đến 1.00")
            return
        }

        val fpsText =
            binding.fps.selectedItem
                .toString()
                .replace(" FPS", "")

        val fps = fpsText.toIntOrNull()

        if (fps == null) {
            toast("FPS không hợp lệ")
            return
        }

        binding.log.text =
            "Đang áp dụng...\n\n" +
            "Game: $pkg\n" +
            "Scale: $scale\n" +
            "FPS: $fps"

        Thread {

            val result = try {

                val command =
                    "cmd game set --mode 2 " +
                    "--downscale $scale " +
                    "--fps $fps $pkg"

                ShizukuHelper.exec(command)

            } catch (e: Exception) {

                "LỖI: ${e.message}"
            }

            runOnUiThread {

                binding.log.text =
                    "Kết quả:\n$result\n\n" +
                    "Game: $pkg\n" +
                    "Scale: $scale\n" +
                    "FPS: $fps"
            }

        }.start()
    }

    private fun setGameMode(mode: String) {

        val pkg = getPackageNameInput()
            ?: return

        if (!ShizukuHelper.isAvailable()) {
            toast("Hãy khởi động Shizuku trước")
            return
        }

        if (!ShizukuHelper.hasPermission()) {
            ShizukuHelper.requestPermission()
            return
        }

        binding.log.text =
            "Đang chuyển Game Mode...\n$mode"

        Thread {

            val result = try {

                ShizukuHelper.exec(
                    "cmd game mode $mode $pkg"
                )

            } catch (e: Exception) {

                "LỖI: ${e.message}"
            }

            runOnUiThread {

                binding.log.text =
                    "Game Mode: $mode\n\n$result"
            }

        }.start()
    }

    private fun toast(message: String) {

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onResume() {

        super.onResume()

        updateShizukuStatus()
    }

    override fun onDestroy() {

        Shizuku.removeRequestPermissionResultListener(
            permissionListener
        )

        super.onDestroy()
    }
}
