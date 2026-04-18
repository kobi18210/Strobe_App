package com.stroberpm

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.stroberpm.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraManager: CameraManager
    private var cameraId: String? = null

    private val handler = Handler(Looper.getMainLooper())
    private var isStrobing = false
    private var torchOn = false

    // Default: 1 Hz = 60 RPM
    private var frequencyHz = 1.0
    private var numBlades = 3

    private val strobeRunnable = object : Runnable {
        override fun run() {
            if (!isStrobing) return
            torchOn = !torchOn
            try {
                cameraId?.let { cameraManager.setTorchMode(it, torchOn) }
            } catch (e: Exception) {
                stopStrobe()
                Toast.makeText(this@MainActivity, "Torch error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            val delayMs = (1000.0 / (frequencyHz * 2)).toLong().coerceAtLeast(20L)
            handler.postDelayed(this, delayMs)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraId = cameraManager.cameraIdList.firstOrNull()

        checkCameraPermission()
        setupUI()
    }

    private fun setupUI() {
        updateDisplays()

        // Frequency SeekBar: 0.5 Hz to 25 Hz, step 0.5
        // seekBar max = 49 → value = (progress+1)*0.5
        binding.seekBarFrequency.max = 49
        binding.seekBarFrequency.progress = 1 // default 1 Hz

        binding.seekBarFrequency.setOnSeekBarChangeListener(object :
            android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                frequencyHz = (progress + 1) * 0.5
                updateDisplays()
                if (isStrobing) {
                    // Restart with new frequency
                    handler.removeCallbacks(strobeRunnable)
                    handler.post(strobeRunnable)
                }
            }
            override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {}
        })

        // Blade count buttons
        binding.btnBladesMinus.setOnClickListener {
            if (numBlades > 1) {
                numBlades--
                updateDisplays()
            }
        }
        binding.btnBladesPlus.setOnClickListener {
            if (numBlades < 9) {
                numBlades++
                updateDisplays()
            }
        }

        // Start / Stop
        binding.btnStartStop.setOnClickListener {
            if (isStrobing) stopStrobe() else startStrobe()
        }

        // Fine tune buttons
        binding.btnSlower.setOnClickListener {
            val newProgress = (binding.seekBarFrequency.progress - 1).coerceAtLeast(0)
            binding.seekBarFrequency.progress = newProgress
        }
        binding.btnFaster.setOnClickListener {
            val newProgress = (binding.seekBarFrequency.progress + 1).coerceAtMost(49)
            binding.seekBarFrequency.progress = newProgress
        }
    }

    private fun updateDisplays() {
        val rpm = frequencyHz * 60.0
        val trueRpm = rpm * numBlades  // accounting for blade symmetry aliasing
        binding.tvFrequency.text = "%.1f Hz".format(frequencyHz)
        binding.tvRpmDirect.text = "%.0f RPM".format(rpm)
        binding.tvRpmTrue.text = "%.0f RPM".format(trueRpm)
        binding.tvBlades.text = "$numBlades"
        binding.tvAliasNote.text = "If fan has $numBlades blades and appears frozen,\nit could also be ${
            (2 until numBlades).joinToString(" or ") { "%.0f".format(rpm * it) }
                .ifEmpty { "only this value" }
        } RPM"
    }

    private fun startStrobe() {
        if (!hasCamera()) {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
            checkCameraPermission()
            return
        }
        if (cameraId == null) {
            Toast.makeText(this, "No camera found", Toast.LENGTH_SHORT).show()
            return
        }
        isStrobing = true
        torchOn = false
        binding.btnStartStop.text = "⬛ STOP"
        binding.btnStartStop.setBackgroundColor(getColor(android.R.color.holo_red_dark))
        handler.post(strobeRunnable)
    }

    private fun stopStrobe() {
        isStrobing = false
        handler.removeCallbacks(strobeRunnable)
        try {
            cameraId?.let { cameraManager.setTorchMode(it, false) }
        } catch (_: Exception) {}
        torchOn = false
        binding.btnStartStop.text = "▶ START STROBE"
        binding.btnStartStop.setBackgroundColor(getColor(android.R.color.holo_green_dark))
    }

    private fun hasCamera() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun checkCameraPermission() {
        if (!hasCamera()) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.firstOrNull() != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Camera permission is required to use the torch", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopStrobe()
    }

    override fun onPause() {
        super.onPause()
        stopStrobe()
    }
}
