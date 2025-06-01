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

在vscode里如何生成java的方法存根?

1. 单击并悬浮在类名上.
2. 按键 `Ctrl+.`, 这会呼出一个菜单.
3. 单击菜单中的 `Override/implement Methods...`, 这会弹出一个悬浮窗口供我选择函数名.
4. 选择要实现的方法, 确认.