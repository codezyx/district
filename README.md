#高德地图-地址区划同步


Windows下增加CMD窗口标题

1.在第二行非注释行添加
if "%TITLE%" == "" set TITLE=District

2.找到"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %DISTRICT_OPTS%  -classpath "%CLASSPATH%" com.nosoft.district.App %CMD_LINE_ARGS%
在前面添加
start "%TITLE%"
