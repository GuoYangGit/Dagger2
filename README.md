> 最近准备用mvvm框架来做一个自己的项目，其中用到了dagger2，发现虽然原来一直再用dagger2，但是再次用到的时候有的地方还是会有些不熟练，所以决定写一篇关于dagger2的文章来记录下。

# Dagger2简介

dagger2是一个基于JSR-330标准的依赖注入框架，在编译期间自动生成代码，负责依赖对象的创建。这里用到了依赖倒置原则(我们应该依赖于抽象，而不应该依赖于实现)。使用dagger2能够更一步的解耦我们的代码。

# 关于依赖倒置以及解耦

上面说了关于dagger2的简介，可能会让人听的有些懵逼，下面我们就拿一个例子来具体讲解一下。

```kotlin
class Dog {
    fun call():String = "汪汪"
}
```

```kotlin
class Animal(private val dog: Dog) {
    fun call(): String = dog.call()
}
```

```
class MainActivity : AppCompatActivity() {
    private lateinit var zoon: Animal

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val dog = Dog()
        zoon = Animal(dog)
        toast(zoon.call())
    }
}
```

从上面代码我们可以看出来，我们定义了个Dog类和Animal类，Animal类需要传入Dog，然后call()方法调用了Dog类的call()方法。那么我们想使用的时候就需要使用Animal类的方法就先要去new一个Dog类。这样的写法就有了耦合性，Animal类如果构造方法的改变，我们可能还需要再调用类里面再去new。每次改动Animal类我们就必须改动MainActivity。其实这样的依赖层级还算少的，如果多了呢。比如A依赖于B，B又依赖于C，D。我们使用的时候还需要按照顺序去创建类。C/D->B-A。dagger2的出现就帮我们很好的解决了这个问题。接下来我们就使用dagger2来解决这个new的问题

# @Inject和@Component

我们直接来改造下我们的代码.

```
class Dog @Inject constructor() {
    fun call(): String = "汪汪"
}
```

```
class Animal @Inject constructor(private val dog: Dog) {
    fun call(): String = dog.call()
}
```

```
@Component
interface MainComponent {
    fun inject(mainActivity: MainActivity)
}
```

```
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var zoon: Animal

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        DaggerMainComponent.create().inject(this)
        toast(zoon.call())
    }
}
```

这里我们用@Inject注解来修饰了Dog类和Animal类的构造方法。新创建了一个MainComponent类来做桥梁(@Component注解来修饰这是一个桥梁接口)，fun inject(mainActivity: MainActivity)来表示我们要注入到MainAcitivty类。在我们MainActivity中DaggerMainComponent.create().inject(this)来进行与桥梁的链接。并且用@Inject注解来修饰我们要用到的Animal类。这样就省去了我们new Dog类和new Animal类的代码。听到这里肯定很多人就懵逼了，那具体是怎么实现的呢。DaggerMainComponent这个类我们没有创建啊。源码待会分析，现在先来了解下@Inject和@Component两个API，想要使用Dagger2进行依赖注入，至少要使用到这两个注解。
@Inject用于标记需要注入的依赖，或者标记用于提供依赖的方法。
@Component则可以理解为注入器，在注入依赖的目标类MainActivity使用Component完成注入。接下来我们去看看Dagger2帮我们生成的代码。代码在app->build->generated->source->kapt->debug下面。Dagger帮我们生成了以下几个类：

1. Animal_Factory
2. Dog_Factory
3. MainActivity_MembersInjector
4. DaggerMainComponent

我们先来看看我们MainActivity中调用的DaggerMainComponent这个类。

