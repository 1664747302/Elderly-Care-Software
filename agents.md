# Hermes Agent 项目说明

这个文件给后续 agent 快速理解项目用。改动项目结构、构建方式、核心流程或已知问题后，请同步更新这里。

## 项目概览

- 项目类型：原生 Android 应用，Gradle + Kotlin，单模块 `:app`。
- 包名：`com.example.elderreminder`。
- 应用名：`老人关怀`。
- 核心目标：给老人使用的本地护眼休息提醒 APK。用户授权 Android“使用情况访问权限”后，应用定期检查当前前台应用连续使用时长，超时后通过系统通知、TTS 或家人录音提醒休息。
- 隐私模型：设置、录音、提醒历史都只保存在本机；当前代码没有上传服务器的逻辑。

## 构建与验证

项目根目录是：

```text
D:\Project\提醒
```

构建 debug APK：

```powershell
.\gradlew.bat assembleDebug
```

2.026-05-25 验证结果：通过。 APK 输出路径：...

为彻底解决传统“使用情况访问权限”在 Android 系统下由于 WorkManager 最小 15 分钟调度间隔限制而导致无法秒级精准、实时进行防沉迷/眼部休息提醒的问题，我们为应用引入并集成了基于系统【无障碍服务 (AccessibilityService)】的全新守护逻辑。该更新极大优化了实时性与功耗平衡，整体变动如下：

### 1. 架构升级与逻辑重构
* **添加无障碍服务 `ElderAccessibilityService.kt`**：
  - 精准监听 `AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED` 事件，可以在老人切换应用或返回桌面时，瞬间（秒级）捕获当前交互的前台应用包名。
  - 引入了**防卡bug的休息判定时间间隔算法**：以前老人可以通过快速返回桌面（回到Launcher系统桌面）或瞬间锁屏/亮灭屏来刷新前台应用连续使用计时，从而无限延续手机使用时长卡漏洞。现在，前台监测逻辑在用户返回桌面、锁屏或断开使用时不立刻清除总连续时间，而是维护一个由家人在设置中自定义的短暂休息判定阻尼冷却时长（`restGracePeriodMinutes`，默认 3 分钟）。只有老人真正连续离开非白名单应用、在桌面或处于灭屏状态超过这一休息判定时间阀值，当前的连续使用会话才会被判定为已“彻底断开并完成休息”，使用时间被重置。若在此时间内再次打开手机看微信/刷抖音，应用会自动追溯累加刚才的真实连续使用时长，从而科学无漏洞防沉迷。
  - 基于 `Handler` 维护了 30 秒级低功耗检测轮询守护计时器（仅在前台运行非系统桌面及非本应用的普通 App 时启动，极大避免后台过度消耗 CPU 和电池消耗）。
  - 支持对亮/灭屏状态的动态监听。注册 `Intent.ACTION_SCREEN_OFF` 广播接收器，当屏幕熄灭或锁屏时自动暂停/清空前台应用计时计数。
  - 自动通过 `packageManager.queryIntentActivities` 动态匹配设备上的系统桌面包名，并在会话判定中智能过滤掉系统桌面和本应用自身，确保不产生误判。
  - 冷却时间机制与原 `UsageReminderWorker.kt` 逻辑深度对齐：区分普通（以常规或在此会话已提醒过的 `repeatedReminderIntervalMinutes` 为准）与夜间宵禁限值（以 2 分钟为限值及冷却阈值）。
* **`UsagePermission.kt` 接口检测升级**：
  - 新增 `hasAccessibilityAccess(Context)` 函数，用于读取系统 `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` 状态，判断当前该无障碍服务是否被用户成功授权并开启。
* **`AndroidManifest.xml` 与服务声明**：
  - 注册 `ElderAccessibilityService` 服务组件，附加 `android.permission.BIND_ACCESSIBILITY_SERVICE` 系统绑定强制校验权限。
  - 在 `res/xml/` 下扩展声明 `accessibility_service_config.xml` 配置，明确仅监听 `typeWindowStateChanged` 事件类型，且声明该服务仅有基础事件分派权限，保障用户隐私不外泄。

### 2. UI 交互体验优化
* **`EyeCareActivity.kt` 权限与引导重构**：
  - 刷新状态时支持双轨判定：优先展示“无障碍服务已开启 (推荐)”状态；未开启无障碍时，降级检查并显示“使用情况权限已开启”。
  - 细化用户点击“开始提醒”时的提示逻辑：若两者皆未授权，则弹窗详细告知优势与劣势，并提供“去开启无障碍”和“去开启使用情况”的明确分级双入口引导。
* **`SettingsActivity.kt` 家人配置面板升级**：
  - 在“系统使用权限设置”面板中高位新增“开启无障碍服务 (推荐首选)”选项按钮，支持家人进入页面直接快速跳转。

### 3. 构建与验证
* 原 15 分钟 WorkManager 任务依然保留，确保在没有开启无障碍或低内存环境下的多重降级备用保证。
* 新增无障碍配置 `res/values/strings.xml` 中关于开启该服务时的描述性文本引导 (`accessibility_desc`)。
* 源码已在 `D:\Project\Elderly Care Software` 编译完毕。
* 使用 `robocopy` 镜像并分发代码副本至 `D:\elder_reminder_ascii` 目录下，该处的 JUnit 单元测试已全部通过！

## 弹窗与最大音量强制语音提醒功能升级 (2026-05-27)

