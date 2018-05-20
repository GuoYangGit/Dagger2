package com.guoyang.android.dagger2

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import javax.inject.Inject

class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var zoon: Animal

    @Inject
    lateinit var animal: Animal

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        DaggerAnimalComponent.builder().build().inject(this)
        Log.i("Debug", animal.toString())
        Log.i("Debug", zoon.toString())
        (findViewById<Button>(R.id.btn)).setOnClickListener {
            startActivity(Intent(this, Main2Activity::class.java))
        }
    }
}