```
/**
 * DaggerMainComponent实现了MainComponent接口。并且使用了Builder建造者模式
 */
public final class DaggerMainComponent implements MainComponent {
  /**
   * 这里私有化了自己的构造方法
   * @param builder
   */
  private DaggerMainComponent(Builder builder) {}

  /**
   * 这里创建了一个内部Builder类
   * @return
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * 这里将new Builder().build()简化成了create()方法
   * @return
   */
  public static MainComponent create() {
    return new Builder().build();
  }

  /**
   * 这里提供了我们需要的Animal类
   * @return
   */
  private Animal getAnimal() {
    return new Animal(new Dog());
  }

  /**
   * 实现了MainComponent接口的inject注入方法
   * @param mainActivity 注入的目标类
   */
  @Override
  public void inject(MainActivity mainActivity) {
    //调用了下面的injectMainActivity()方法
    injectMainActivity(mainActivity);
  }

  /**
   * inject具体实现
   * @param instance
   * @return
   */
  private MainActivity injectMainActivity(MainActivity instance) {
    //这里调用了MainActivity_MembersInjector类的方法,并且将MainActivity的实例和Animal实例传入了进去
    MainActivity_MembersInjector.injectZoon(instance, getAnimal());
    return instance;
  }

  /**
   * 内部Builder类
   */
  public static final class Builder {
    private Builder() {}

    /**
     * 这里创建了我们的DaggerMainComponent类
     * @return
     */
    public MainComponent build() {
      return new DaggerMainComponent(this);
    }
  }
}
```
注释我都写清楚了，我们最终发现它调用了MainActivity_MembersInjector.injectZoon(instance, getAnimal())方法。那么我们去MainActivity_MembersInjector来看一下具体实现。

```
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
    private final Provider<Animal> zoonProvider;

    public MainActivity_MembersInjector(Provider<Animal> zoonProvider) {
        this.zoonProvider = zoonProvider;
    }

    public static MembersInjector<MainActivity> create(Provider<Animal> zoonProvider) {
        return new MainActivity_MembersInjector(zoonProvider);
    }

    @Override
    public void injectMembers(MainActivity instance) {
        injectZoon(instance, zoonProvider.get());
    }

    /**
     * 调用了这里的方法
     *
     * @param instance MainActivity
     * @param zoon     Animal
     */
    public static void injectZoon(MainActivity instance, Animal zoon) {
        //我们这里发现将DaggerMainComponent.getAnimal()得到的Animal实例赋值给了MainActivity.zoon属性
        //所以这里我们也可以得到为什么@Inject修饰的属性不能被私有化
        instance.zoon = zoon;
    }
}
```

通过这里我们就可以清晰的看出来Dagger2是如何帮我们进行注入的，那肯定有人会问了，Dog_Factory类和Animal_Factory没有用到啊(但是注意的是，没有这个依赖工厂，我们在build项目的时候会报错)。这里没有用到这两个依赖的工厂我也不清楚，哪位大佬可以指出说明下。但是后面一定会用到的。既然类编译出来了，我们还是顺带看一下吧。

```
/**
 * Animal_Factory实现了Factory接口,并将自己的范型传了过去
 */
public final class Animal_Factory implements Factory<Animal> {
  //这里定义了个Dog类型的Provider实例，这里的疑惑我们一会后面在看
  private final Provider<Dog> dogProvider;

  /**
   * Animal_Factory 构造方法
   * @param dogProvider Dog类型的Provider实例进行赋值
   */
  public Animal_Factory(Provider<Dog> dogProvider) {
    this.dogProvider = dogProvider;
  }

  /**
   * 实现了接口的get方法，并且创建了一个Animal的实例
   * @return Animal
   */
  @Override
  public Animal get() {
    //我们知道Animal类的创建需要Dog的实例，这里在new Animal的时候传入了dogProvider.get()
    //那么我们大概可以猜到Provider.get()方法是返回的范型的实例
    return new Animal(dogProvider.get());
  }

  /**
   * create静态方法，调用自己的构造方法创建了一个Animal_Factory实例
   * @param dogProvider Dog类型的Provider实例
   * @return Animal_Factory
   */
  public static Animal_Factory create(Provider<Dog> dogProvider) {
    return new Animal_Factory(dogProvider);
  }
}
```
接下来我们再去看看Dog_Factory类

```
/**
 * 这里同样是实现了Factory接口，并定义了Dog类型
 */
public final class Dog_Factory implements Factory<Dog> {
  private static final Dog_Factory INSTANCE = new Dog_Factory();

  /**
   * 同样的实现了get方法，返回了Dog的实例
   * @return
   */
  @Override
  public Dog get() {
    return new Dog();
  }

  public static Dog_Factory create() {
    return INSTANCE;
  }
}
```

既然两个依赖工厂都实现了Factory接口，那我们去看看Factory究竟是个什么东东。

```
public interface Factory<T> extends Provider<T> {
}
```

