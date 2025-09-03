配置java和maven

* 下载java和maven的tar包, 解压后把PATH和CLASSPATH设置到相应位置.
* 配置maven中国大陆镜像. 修改 `maven解压目录/conf/settings.xml` 中的 `<mirrors>` 部分, 在其中添加如下内容.

```
    <mirror>
      <id>aliyun-maven</id>
      <name>aliyun-maven</name>

      <!-- 只镜像中央仓库 -->
      <mirrorOf>central</mirrorOf>
      <url>https://maven.aliyun.com/repository/central</url>

      <!-- 镜像所有仓库 -->
      <!--<mirrorOf>*</mirrorOf>-->
      <!--<url>https://maven.aliyun.com/repository/public</url>-->
    </mirror>
```
* 如果新装vscode java插件, 由于 `vscode-server` 相关进程看到的还是旧的环境变量, 因此需重启vscode.

规范项目代码的格式

```
mvn spotless:apply
```

编译项目代码

```
clear && mvn clean compile
```

执行一个测试用例

```
clear && mvn test -Dtest=类名#方法名
```

执行一个主类

```
clear && mvn exec:java -Dexec.mainClass="主类的完全限定类名" -Dexec.args="命令行参数"
```

比如, 执行 server

```
clear && mvn exec:java -Dexec.mainClass="com.cregis.svarog.MpcSessionManagerServer"
```

在vscode里如何生成java的方法存根?

1. 单击并悬浮在类名上.
2. 按键 `Ctrl+.`, 这会呼出一个菜单.
3. 单击菜单中的 `Override/implement Methods...`, 这会弹出一个悬浮窗口供我选择函数名.
4. 选择要实现的方法, 确认.