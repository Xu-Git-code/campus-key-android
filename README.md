# CampusKey · 校园网自动登录（Android）

配置一次账号密码，之后连上校园 WiFi 自动检测：能上网就什么都不做，掉线了才自动补登录。

这是 [CampusKey 桌面版](../campus-login) 的手机端：同样的思路，改成 Android 前台服务 + 网络监听。

## 功能

- 首次填一次学号 / 密码 / 登录 URL，之后不用再管
- 连上 WiFi 自动检测，只有未登录时才发登录请求
- 开机自启（可选）
- 前台服务常驻，掉线后自动重试
- 日志页可看每次检测和登录结果
- 密码用 `EncryptedSharedPreferences` 加密保存在本机
- 不联网上传任何数据，不收集个人信息

## 适用条件

- Android 8.0（API 26）及以上
- 校园网是**网页认证**（浏览器跳登录页，输入学号密码）
- 你能按 F12 / 手机浏览器抓到那条登录请求（GET 或 POST 表单）

不适用：客户端认证（锐捷、哆点等）、验证码、密码 JS 加密且无法复用密文。

## 构建 APK（不需要装 Android Studio）

1. 把本目录推到一个 GitHub 仓库；
2. 打开仓库的 **Actions** 页面，允许 workflows；
3. 每次 push 会自动编译，完成后在 **Artifacts** 里下载 `CampusKey-debug-apk`；
4. 解压得到 `app-debug.apk`，传到手机安装。

也可以本地用 Android Studio 打开本目录直接 Run。

> 工作流会缓存 debug keystore，保证多次构建签名一致，可以直接覆盖升级。
> 如果缓存被清掉，需要先卸载旧版再装新版。

## 安装

1. 把 APK 传到手机（微信/QQ/网盘/数据线均可）；
2. 点击安装，系统提示“未知来源应用”时允许；
3. 打开 CampusKey。

## 首次使用

1. 点 **设置**，填：
   - 学号 / 账号
   - 密码
   - 登录 URL（默认已经填好某校 Dr.COM 的模板）
2. 点 **测试登录**，确认能成功；
3. 返回首页，打开 **自动模式**；
4. 点 **忽略电池优化**，允许后台运行；
5. 允许通知权限（前台服务需要）。

之后连上校园 WiFi 就会自动检测，未登录时自动登录。

## 登录 URL 怎么写

默认 URL 是 Dr.COM 的 GET 登录接口，占位符会被替换：

| 占位符 | 替换成 |
|---|---|
| `{username}` | 账号（不编码） |
| `{password}` | 密码（不编码，JSON 用） |
| `{username_urlencoded}` | 账号（URL 编码，表单/URL 用） |
| `{password_urlencoded}` | 密码（URL 编码，表单/URL 用） |
| `{ip}` | 手机当前 IPv4 地址 |

其他学校：把抓包得到的请求改写成同样的格式即可。POST 表单目前不支持（本 App 只发 GET），需要的话可以自行改 `PortalClient`。

> 如果换了学校/认证系统，`program_index`、`page_index` 这类参数可能不同，直接用你抓到的那条 URL 替换即可。

## 工作原理

```
连上 WiFi
  ↓ ConnectivityManager 回调
前台服务等 3 秒（拿 IP）
  ↓
GET example.com / baidu 检测
  ├─ 通 → 记录“已在线”，退出
  └─ 不通 → 替换 URL 占位符 → 发登录请求
              ↓ 等 2 秒
              ├─ 通 → 通知“已自动登录”
              └─ 不通 → 记录失败，等下次网络变化/10 分钟后重试
```

## 权限说明

| 权限 | 用途 |
|---|---|
| `INTERNET` | 发登录请求、检测网络 |
| `ACCESS_NETWORK_STATE` | 监听网络变化 |
| `ACCESS_WIFI_STATE` | 读取网络状态 |
| `RECEIVE_BOOT_COMPLETED` | 开机自启 |
| `FOREGROUND_SERVICE` | 前台服务常驻 |
| `POST_NOTIFICATIONS` | 显示常驻通知 |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | 避免后台被杀 |

不申请定位、通讯录、存储等无关权限。

## 常见问题

| 现象 | 原因 | 解决 |
|---|---|---|
| 通知不显示 | Android 13+ 没给通知权限 | 设置里允许通知 |
| 一段时间后不自动登录 | 后台被系统杀掉 | 允许后台运行 / 忽略电池优化 |
| 登录成功但网页打不开 | 检测地址被学校限制 | 改 `PortalClient` 里的检测地址 |
| 换 WiFi 后误登录 | App 不区分 WiFi | 目前任意 WiFi 都会检测，未登录才登录 |
| 手机重启后没启动 | 部分系统限制开机启动 | 手动打开一次 App，或关闭系统的“自启动限制” |
| 安装时提示风险 | 没有上架应用商店 | 允许未知来源即可 |

## 隐私与免责

- 账号密码只保存在手机本地（加密），不会上传到任何服务器；
- App 只向你的校园网门户和检测站点发请求；
- 仅供自己的设备自动完成校园网认证，请遵守学校网络使用规定；
- 不提供绕过认证、共享账号、破解客户端等能力；
- 使用风险自负。

## 目录结构

```
app/src/main/java/com/campuskey/app/
├─ CampusKeyApp.kt      Application 初始化
├─ Prefs.kt             加密配置存储 + 默认 URL
├─ AppLog.kt            本地日志
├─ PortalClient.kt      检测联网 / 发登录请求 / 取本机 IP
├─ LoginService.kt      前台服务：网络监听 + 自动登录
├─ BootReceiver.kt      开机自启
├─ MainActivity.kt      首页
├─ SettingsActivity.kt  设置
└─ LogActivity.kt       日志
```
