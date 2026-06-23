# 词屿 LexiFlow

一个使用 Kotlin 编写的 Android 原生背单词应用，产品方向参考“不背单词”和“扇贝单词”，但采用独立的视觉、学习调度和数据结构。

## 已实现

- 今日学习计划与连续学习天数
- 单词卡片、音标、例句和中文释义
- Android 系统 TTS 英语发音
- “忘记 / 模糊 / 认识 / 熟练”四档记忆反馈
- 根据难度与记忆稳定度动态调整的间隔重复算法
- Room 本地数据库与完整作答历史
- 新学、复习、正确率、连续学习、长期记忆统计
- 每日新词上限与任务完成状态
- 四套训练：看词辨义、听音辨词、释义拼写、例句填空
- 多词书切换、搜索、收藏与自动错词本
- JSON 自定义词库导入、学习数据导出和进度清除
- 每日目标、目标记忆率、自动发音和提醒设置
- 七日学习日历、学习时长与周统计
- 深色模式、通知权限处理和 TTS 降级提示
- 24 个内置四六级/通用高频词

## 技术栈

- Kotlin
- Android XML + Material 3 + Fragment
- MVVM + StateFlow
- Room 数据库
- minSdk 23 / targetSdk 36

## 运行

使用 Android Studio 打开项目根目录，等待 Gradle Sync 完成后运行 `app`。

命令行构建：

```powershell
.\gradlew.bat assembleDebug
```

Debug APK 输出在 `app/build/outputs/apk/debug/app-debug.apk`。

## 自定义词库格式

应用内“词库 → 导入 JSON”可以导入 JSON 数组。示例位于
`app/src/main/assets/import_example.json`，字段包括：

- `word`、`phonetic`、`meaning`
- `example`、`translation`
- `root`、`collocation`、`synonyms`

## 后续建议

1. 支持 CSV/JSON 词书导入与多词书切换。
2. 使用 WorkManager 增加每日复习提醒。
3. 接入真人发音音频和离线缓存。
4. 增加拼写、听力、选择题三种训练模式。
5. 增加登录与云端同步。
