<p align="center">
  <h1 align="center">词屿 LexiFlow</h1>
  <p align="center">不是记住所有，而是每天多认识一点。</p>
</p>

---

词屿是一个使用 Kotlin 编写的 Android 原生背单词应用，基于类 FSRS 间隔重复算法，支持看词辨义、听音辨词、释义拼写、例句填空四种训练模式，帮助你在单词记忆的长河中搭建一座属于自己的岛屿。

## 功能

### 学习系统
- **今日计划** — 根据算法自动安排每日新学与复习任务
- **单词卡片** — 英文、音标、例句和中文释义，轻触揭晓
- **系统 TTS 发音** — 调用 Android 内置语音引擎朗读单词
- **四档反馈** — 忘记 / 模糊 / 认识 / 熟练，分别影响稳定性与难度
- **间隔重复** — 基于 FSRS 思路的自研调度算法，根据记忆难度和目标保持率动态计算复习间隔
- **连续学习** — 追踪每日打卡与连续学习天数

### 多维训练
| 模式 | 描述 |
|------|------|
| 看词辨义 | 查看单词卡片，轻触揭晓释义 |
| 听音辨词 | 播放发音，根据声音拼写单词 |
| 释义拼写 | 根据中文释义拼写英文 |
| 例句填空 | 读取例句与翻译，填入缺失单词 |

### 词库管理
- **多词书支持** — 内置四级/六级/考研/雅思/托福词书，支持切换
- **搜索与收藏** — 按英文或中文释义搜索，收藏重点词汇
- **自动错词本** — 标记为"忘记"的词自动进入错词本
- **JSON 导入** — 支持自定义词库导入
- **在线查词** — 接入 DeepSeek API，在线查询单词详细信息并一键添加到词库

### 统计与设置
- **成长记录** — 新学/复习/正确率/长期记忆/连续天数
- **学习日历** — 七日学习热力图与每日心情记录
- **学习时长** — 当日学习投入与本周完成次数
- **每日目标** — 可自定义每组单词量与每日新词上限
- **目标记忆率** — 可调节的期望记忆保持率（默认 90%）
- **深色模式** — 跟随系统或手动切换
- **每日提醒** — 可设置固定时间的学习通知提醒

## 截图

<img width="360" height="803" alt="首页" src="https://github.com/user-attachments/assets/e86a831f-1b30-4c74-aaa0-ef197cfda8b6" />
<img width="357" height="803" alt="词书" src="https://github.com/user-attachments/assets/0ff7e155-0271-40ef-835b-23cddfa7e2ab" />

## 技术栈

| 类别 | 选择 |
|------|------|
| 语言 | Kotlin |
| UI | Android XML + Material 3 + ViewBinding |
| 架构 | MVVM + StateFlow |
| 数据库 | Room (SQLite) |
| 发音 | Android TextToSpeech (TTS) |
| 构建 | Gradle (Kotlin DSL) |
| 最低版本 | minSdk 23 (Android 6.0) |
| 目标版本 | targetSdk 36 |

## 项目结构

```
├── app/src/main/java/com/lexiflow/wordbook/
│   ├── data/               # 数据层：Room 数据库、DAO、仓库、偏好设置
│   │   ├── AppDatabase.kt
│   │   ├── Daos.kt
│   │   ├── Entities.kt
│   │   ├── LearningRepository.kt
│   │   ├── UserPreferences.kt
│   │   ├── SeedWords.kt
│   │   ├── AssetWordbookImporter.kt
│   │   └── DictionaryClient.kt
│   ├── domain/             # 领域层：调度算法、模型、拼写容错
│   │   ├── ReviewScheduler.kt
│   │   └── Models.kt
│   ├── ui/                 # 界面层：Fragment、ViewModel
│   │   ├── HomeFragment.kt
│   │   ├── StudyFragment.kt
│   │   ├── StatsFragment.kt
│   │   └── LibraryFragment.kt
│   ├── MainActivity.kt
│   └── ReminderScheduler.kt
├── app/schemas/            # Room 数据库迁移 schema
└── tools/                  # 辅助脚本
```

## 运行

### 环境要求

- Android Studio Ladybug 或更高版本
- JDK 17+

### 步骤

1. 克隆仓库
   ```bash
   git clone https://github.com/your-username/LexiFlow.git
   ```
2. 使用 Android Studio 打开项目根目录
3. 等待 Gradle Sync 完成
4. 运行 `app` module

### 命令行构建

```bash
# Windows
.\gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```

Debug APK 输出路径：`app/build/outputs/apk/debug/app-debug.apk`

## 自定义词库

应用内"词库 → 导入 JSON"支持导入自定义单词列表，JSON 格式为数组，字段包括：

| 字段 | 说明 | 必填 |
|------|------|------|
| `word` | 英文单词 | 是 |
| `phonetic` | 音标 | 否 |
| `meaning` | 中文释义 | 是 |
| `example` | 英文例句 | 否 |
| `translation` | 例句翻译 | 否 |
| `root` | 词根词缀 | 否 |
| `collocation` | 常见搭配 | 否 |
| `synonyms` | 近义词 | 否 |

示例文件位于 `app/src/main/assets/import_example.json`。

## DeepSeek API 配置

在线查词功能需要配置 DeepSeek API Key：

1. 前往 [DeepSeek Platform](https://platform.deepseek.com) 注册并获取 API Key
2. 在应用内"词库 → 学习设置 → DeepSeek API Key"中填入

功能在不配置 API Key 的情况下仍可正常使用，仅在线查词和 AI 例句生成不可用。

## License

本项目基于 MIT License 开源，详见 [LICENSE](LICENSE)。
