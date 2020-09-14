package co.nutra.nt

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
        textView.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

//        runOnUiThread {
        // Add line
        linearLayoutConsole.addView(textView)

        // Scroll
        scrollViewConsole.post {
            scrollViewConsole.fullScroll(
                ScrollView.FOCUS_DOWN
            )
        }
//        }
    }

    fun retry(view: View) {
        // First allow read/write
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)

        // Download, unpack database
        val downloadsOverviewApiUrl =
            "https://api.bitbucket.org/2.0/repositories/dasheenster/nutra-utils/downloads"

        // Make "overview" network call
        var downloadsOverviewStr = String()
        val ovThread = Thread {
            downloadsOverviewStr = URL(downloadsOverviewApiUrl).readText();
        }
        log("GET $downloadsOverviewApiUrl")
        ovThread.start()
        ovThread.join()

        // Find the target file, if it exists
        val downloadsOverview: JSONArray = JSONObject(downloadsOverviewStr)["values"] as JSONArray
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
            val sizeStr = "%.3f".format(size / 1000000.0)
            val downloadUrl = "$downloadsOverviewApiUrl/$targetFileName"

            // Prompt user to download large db file (> 5MB)
            showAlertDialogButtonClicked(view, downloadUrl, nDownloads, creationDateStr, sizeStr)
        }
    }

    private fun showAlertDialogButtonClicked(
        view: View,
        downloadUrl: String,
        nDownloads: Int,
        creationDate: String,
        size: String
    ) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Download large file?")
        val message =
            "Download nutra.db v${Settings.dbTarget}?\n\nSize: ${size}MB\nDownloads: ${nDownloads}\n\nCreated: ${creationDate}"
        builder.setMessage(message)
        builder.setPositiveButton("OK") { _, _ ->
            startDownload(downloadUrl)
        }
        builder.setNegativeButton("Cancel", null)

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun startDownload(url: String) {

    }
}