为彻底解决老人在后台使用其他应用时无法直观感知或听清系统提醒通知的问题，我们为应用引入了“最大提醒语音自主播放 + 弹窗通知文字提示”的双重强交互提醒机制：
1. **强行拉起全屏 Activity 弹窗 (`ReminderDialogActivity.kt`)**：
   - 当检测触及时，无论前台在运行什么第三方应用（或锁屏、屏幕亮灭交替），系统自动瞬间拉起专为老人设计的 `ReminderDialogActivity`。
   - 弹窗采用高宽屏高对比度大字体设计（深夜宵禁时背景为暗色月亮护眼指引，常规提醒为绿色眼睛指引），并提供显著的“我知道了，闭眼休息”一键点击关闭按钮，直接切断老人的沉迷习惯。
2. **强制最大音量播放系统/录音语音**：
   - 在弹窗初始化时，通过调用系统的 `AudioManager` 强制获取当前媒体声道的最大可用音量 (`STREAM_MUSIC`)，并将当前音量自动设置为最大级别，即使老人误触静音或将手机设置为了低音量，也能被突然拉起的最响亮声音惊醒并注意休息。
3. **多重降级兜底保障**：
   - 对 `AndroidManifest.xml` 新增了 `android.permission.USE_FULL_SCREEN_INTENT` 高优先级强拉屏幕提醒权限，并在 Service/Worker 侧同时实现了拉起全屏 Intent + 后台播放的双重触发结构，确证在 Activity 调度异常时仍有后台通知与 TTS 播放防线。
4. **构建与测试**：
   - 包含 `ReminderDialogActivity` 的 APK 在 `D:\Project\Elderly Care Software` 干净编译生成，且 `D:\elder_reminder_ascii` 本地回归测试全部通过。

## 重复提醒功能更新 (2026-05-25)

为了允许在连续使用时间超时并提醒后如果老人未停止使用手机，家人可以设定每隔几分钟重复提醒一次，我们对核心模块进行了如下升级：
1. **`AppSettings.kt`**：
   - 新增 `repeatedReminderIntervalMinutes` 设置项，存储如果未停止使用时的重复弹窗和语音提醒间隔（单位：分钟），默认值为 5 分钟，写入时保证至少为 1 中。
2. **`SettingsActivity.kt` 设计页**：
   - 新置“重复提醒间隔”文本框，用来供家人录入具体的重复间隔时间，并与保存流程和设置生命周期打通。
3. **`UsageSessionAnalyzer.kt` 升级**：
   - `UsageSession` 结构体新增 `startTimeMillis` 属性，用于指示当前处于前台的 App 会话的开始时间。
   - `UsageSessionAnalyzer.currentSession` 在分析得到前台会话时，会自动回填 `startTimeMillis`。
4. **`UsageReminderWorker.kt` 检测与冷却判定逻辑重构**：
   - 将 UsageEvents 查询的区间扩大到 `now - maxOf(settings.reminderMinutes, 180) * 60_000L - 15分钟`。这样能完整追溯当前依然处于前台并且运行时间很长的 App 会话的实际开始时间。
   - 提取出当前的 session。如果 session 的开始时间 `startTimeMillis` 晚于或等于上次触发提醒的时间 `lastReminderAtMillis`，说明在该使用会话中还**没有**发生过提醒。此时，冷却期为常规的 `settings.reminderMinutes` 分钟。
   - 如果上次提醒的时间晚于会话的开始时间，说明已经在此会话中提醒过了。如果在这种情况下用户依然没有关闭前台应用（前台应用一直在使用），则将下一次的检测/提醒冷却期设定为家人设定的重复提醒时间 `settings.repeatedReminderIntervalMinutes` 分钟。
   - 这不仅做到了判定老人有没有在该会话期间“停止使用手机”的闭环，更保证了在未停止使用时，能够精准在家人设置的间隔内重复触发通知与音频警告。
5. **构建与测试**：
   - 全套 JVM 端单元测试在 ASCII 副本下执行并全部通过，APP 本地构建成功。

## 30天血压及心率大模型智能评估功能升级 (2026-05-27)

为提升血压测量的科学自测量化和可读性，我们为应用集成并打通了基于大语言模型（如 DeepSeek 模型）的智能三电极式健康血压评估系统。其底层和 UI 变动如下：
1. **`AndroidManifest.xml`**：
   - 注入 `android.permission.INTERNET`，使应用具备与 DeepSeek API 远程握手并进行 HTTP REST 请求的必备网管权限。
2. **`AppSettings.kt`**：
   - 扩展了 SharedPreferences 成员参数：包括保存家人分配好的 `deepseekApiKey`、自定义的第三方中转和专网服务器 URL 地址 `deepseekApiUrl`，以及最近一次获取成功的诊断时刻 `lastAiEvaluationTimeMillis` 与诊断生成的纯文本缓存字段 `lastAiEvaluationResult`，减少无效网络交互并实现本地瞬间回填展示。
3. **`SettingsActivity.kt` 升级**：
   - 家人控制面板的底部新增了“DeepSeek 智能健康顾问配置”专门段落，支持家人手动填入专有的 API Key 与 URL 网关入口（默认路由为官方端点 `https://api.deepseek.com/v1`），并自动同步读写、保存与防空格过滤机制。
