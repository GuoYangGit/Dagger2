package com.guoyang.android.dagger2

import javax.inject.Inject

/**
 * Created by guoyang on 2018/5/20.
 * github https://github.com/GuoYangGit
 * QQ:352391291
 */
class Dog @Inject constructor() : Action {
    override fun call(): String = "汪汪"
}