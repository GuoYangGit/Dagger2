package com.guoyang.android.dagger2

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by guoyang on 2018/5/20.
 * github https://github.com/GuoYangGit
 * QQ:352391291
 */
@Singleton
class Animal @Inject constructor(@CatAction private val action: Action) {
    fun call(): String = action.call()
}