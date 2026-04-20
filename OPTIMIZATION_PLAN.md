# 液态玻璃播放器 优化方案 v1.1

> 基于代码审查（43 个 Kotlin 源文件）+ UI 截图分析（6 张界面截图）整理
> 创建：2026-04-20 | 最后更新：2026-04-20

---

## 完成状态总览

| 优先级 | 类别 | 状态 |
|--------|------|------|
| P0 — 功能性 Bug | 2 项 | ✅ 全部完成 |
| P1 — 架构 / 数据安全 | 4 项 | ✅ 全部完成 |
| P2 — UI / UX 体验 | 4 项 | ✅ 全部完成 |
| P3 — 代码质量 | 3 项 | ✅ 全部完成 |

---

## P0 — 功能性 Bug ✅

### 1. 清除缓存点击无响应 ✅

**修复方案**
- `domain/usecase/ClearThumbnailCacheUseCase.kt` — 新建 UseCase，调用 `ThumbnailCache.dir(context).deleteRecursively()`
- `HomeViewModel.kt` — `clearThumbnailCache(onResult)` 调用 UseCase，结果切回主线程回调
- `ProfileScreen.kt` — 确认 Dialog 调用 `viewModel.clearThumbnailCache { ok -> Toast.show(...) }`

**涉及文件**
- `app/src/main/java/com/example/player/domain/usecase/ClearThumbnailCacheUseCase.kt`
- `app/src/main/java/com/example/player/viewmodel/HomeViewModel.kt`
- `app/src/main/java/com/example/player/ui/screens/ProfileScreen.kt`

---

### 2. MediaController 连接无超时保护 ✅

**修复方案**
- `PlayerViewModel.kt` — `connectController()` 启动 5 秒超时协程（`connectionTimeoutJob`），超时后设 `_connectionError.value = true` 并取消 future
- `connectionError: StateFlow<Boolean>` 暴露给 UI
- `PlayerScreen.kt` — `connectionError` 为 true 时显示错误文案 + "重试"按钮，调用 `viewModel.retryConnection()`

**涉及文件**
- `app/src/main/java/com/example/player/viewmodel/PlayerViewModel.kt`
- `app/src/main/java/com/example/player/ui/screens/PlayerScreen.kt`
- `app/src/main/res/values/strings.xml` — `player_connection_error`

---

## P1 — 架构 / 数据安全 ✅

### 3. Room 未导出 Schema，升级风险高 ✅

**修复方案**
- `PlayerDatabase.kt` — `exportSchema = true`
- `app/build.gradle.kts` — `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`
- `AppModule.kt` — 移除 `fallbackToDestructiveMigration()`，改用纯 `.build()`（v1 无需 Migration）

**涉及文件**
- `app/src/main/java/com/example/player/data/db/PlayerDatabase.kt`
- `app/src/main/java/com/example/player/di/AppModule.kt`
- `app/build.gradle.kts`

---

### 4. LegacyPrefsMigrator 缺少事务保护 ✅

**修复方案**
- 所有 Room 写入包裹在 `db.withTransaction {}`
- 迁移前先 `hasRoomData()` 幂等检查，避免重复插入
- `clearLegacyStores()` 在事务提交后执行，完成 Flag 在最后写入

**涉及文件**
- `app/src/main/java/com/example/player/data/migration/LegacyPrefsMigrator.kt`

---

### 5. HomeViewModel 职责过重 ✅

**修复方案（已实施阶段一）**
- 新建 `domain/usecase/` 目录，抽出三个 UseCase：
  - `ClearHistoryUseCase` — 清除播放记录
  - `ClearFavoritesUseCase` — 清除收藏
  - `ClearThumbnailCacheUseCase` — 清除缩略图缓存
- `HomeViewModel` 注入 UseCase，不再直接持有 DAO 执行清除操作

**涉及文件**
- `app/src/main/java/com/example/player/domain/usecase/`
- `app/src/main/java/com/example/player/viewmodel/HomeViewModel.kt`