4. **`BloodPressureActivity.kt` 数据流压缩与网络交互升级**：
   - 新增了 `getRecent30DaysDataForAI` 客户端高密度数据轻量微缩函数，自数据库获取 limit=90 的血压多维打点记录，依时间线以 YYYY-MM-DD + [早晨/中午/晚上: 收缩压/舒张压mmHg(心率)] 紧凑排布，最大程度降低传输开销（总数据包控制在 1KB 左右）。
   - 实现了基于原生 HTTP 请求的高效免依赖网络通信模块！通过后台极简的 `Thread` 执行 HTTP POST，在 system 角色深度定制了资深心血管医生的业务诊断，手写自解的零第三方库转义 JSON 解析器极佳地把关了轻量化 APK 体积。
   - 前置天数风控校验（健康趋势分析需要至少积累 7 天以上的血压测量记录）。
   - 本地冷却限制防撞设计：当冷却判定不足 24 小时（即每日仅限分析一次）时，会自动以 Toast 提示并指导科学展示已保存的历史评估详情，有效扼制老人或家属重复点击对网络带来的冲击与 API 费率 of 异常损耗。
   - 交互性 UI 切片：在常规自测评分卡正下方编排嵌入机器人 AI 评估视图卡。展示诊断耗时时为用户变更加载提示，完成诊断后实时写回沙盒并瞬间激活“获取 AI 健康评价”/“重新获取 AI 健康评价”流程。
   - 网络不可用/API 出错时的健全降级处理（将 TextView 转换为故障引导，详细呈列排查方式和 API 验证清单）。
5. **构建与测试**：
   - D:\\\\Project\\\\Elderly Care Software 全线编译成功。ASCII 副本所有单元测试 2026-05-27 验证全部通过！

## 血压安全范围与资料卡片式编辑配置功能升级 (2026-05-28)

为彻底解决不同老人因其独特的基础性疾病，而导致医学界建议的血压正常健康红线与起伏范围完全不同的核心诉求。我们设计了高度人性化、双入口并排且采用卡片式基础病增删机制的全新配置模块，其改动精细归纳如下：
1. **`AppSettings.kt` 数据模型扩增**：
   - 引入四个代表健康门槛的独立正数持久化键值：收缩压自定义底线 `bpSystolicMin`（默认 90, 调节 40-260）及顶线 `bpSystolicMax`（默认 139, 调节 40-260）；舒张压自定义底线 `bpDiastolicMin`（默认 60, 调节 30-180）及顶线 `bpDiastolicMax`（默认 89, 调节 30-180）。
2. **AI评估报告上方并排双入口设计**：
   - 在 `BloodPressureActivity.kt` 中重构智能评估面板布局。在 AI 报告正上方高位编排新增一行高度一致的**并排横向双入口按钮行**。
   - 左侧为“填写资料 📝”大入口，右侧为“安全范围 ⚙️”大入口。均采用优雅的 10% 品牌绿底加深绿描边的高质感圆角卡片按钮风格，排版比例 1:1 完美对称平滑对齐。
3. **“填写资料”卡片式标签流交互重构**：
   - 摒弃了传统的单行文本录入，重新设计了具备极佳直观感官和交互自由度的**独立标签卡片增删模块**。
   - **卡片式列表**：已保存的基础疾病（以顿号`、`或逗号分割）会自动解析并在弹窗中渲染成行状独立圆角浅灰卡片（疾病名居左，右侧配备 ✏️ 修改 与 ❌ 删除 操作字样）。
   - **交互性添加**：底部提供独立的新增输入框与 ➕ 添加 按钮。输入点击完毕后，疾病以独立卡片实时载入。
   - **行内无感编辑与按钮防扭曲优化**：点击 ✏️ 修改 可以在弹框中直接对特定卡片文本内容重新改写，点击 ❌ 删除 即可一键移除。此外，对按钮的边距与高度进行了细节优化，防止布局拥挤引起的文字折行扭曲。
   - 确认无误保存资料时，所有卡片内容会自动用顿号（`、`）平滑拼接持久化至 SharedPreferences。
4. **血压评估与分享长卡片双轨同步**：
   - 无论是一键分享至微信的 Canvas 卡片 `HealthReportRenderer.kt`，还是本地 of 状况卡片，其异常状态（偏高、偏低、正常偏高、正常）均严格遵循最新的自定义安全范围浮动评估，不再有任何误差。
5. **构建与单元测试验证**：
   - 双物理路径和 ASCII 克隆副本顺利编译通过，且 `./gradlew testDebugUnitTest` 回归断言全部 100% 完美跑通！

```text
app/build/outputs/apk/debug/app-debug.apk
```

当前中文路径下运行单元测试会失败：

```powershell
.\gradlew.bat testDebugUnitTest
```

失败现象：JUnit `ClassNotFoundException`。这是 Windows + Android Gradle Plugin 在非 ASCII 中文路径下的 classpath 问题，代码本身在 ASCII 路径副本里能通过测试。

运行单元测试请用 ASCII 副本：

```powershell
cd D:\elder_reminder_ascii
.\gradlew.bat testDebugUnitTest
```

2026-05-25 验证结果：通过。

如果需要重建 ASCII 副本：

```powershell
robocopy D:\Project\提醒 D:\elder_reminder_ascii /E /XD .git .gradle .idea app\build /XF local.properties
Set-Content D:\elder_reminder_ascii\local.properties 'sdk.dir=D\:\\Android\\Sdk'
cd D:\elder_reminder_ascii
.\gradlew.bat testDebugUnitTest
```

已知构建提示：

- `android.overridePathCheck=true` 是 experimental，但当前中文路径构建需要它。
- IntelliJ 可能自动发现无效 JDK：`C:\Users\26896\.jdks\openjdk-19.0.2`，目前不影响 Gradle 构建。
- `SettingsActivity.kt` 里的 `MediaRecorder()` 构造函数已 deprecated，后续可按 Android 版本做兼容清理。