这里我们发现它继承了Provider接口，在Animal_Factory类中我们是不是见过这个Provider。我们继续点击进去看看Provider接口。

```
public interface Provider<T> {
    T get();
}
```
这里我们就清楚了，XXX_Factory最终实现的是Provider接口，并且在get方法中将需要提供的依赖创建了出来。接下来我们说下@Module这个注解。

# @Module和@Provides

如果没有接触过dagger2的同学肯定会问@Module是干啥用的，@Provides又是啥，这个嘛，建议你问问身边会dagger2的同学好吧(Ps:开个玩笑)。在上面的Dog类和Animal类都是我们自己定义的类，而且可以在构造方法上加上@Inject注解标识需要注入的依赖，那么现在问题来了，如果我们需要的是第三方库，或者是使用依赖倒置原则，需要的是抽象类或者接口呢。@Inject也无法使用，因为抽象的类并不能实例化。接下来我们就举个例子好吧。

```
interface Action {
    fun call():String
}
```

```
class Dog @Inject constructor() : Action {
    override fun call(): String = "汪汪"
}
```

```
class Animal @Inject constructor(private val action: Action) {
    fun call(): String = action.call()
}
```
我们这里新创建了一个接口Action，然后让Dog实现了该接口，在Animal的构造方法我们将Action接口传入了进去。然后我们build一下项目，会发现报错了，``Action cannot be provided without an @Provides-annotated method.``(Action不能提供依赖，因为它没有一个@注解可以提供)。那么这怎么办呢，别着急，我们可以使用@Module和@Provides。直接啪啪啪键盘上代码。

```kotlin
/**
* 这里我们创建了一个MainModule类，并且用@Module标示它是一个提供依赖的类
*/
@Module
class MainModule {
	/**
	* 这里用@Provides注解来修饰我们要提供的依赖Action
	* 因为Dog类的构造方法我们用@Inject修饰过，所以我们这里不去直接创建Dog类。
	*/
    @Provides
    fun provideAction(dog: Dog): Action = dog
}
```

```
/**
* 我们改造下原先的MainComponent,使用modules = [(MainModule::class)]来指向我们需要提供modules是哪个类
*/
@Component(modules = [(MainModule::class)])
interface MainComponent {
    fun inject(mainActivity: MainActivity)
}
```
这样我们完了在build项目进行run，发现结果也已经出来了。那module是如何提供依赖的呢，我们还是可以去dagger2给我们生成的代码中去看看。

```
public final class DaggerMainComponent implements MainComponent {
  //这里定义了我们需要的MainModule
  private MainModule mainModule;
  
  private DaggerMainComponent(Builder builder) {
    //这里调用了initialize，并将内部类Builder传入了进去
    initialize(builder);
  }
  
  ...
	
  /**
   * 这里调用了新创建的MainModule_ProvideActionFactory类proxyProvideAction去获取Action
   * 并将mainModule和我们需要的Dog类传入了进去
   * @return Action
   */
  private Action getAction() {
    return MainModule_ProvideActionFactory.proxyProvideAction(mainModule, new Dog());
  }

  /**
   * 获取我们需要的Animal依赖
   * @return
   */
  private Animal getAnimal() {
    //这里调用了getAction()去获取Action接口
    return new Animal(getAction());
  }

  /**
   * 这里将Builder中的mainModule实例赋值给了DaggerMainComponent.mainModule
   * @param builder
   */
  @SuppressWarnings("unchecked")
  private void initialize(final Builder builder) {
    this.mainModule = builder.mainModule;
  }
	
	...
	
  public static final class Builder {
    private MainModule mainModule;

    private Builder() {}

    /**
     * 这里在Builder内部类构建DaggerMainComponent时创建了MainModule类并赋值给了Builder.mainModule
     * @return
     */
    public MainComponent build() {
      if (mainModule == null) {
        this.mainModule = new MainModule();
      }
      return new DaggerMainComponent(this);
    }
	...
  }
}
```
为了看的清晰，我省略了一些没有必要的方法，从上面我们可以看出来获取我们需要的Action类，最终是去MainModule_ProvideActionFactory的proxyProvideAction方法完成的，那么我们就去看看这次新创建的这个MainModule_ProvideActionFactory类。

