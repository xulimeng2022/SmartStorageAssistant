package com.example.smartstorage.presentation.donate

import androidx.annotation.StringRes

import androidx.annotation.DrawableRes
import com.example.smartstorage.R

/**
 * 赞助档位数据模型。
 *
 * @param amount 金额（元）
 * @param labelRes 显示文案资源
 * @param emoji 表情符号（如“❤️”）
 * @param alipayRes 支付宝收款码图片资源 ID
 * @param wechatRes 微信收款码图片资源 ID
 */
data class DonateOption(
    val amount: Double,
    @StringRes val labelRes: Int,
    val emoji: String,
    @DrawableRes val alipayRes: Int,
    @DrawableRes val wechatRes: Int,
) {
    /** 金额文本保留稳定数字，货币文案由显示层本地化。 */
    val amountValue: String
        get() = if (amount % 1.0 == 0.0) amount.toInt().toString() else amount.toString()

    companion object {
        /**
         * 预设档位：0.66 / 2 / 5 / 20 元。
         * 预留扩展：如需 66 元档位，取消下方注释并放入 alipay_66.png / wechat_66.png。
         */
        fun presetOptions(): List<DonateOption> = listOf(
            DonateOption(0.66, R.string.donate_option_flower, "❤️", R.drawable.alipay_0_66, R.drawable.wechat_0_66),
            DonateOption(2.0, R.string.donate_option_water, "🧊", R.drawable.alipay_2, R.drawable.wechat_2),
            DonateOption(5.0, R.string.donate_option_noodles, "🍜", R.drawable.alipay_5, R.drawable.wechat_5),
            DonateOption(20.0, R.string.donate_option_pork, "🍛", R.drawable.alipay_20, R.drawable.wechat_20),
            DonateOption(66.0, R.string.donate_option_true_fan, "🎉", R.drawable.alipay_66, R.drawable.wechat_66),
            DonateOption(99.0, R.string.donate_option_true_love, "👑", R.drawable.alipay_99, R.drawable.wechat_99),
        )
    }
}