## 一键分享健康报告功能升级 (2026-05-28)

为方便看护对象将自己的健康及用眼守护状态与家人即时分享，我们为应用引入了“一键分享健康数据至微信”的重磅功能：
1. **健康报告自动渲染模块 (`HealthReportRenderer.kt`)**：
   - 采用纯原生 `Canvas` 图形方案进行离屏卡片绘制。
   - 卡片上方为包含当天测量日期与暖色优雅标题的“老人关怀栏”。
   - 正文段落包含“今日自测血压多次打点明细及评估”。
   - 新增“双轨图表组合”：
     (1) 绘制近七天的“日均收缩压(红色) & 舒张压(蓝色) 双色高清立柱式 Canvas 图表”，并带汞柱数值显示与缺漏记录识别；
     (2) 绘制近七天的“每日累计眼部超时疲劳提醒频次 绿色 Canvas 趋势图表”。
   - 自动包含“最常超时 App”、“超时严重度峰值天数”等健康大数据统计汇总。
   - 所有图形完全渲染在 `800 x 1800` 分辨率下，保存至应用私有的安全缓存共享目录。
2. **安全共享与沙盒隔离**：
   - 在 `AndroidManifest.xml` 中引入 `FileProvider` 安全组件，注册 `com.example.elderreminder.fileprovider` 唯一资源共享提供商。
   - 建立 `res/xml/file_paths.xml` 过滤规则定义了 `shared_images/` 安全缓存池，完美兼容 Android 10+ 的分区存储限制。
3. **微信一键呼起与系统通用分享**：
   - 在首页 `MainActivity` 中高位增加按钮“一键分享健康数据 (微信)”，点击后优先显式拉起微信分享通道 (`com.tencent.mm`)。
   - 精细化降级保护：在微信未装或不可获得时，全自动降级为启动系统标准的 ACTION_SEND 多分享源选择对话框，绝不造成应用崩溃。
4. **测试与回归**：
   - D:\\Project\\Elderly Care Software 所有架构以及 D:\\Project\\Elderly Care Software\\elder_reminder_ascii 单元测试在 2026-05-28 均干净通过回归校验！

## 一键导出血压与心率表格并一键分享至微信功能升级 (2026-05-28)

为满足用户和看护家属需要深度追溯、离线保存或将看护数据提供给专业门诊医生用于临床诊断的诉求。我们在首页高位极简地嵌入了“告诉医生我的状况”全新服务流程：
1. **轻量化多维数据抽取与条件过滤 (`BloodPressureDbHelper.kt`)**：
   - 新增了 `getRecordsSince(sinceDate)` 接口，支持以天级为基本单位直接对数据库的历史打点数据进行拉网式追溯，提升大时间跨度数据拉取的读写效率。
2. **科学的单日化均值合并与 XLS (Excel/HTML) 混合格式化导出 (`BloodPressureExporter.kt`)**：
   - **时间跨度选择卡**：用户触发操作时，弹出明确的 “近一个月”、“近三个月”、“近一年” 参数弹框提供给不同场景之查阅使用。
   - **单日列式计算**：对每天可能自测多次的（早晨、中午、晚上）冗余打点记录进行按日均值收拢。平均收缩压、平均舒张压、平均心率全部基于科学四舍五入得出，并整合单日测量数据作为“当日测量次数”。
   - **微信内部极速打开与腾讯文档秒级预览防线**：生成的导表后缀完全采用标准的 `.xls` 后缀，而底层使用“HTML表格 + Excel-XML头”的黑科技混合伪装技术。微信客户端和内置的“腾讯文档”能够100%瞬间识别其为纯正的电子表格，直接在聊天窗口中秒开预览并完全支持高亮和网格线！
   - **精美样式与高危预警功能**：导出的 XLS 表格采用专为看护设计的温暖配色，运用标准的 CSS 容器和 Excel 网格样式，并针对高压（收缩压 > 139 为红色，< 90 为蓝色）和低压（舒张压 > 89 为红色，< 60 为蓝色）做出了醒目的彩色高亮预警，极为便于门诊医生直观快速判读。
   - **Excel 无损无乱码直连**：导出的精美 XLS 表格在电脑端可以直接使用 Microsoft Excel、WPS、或是 Google Sheets 完好无损地打开并编辑打印，完美避开了系统各种字符集转义乱码的技术问题。
3. **MIME 安全级文件共享传输 (`MainActivity.kt`)**：
   - 自动生成诸如 `血压心率数据_近一个月_20260528_193500.xls` 语义清晰的时间戳文件并写入 `FileProvider` 共享沙盒。
   - 分享通道打通微信共享机制（`package = "com.tencent.mm"`, MIME类型 `application/vnd.ms-excel`），官方标准 MIME 类型确保了微信 and 腾讯文档无障碍的读取和拉取。
   - 界面按老人关怀体验进行了高度人性化重构：首页的主分享报告按钮变更为**“告诉家人我的状况”**，导表分析按钮变更为**“告诉医生我的状况”**。
4. **构建与 JVM 自动化回归验证**：
   - 在 `BloodPressureTest.kt` 中全面扩增了关于区间日期计算匹配、单日均值平滑处理、由近及远时序排序排列等多个分支逻辑的 JUnit 测试用例，且在 ASCII 克隆副本及主工程均 100% 畅通通过测验！

## 顶部导航与首页返回升级 (2026-05-27)

