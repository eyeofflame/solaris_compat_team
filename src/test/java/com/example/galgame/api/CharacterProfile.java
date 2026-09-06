package com.example.galgame.api;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * 一位可出现在剧本中的角色：负责提供显示名（i18n key）与各姿势的立绘贴图。
 *
 * <p>立绘贴图路径约定：{@code assets/<modid>/textures/galgame/portraits/<speakerId>/<pose>.png}。
 * 至少应注册一个名为 {@code "base"} 的姿势，作为缺省立绘。</p>
 */
public final class CharacterProfile {

    /** 显示名（i18n key），例如 {@code "galgame.char.alice"}。 */
    public final String displayNameKey;

    /** 姿势名（如 base/happy）与贴图路径的映射。 */
    private final Map<String, ResourceLocation> textures = new HashMap<>();

    public CharacterProfile(String displayNameKey) {
        this.displayNameKey = displayNameKey;
    }

    /**
     * 注册一种姿势的立绘贴图。
     *
     * @param pose     姿势名（如 {@code "base"}、{@code "happy"}、{@code "angry"}）
     * @param texture  该姿势的贴图 ResourceLocation
     * @return this，便于链式调用
     */
    public CharacterProfile pose(String pose, ResourceLocation texture) {
        textures.put(pose, texture);
        return this;
    }

    /** 是否注册了指定姿势。 */
    public boolean hasPose(String pose) {
        return textures.containsKey(pose);
    }

    /**
     * 取得指定姿势的立绘；若未注册则回退到 {@code "base"}；再没有则返回 null。
     *
     * @param pose 目标姿势名
     * @return 立绘贴图，可能为 null（该角色没有可用立绘）
     */
    @Nullable
    public ResourceLocation texture(String pose) {
        ResourceLocation t = textures.get(pose);
        return t != null ? t : textures.get("base");
    }
}