---

### 6. 字幕解析不支持导入视频 ✅

**修复方案**
- `SubtitleResolver.kt` — 新增 `resolveFromSafSiblings()`：
  - 检测是否为 MediaStore URI（`content://media/external/...`）
  - 非 MediaStore URI 走 `DocumentFile.fromSingleUri()` 获取父目录
  - 遍历兄弟文件，匹配同名字幕扩展名

**涉及文件**
- `app/src/main/java/com/example/player/data/subtitle/SubtitleResolver.kt`

---

## P2 — UI / UX 体验 ✅

### 7. 视频文件名截断过激 ✅

**修复方案**
- `VideoCard` 中文件名 `maxLines` 从 1 改为 2，允许折行显示

**涉及文件**
- `app/src/main/java/com/example/player/ui/screens/HomeScreen.kt` — `VideoCard`

---

### 8. 底部导航"+"按钮缺少标签 ✅

**修复方案**
- `BottomNavBar` 中"+"按钮下方添加"导入"文字标签
- `strings.xml` 新增 `nav_import = "导入"`

**涉及文件**
- `app/src/main/java/com/example/player/ui/screens/HomeScreen.kt` — `BottomNavBar`
- `app/src/main/res/values/strings.xml`

---

### 9. 版本信息"最新"标签硬编码 ✅

**修复方案（方案 A）**
- `ProfileScreen.kt` — 版本号通过 `context.packageManager.getPackageInfo()` 动态读取
- `profile_version_desc = "v%1$s"`，不再有静态"最新"字样

**涉及文件**
- `app/src/main/java/com/example/player/ui/screens/ProfileScreen.kt`
- `app/src/main/res/values/strings.xml`

---

### 10. 横屏退出动画依赖硬编码延迟 ✅

**修复方案**
- `MainActivity.kt` — 移除 `delay(380)`，改用事件驱动：
  ```kotlin
  snapshotFlow { deviceOrientation }
      .filter { it == Configuration.ORIENTATION_PORTRAIT }
      .first()  // 等待方向真正切换完成
  withFrameNanos { }  // 等待一帧确保坐标系稳定
  navController.popBackStack()
  ```

**涉及文件**
- `app/src/main/java/com/example/player/MainActivity.kt`

---

## P3 — 代码质量 ✅

### 11. 静态分析未强制执行 ✅

**修复方案**
- `build.gradle.kts` — detekt 移除 `ignoreFailures = true`（已默认 false）
- `build.gradle.kts` — ktlint 移除 `ignoreFailures.set(true)`

> 若现有代码有风格问题，可先执行 `./gradlew ktlintFormat` 自动修复大部分问题，
> 再执行 `./gradlew detekt` 查看并修复剩余告警。

**涉及文件**
- `build.gradle.kts`（根目录）

---

### 12. WRITE_SETTINGS 权限声明但无使用 ✅

**修复方案**
- `AndroidManifest.xml` — 删除 `android.permission.WRITE_SETTINGS` 声明

**涉及文件**
- `app/src/main/AndroidManifest.xml`

---

### 13. Volley 依赖未使用 ✅

**修复方案**
- `libs.versions.toml` — 删除 volley 版本条目
- `app/build.gradle.kts` — 删除 volley 依赖实现行（约减少 120KB APK 体积）

**涉及文件**
- `gradle/libs.versions.toml`
- `app/build.gradle.kts`

---

## 验证清单

```bash
# 静态分析（应无 error 级别告警）
./gradlew detekt

# 代码风格（移除 ignoreFailures 后，若有违规先自动修复）
./gradlew ktlintFormat
./gradlew ktlintCheck

# 构建验证
./gradlew assembleDebug

# Room Schema 生成验证（应在 app/schemas/ 生成 JSON 文件）
./gradlew kspDebugKotlin
```

---

*全部 13 项优化已完成。*