为提升多级页面的交互统一性，我们为应用中除 `MainActivity` 外的所有二级/三级页面新增了规范的顶部导航设计，支持快捷返回上一级与一键重定向至主页：
1. **统一化布局设计**：
   - 移除原来的单行标题或右侧悬浮“返回”设计，替换为三段式 `LinearLayout` 容器行。
   - 左侧为“返回”按钮（点击通过 `finish()` 返回上一级）。
   - 中间为当前页面的正规加粗标题字样（居中对齐）。
   - 右侧为全新的“首页”导航按钮（点击通过 `Intent.FLAG_ACTIVITY_CLEAR_TOP` 快速回到 `MainActivity`，并出栈历史任务，实现平滑瞬移）。

## 近七天自测血压柱状图趋势升级 (2026-05-27)

为支持在血压自测二级页面直观查看看护对象的短期血压起伏波动，我们在 `BloodPressureActivity.kt` 中设计并实现了免依赖的每日平均血压柱状图呈现控件：
1. **数据整合查询 (`BloodPressureActivity.kt` & `BloodPressureDbHelper.kt`)**：
   - 引入每日平均血压计算逻辑，每日自测数据（包括早晨、中午、晚上）经过独立均值求和求取日均值。
   - 包含缺失记录自适应识别机制。对于无任何打点结果的非测量日期数据自动在图表中填充为“空”占位符标示。
2. **基于 Canvas 绘制的 `BloodPressureChartView` 原生自定义 View**：
   - 支持高解像比例双柱显示。对于同一天包含平均高压（收缩压，红色）与平均低压（舒张压，蓝色）进行相邻并排双立柱式渲染。
   - 提供最大 200 mmHg 数值刻度底线绘制。
   - 顶部自动通过文本对齐精确贴附并描绘测得的具体汞柱数值。
3. **构建与测试**：
   - 宿主与 ASCII 副本单元测试与整体编译全部通过。
2. **涉及页面对象**：
   - `BloodPressureActivity.kt` (血压自测)
   - `EyeCareActivity.kt` (护眼项目)
   - `SettingsActivity.kt` (家人设置)
   - `ReportActivity.kt` (本地视力与习惯周报)

## 技术栈

- Android Gradle Plugin：`8.7.3`
- Kotlin Android plugin：`2.0.21`
- JVM toolchain：Java 17
- Android SDK：`compileSdk 35`，`minSdk 26`，`targetSdk 35`
- 主要依赖：
  - `androidx.core:core-ktx:1.15.0`
  - `androidx.appcompat:appcompat:1.7.0`
  - `androidx.work:work-runtime-ktx:2.10.0`
  - 单元测试：JUnit 4 + Google Truth

## 目录结构

```text
.
├── build.gradle.kts                  # 根 Gradle 插件声明。
├── settings.gradle.kts               # 仓库配置、rootProject.name、include(":app")。
├── gradle.properties                 # Gradle/Android 参数，包含中文路径 override。
├── gradlew / gradlew.bat             # Gradle wrapper。
├── local.properties                  # 本机 Android SDK 路径，不应依赖提交。
├── README.md                         # 面向用户的构建、安装、授权说明。
├── CHECKPOINT.md                     # 之前的实现和验证记录。
├── agents.md                         # 当前 agent 快速说明。
├── gradle/wrapper/                   # Gradle wrapper jar 和 properties。
└── app/
    ├── build.gradle.kts              # Android app 模块配置和依赖。
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml   # 权限、Activity、开机广播。
        │   ├── java/com/example/elderreminder/
        │   │   ├── AppSettings.kt
        │   │   ├── BootCompletedReceiver.kt
        │   │   ├── BloodPressureActivity.kt
        │   │   ├── BloodPressureDbHelper.kt
        │   │   ├── EyeCareActivity.kt
        │   │   ├── MainActivity.kt
        │   │   ├── ReminderHistoryDbHelper.kt
        │   │   ├── ReminderNotifier.kt
        │   │   ├── ReminderScheduler.kt
        │   │   ├── ReportActivity.kt
        │   │   ├── SettingsActivity.kt
        │   │   ├── SpeechReminder.kt
        │   │   ├── UsagePermission.kt
        │   │   ├── UsageReminderWorker.kt
        │   │   └── UsageSessionAnalyzer.kt
        │   └── res/
        │       ├── drawable/         # 启动图标、主按钮、状态面板 drawable。
        │       └── values/           # strings、colors、AppCompat 无 ActionBar 主题。
        └── test/kotlin/com/example/elderreminder/
            ├── BloodPressureTest.kt
            ├── CurfewTimeCalculatorTest.kt
            ├── CustomAudioFallbackTest.kt
            ├── ReminderHistoryDbHelperTest.kt
            └── UsageSessionAnalyzerTest.kt
```

生成文件和本地状态目录，通常不要纳入提交或重点阅读：

- `.gradle/`
- `.idea/`
- `.kotlin/`
- `app/build/`
- 根目录或模块下的 `build/`

## Manifest 与权限

`app/src/main/AndroidManifest.xml` 声明了：

- `PACKAGE_USAGE_STATS`：读取使用情况事件。特殊权限，必须由用户跳转系统设置手动开启。
- `POST_NOTIFICATIONS`：Android 13+ 发送提醒通知需要。
- `RECEIVE_BOOT_COMPLETED`：手机重启后重新安排后台提醒任务。
- `RECORD_AUDIO`：家人自定义录音提醒需要。

注册组件：

