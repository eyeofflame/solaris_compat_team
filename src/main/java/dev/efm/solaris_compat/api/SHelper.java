package dev.efm.solaris_compat.api;

import net.minecraft.resources.ResourceLocation;

public class SHelper {
    public static ResourceLocation buildRes(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
}