```
public final class MainModule_ProvideActionFactory implements Factory<Action> {
	...这里的代码可以先省略不看

  /**
   * 调用了这个方法
   * @param instance 我们创建的MainModule
   * @param dog MainModule.provideAction方法需要的参数
   * @return Action
   */
  public static Action proxyProvideAction(MainModule instance, Dog dog) {
    //我们这里发现它实际上还是调用了传入的MainModule.provideAction返回了我们需要的Action
    return Preconditions.checkNotNull(
        instance.provideAction(dog), "Cannot return null from a non-@Nullable @Provides method");
  }
}
```

从上面这一套代码的执行我们可以清晰的看出来，当我们无法对第三方库的构造方法进行改造，或者需要抽象类/接口注入时，我们可以使用@Module来进行提供，而@Provides注解则是标示我们需要提供这个依赖。同时我们还需要在@Component桥梁类中去指定相对应的Module类。接下来我们来讲解下dagger2其他一些注解符号的作用。

# @Qualifier和@Named

其实这两个注解符号都是起到一个标示的作用，比如我们需要注入的是一个抽象类，而我们具体的实现有多个，那么dagger2就会迷失方法，不知道我们具体注入的哪个对应的是哪个，这里就需要用到标识符。举个例子：

```
class Cat @Inject constructor() : Action {
    override fun call(): String = "喵～"
}
```

```
@Module
class MainModule {
    @Provides
    fun provideDog(dog: Dog): Action = dog

    @Provides
    fun provideCat(cat: Cat): Action = cat
}
```
我们这里创建了一个cat类，它的功能和dog类都是一样的，然后我们在Module类里面同样提供cat的依赖，同样我们build一下项目，我们会发现编译不通过，dagger2会提醒我们Action is bound multiple times(动作被绑定了多次)，那么我们试着用@Named符号来操作一波，又是一顿啪啪啪(键盘声)。

```
@Module
class MainModule {
    @Named("dog")
    @Provides
    fun provideDog(dog: Dog): Action = dog

    @Named("cat")
    @Provides
    fun provideCat(cat: Cat): Action = cat
}
```

```
class Animal @Inject constructor(@Named("cat") private val action: Action) {
    fun call(): String = action.call()
}
```

然后我们再次编译项目，进行运行，发现结果成功输出了“喵～”，这里dagger2编译出来的代码就不带大家看了。  
@Qualifier的作用和@Named是完全一样的，@Qualifier不是直接注解在属性上的，而是用来自定义注解的。@Named其实也是一个自定义的@Qualifier，我们可以从@Named注解中的代码可以看出来。

```
@Qualifier
@Documented
@Retention(RUNTIME)
public @interface Named {

    /** The name. */
    String value() default "";
}
```

那么接下来我们自定义两个@Qualifier来使用下。

```
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface CatAction {
}
```

```
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface DogAction {
}
```

```
/**
*
*/原来的@Named替换成自己创建的注解
@Module
class MainModule {
    @DogAction
    @Provides
    fun provideDog(dog: Dog): Action = dog

    @CatAction
    @Provides
    fun provideCat(cat: Cat): Action = cat
}
```

```
/**
* 这里我们也进行替换下
*/
class Animal @Inject constructor(@CatAction private val action: Action) {
    fun call(): String = action.call()
}
```
最终结果输出与上面一样。

# @Component的dependence和@SubComponent

其实Component可以依赖于其他的Component，可以使用@Component的dependence属性或者使用@SubComponent来进行实现。比如我们的Repertory初始化，图片加载框架初始化，都需要在AppComponent中来进行，而我们的ActivityComponent中也需要去请求数据和图片，那么我们的ActivityComponent/FragmentComponent就需要依赖与AppComponent。接下来我们还是通过代码来看下实现吧。

```
/**
* 这里我将MainModule改成了AnimalModule
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
```

```
@Component(modules = [(AnimalModule::class)])
interface AnimalComponent {
    //这里我们需要将Action这个接口的依赖提供出去。
    //还记得上节的@Qualifier标示符么，如果同一个依赖，我们这里也需要进行标示
    @DogAction
    fun getDog(): Action

    @CatAction
    fun getCat(): Action
}
```

