package com.example.smartstorage.presentation.donate

import androidx.annotation.DrawableRes
import com.example.smartstorage.R

/**
 * 赞助档位数据模型。
 *
 * @param amount 金额（元）
 * @param label 显示文案（如“给作者一朵小红花”）
 * @param emoji 表情符号（如“❤️”）
 * @param alipayRes 支付宝收款码图片资源 ID
 * @param wechatRes 微信收款码图片资源 ID
 */
data class DonateOption(
    val amount: Double,
    val label: String,
    val emoji: String,
    @DrawableRes val alipayRes: Int,
    @DrawableRes val wechatRes: Int,
) {
    /** 按钮第一行：表情 + 文案。 */
    val displayText: String get() = "$emoji $label"

    /** 按钮第二行：金额（2 元而不是 2.0 元）。 */
    val amountText: String
        get() = if (amount % 1.0 == 0.0) "${amount.toInt()} 元" else "$amount 元"

    companion object {
        /**
         * 预设档位：0.66 / 2 / 5 / 20 元。
         * 预留扩展：如需 66 元档位，取消下方注释并放入 alipay_66.png / wechat_66.png。
         */
        fun presetOptions(): List<DonateOption> = listOf(
            DonateOption(0.66, "给作者一朵小红花", "❤️", R.drawable.alipay_0_66, R.drawable.wechat_0_66),
            DonateOption(2.0, "请作者喝瓶水", "🧊", R.drawable.alipay_2, R.drawable.wechat_2),
            DonateOption(5.0, "请作者吃桶泡面", "🍜", R.drawable.alipay_5, R.drawable.wechat_5),
            DonateOption(20.0, "请作者吃顿猪脚饭", "🍛", R.drawable.alipay_20, R.drawable.wechat_20),
            DonateOption(66.0, "包养作者（真爱粉）", "🎉", R.drawable.alipay_66, R.drawable.wechat_66),
            DonateOption(99.0, "真爱支持（包养作者）", "👑", R.drawable.alipay_99, R.drawable.wechat_99),
        )
    }
}