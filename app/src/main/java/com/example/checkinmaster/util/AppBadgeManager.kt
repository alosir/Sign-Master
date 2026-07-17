package com.alosir.task.util

import android.content.Context
import me.leolin.shortcutbadger.ShortcutBadger

/**
 * 桌面图标数字角标管理器。
 *
 * 数字等于「今日」待签到任务数量；待签到为 0 时自动移除角标。
 */
object AppBadgeManager {

    /**
     * 更新桌面角标数量。
     *
     * @param context 上下文
     * @param count 今日待签到任务数量，0 表示移除角标
     */
    fun updateBadge(context: Context, count: Int) {
        try {
            if (count <= 0) {
                ShortcutBadger.removeCount(context)
            } else {
                ShortcutBadger.applyCount(context, count)
            }
        } catch (e: Throwable) {
            // 部分 OEM 桌面不支持或异常，静默忽略，避免崩溃
            e.printStackTrace()
        }
    }

    /**
     * 强制移除桌面角标。
     */
    fun removeBadge(context: Context) {
        try {
            ShortcutBadger.removeCount(context)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}
