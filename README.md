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

# TODO
- [ ] **Resource**
    - [x] XML patch
        - [ ] Replace existing child nodes(likes Magisk's mount file)
    - [ ] Replace file
- [ ] **Dex**
    - [ ] Find method and replace
        - [ ] Find class with SourceFile