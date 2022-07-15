package com.addx.ai.demo

object DemoGlobal {
    open var isLogin = false

    @JvmField
    open var isSDKInited = false

    // 建议生成新的token
    // 格式是 key(userId) : value(token)
}