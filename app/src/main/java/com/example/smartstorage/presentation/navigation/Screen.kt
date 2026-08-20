package com.example.smartstorage.presentation.navigation

/**
 * 应用内页面路由定义。
 */
sealed class Screen(val route: String) {

    /** 首页：物品清单 */
    data object Home : Screen("home")

    /** 添加：新增物品 */
    data object Add : Screen("add")

    /** 编辑：修改物品 */
    data object Edit : Screen("edit")

    /** 详情：查看物品（大图/全屏预览） */
    data object Detail : Screen("detail")

    /** 设置 */
    data object Settings : Screen("settings")

    /** 回收站（设置页进入） */
    data object Trash : Screen("trash")
}