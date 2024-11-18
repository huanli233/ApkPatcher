# ApkPatcher
修补 APK 进行一些特定寻找与修改操作并重新打包为 APK 的工具。

由于在此项目开始编写时经验不足，导致在设计、代码质量上有着诸多问题，故此项目已停止维护。我计划在最近创建一个用处差不多的新项目，但有着更好的 API 设计和实现细节。
[Re-ApkPatcher](https://github.com/huanli233/Re-ApkPatcher)

# Usage
```
apkpatcher <Script> <APK> [Sign Option]
  Script - ApkPatcher Script File
  APK - Input APK File
  Signing Option:
    remove - (Default)Do not retain the original signature
    keep - Do not modify the original signature information
```

# 一些示例 Script
修补 bools.xml 中 config_supportAutoRotation 为 true :
```
scripts/PatchFrameworkRes.xml
```
修补小天才桌面去除上课禁用:
```
scripts/DisableLauncherRunMode.xml
```
修补小天才桌面去除非法安装:
```
scripts/RemoveIllegalApp.xml
```
修补小天才桌面启用九宫格应用列表:
```
scripts/Enable3x3.xml
```

# Script
脚本文件使用 XML 格式
```xml
<apkpatcher>
	<!-- Resource 修补 -->
	<resource>
		<!-- 修改 XML 内容 -->
		<patch type="xml" path="path/to/xmlfile.xml">
			<example>123</example>
		</patch>
		<!-- 替换文件 -->
		<patch type="replace" path="path/to/file">path/to/my/file</patch>
	</resource>
	<!-- Dex 类方法修补 -->
	<dex>
		<!-- 提供准确的类名查找类 -->
		<class type="precise" find="com.xtc.i3launcher.duration.AppProcessManager$1">
			<!-- 提供准确的方法信息：[方法名;参数类型(用","分隔);返回类型;是否为static] 并修改其为RETURN-VOID -->
			<patch patchtype="RETURN-VOID" type="precise" find="onValidate;Ljava/lang/String,Z;V;false"></patch>
		</class>
		<!-- 提供类的SourceFile的值查找类，并提供类名的后缀(0就是类名不含$) -->
		<class type="source" find="SpanManager.java;0">
			<!-- 提供准确的方法信息 并修改其为RETURN-BOOL，返回true -->
			<patch type="precise" patchtype="RETURN-BOOL" find="a;V;Z;false">true</patch>
		</class>
	</dex>
</apkpatcher>
```

# TODO
- [x] **Resource**
    - [x] XML patch
        - [x] replace existing child nodes(likes Magisk's mount file)
    - [x] Replace file
- [x] **Dex**
    - [x] Find method and replace
        - [x] Find class with SourceFile
- [x] **Script**
    - [x] Multi script
- [x] **Other**
    - [x] Sign config
    
# Resource folders implicit qualifiers
Apktool:
> Currently we have a mismatch between reading the folders and reading the qualifiers which leads to a mismatch between
implicit qualifiers like version (-v4, v13, etc).

I modify the code in the ResourceDecoder section to record the pre decoding and post decoding paths, and then have ApkPatcher operate on this to change the res folder structure to its original state.
