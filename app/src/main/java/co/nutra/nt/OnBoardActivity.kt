package co.nutra.nt

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class OnBoardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun quit(view: View) {
        finishAndRemoveTask();
    }
}