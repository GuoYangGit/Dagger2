package com.guoyang.android.dagger2

import dagger.Subcomponent

/**
 * Created by guoyang on 2018/5/20.
 * github https://github.com/GuoYangGit
 * QQ:352391291
 */
/**
 * 这里我们使用dependencies来指向要依赖的Component
 * 我们这个MainComponent同样还可以modules指向自己的Modules
 */
@ActivityScope
@Subcomponent
interface MainComponent {
    fun inject(mainActivity: MainActivity)
}