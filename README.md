# ApkPatcher
修补 APK 进行一些特定寻找与修改操作并重新打包为 APK 的工具。

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

I plan to disable the decoding resource option of Apktool when decoding Apk, and decode the required XML separately based on the patch script, replacing the original XML with the encoded XML after the patch is completed.
This may take some time.