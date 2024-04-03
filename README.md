# ApkPatcher
修补 APK 进行一些特定寻找与修改操作并重新打包为 APK 的工具。

# Usage
```
apkpatcher <Script> <APKFile>
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
- [ ] **Resource**
    - [x] XML patch
        - [ ] Replace existing child nodes(likes Magisk's mount file)
    - [x] Replace file
- [x] **Dex**
    - [x] Find method and replace
        - [x] Find class with SourceFile
- [x] **Script**
    - [x] Multi script
- [x] **Other**
    - [x] Sign config