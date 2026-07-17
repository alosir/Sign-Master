package com.alosir.task.ui

/**
 * 面向用户的版本更新日志。
 *
 * 按倒序排列，最新版本在前。
 */
object ChangelogData {

    data class VersionInfo(
        val name: String,
        val date: String,
        val items: List<String>
    )

    val versions: List<VersionInfo> = listOf(
        VersionInfo(
            name = "v1.1.0",
            date = "2026-07-13",
            items = listOf(
                "新增版本更新页面，可查看历史更新日志",
                "新增检查更新功能，可从 GitHub 下载最新安装包",
                "我的页新增当前版本卡片，点击即可进入版本更新",
                "统一今日、任务、统计、我的四页顶部间距",
                "更换全新 APP 图标"
            )
        ),
        VersionInfo(
            name = "v1.0.2",
            date = "2026-07-13",
            items = listOf(
                "新增桌面图标数字角标，实时显示今日待签到数量",
                "今日任务全部完成后角标自动消失",
                "统计页标题改为与其他页面一致的 Toolbar 样式",
                "我的页新增版本号与更新日期展示"
            )
        ),
        VersionInfo(
            name = "v1.0.1",
            date = "2026-07-13",
            items = listOf(
                "新增桌面图标数字角标，实时显示今日待签到数量",
                "今日任务全部完成后角标自动消失"
            )
        ),
        VersionInfo(
            name = "v1.0.0",
            date = "2026-07-13",
            items = listOf(
                "全新四页架构：今日、任务、统计、我的",
                "支持 APP、网站、其他三种签到类型",
                "支持每天、每周、每月及自定义签到周期",
                "支持签到提醒与系统通知",
                "支持数据导出到系统下载文件夹与导入恢复",
                "统计页提供日历、完成率、类型分布、连续签到排行"
            )
        )
    )
}
