ServerEvents.recipes(evt => {
    const efm_list = [
        "bhc:god_apple"
    ];

    efm_list.forEach(id => {
        evt.remove({ id: `${id}` });
    });
})