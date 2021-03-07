package com.example.lestutosdeproc

import android.annotation.SuppressLint
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.webkit.WebView
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.proc.lestutosdeproc.R
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {
    private val _tag: String = "Example MainActivity"

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {

        // remove app title
        try {
            this.supportActionBar!!.hide()
        } catch (e: NullPointerException) {
            Log.e(_tag, e.toString())
        }

        // start instance
        super.onCreate(savedInstanceState)
        Log.d(_tag, "Starting instance")

        // make app fullscreen
        val API_VERSION_DEPRECIATED_FLAG_FULLSCREEN = 30
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= API_VERSION_DEPRECIATED_FLAG_FULLSCREEN) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        // default activity main layout
        setContentView(R.layout.activity_main)

        // webview to show processus'peertube
        val myWebView: WebView = findViewById(R.id.procwebview)
        val webSettings = myWebView.settings
        @SuppressLint("SetJavaScriptEnabled")
        webSettings.javaScriptEnabled = true
        myWebView.loadUrl("https://peertube.lestutosdeprocessus.fr/videos/recently-added")

        Log.d("ProcService", "Starting ProcService from MainActivity")
        val jobInfo = JobInfo.Builder(11, ComponentName(this, ProcService::class.java))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPeriodic(TimeUnit.MINUTES.toMillis(15))
            .setPersisted(true)
            .build()
        val scheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        scheduler.schedule(jobInfo)
    }

    // imagebutton for discord app
    fun discordButtonListener(view: View) {
        try {
            val url = "https://discord.gg/JJNxV2h"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(
                applicationContext,
                "There are no web clients installed.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // imagebutton for mail app
    fun emailButtonListener(view: View) {
        val i = Intent(Intent.ACTION_SEND)
        i.type = "message/rfc822"
        i.putExtra(Intent.EXTRA_EMAIL, arrayOf("processus@thiefin.fr"))
        i.putExtra(Intent.EXTRA_SUBJECT, "Email via l'application Android")
        i.putExtra(Intent.EXTRA_TEXT, "")
        try {
            startActivity(Intent.createChooser(i, "Send mail..."))
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(
                applicationContext,
                "There are no email clients installed.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}