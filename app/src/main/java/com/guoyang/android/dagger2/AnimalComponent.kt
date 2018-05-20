package com.guoyang.android.dagger2

import dagger.Component
import javax.inject.Singleton

/**
 * Created by guoyang on 2018/5/20.
 * github https://github.com/GuoYangGit
 * QQ:352391291
 */
@Singleton
@Component(modules = [(AnimalModule::class)])
interface AnimalComponent {
    //    fun plus(): MainComponent
    fun inject(mainActivity: MainActivity)

    fun inject(main2Activity: Main2Activity)

}