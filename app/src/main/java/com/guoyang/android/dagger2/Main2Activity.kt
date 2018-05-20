package com.guoyang.android.dagger2

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import javax.inject.Inject

class Main2Activity : AppCompatActivity() {
    @Inject
    lateinit var zoon: Animal

    @Inject
    lateinit var animal: Animal
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        DaggerAnimalComponent.builder().build().inject(this)

        Log.i("Debug",animal.toString())
        Log.i("Debug",zoon.toString())
    }
}
