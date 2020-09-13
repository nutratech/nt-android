package co.nutra.nt

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.StrictMode
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL


class OnBoardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
    }

    fun quit(view: View) {
        finishAndRemoveTask()
    }

    fun retry(view: View) {
        // First allow read/write
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)

        // Download, unpack database
        val downloadsOverviewApiUrl =
            "https://api.bitbucket.org/2.0/repositories/dasheenster/nutra-utils/downloads"
//        val downloadUrl = "${downloadsOverviewApiUrl}/${targetFileName}"

        val progressBar: ProgressBar = findViewById(R.id.progressBar)
        // Make overview network call
        progressBar.visibility = ProgressBar.VISIBLE;
        val downloadsOverviewStr = URL(downloadsOverviewApiUrl).readText();
        progressBar.visibility = ProgressBar.INVISIBLE

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
                "ERROR: download doesn't exist, contact support: ${targetFileName}",
                Toast.LENGTH_LONG
            ).show()
        else {
            val nDownloads = targetFile["downloads"] as Int
            val creationDateStr = targetFile["created_on"].toString()
            val size = targetFile["size"] as Int
            val sizeStr = "%.3f".format(size / 1000000.0)
            showAlertDialogButtonClicked(view, nDownloads, creationDateStr, sizeStr)
        }
    }

    fun showAlertDialogButtonClicked(
        view: View,
        nDownloads: Int,
        creationDate: String,
        size: String
    ) {        // setup the alert builder
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Download large file?")
        val message =
            "Download nutra.db v${Settings.dbTarget}?\n\nSize: ${size}MB\nDownloads: ${nDownloads}\n\nCreated: ${creationDate}"
        builder.setMessage(message)
        builder.setPositiveButton("OK", null)
        builder.setNegativeButton("Cancel", null)

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }
}