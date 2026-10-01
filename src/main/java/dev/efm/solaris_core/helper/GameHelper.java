package dev.efm.solaris_core.helper;

import net.minecraft.resources.ResourceLocation;

public class GameHelper {
    public static ResourceLocation buildRes(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    public static ResourceLocation buildRes(String name, char sp) {
        return ResourceLocation.bySeparator(name, sp);
    }
}