```
/**
 * 这里我们使用dependencies来指向要依赖的Component
 * 我们这个MainComponent同样还可以modules指向自己的Modules
 */
@Component(dependencies = [(AnimalComponent::class)])
interface MainComponent {
    fun inject(mainActivity: MainActivity)
}
```

```
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var zoon: Animal

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //初始化DaggerMainComponent
        //我们这里需要将DaggerAnimalComponent的实例传入进来
        DaggerMainComponent.builder().animalComponent(DaggerAnimalComponent.create()).build().inject(this)
        toast(zoon.call())
    }
}
```

我们这里还是来看看Dagger2编译出来的代码。  

```
public final class DaggerMainComponent implements MainComponent {
  //这里便是我们要依赖的AnimalComponent
  private AnimalComponent animalComponent;
	
	...
	
  private Animal getAnimal() {
    return new Animal(
    	//这里我们发现Animal需要的Action是从AnimalComponent.getCat()获取到的
        Preconditions.checkNotNull(
            animalComponent.getCat(), "Cannot return null from a non-@Nullable component method"));
  }

	...

  public static final class Builder {
  	
    private AnimalComponent animalComponent;

    private Builder() {}

    public MainComponent build() {
      if (animalComponent == null) {
        throw new IllegalStateException(AnimalComponent.class.getCanonicalName() + " must be set");
      }
      return new DaggerMainComponent(this);
    }
	
	/**
	* 我们从这里知道了为什么我们在MainActivity中构建DaggerMainComponent需要传入AnimalComponent
	*/
    public Builder animalComponent(AnimalComponent animalComponent) {
      this.animalComponent = Preconditions.checkNotNull(animalComponent);
      return this;
    }
  }
}
```

``DaggerAnimalComponent``类

```
public final class DaggerAnimalComponent implements AnimalComponent {
  private AnimalModule animalModule;
	....
	
  /**
   * 这里去AnimalModule中获取我们需要的Action/Dog
   * @return
   */
  @Override
  public Action getDog() {
    return AnimalModule_ProvideDogFactory.proxyProvideDog(animalModule, new Dog());
  }

  /**
   * 这里去AnimalModule中获取我们需要的Action/Cat
   * @return
   */
  @Override
  public Action getCat() {
    return AnimalModule_ProvideCatFactory.proxyProvideCat(animalModule, new Cat());
  }

	...
}

```

我们从上面代码可以看出来，MainComponent依赖于AnimalComponent，实际上就是将AnimalComponent的实例传入了MainComponent。那么我们再来看看Subcomponent的具体用法。接下来我们改造一下代码。

```
@Component(modules = [(AnimalModule::class)])
interface AnimalComponent {
    fun plus():MainComponent
}
```

```
/**
* 原来这里的@Component注解改成@Subcomponent注解
*/
@Subcomponent
interface MainComponent {
    fun inject(mainActivity: MainActivity)
}
```

```
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var zoon: Animal

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //我们这里通过DaggerAnimalComponent去进行构建
        DaggerAnimalComponent.builder().build().plus().inject(this)
        toast(zoon.call())
    }
}
```

我们从上面代码看出来主要就是将MainComponent上的@Component注解替换成了@SubComponent注解，在MainActivity中我们原先是通过DaggerMainComponent进行注入的，现在是通过DaggerAnimalComponent进行注入，我们来看下DaggerAnimalComponent的代码。

```
public final class DaggerAnimalComponent implements AnimalComponent {
  	
  	...
  	
  /**
   * 这里我们可以看到通过DaggerAnimalComponent实现AnimalComponent的plus()方法
   * 创建了一个MainComponentImpl类
   * @return
   */
  @Override
  public MainComponent plus() {
    return new MainComponentImpl();
  }

	...

  /**
   * MainComponentImpl是DaggerAnimalComponent的内部类，实现了我们MainComponent接口
   */
  private final class MainComponentImpl implements MainComponent {
    private MainComponentImpl() {}

    /**
     * 这里提供Animal类，并通过DaggerAnimalComponent.getCatActionAction()提供module里的Action给Animal
     * @return
     */
    private Animal getAnimal() {
      return new Animal(DaggerAnimalComponent.this.getCatActionAction());
    }

    /**
     * 这里将Component注入到了MainActivity
     * @param mainActivity
     */
    @Override
    public void inject(MainActivity mainActivity) {
      injectMainActivity(mainActivity);
    }

    /**
     * 这个方法其实就是将MainActivity类@Inject修饰的成员量进行赋值
     * @param instance
     * @return
     */
    private MainActivity injectMainActivity(MainActivity instance) {
      MainActivity_MembersInjector.injectZoon(instance, getAnimal());
      return instance;
    }
  }
}
```

