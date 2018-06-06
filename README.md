# 高德地图-行政区域 Rest API
提供行政区域查询 Rest 服务，定时同步[高德地图开放平台](http://lbs.amap.com/)提供的[行政区域](http://lbs.amap.com/api/webservice/guide/api/district)数据，保存到本地数据库，双表备份，保证同步期间不间断本地服务。
## 依赖
* [Vert.x](https://github.com/eclipse/vert.x)
* [Netty](https://github.com/netty/netty)
* [Log4j2](https://github.com/apache/logging-log4j2)

## 部署用法
1 - 本项目使用 PostgreSQL 数据库，执行 db.postgresql.scripts.sql 创建表结构

2 - 修改配置文件
```java
/src/main/resources/app.json
```
```java
api.param 中的 key 修改成自己在高德地图开放平台中申请的 key
host 配置 PostgreSQL 数据库的 IP
port 配置 PostgreSQL 数据库的端口
database 配置 PostgreSQL 数据库
username 和 password 配置数据库用户名和密码
```
通常配置完上面内容就可以打包运行了，下面给出其他配置说明：
```java
http.port 配置服务监听端口
job.period 定时同步时间间隔，单位毫秒(ms)
api.host 高德地图api域名
api.port 高德地图api端口
api.uri 高德地图api资源路径
api.param 高德地图api参数
maxPoolSize 数据库链接池最大链接数
```

3 - cd 到项目目录，执行打包命令 
```java 
gradlew installDist
```
成功打包会在当前目录生成 ./build/install/district/ 
目录结构说明：
* district/bin/ 可执行脚本目录
* district/conf/ 配置文件目录
* district/lib/ 依赖库文件(jar)目录
* district/logs/ 日志文件目录

4 - 进入 ./build/install/district/bin/
Windows系统双击或者CMD下运行
```java
district.bat
```
Linux系统调试时执行 
```java 
./district
```
Linux系统后台运行时执行
```java
nohup ./district&
```
5 - 访问 Rest 服务
查询某地行政区域：
```java
http://yourip:port/district/api/index?code=110100
```
* port 默认8090，如果是本机调试可以访问 http://localhost:8090/district/api/index?code=110100
* code 参数是上级区划编码，如果不传默认是返回全国所有省、直辖市和自治区
* JSON返回值，以北京城区(110100)为上级查询为例：
```java
{
  "result" : [ {
    "adcode" : "110101",
    "name" : "东城区"
  }, {
    "adcode" : "110102",
    "name" : "西城区"
  }, {
    "adcode" : "110105",
    "name" : "朝阳区"
  }, {
    "adcode" : "110106",
    "name" : "丰台区"
  }, {
    "adcode" : "110107",
    "name" : "石景山区"
  }, {
    "adcode" : "110108",
    "name" : "海淀区"
  }, {
    "adcode" : "110109",
    "name" : "门头沟区"
  }, {
    "adcode" : "110111",
    "name" : "房山区"
  }, {
    "adcode" : "110112",
    "name" : "通州区"
  }, {
    "adcode" : "110113",
    "name" : "顺义区"
  }, {
    "adcode" : "110114",
    "name" : "昌平区"
  }, {
    "adcode" : "110115",
    "name" : "大兴区"
  }, {
    "adcode" : "110116",
    "name" : "怀柔区"
  }, {
    "adcode" : "110117",
    "name" : "平谷区"
  }, {
    "adcode" : "110118",
    "name" : "密云区"
  }, {
    "adcode" : "110119",
    "name" : "延庆区"
  } ]
}
```
6 - 手动同步地址
```java
http://yourip:port/district/api/update
```
访问这个地址会强制从高德地图开放平台同步最新行政区域数据到本地数据库

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