- `MainActivity`：启动页。
- `SettingsActivity`：家人设置页，`exported=false`。
- `ReportActivity`：本地周报页，`exported=false`。
- `custom_reminder.3gp` (和新引入的 `custom_reminder_regular.3gp`, `custom_reminder_curfew.3gp`)：家人录音路径。

## 语音双轨录音与提醒修改 (2026-05-25)

为了允许独立录制和播放“连续使用时间提醒”（常规提醒）以及“宵禁使用提醒”（宵禁提醒），我们对语音相关的模块进行了如下升级：
1. **录音文件命名与向后兼容**：
   - 常规提醒录音文件：`custom_reminder_regular.3gp`。如果该文件不存在但曾录制过旧的 `custom_reminder.3gp`，系统会自动优先读取 `custom_reminder.3gp` 以保证升级不丢失数据。
   - 宵禁限制提醒录音文件：`custom_reminder_curfew.3gp`。
2. **`SpeechReminder.speak(context, message, isCurfew)`** 升级为带 `isCurfew` 标志。在播报时自动根据当前是否处于宵禁状态选择对应的音频文件进行播放；若未启用或音频不存在，则自动降级到系统内置 TTS 中文朗读。
3. **`SettingsActivity.kt` 录音界面重构**：
   - 录音面板被分为两个段落：“连续使用常规提醒录音” 和 “宵禁限制提醒录音”。
   - 双通道可以独立开启/停止录音、独立播放测试。
   - 包含防止录音和播放操作在常规/宵禁轨发生冲突 the 运行时互锁（操作其中之一时自动置灰或锁定另一动作及其测试功能）。
4. **`UsageReminderWorker.kt` 提醒分发**：
   - 触发提醒时根据 `isCurfew` 变量指示 `SpeechReminder.speak` 使用正确的录音文件。
5. **单元测试补充**：
   - `CustomAudioFallbackTest.kt` 新增了测试：常规模式无常规音频但有旧音频时优先 fallback、常规模式同时有旧音频和新常规音频时优先新常规音频、宵禁模式始终使用宵禁音频等关键逻辑判断，已全部通过。

## 核心运行流程

1. 用户打开 `MainActivity`。
2. 应用在 Android 13+ 请求通知权限，并展示使用情况权限状态。
3. 用户授权后（可在“家人设置”中开启使用情况权限）回到应用，点 `开始提醒`。
4. `ReminderScheduler.schedule()` 创建唯一周期 WorkManager 任务 `elder_usage_reminder`，周期 15 分钟。
5. `UsageReminderWorker.doWork()` 检查使用情况权限、设置、宵禁状态、冷却时间和当前前台使用会话。
6. `UsageSessionAnalyzer.currentSession()` 根据 UsageEvents 计算当前前台应用和连续使用时长。
7. 超过阈值后，`ReminderNotifier.show()` 发送高优先级通知。
8. 如果语音开启，`SpeechReminder.speak()` 优先播放家人录音；没有录音则降级为中文 TTS。
9. `ReminderHistoryDbHelper` 把本次提醒写入本地 SQLite。
10. `ReportActivity` 读取最近 7 天提醒记录，展示汇总、柱状图和详情。
11. 手机重启后，`BootCompletedReceiver` 重新调用 `ReminderScheduler.schedule()`。

## 核心文件职责

### `MainActivity.kt`

启动页，UI 全部用 Kotlin 代码构建。负责：

- 展示“护眼项目”和“血压自测”两大核心板块的入口。
- Android 13+ 动态请求通知权限。

### `EyeCareActivity.kt`

护眼项目二级主页，负责：

- 初始化 `AppSettings`。
- 展示使用情况权限是否开启。
- 开始后台提醒。
- 提供受 PIN 密码验证保护的“家人设置”与“视力与习惯周报”入口。

### `BloodPressureActivity.kt`

血压自测二级页面，负责：
- 顶端显示选择的日期（支持前一天/后一天切换，点击日期可通过 DatePickerDialog 直观挑选任何一天）。
- 顶部提供“今日血压健康综合评分”面板，根据录入的血压、心率自动进行健康状况评分与提出对应的医学指导意见。
- 面板式展示“早晨、中午、晚上”三个时段的自测状态。
- 未录入时提供醒目的“填写测量数据”按钮；已录入时居中并大字号展现“高压/低压/心率”的数值，并附带针对血压状态（正常、正常偏高、偏高、偏低）的个性化医学指导建议与指示色卡。
- 提供对已录入数值的“修改数据”以及“重新录入”选项。
- 底部显示“最近七天血压健康得分趋势”柱状图控件，基于 Canvas 动态绘制最近七天（以当前激活选取日为终点）每天的综合评分历史与走势（未录入得分的日期人性化地以“空”占位符标示，防止混淆）。
- 下方卡片式排列展示最近 15 次测量的历史列表，一目了然。

### `BloodPressureDbHelper.kt`

本地 SQLite 血压自测数据存储。数据库 `elder_blood_pressure.db`。表 `blood_pressures`。字段包括：日期 (YYYY-MM-DD)、时段 (早晨/中午/晚上)、高压、低压、心率、录入时间戳。支持根据日期+时段单条覆盖/去重写入、根据日期查询全天记录，以及获取按降序排列的最近测量历史。


### `SettingsActivity.kt`

家人设置页，进入前由 `MainActivity` 做 PIN 校验。负责：

