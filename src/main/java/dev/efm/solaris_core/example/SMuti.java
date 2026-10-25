package dev.efm.solaris_core.example;

import dev.efm.solaris_core.api.multiblock.Multiblock;
import dev.efm.solaris_core.api.multiblock.MultiblockRegistry;
import net.minecraft.world.level.block.Blocks;

public class SMuti {
    public static final Multiblock COKE = MultiblockRegistry.register(
            Multiblock.builder("solaris:coke")
                    .template("solaris:test")
                    .triggerOffset(0, 1, 1)
                    .trigger(Blocks.FURNACE)
                    .build()
    );
}
