package com.guoyang.android.dagger2

import dagger.Module
import dagger.Provides

/**
 * Created by guoyang on 2018/5/20.
 * github https://github.com/GuoYangGit
 * QQ:352391291
 */
@Module
class AnimalModule {

    @DogAction
    @Provides
    fun provideDog(dog: Dog): Action = dog

    @CatAction
    @Provides
    fun provideCat(cat: Cat): Action = cat
}