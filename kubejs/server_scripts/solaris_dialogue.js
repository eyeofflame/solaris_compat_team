// 对话系统的 KubeJS 示例。改完在这里执行 /reload 就能生效。
// 打开方式：/std_create js_demo
//
// 两个全局名要分清：
//   SolarisRPG      —— 事件组，KubeJS 自动绑定的，用来注册 scripts / choice
//   SolarisDialogue —— 我们绑的 API 门面，用来 open / has / HIDE_PORTRAIT
//
// 两个事件都是 server 事件，所以必须放在 server_scripts 里。

SolarisRPG.scripts(event => {
    event.create('js_demo', script => {
        script.start('start')

        // line(id, text, next) —— 旁白
        script.line('start', '这个剧本是 KubeJS 写的，没碰过 Java。', 'ask')

        // choose(id, speaker, text, choices => ...) —— 分支
        script.choose('ask', '旁白', '要试试选项回调吗？', c => {
            c.option('yes', '要', 'yes_node')
            c.option('no', '算了', 'no_node')
        })

        script.line('yes_node', '收到。奖励已经发到你背包里了。', '')
        script.line('no_node', '好吧，下次再说。', '')

        // next 传空字符串 = 剧情结束，再点一下界面就关掉
        //script.line('end', '（结束）', '')
    })

    // 想加说话人和立绘的话：
    //   script.say('greet', '奥利维亚', '哟，稀客。', 'next_node')
    //         .portrait('solaris_compat:textures/gui/portrait/olivia.png')
    // 收起立绘：.portrait(SolarisDialogue.HIDE_PORTRAIT)
})

// 玩家在对话里选了某个选项时触发。
// 注意这是「通知」——不能改变对话走向，走向在客户端按下按钮那一刻就定了。
SolarisRPG.choice(event => {
    console.info(`[SolarisRPG] ${event.scriptId} / ${event.nodeId} / ${event.choiceId}`)

    if (event.scriptId === 'js_demo' && event.choiceId === 'yes') {
        event.player.give('minecraft:diamond')
        event.player.tell('你得到了一颗钻石。')
    }
})

// 也可以从别的地方主动打开对话，比如玩家加入时：
// PlayerEvents.loggedIn(event => SolarisDialogue.open(event.player, 'js_demo'))
