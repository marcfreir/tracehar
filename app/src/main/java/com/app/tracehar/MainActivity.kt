// MainActivity.kt
package com.app.tracehar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.components.XAxis
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var linearAcceleration: Sensor? = null
    private var magnetometer: Sensor? = null

    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var helpButton: ImageButton
    private lateinit var infoButton: ImageButton
    private lateinit var activityEditText: EditText
    private lateinit var statusTextView: TextView
    private lateinit var dataCountTextView: TextView
    private lateinit var currentActivityTextView: TextView
    private lateinit var accelerometerChart: LineChart
    private lateinit var activityIndicator: TextView

    private var isRecording = false
    private var dataCount = 0
    private var currentActivity = ""
    private var fileWriter: FileWriter? = null
    private var dataFile: File? = null

    // Chart data
    private val accelerometerEntries = ArrayList<Entry>()
    private var chartDataIndex = 0f
    private val maxDataPoints = 50
    private val handler = Handler(Looper.getMainLooper())
    private var chartUpdateRunnable: Runnable? = null

    private val PERMISSION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        initializeSensors()
        setupClickListeners()
        setupChart()
        checkPermissions()
    }

    private fun initializeViews() {
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        helpButton = findViewById(R.id.helpButton)
        infoButton = findViewById(R.id.infoButton)
        activityEditText = findViewById(R.id.activityEditText)
        statusTextView = findViewById(R.id.statusTextView)
        dataCountTextView = findViewById(R.id.dataCountTextView)
        currentActivityTextView = findViewById(R.id.currentActivityTextView)
        accelerometerChart = findViewById(R.id.accelerometerChart)
        activityIndicator = findViewById(R.id.activityIndicator)

        stopButton.isEnabled = false
        updateStatus("Ready to record")
        updateActivityIndicator("idle")
    }

    private fun initializeSensors() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        linearAcceleration = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        // Check sensor availability
        val availableSensors = mutableListOf<String>()
        accelerometer?.let { availableSensors.add("Accelerometer") }
        gyroscope?.let { availableSensors.add("Gyroscope") }
        linearAcceleration?.let { availableSensors.add("Linear Acceleration") }
        magnetometer?.let { availableSensors.add("Magnetometer") }

        Toast.makeText(this, "Available sensors: ${availableSensors.joinToString()}", Toast.LENGTH_LONG).show()
    }

    private fun setupChart() {
        accelerometerChart.apply {
            description.isEnabled = false
            setTouchEnabled(false)
            isDragEnabled = false
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            legend.isEnabled = false

            // Dark theme styling
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.chart_background))

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = ContextCompat.getColor(this@MainActivity, R.color.chart_text)
                axisLineColor = ContextCompat.getColor(this@MainActivity, R.color.chart_axis)
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = ContextCompat.getColor(this@MainActivity, R.color.chart_grid)
                textColor = ContextCompat.getColor(this@MainActivity, R.color.chart_text)
                axisLineColor = ContextCompat.getColor(this@MainActivity, R.color.chart_axis)
            }

            axisRight.isEnabled = false
        }

        startChartAnimation()
    }

    private fun startChartAnimation() {
        chartUpdateRunnable = object : Runnable {
            override fun run() {
                if (!isRecording) {
                    // Generate sample wave pattern when not recording
                    val time = System.currentTimeMillis() / 100.0
                    val value = (Math.sin(time / 10.0) * 5 + Math.cos(time / 7.0) * 3).toFloat()
                    updateChart(value)
                }
                handler.postDelayed(this, 100)
            }
        }
        handler.post(chartUpdateRunnable!!)
    }

    private fun updateChart(magnitude: Float) {
        accelerometerEntries.add(Entry(chartDataIndex++, magnitude))

        if (accelerometerEntries.size > maxDataPoints) {
            accelerometerEntries.removeAt(0)
            // Shift all x values
            for (i in accelerometerEntries.indices) {
                accelerometerEntries[i] = Entry(i.toFloat(), accelerometerEntries[i].y)
            }
            chartDataIndex = maxDataPoints.toFloat()
        }

        val dataSet = LineDataSet(accelerometerEntries, "Accelerometer").apply {
            color = ContextCompat.getColor(this@MainActivity, R.color.chart_line_primary)
            setCircleColor(ContextCompat.getColor(this@MainActivity, R.color.chart_line_primary))
            lineWidth = 2f
            circleRadius = 1f
            setDrawValues(false)
            setDrawCircles(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        accelerometerChart.data = LineData(dataSet)
        accelerometerChart.invalidate()
    }

    private fun setupClickListeners() {
        startButton.setOnClickListener {
            val activity = activityEditText.text.toString().trim()
            if (activity.isEmpty()) {
                Toast.makeText(this, "Please enter activity name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startRecording(activity)
        }

        stopButton.setOnClickListener {
            stopRecording()
        }

        helpButton.setOnClickListener {
            showHelpDialog()
        }

        infoButton.setOnClickListener {
            showInfoDialog()
        }
    }

    private fun showHelpDialog() {
        AlertDialog.Builder(this, R.style.DarkAlertDialog)
            .setTitle("How to Use TraceHAR")
            .setMessage("""
                📱 Instructions:
                
                1. Enter the activity you want to record (e.g., walking, running, sitting)
                
                2. Press "Start Recording" to begin sensor data collection
                
                3. Perform the desired activity while holding your device
                
                4. Press "Stop Recording" when finished
                
                📊 Features:
                • Real-time sensor visualization
                • Multi-sensor data collection (accelerometer, gyroscope, magnetometer, linear acceleration)
                • CSV export for data analysis
                • Automatic file naming with timestamps
                
                💾 Data Storage:
                Data is saved as CSV files in your device's app-specific storage under HAR_Data folder.
            """.trimIndent())
            .setPositiveButton("Got it") { dialog, _ -> dialog.dismiss() }
            .create()
            .apply {
                window?.setBackgroundDrawableResource(R.drawable.dialog_rounded_background)
                show()
            }
    }

    private fun showInfoDialog() {
        AlertDialog.Builder(this, R.style.DarkAlertDialog)
            .setTitle("About TraceHAR")
            .setMessage("""
                🔬 Developer Information:
                
                📱 TraceHAR (Human Activity Recognition Tracer)
                📊 Version: 1.0
                
                🎯 Purpose:
                This app collects multi-sensor data from your device to support human activity recognition research and machine learning applications.
                
                📈 Sensors Used:
                • Accelerometer - Movement detection
                • Gyroscope - Rotation detection  
                • Magnetometer - Orientation detection
                • Linear Acceleration - Pure motion
                
                🔧 Technical Details:
                • Sampling Rate: ~50Hz (SENSOR_DELAY_GAME)
                • Data Format: CSV with timestamps
                • Storage: App-specific external directory
                
                Made with ❤️ for research and development by Marc Freir
                E-mail: markfreir@gmail.com | Licence: AGPL-3.0
            """.trimIndent())
            .setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
            .create()
            .apply {
                window?.setBackgroundDrawableResource(R.drawable.dialog_rounded_background)
                show()
            }
    }

    private fun updateActivityIndicator(activity: String) {
        val (text, color) = when (activity.lowercase()) {
            "walking" -> "🚶 WALKING" to R.color.activity_walking
            "running" -> "🏃 RUNNING" to R.color.activity_running
            "sitting" -> "🪑 SITTING" to R.color.activity_sitting
            "standing" -> "🧍 STANDING" to R.color.activity_standing
            "lying" -> "🛏️ LYING" to R.color.activity_lying
            else -> "⚫ ${activity.uppercase()}" to R.color.activity_other
        }

        activityIndicator.text = text
        activityIndicator.setBackgroundColor(ContextCompat.getColor(this, color))
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return
        }

        val permissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    private fun startRecording(activity: String) {
        if (!isRecording) {
            currentActivity = activity
            dataCount = 0

            createDataFile()
            registerSensorListeners()

            isRecording = true
            startButton.isEnabled = false
            stopButton.isEnabled = true
            activityEditText.isEnabled = false

            updateStatus("Recording: $currentActivity")
            updateActivityIndicator(currentActivity)
            currentActivityTextView.text = "Current: $currentActivity"
            Toast.makeText(this, "Started recording $currentActivity", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        if (isRecording) {
            sensorManager.unregisterListener(this)

            try {
                fileWriter?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            isRecording = false
            startButton.isEnabled = true
            stopButton.isEnabled = false
            activityEditText.isEnabled = true

            updateStatus("Recording stopped. Data saved to: ${dataFile?.name}")
            updateActivityIndicator("idle")
            currentActivityTextView.text = "Current: None"
            Toast.makeText(this, "Recording stopped. $dataCount samples saved", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createDataFile() {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "${currentActivity}_${timestamp}.csv"

            val harDataDir = File(getExternalFilesDir(null), "HAR_Data")
            if (!harDataDir.exists()) {
                harDataDir.mkdirs()
            }

            dataFile = File(harDataDir, fileName)
            fileWriter = FileWriter(dataFile)

            val header = "timestamp,activity,acc_x,acc_y,acc_z,gyro_x,gyro_y,gyro_z,linear_acc_x,linear_acc_y,linear_acc_z,mag_x,mag_y,mag_z\n"
            fileWriter?.write(header)
            fileWriter?.flush()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error creating data file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun registerSensorListeners() {
        val samplingRate = SensorManager.SENSOR_DELAY_GAME

        accelerometer?.let {
            sensorManager.registerListener(this, it, samplingRate)
        }

        gyroscope?.let {
            sensorManager.registerListener(this, it, samplingRate)
        }

        linearAcceleration?.let {
            sensorManager.registerListener(this, it, samplingRate)
        }

        magnetometer?.let {
            sensorManager.registerListener(this, it, samplingRate)
        }
    }

    // Sensor data storage
    private var lastAccData = FloatArray(3) { 0f }
    private var lastGyroData = FloatArray(3) { 0f }
    private var lastLinearAccData = FloatArray(3) { 0f }
    private var lastMagData = FloatArray(3) { 0f }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, lastAccData, 0, 3)
                if (isRecording) {
                    val magnitude = sqrt(lastAccData[0] * lastAccData[0] +
                            lastAccData[1] * lastAccData[1] +
                            lastAccData[2] * lastAccData[2])
                    runOnUiThread { updateChart(magnitude) }
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                System.arraycopy(event.values, 0, lastGyroData, 0, 3)
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                System.arraycopy(event.values, 0, lastLinearAccData, 0, 3)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, lastMagData, 0, 3)
            }
        }

        if (isRecording && event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            writeDataToFile()
        }
    }

    private fun writeDataToFile() {
        try {
            val timestamp = System.currentTimeMillis()
            val dataLine = "$timestamp,$currentActivity,${lastAccData[0]},${lastAccData[1]},${lastAccData[2]}," +
                    "${lastGyroData[0]},${lastGyroData[1]},${lastGyroData[2]}," +
                    "${lastLinearAccData[0]},${lastLinearAccData[1]},${lastLinearAccData[2]}," +
                    "${lastMagData[0]},${lastMagData[1]},${lastMagData[2]}\n"

            fileWriter?.write(dataLine)
            fileWriter?.flush()

            dataCount++
            runOnUiThread {
                dataCountTextView.text = "Samples: $dataCount"
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used for this application
    }

    private fun updateStatus(status: String) {
        statusTextView.text = status
    }

    override fun onDestroy() {
        super.onDestroy()
        chartUpdateRunnable?.let { handler.removeCallbacks(it) }
        if (isRecording) {
            stopRecording()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val deniedPermissions = permissions.filterIndexed { index, _ ->
                grantResults[index] != PackageManager.PERMISSION_GRANTED
            }

            if (deniedPermissions.isNotEmpty()) {
                Toast.makeText(this, "Storage permissions required for data collection", Toast.LENGTH_LONG).show()
            }
        }
    }
}