Component dependencies和Subcomponent使用上的总结  
>- Component Dependencies：
	1. 你想保留独立的想个组件（Flower可以单独使用注入，Pot也可以）
	2. 要明确的显示该组件所使用的其他依赖
- Subcomponent：
	1. 两个组件之间的关系紧密
	2. 你只关心Component，而Subcomponent只是作为Component的拓展，可以通过Component.xxx调用。

# @Scope和@Singleton

@Scope是用来管理依赖的生命周期。@Singleton则是@Scope的默认实现。我们可以通过@Singleton和自定义的@Scope来实现提供依赖的单例模式。

```
@Scope
@Documented
@Retention(RUNTIME)
public @interface Singleton {}
```
我们可以再来用代码验证一下。先写一段没有@Singleton标注的代码。

```
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var zoon: Animal

    @Inject
    lateinit var animal: Animal

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        DaggerAnimalComponent.builder().build().plus().inject(this)
        Log.i("Debug",animal.toString())
        Log.i("Debug",zoon.toString())
    }
}
```
打印结果

```
05-20 07:47:28.151 13684-13684/com...Animal@c5b73ac
05-20 07:47:28.152 13684-13684/com...Animal@336d875
```

我们来通过Dagger2生成的代码具体来看一下吧。

```
 private final class MainComponentImpl implements MainComponent {
        private MainComponentImpl() {
        }

        /**
         * 这里对Animal进行了初始化
         *
         * @return
         */
        private Animal getAnimal() {
            return new Animal(DaggerAnimalComponent.this.getCatActionAction());
        }

        @Override
        public void inject(MainActivity mainActivity) {
            injectMainActivity(mainActivity);
        }

        /**
         * MainActivity所需要的Animal是从这里赋值过去的
         *
         * @param instance
         * @return
         */
        private MainActivity injectMainActivity(MainActivity instance) {
            //这里调用getAnimal()来获取Animal的实例，但你会发现每次调用getAnimal()都会去new Animal()
            MainActivity_MembersInjector.injectZoon(instance, getAnimal());
            MainActivity_MembersInjector.injectAnimal(instance, getAnimal());
            return instance;
        }
    }   
```

接下来我们用@Singleton来进行注入对象的单例模式实现。

```
/**
* 因为我们activity中需要的Animal是从这里提供依赖的，所以我们将@Singleton注解加入到这里
*/
@Singleton
class Animal @Inject constructor(@CatAction private val action: Action) {
    fun call(): String = action.call()
}
```
同样我们的AnimalComponent也需要加上注解，别问我为什么，你build一下编译器就会告诉你答案。

```
@Singleton
@Component(modules = [(AnimalModule::class)])
interface AnimalComponent {
	//这里为了方便演示，我们先直接将AnimalComponent注入到MainActivity中
    //    fun plus():MainComponent
    fun inject(mainActivity: MainActivity)
}
```

```
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var zoon: Animal

    @Inject
    lateinit var animal: Animal

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        DaggerAnimalComponent.builder().build().inject(this)
        Log.i("Debug",animal.toString())
        Log.i("Debug",zoon.toString())
    }
}
```

其实MainActivity并没有进行代码改动，我们再来打印下值。

```
05-20 08:09:38.963 23974-23974/com...Animal@c5b73ac
05-20 08:09:38.963 23974-23974/com...Animal@c5b73ac
```

我们同样可以去看看dagger2帮我们实现的代码。

