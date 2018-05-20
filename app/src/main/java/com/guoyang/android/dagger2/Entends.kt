package com.guoyang.android.dagger2

import android.app.Activity
import android.widget.Toast

/**
 * Created by guoyang on 2018/5/19.
 * github https://github.com/GuoYangGit
 * QQ:352391291
 */
fun Activity.toast(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}