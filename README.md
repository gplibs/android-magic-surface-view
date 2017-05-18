# android-magic-surface-view

这是一个 android 动画特效库， 可以实现各种炫酷动画。

_**1. 安装**_

_gradle:_

```Gradle
dependencies {
    compile 'com.gplibs:magic-surface-view:1.1.1'
}
```

<br />

---
_**2. 一些示例效果**_

此文档只做一些简单说明, 具体使用方法还请参考 示例项目.<br/>
示例项目源码: <https://github.com/gplibs/android-magic-surface-view-sample> <br/>
示例项目apk: [Download](https://github.com/gplibs/resources/raw/master/android/magic-surface-view/apk/magic-surface-view-sample-release.apk)

_以下是示例项目中的一些效果 (gif图片帧数低，使用真机运行效果更好):_

启动及退出动画 :<br/>
![image](https://github.com/gplibs/resources/raw/master/android/magic-surface-view/readme/launch.gif) 

模仿MacWindow动画 :<br/>
![image](https://github.com/gplibs/resources/raw/master/android/magic-surface-view/readme/mac_window_anim.gif) 

碎片化曲面动画 :<br/>
![image](https://github.com/gplibs/resources/raw/master/android/magic-surface-view/readme/scrap_anim.gif) 

其他示例效果，可以下载示例项目运行查看。

<br />

---
_**3. 概述**_

一个MagicSurfaceView只能同时渲染一个MagicScene<br/>
一个MagicScene可以包含多个MagicSurface<br/>
一个MagicSurface可以对应一个View或者Bitmap对象<br/>

_场景创建及渲染_

```Java
// 创建一个MagicSurface对象
MagicSurface surface = new MagicSurface(view) // view为要进行动画操作的View
        .setVisible(true)                // 设置模型是否要渲染 (默认为true)
        .setShininess(64)                // 设置模型材质光泽度,默认64; 数值越大越光滑, 只对光照生效,无光照效果可忽略.
        .setGrid(30, 40)                 // 设置网格模型行列数,行列数越多效果越精致,但也更耗性能; 默认 30,30
        .setEnableBlend(true)            // 是否开启混合，为透明对象时需开启，(默认为开启)
        .setEnableDepthTest(true)        // 是否开启深度测试，开启后会按三维坐标正常显示，如果关闭，绘制时将覆盖之前已经绘制的东西，(默认为开启)
        .setModelUpdater(modelUpdater)   // 设置模型更新器, 可以执行顶点坐标及颜色相关动画操作; 详情见 "5. 模型更新器 MagicSurfaceModelUpdater"
        .setMatrixUpdater(matrixUpdater) // 设置矩阵更新器, 可以执行矩阵变换相关动画操作; 详情见 "6. 矩阵更新器 MagicSurfaceMatrixUpdater"
        .drawGrid(false);                // 设置绘制时是否只绘制网络，默认false. (调试动画找问题时可以只画网格，可能有点帮助)

// 创建一个多曲面MagicMultiSurface对象
MagicMultiSurface multiSurface = new MagicMultiSurface(view, 20, 10) // view为要进行动画操作的View， (20，10)表示将曲面分解成 20行 10列 共两百个子曲面
        .setUpdater(mMultiUpdater)       // 设置 MagicMultiSurfaceUpdater 对子曲面进行动画操作 详情见 "7. MagicMultiSurfaceUpdater"
        .setVisible(true)                // 设置模型是否要渲染 (默认为true)
        .setShininess(64)                // 设置模型材质光泽度,默认64; 数值越大越光滑, 只对光照生效,无光照效果可忽略.
        .setEnableBlend(true)            // 是否开启混合，为透明对象时需开启，(默认为开启)
        .setEnableDepthTest(true);       // 是否开启深度测试，开启后会按三维坐标正常显示，如果关闭，绘制时将覆盖之前已经绘制的东西，(默认为开启)

// 创建场景
MagicScene scene = new MagicSceneBuilder(myMagicSurfaceView)
        .addSurfaces(surface, multiSurface) // 添加Surface对象；可以添加多个 如: addSurfaces(surface, surface1, surface2)
        .ambientColor(0XFF222222)           // 设置场景环境光, 默认为0XFFFFFFFF
        .addLights(light)                   // 添加光源对象,类型可以为PointLight或者DirectionalLight; 可以添加多个 如: addLights(light, light1, light2)
        .setUpdater(sceneUpdater)           // 添加场景更新器, 可以执行场景相关变量的动画操作; 详情见 "4. 场景更新器 MagicSceneUpdater"
        .build();
// 渲染
myMagicSurfaceView.render(scene);
```

_模型更新器 MagicSurfaceModelUpdater 动画原理_

MagicSurfaceModelUpdater 原理相对麻烦些，单独说明如下：

渲染使用openGL；每个MagicSurface对象都包含一个SurfaceModel，这个SurfaceModel即是openGl要绘制的曲面模型;<br/>
SurfaceModel中包含着openGL绘制时需要的一些必要元素, 顶点坐标集合、顶点索引集合、法向量集合等;<br/>
构造MagicSurface时传入View后，会转为Bitmap 作为纹理绑定到曲面模型上。<br/>
曲面模型的生成，及纹理绑定过程都由此库自动完成。

我们可以做的是：<br/>
SurfaceModel中的顶点集合，是一个 r(行) * c(列) 的一个矩形网格；<br/>
我们修改这个矩形网格上每个点的属性，渲染效果就会有相应的变化；(网格的点与点之间每像素点的属性，由openGL根据我们设置的网格关键点的属性进行插值计算自动完成)<br/>
网格上每个点可以修改的属性包含: 
1. 顶点坐标。修改网格上某一点的顶点坐标后，绑定在该点上的纹理也会跟着发生相应的形变。<br/>
2. 顶点颜色。修改网格上某一点的顶点颜色后，该点上的最终颜色会按 "_顶点最终颜色计算过程_" 那样进行变化。<br/>

<br/>

_顶点最终颜色计算过程 为:_

**顶点最终颜色 = 原始颜色 * 顶点颜色 * 场景环境光颜色 + 原始颜色 * 顶点颜色 * 灯光颜色**<br/>
原始颜色：构建MagicSurface时传入的View或者Bitmap生成的纹理在对应坐标的颜色值.<br/>
顶点颜色：默认为rgba(1,1,1,1); 可以由 模型更新器 修改.<br/>
场景环境光颜色：默认为rgba(1,1,1,1); 可以由 MagicSceneUpdater 修改.<br/>
灯光颜色：构建MagicScene时传入的灯光对象集合，在模型对应顶点产生的光照颜色值; 如果未设置灯光，灯光颜色值为rgba(0,0,0,0); 可以由 MagicSceneUpdater 修改.

颜色相乘算法为：(其中r,g,b,a都为0 ~ 1的浮点数, 对应整形颜色值0 ~ 255)<br/>
color1(r1, g1, b1, a1) * color2(r2, g2, b2, a2) == color3(r1* r2, g1* g2, b1* b2, a1* a2) <br/>
颜色相加算法为：(其中r,g,b,a都为0 ~ 1的浮点数, 对应整形颜色值0 ~ 255)<br/>
color1(r1, g1, b1, a1) + color2(r2, g2, b2, a2) == color3(r1+r2, g1+g2, b1+b2, a1+a2)<br/>

<br/>

_场景坐标(即openGL坐标):_

跟坐标相关的动画操作都使用场景坐标，与Android的View的坐标系无关<br/>
MagicSurfaceView的中心即为场景坐标原点(0,0,0) x轴向右 y轴向上 z轴向屏幕外 <br/>
MagicSurface网格模型各点坐标z轴默认为0<br/>
MagicSurfaceView 及 MagicSurface 相关点场景坐标获取方法 参考 "_**5. 模型更新器 MagicSurfaceModelUpdater**_"

<br/>

_关于偏移量:_

当一个MagicSurface上同时使用模型更新器和矩阵更新器时.<br />
两个只能有一个应用偏移量，不然位置会出现偏差。
偏移量具体说明，参考 模型更新器 和 矩阵更新器。

<br/>

_Updater性能优化:_

MagicSceneUpdater, MagicSurfaceModelUpdater及MagicSurfaceMatrixUpdater 都为 MagicUpdater 的子类<br/>
当同时有多个MagicUpdater运行时，可以考虑进行分组<br/>
MagicUpdater都有一个可以传入分组参数的构造函数和一个setGroup方法可以对他们进行分组; (setGroup必须要在调用render方法之前执行)<br/>
如果不进行分组，它们会独自开启一线程更新。将一些计算量较少的MagicUpdater分到同一组，他们将在一个线程中执行，这样可以节约资源，提升性能。

<br />

---
_**4. 场景更新器 MagicSceneUpdater**_

MagicSceneUpdater 对场景变量进行修改; 场景变量包含 环境光和光源。

调用过程为 willStart -> didStart -> (update [此部分通过notifyChanged触发，循环调用直到 调用Updater stop方法]) -> didStop

```Java
public class MySceneUpdater extends MagicSceneUpdater {

    public MySceneUpdater(int group) {
        super(group);
    }

    // 在绘制第一帧之前调用 (可以在此方法里进行一些初始化操作)
    @Override
    protected void willStart(MagicScene scene) {
    }

    // 在开始绘制后调用（绘制第一帧后调用，一般动画可以在此开始） 
    // 动画有更新时，需调用 notifyChanged()方法 通知框架可以调用 update 相关方法进行更新。
    @Override
    protected void didStart(MagicScene scene) {
    }

    // 当调用Updater 的 stop() 方法之后，真正停止后会回调此方法
    @Override
    protected void didStop(MagicScene scene) {
    }

    // 更新环境光及灯光
    @Override
    protected void update(MagicScene scene, Vec outAmbientColor) {

        // 修改环境光
        // outAmbientColor.setColor(...)

        // 获取第0个光源并修改 假设是点光源
        // PointLight pl = scene.getLight(0);
        // pl.setColor(...);
        // pl.setPosition(...);

        // 获取第1个光源并修改 假设是方向光源
        // DirectionalLight dl = scene.getLight(1);
        // dl.setColor(...);
        // dl.setDirction(...);

        // 根据需要还可以使用如下方法获取某个MagicSurface对象
        // scene.getSurface(index);
    }
}
```

<br />

---
_**5. 模型更新器 MagicSurfaceModelUpdater**_

MagicSurfaceModelUpdater 对MagicSurface网格模型各顶点坐标及颜色值进行修改，

调用过程为 willStart -> didStart -> (updateBegin -> (updatePosition [遍历网格每个点]) -> updateEnd [此部分通过notifyChanged触发，循环调用直到 调用 Updater stop方法]) -> didStop

```Java
public class MyModelUpdater extends MagicSurfaceModelUpdater {

    public MyModelUpdater() {
    }

    // 在绘制第一帧之前调用 (可以在此方法里进行一些初始化操作)
    @Override
    protected void willStart(MagicSurface surface) {
    }

    // 在开始绘制后调用（绘制第一帧后调用，一般动画可以在此开始） 
    // 动画有更新时，需调用 notifyChanged()方法 通知框架可以调用 update 相关方法进行更新。
    @Override
    protected void didStart(MagicSurface surface) {
    }

    // 当调用 updater stop方法之后，真正停止后会回调此方法
    @Override
    protected void didStop(MagicSurface surface) {
    }

    // 每次顶点更新之前调用
    @Override
    protected void updateBegin(MagicSurface surface) {
    }

    /**
     * 修改网格模型 r行, c列处 的坐标及颜色， 修改后的值存到 outPos 和 outColor
     * (只需要修改网格模型各行各列点及可，点与点之间的坐标和颜色由 openGL 自动进行插值计算完成)
     * 注：此方法执行频率非常高；一般不要有分配新的堆内存的逻辑，会频繁产生gc操作影响性能
     *
     * @param surface
     * @param r 行
     * @param c 列
     * @param outPos 默认值为 r行c列点包含偏移量的原始坐标, 计算完成后的新坐标要更新到此变量
     * @param outColor 默认值为 rgba(1,1,1,1), 计算完成后的新颜色要更新到此变量
     */
    @Override
    protected void updatePosition(MagicSurface surface, int r, int c, Vec outPos, Vec outColor) {
    }

    // 每次所有顶点更新完成后调用
    @Override
    protected void updateEnd(MagicSurface surface) {
        // 有光照效果时，顶点坐标值更新后, 需要更新法向量以得到正常光照效果，但按标准法向量计算方法开销过大，顶点数较少时可以使用
        // surface.getModel().updateModelNormal();

        //可以在此方法里判断动画是否结束，结束需调用 stop()方法，以结束updater.
    }
    
}
```

_MagicSurfaceModelUpdater相关方法说明_

在Updater生命周期内 以下这些方法都是可以有效调用的。

```Java
// 获取 MagicSurfaceView 在openGL坐标系中的宽度
surface.getScene().getWidth();

// 获取 MagicSurfaceView 在openGL坐标系中的高度
surface.getScene().getHeight();

// 获取 MagicSurfaceView 某点在openGL坐标系中的坐标并存入pos
// 比如 surface.getScene().getPosition(0.0f, 0.0f, pos); 获取 MagicSurfaceView 左上角的坐标
// 比如 surface.getScene().getPosition(0.0f, 1.0f, pos); 获取 MagicSurfaceView 左下角的坐标
// 比如 surface.getScene().getPosition(1.0f, 0.0f, pos); 获取 MagicSurfaceView 右上角的坐标
// 比如 surface.getScene().getPosition(1.0f, 1.0f, pos); 获取 MagicSurfaceView 右下角的坐标
surface.getScene().getPosition(ratioX, ratioY, pos);



// 获取 MagicSurface 在openGL坐标系中的宽度
surface.getModel().getWidth();

// 获取 MagicSurface 在openGL坐标系中的高度
surface.getModel().getHeight();

// 获取 MagicSurface 网格模型总行数
surface.getModel().getRowLineCount();

// 获取 MagicSurface 网格模型总列数
surface.getModel().getColLineCount();

// 获取 MagicSurface 网格模型 r行 c列 在openGL坐标系中的坐标并存入pos
// 注: 此方法包含偏移量，模型中心在 场景中心+偏移量 的位置; 偏移量是在创建SurfaceView时根据传入的View与MagicSurfaceView相对位置自动生成.
surface.getModel().getPosition(r, c, pos);

// 获取 MagicSurface 网格模型 r行 c列 在openGL坐标系中的坐标并存入pos
// 注: 此方法不包含偏移量，模型中心在场景中心处
surface.getModel().getPositionExcludeOffset(r, c, pos);

```

<br />

---
_**6. 矩阵更新器 MagicSurfaceMatrixUpdater**_

MagicSurfaceMatrixUpdater 对MagicSurface网格模型进行各种矩阵变换(缩放，旋转，平移).

调用过程为 willStart -> didStart -> (updateMatrix [此部分通过notifyChanged触发，循环调用直到 调用 Updater stop方法]) -> didStop

```Java
public class MyMatrixUpdater extends MagicSurfaceMatrixUpdater {

    public MyMatrixUpdater(int gorup) {
        super(group);
    }

    // 在绘制第一帧之前调用 (可以在此方法里进行一些初始化操作)
    @Override
    protected void willStart(MagicSurface surface) {
    }

    // 在开始绘制后调用;（绘制第一帧后调用，一般动画可以在此开始） 
    // 动画有更新时，需调用 notifyChanged()方法 通知框架可以调用 update 相关方法进行更新。
    @Override
    protected void didStart(MagicSurface surface) {
    }

    // 当调用 updater stop方法之后，真正停止后会回调此方法
    @Override
    protected void didStop(MagicSurface surface) {
    }

    /**
     * 矩阵变换
     * @param surface 需更新的MagicSurface对象
     * @param offset offse为模型相对场景中心的坐标偏移量, 如果不进行 offset 位移， model 就会显示在场景中心；
     *
     *               当使用 View 构造 MagicSurface 时，
     *               View中心位置 相对 MagicSurfaceView中心位置的坐标偏移量 在场景坐标系中的表现就是 offset。
     *
     * @param matrix 矩阵
     */
    @Override
    protected void updateMatrix(MagicSurface surface, Vec offset, float[] matrix) {
        // 重置matrix
        reset(matrix);

        // 缩放 
        scale(matrix, xScale, yScale, zScale);

        // 包含偏移量的位移
        translate(matrix, x + offset.x(), y + offset.y(), z + offset.z());

        // 位移
        // translate(matrix, x, y, z);

        // 绕 axis轴 旋转 angle度。
        rotate(matrix, axis, angle);

        // 注: 当同时有位移和旋转操作时，位移操作要放在前面。
    }
}
```

<br />

_**7. MagicMultiSurfaceUpdater**_

MagicMultiSurfaceUpdater 对 MagicMultiSurface 中每个子模型进行矩阵变换及颜色值修改，

调用过程为 willStart -> didStart -> (updateBegin -> (update [遍历每个子模型]) -> updateEnd [此部分通过notifyChanged触发，循环调用直到 调用 Updater stop方法]) -> didStop

```Java
public class MyMultiSurfaceUpdater extends MagicMultiSurfaceUpdater {

    // 在绘制第一帧之前调用 (可以在此方法里进行一些初始化操作)
    @Override
    protected abstract void willStart(MagicMultiSurface surface) {
    }

    // 在开始绘制后调用（绘制第一帧后调用，一般动画可以在此开始） 
    // 动画有更新时，需调用 notifyChanged()方法 通知框架可以调用 update 相关方法进行更新。
    @Override
    protected abstract void didStart(MagicMultiSurface surface) {
    }

    // 当调用 updater stop方法之后，真正停止后会回调此方法
    @Override
    protected abstract void didStop(MagicMultiSurface surface) {
    }

    // 每次各子模型更新之前调用
    @Override
    protected abstract void updateBegin(MagicMultiSurface surface) {
    }

    /**
     * 修改r行 c列处子模型的矩阵matrix， 及子模型颜色color
     * @param surface
     * @param r 行
     * @param c 列
     * @param matrix 矩阵
     * @param offset 偏移量 (跟MagicSurfaceMatrixUpdater中偏移量一样的意义，只是此处为某个子模型的偏移量)
     * @param color 默认值为 rgba(1,1,1,1), 计算完成后的新颜色要更新到此变量 模型最终颜色计算方法参考 "顶点最终颜色计算过程"
     */
    @Override
    protected abstract void update(MagicMultiSurface surface, int r, int c, float[] matrix, Vec offset, Vec color) {
        // 重置矩阵
        // reset(matrix);

        // 平移
        // translate(matrix, offset);

        // 旋转
        //rotate(matrix, mAxis, angle);

        // 缩放
        //scale(matrix, mScale);

        // 修改颜色
        // color.setColor(xxx);
    }

    // 每次所有子模型更新完成后调用
    @Override
    protected abstract void updateEnd(MagicMultiSurface surface) {
        // 可以在此方法里判断动画是否结束，结束需调用 stop()方法，以结束updater.
    }
}
```


有兴趣的同学可以加qq群 **539614731** 探讨 
