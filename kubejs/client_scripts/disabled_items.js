// 黑名单来自 startup_scripts/disabled_items.js；客户端配置须与服务器一致。
JEIEvents.hideItems(event => {
    if (!global.SolarisDisabledItems) throw new Error('[Disabled Items] Startup blacklist was not loaded.')
    global.SolarisDisabledItems.ids.forEach(id => event.hide(id))
})
