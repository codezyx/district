# 高德地图-地址区划同步

## 部署用法
1 - 配置数据库，执行db.**.script.sql 创建表结构

2 - cd 到项目目录，执行打包命令 
```java 
gradlew distZip
```
3 - 进入 ./build/distributions/district-1.0.0/bin/
执行 
```java 
district|district.sh
```

## Windows下增加CMD窗口标题

1.在第二行非注释行添加
if "%TITLE%" == "" set TITLE=District

2.找到"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %DISTRICT_OPTS%  -classpath "%CLASSPATH%" com.nosoft.district.App %CMD_LINE_ARGS%
在前面添加
start "%TITLE%"

## License
```
The MIT License (MIT)

Copyright (c) 2016 Adriel Café

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
```