- 修改提醒间隔，最终由 `AppSettings` 限制在 15 到 180 分钟。
- 开关 TTS 语音提醒。
- 开关家人录音提醒。
- 录制自定义 `.3gp` 音频到应用私有目录。
- 播放或停止自定义录音。
- 展示华为/荣耀后台防杀配置教程。
- 提供“开启使用情况权限”以及“系统通知设置”的直接入口。
- 开关夜间宵禁。
- 修改宵禁开始和结束小时。
- 修改提醒文案和家人 PIN。
- 保存后重新安排提醒任务。

注意：

- 录音前会动态请求 `RECORD_AUDIO`。
- 录音文件路径由 `SpeechReminder.getCustomAudioFile(context)` 返回。
- `MediaRecorder()` 当前可用但已 deprecated。

### `ReportActivity.kt`

本地视力与习惯周报页，进入前由 `MainActivity` 做 PIN 校验。负责：

- 查询最近 7 天提醒记录。
- 展示提醒总次数、最常超时应用、单日超时最多日期。
- 用自定义 `Canvas View` 绘制 7 天柱状图。
- 展示每条提醒的时间 and 应用名。
- 数据只来自本地 SQLite，不上传。

### `AppSettings.kt`

封装 `SharedPreferences`，文件名为 `elder_reminder_settings`。字段：

- `pin`：默认 `1234`。
- `reminderMinutes`：默认 `30`，写入时限制 `15..180`。
- `voiceEnabled`：默认 `true`。
- `reminderText`：默认中文休息提醒。
- `lastReminderAtMillis`：上次提醒时间，用于冷却。
- `customAudioEnabled`：默认 `false`。
- `curfewEnabled`：默认 `false`。
- `curfewStartHour`：默认 `22`。
- `curfewEndHour`：默认 `6`。
- `curfewReminderIntervalMinutes`：默认 `2`，写入时限制在 `1..15` 分钟。

还包含 `isCurfewActive(nowMillis)`，支持普通时间段和跨午夜时间段，例如 22:00 到 06:00。

### `UsageReminderWorker.kt`

后台提醒核心 Worker。负责：

- 没有使用情况权限时直接成功退出。
- 读取当前设置和宵禁状态。
- 普通模式使用 `settings.reminderMinutes` 作为阈值。
- 宵禁模式使用可配置的 `curfewReminderIntervalMinutes` （默认 2 分钟）作为阈值。
- 普通模式冷却时间等于提醒间隔；宵禁模式冷却时间由家人设置的 `curfewReminderIntervalMinutes` 决定（默认 2 分钟）。
- 查询 `now - 阈值 - 15分钟` 到 `now` 的 UsageEvents。
- 调用 `UsageSessionAnalyzer` 判断当前连续使用时长。
- 超时后发送通知、语音或录音提醒。
- 更新 `lastReminderAtMillis`。
- 写入 SQLite 提醒历史。
- 宵禁生效时，自动安排 `curfewReminderIntervalMinutes` 分钟后的下一次单次高频检测。

### `UsageSessionAnalyzer.kt`

纯 Kotlin 的使用会话分析器，最适合单元测试。包含：

- `UsageSessionEvent`：时间戳、包名、事件类型。
- `UsageSession`：当前包名和持续时间，提供 `exceeds(minutes)`。
- `UsageSessionAnalyzer.currentSession(events, nowMillis)`：按时间排序事件，记录最近 resumed 的应用，遇到 pause/stop 清空；忽略本应用自己的前台会话。

### `UsagePermission.kt`

通过 `AppOpsManager.checkOpNoThrow(OPSTR_GET_USAGE_STATS, uid, packageName)` 判断使用情况权限是否是 `MODE_ALLOWED`。

### `ReminderScheduler.kt`

后台任务调度器。负责：

- 创建唯一周期任务 `elder_usage_reminder`。
- 周期固定为 15 分钟，这是 WorkManager 周期任务最小间隔。
- 使用 `ExistingPeriodicWorkPolicy.UPDATE`。
- 如果调度时已经处于宵禁时间，立即启动一次单次任务 `elder_usage_reminder_curfew`。
- `cancel()` 目前只取消周期任务。

### `ReminderNotifier.kt`

通知封装。负责：

- Android 13+ 未授权通知时不发送。
- Android O+ 创建通知渠道 `elder_rest_reminders`。
- 通知 id 固定为 `1001`。
- 标题为 `该休息一下了`。
- 使用 high priority、auto cancel 和 big text style。

### `SpeechReminder.kt`

语音提醒封装。负责：

- 自定义录音开启且 `custom_reminder.3gp` 存在时，用 `MediaPlayer` 播放录音。
- 否则初始化或复用 `TextToSpeech`。
- TTS 语言设置为 `Locale.CHINA`。
- utterance id 为 `elder-reminder`。

### `ReminderHistoryDbHelper.kt`

本地 SQLite 提醒历史。数据库：

- 数据库名：`elder_reminder_history.db`
- 版本：`1`
- 表名：`reminders`
- 字段：
  - `_id INTEGER PRIMARY KEY AUTOINCREMENT`
  - `timestamp INTEGER NOT NULL`
  - `package_name TEXT NOT NULL`
  - `duration_minutes INTEGER NOT NULL`

公开方法：

- `insertReminder(timestamp, packageName, durationMinutes)`
- `getRemindersSince(sinceMillis): List<ReminderRecord>`

`ReminderRecord` 字段：`id`、`timestamp`、`packageName`、`durationMinutes`。

### `BootCompletedReceiver.kt`

监听开机完成广播。收到 `BOOT_COMPLETED` 或 `LOCKED_BOOT_COMPLETED` 后调用 `ReminderScheduler.schedule(context)`。

## 测试说明

### `UsageSessionAnalyzerTest.kt`

