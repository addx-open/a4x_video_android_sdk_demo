package com.addx.ai.demo

class Settings {
    companion object {
        /**
         * zendesk 配置项
         */
        //是否允许使用zendeskSDK ,允许就会使用zendesk原生网页，不允许，就会解析连接，使用网页。null 默认，false不允许，true 允许
        const val  enableZendesk: Boolean = false

        /**
         * 设置页面配置项
         */
        //是否隐藏设置时长的功能，true隐藏，false 不隐藏，默认不隐藏
        const val disableMotionPageSettings = false

        //是否使用更多按钮功能
        const val userMoreSettings = true
    }
}