```
public final class DaggerAnimalComponent implements AnimalComponent {
  private AnimalModule_ProvideCatFactory provideCatProvider;

  private Provider<Animal> animalProvider;

  private DaggerAnimalComponent(Builder builder) {
    //进行注入参数的初始化
    initialize(builder);
  }

  /**
   * 在这里进行了Action和Animal的初始化
   * @param builder
   */
  @SuppressWarnings("unchecked")
  private void initialize(final Builder builder) {
    this.provideCatProvider =
        AnimalModule_ProvideCatFactory.create(builder.animalModule, Cat_Factory.create());
    this.animalProvider = DoubleCheck.provider(Animal_Factory.create(provideCatProvider));
  }

  /**
   * 这里给MainActivity赋值的时候取的是Provider里面注入类的两个实例
   * @param instance
   * @return
   */
  private MainActivity injectMainActivity(MainActivity instance) {
    MainActivity_MembersInjector.injectZoon(instance, animalProvider.get());
    MainActivity_MembersInjector.injectAnimal(instance, animalProvider.get());
    return instance;
  }
```

从上面代码中我们可以看出来注入的依赖实现直接放到了DaggerAnimalComponent构造方法初始化中。接下来我们将AnimalComponent刚才的注释放开，我们还是让MainComponent依赖AnimalComponent，然后还是在MainComponent进行注入。为了保证单例，我们也在MainComponent的类上加入@Singleton注解，我们进行build项目，发现编译不通过。编译器提示我们``MainComponent has conflicting scopes:
public abstract interface AnimalComponent {.AnimalComponent also has @Singleton}``,主组建与公共组件有冲突scope为@Singleton。既然编译器都提示的这么明显了，那么我们就自定义一个@Scope组件吧。

```
@Scope
@Retention(RetentionPolicy.RUNTIME)
public @interface ActivityScope {
}
```

将创建的@ActivityScope注解用于@MainComponent上面。

```
@ActivityScope
@Subcomponent
interface MainComponent {
    fun inject(mainActivity: MainActivity)
}
```

我们再次编译运行项目，发现编译通过了，并且输出的值也一样。同样@Scope可以使用在@Module类的@Provides修饰的提供的依赖上面。这里我就不进行演示了。这里我们可以对@Scope进行总结一下。

> Scope是用来给开发者管理依赖的生命周期的，它可以让某个依赖在Component中保持 “局部单例”（唯一），如果将Component保存在Application中复用，则可以让该依赖在app中保持单例。如果依赖的Component有@Scope标示，那么该主Component也需要使用@Scope进行标示，并且命名与依赖的Component的@Scope不能一样。同级的Component的单例只能保持在注入的类中，这句话可能有点难以理解。

打个比方：

```
@Singleton
@Component(modules = [(AnimalModule::class)])
interface AnimalComponent {
    //    fun plus(): MainComponent
    fun inject(mainActivity: MainActivity)

    fun inject(main2Activity: Main2Activity)

}

```

```
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
```

```
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
```

打印出来的值：

```
05-20 08:39:44.994 14411-14411/com...Animal@9dc2899 ->MainActivity
05-20 08:39:44.996 14411-14411/com...Animal@9dc2899 ->MainActivity
05-20 08:39:48.067 14411-14411/com...Animal@b790c40 ->Main2Activity
05-20 08:39:48.067 14411-14411/com...Animal@b790c40 ->Main2Activity
```

所以说同级注入的类中，单例只能保持在每个注入的类中，而不能在注入的所有的类中保持单例。其实从dagger生成的代码中我们就可以看出其原因。

```
public final class DaggerAnimalComponent implements AnimalComponent {
  private AnimalModule_ProvideCatFactory provideCatProvider;

  private Provider<Animal> animalProvider;

  /**
   * 在构造方法中创建了Animal
   * @param builder
   */
  private DaggerAnimalComponent(Builder builder) {
    initialize(builder);
  }
	...

  @SuppressWarnings("unchecked")
  private void initialize(final Builder builder) {
    this.provideCatProvider =
        AnimalModule_ProvideCatFactory.create(builder.animalModule, Cat_Factory.create());
    this.animalProvider = DoubleCheck.provider(Animal_Factory.create(provideCatProvider));
  }
	...

  public static final class Builder {
    ...
    /**
     * 每次build都会创建一个新的DaggerAnimalComponent，都会执行它都构造方法
     * @return
     */
    public AnimalComponent build() {
      if (animalModule == null) {
        this.animalModule = new AnimalModule();
      }
      return new DaggerAnimalComponent(this);
    }
	...
  }
}
```

---
到这里关于dagger2的整体使用讲解就说完了。