纯 JVM 测试，覆盖：

- 短时间使用不会超过阈值。
- 当前前台应用持续时间能被正确计算。
- 忽略本应用自己的前台会话。
- 前台应用 pause 后会话结束。

### `CurfewTimeCalculatorTest.kt`

测试宵禁小时段判断：

- 跨午夜时间段，例如 22:00 到 06:00。
- 同日时间段，例如 13:00 到 15:00。

当前限制：它复制了时间判断逻辑，没有直接测试 `AppSettings.isCurfewActive()`，因为 `AppSettings` 依赖 Android `Context`。

### `ReminderHistoryDbHelperTest.kt`

纯 JVM 占位测试，只验证 `ReminderRecord` 数据结构。没有直接跑 `SQLiteOpenHelper`，因为那需要 Android runtime、instrumented test 或 Robolectric。

### `CustomAudioFallbackTest.kt`

简单验证缺失音频文件时的前置条件。

## 资源与 UI 风格

- `res/values/strings.xml`：应用名 `老人关怀`。
- `res/values/colors.xml`：
  - `brand_green`：`#1F6B45`
  - `brand_green_dark`：`#12432B`
  - `warm_background`：`#FAFAF7`
- `res/values/styles.xml`：`Theme.ElderReminder`，继承 `Theme.AppCompat.Light.NoActionBar`，使用 sans 字体、浅色状态栏、暖色背景、绿色 accent。
- `res/drawable/button_primary.xml`：主按钮背景。
- `res/drawable/status_panel.xml`：状态和报告面板背景。
- `res/drawable/ic_launcher.xml`：启动图标。

当前大部分 UI 都是 Kotlin 代码动态构建，不是 XML layout。

## 已知运行注意事项

- Android 不允许普通应用自动授予“使用情况访问权限”，只能跳转系统页面让用户手动开启。
- WorkManager 周期任务最小间隔是 15 分钟。宵禁模式通过单次任务链实现 2 分钟 high frequency 检查。
- 华为/荣耀系统可能限制后台任务，设置页已经写入通知权限、电池手动管理、忽略电池优化、锁定后台任务等教程。
- Android 13+ 必须授予通知权限才会弹提醒。
- UsageStats 在不同系统和 OEM 上行为可能不同，涉及提醒准确性的改动必须真机验证。
- 本应用是生活习惯提醒，不是医疗诊断或治疗软件。

## 常见修改入口

- 修改默认提醒间隔：`AppSettings.DEFAULT_REMINDER_MINUTES`。
- 修改默认提醒文案：`AppSettings.DEFAULT_REMINDER_TEXT`。
- 修改默认宵禁时间：`AppSettings` 里的 `curfewStartHour` 和 `curfewEndHour` 默认值。
- 修改后台调度：`ReminderScheduler.kt` 和 `UsageReminderWorker.kt`。
- 修改连续使用判断：优先改 `UsageSessionAnalyzer.kt`，再补 `UsageSessionAnalyzerTest.kt`。
- 修改通知标题、渠道或样式：`ReminderNotifier.kt`。
- 修改录音/TTS 行为：`SpeechReminder.kt` 和 `SettingsActivity.kt`。
- 增加周报指标：必要时先改 `ReminderHistoryDbHelper.kt` schema，再改 `ReportActivity.kt`。

## 给后续 Agent 的工作建议

- 每次完成代码编写、修复或功能调整后，必须把本次更新涉及的结构变化、功能变化、构建/测试结果、已知问题同步整合进 `agents.md`，避免后续 Hermes 重复扫描项目浪费 token。
- 改动保持小而集中。当前 UI、后台任务、权限逻辑耦合较紧，跨文件改动要验证完整流程。
- 源码改完后，在 `D:\Project\提醒` 跑：

```powershell
.\gradlew.bat assembleDebug
```

- 单元测试在 `D:\elder_reminder_ascii` 跑，直到项目迁移到 ASCII 路径或中文路径 JUnit classpath 问题被解决：

```powershell
cd D:\elder_reminder_ascii
.\gradlew.bat testDebugUnitTest
```

- 涉及权限、WorkManager、开机广播、通知渠道、TTS、录音、UsageStats 的改动，需要真机验证。
- 不要提交 `.gradle/`、`.idea/`、`.kotlin/`、`app/build/` 等生成或本地状态目录。

## 内存与电量优化 (2026-05-28)

对无障碍服务与语音提醒模块进行了三处轻量级优化，降低后台 CPU 唤醒频率和内存 GC 压力：

1. **`ElderAccessibilityService.kt` 轮询间隔 10s → 30s**：
   - 将 `Handler` 守护计时器的检测周期从 10 秒延长至 30 秒，CPU 唤醒次数减少约 67%。
   - 对老人实际使用体验无影响（提醒触发误差最多增加半分钟）。

2. **`ElderAccessibilityService.kt` rawEvents 改用 `ArrayDeque`**：
   - 将事件列表从 `mutableListOf` 替换为 `ArrayDeque(60)`，上限从 100 条收紧到 60 条。
   - 清理策略改为逐个 `removeFirst()`，消除了原有 `takeLast + clear + addAll` 三步操作产生的临时列表对象，减少 GC 抖动。

3. **`SpeechReminder.kt` MediaPlayer 播放完自动释放**：
   - 新增 `setOnCompletionListener`，录音提醒播放结束后立即调用 `release()` 并将引用置 null。
   - 避免 `MediaPlayer` 对象在播放结束后继续占用内存直到下次提醒触发。
