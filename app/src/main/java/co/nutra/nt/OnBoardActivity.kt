package co.nutra.nt

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URL


class OnBoardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
//        StrictMode.setThreadPolicy(policy)
    }

    fun quit(view: View) {
        finishAndRemoveTask()
    }

    private fun log(line: String) {
        val textView = TextView(this)
        textView.text = line
        textView.typeface = Typeface.createFromAsset(
            applicationContext.resources.assets,
            "font/droid_sans_mono.ttf"
        )
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        runOnUiThread {
            // Add line
            linearLayoutConsole.addView(textView)

            // Scroll
            scrollViewConsole.post {
                scrollViewConsole.fullScroll(
                    ScrollView.FOCUS_DOWN
                )
            }

            linearLayoutConsole.refreshDrawableState()
        }
    }

    fun retry(view: View) {
        // First allow read/write
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)

        // Download, unpack database
        // (Background thread)
        val ovThread = Thread {
            val downloadsOverviewApiUrl =
                "https://api.bitbucket.org/2.0/repositories/dasheenster/nutra-utils/downloads"
            log("GET $downloadsOverviewApiUrl")
            runOnUiThread { buttonRetry.isEnabled = false }

            // Make "overview" network call
            val downloadsOverviewStr: String = URL(downloadsOverviewApiUrl).readText()

            // Find the target file, if it exists
            val downloadsOverview: JSONArray =
                JSONObject(downloadsOverviewStr)["values"] as JSONArray
            val targetFileName = "nutra.db-${Settings.dbTarget}.tar.xz"
            var targetFile = JSONObject()

            for (i in 0 until downloadsOverview.length()) {
                val downloadOverview: JSONObject = downloadsOverview[i] as JSONObject
                val name: String = downloadOverview.getString("name")
                if (!name.startsWith("nutra.db"))
                    continue
                if (name == targetFileName)
                    targetFile = downloadOverview
            }

            if (targetFile.length() == 0)
                Toast.makeText(
                    applicationContext,
                    "ERROR: download doesn't exist, contact support: $targetFileName",
                    Toast.LENGTH_LONG
                ).show()
            else {
                val nDownloads = targetFile["downloads"] as Int
                val creationDateStr = targetFile["created_on"].toString()
                val size = targetFile["size"] as Int
                val sizeStr = "%.1f".format(size / 1000.0)
                val downloadUrl = "$downloadsOverviewApiUrl/$targetFileName"

                // Prompt user to download large db file (> 5MB)
                showAlertDialogButtonClicked(
                    view,
                    downloadUrl,
                    targetFileName,
                    nDownloads,
                    creationDateStr,
                    sizeStr
                )
            }

            runOnUiThread { buttonRetry.isEnabled = true }
        }

        ovThread.start()
    }

    private fun showAlertDialogButtonClicked(
        view: View,
        downloadUrl: String,
        fileName: String,
        nDownloads: Int,
        creationDate: String,
        size: String
    ) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Download large file?")
        val message =
            "Download nutra.db (v${Settings.dbTarget})?\n\nSize: $size KB\nDownloads: $nDownloads\n\nCreated: $creationDate"
        builder.setMessage(message)
        builder.setPositiveButton("OK") { _, _ ->
            // Lambda for when users presses "OK" to download
            buttonRetry.isEnabled = false
            runDownload(downloadUrl, fileName)
            buttonRetry.isEnabled = true
        }
        builder.setNegativeButton("Cancel", null)

        // Show alert
        runOnUiThread {
            val dialog: AlertDialog = builder.create()
            dialog.show()
        }
    }

    private fun runDownload(url: String, fileName: String) {
        var externalFilesDir = getExternalFilesDir(null)
        val dbFolder = File("$externalFilesDir/db")
        log("mkdir -p $externalFilesDir/db")
        dbFolder.mkdirs()

        val request = DownloadManager.Request(Uri.parse(url))
        request.setDescription("Some descrition")
        request.setTitle("Some title")
        request.setDestinationInExternalPublicDir(
            Environment.DIRECTORY_DOWNLOADS,
            fileName
        )

        // get download service and enqueue file
        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
    }
}