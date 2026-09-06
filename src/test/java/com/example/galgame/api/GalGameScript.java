package com.example.galgame.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 视觉小说（GalGame）剧本模型：由若干顺序执行的“步骤(Step)”构成，支持背景切换、立绘、
 * 对白（打字机）、选项分支与无条件跳转。用链式 {@link Builder} 构建，最终是不可变对象。
 *
 * <p>执行语义：默认按 {@code index+1} 顺序推进；{@link ChoiceStep} 暂停等待玩家选择，
 * 每个选项携带解析好的绝对目标索引；{@link JumpStep} 是 label 解析后的无条件跳转；
 * {@link EndStep} 终止演出（关闭界面）。</p>
 */
public final class GalGameScript {

    /** 全部步骤（顺序索引即剧本指令位置）。 */
    public final List<Step> steps;

    /** label 名 -> 步骤绝对索引（由 {@link Builder#build()} 解析填充）。 */
    public final Map<String, Integer> labels;

    private GalGameScript(List<Step> steps, Map<String, Integer> labels) {
        this.steps = List.copyOf(steps);
        this.labels = Map.copyOf(labels);
    }

    /* ==================================================================
     * 步骤类型（全部作为 GalGameScript.Step 的嵌套静态子类）
     * ================================================================== */

    /** 所有步骤的基类。 */
    public abstract static class Step {
        /** 顺序流下的默认下一个步骤索引（由 build() 统一赋值为 index+1）。 */
        int next = -1;
    }

    /** 一句对白（可来自某角色，也可为旁白：speakerId 为 null 则隐藏立绘与姓名）。 */
    public static final class SpeakStep extends Step {
        /** 说话者 id；null 表示旁白/内心独白。 */
        public final @Nullable String speakerId;
        /** 对话文本。 */
        public final Component text;
        /** 说话时使用的立绘姿势（null = 用角色的 base）。 */
        public final @Nullable String pose;

        public SpeakStep(@Nullable String speakerId, Component text, @Nullable String pose) {
            this.speakerId = speakerId;
            this.text = text;
            this.pose = pose;
        }
    }

    /** 切换（显示）立绘，并带一次滑入动画。 */
    public static final class ShowPortraitStep extends Step {
        public final String speakerId;
        public final String pose;

        public ShowPortraitStep(String speakerId, String pose) {
            this.speakerId = speakerId;
            this.pose = pose;
        }
    }

    /** 隐藏立绘。 */
    public static final class HidePortraitStep extends Step {
    }

    /** 切换背景贴图。 */
    public static final class SetBackgroundStep extends Step {
        public final ResourceLocation background;

        public SetBackgroundStep(ResourceLocation background) { this.background = background; }
    }

    /** 选项分支：暂停等待玩家选择。 */
    public static final class ChoiceStep extends Step {
        public final List<ChoiceOption> options;

        public ChoiceStep(List<ChoiceOption> options) { this.options = List.copyOf(options); }
    }

    /** 选项分支中的单条候选。 */
    public static final class ChoiceOption {
        /** 按钮显示文本。 */
        public final Component text;
        /** 选中后跳转的绝对步骤索引（build() 时解析）；未指定 label 时默认顺序下一条。 */
        public int target = -1;
        /** 内部用：目标 label 名（null 表示顺序下一条）。 */
        final @Nullable String targetLabel;

        public ChoiceOption(Component text, @Nullable String targetLabel) {
            this.text = text;
            this.targetLabel = targetLabel;
        }
    }

    /** 无条件跳转到某 label（由 build() 解析）。 */
    public static final class JumpStep extends Step {
        public int target = -1;
        final String targetLabel;

        public JumpStep(String targetLabel) { this.targetLabel = targetLabel; }
    }

    /** 结束演出。 */
    public static final class EndStep extends Step {
    }

    /* ==================================================================
     * 链式构建器
     * ================================================================== */

    public static final class Builder {

        private final List<Step> steps = new ArrayList<>();
        private final Map<String, Integer> labels = new HashMap<>();

        /** 旁白。 */
        public Builder say(String text) { return say(null, text, null); }

        /** 角色对白（使用其 base 立绘）。 */
        public Builder say(String speakerId, String text) { return say(speakerId, text, null); }

        /** 角色对白并指定其姿势。 */
        public Builder say(@Nullable String speakerId, String text, @Nullable String pose) {
            return step(new SpeakStep(speakerId, Component.literal(text), pose));
        }

        /** 显示角色某姿势立绘（带滑入动画）。 */
        public Builder show(String speakerId, String pose) {
            return step(new ShowPortraitStep(speakerId, pose));
        }

        /** 隐藏立绘。 */
        public Builder hidePortrait() { return step(new HidePortraitStep()); }

        /** 切换背景贴图。 */
        public Builder background(ResourceLocation tex) { return step(new SetBackgroundStep(tex)); }

        /** 定义选项分支：{@code choice(b -> b.opt("去森林", "forest").opt("回村"));} */
        public Builder choice(Consumer<ChoiceBuilder> consumer) {
            ChoiceBuilder cb = new ChoiceBuilder();
            consumer.accept(cb);
            return step(new ChoiceStep(cb.options));
        }

        /** 无条件跳转。 */
        public Builder jump(String label) { return step(new JumpStep(label)); }

        /** 在当前位置打一个跳转标签（供 jump / choice 引用）。 */
        public Builder label(String name) {
            labels.put(name, steps.size());
            return this;
        }

        /** 结束。 */
        public Builder end() { return step(new EndStep()); }

        public Builder step(Step s) {
            steps.add(s);
            return this;
        }

        /**
         * 解析 default-next、选项 target、跳转 target，并返回不可变剧本。
         *
         * @throws IllegalStateException 引用了未定义的 label
         */
        public GalGameScript build() {
            for (int i = 0; i < steps.size(); i++) {
                Step s = steps.get(i);
                s.next = i + 1; // 默认顺序下一条
                if (s instanceof JumpStep j) j.target = resolve(j.targetLabel, i);
                if (s instanceof ChoiceStep c) {
                    for (ChoiceOption o : c.options) {
                        o.target = o.targetLabel == null ? s.next : resolve(o.targetLabel, i);
                    }
                }
            }
            return new GalGameScript(steps, labels);
        }

        private int resolve(String label, int from) {
            Integer t = labels.get(label);
            if (t == null) {
                throw new IllegalStateException(
                        "GalGame script: 未定义跳转标签 '" + label + "' (引用自步骤 " + from + ")");
            }
            return t;
        }

        /** 选项子构建器：构建一个分支的多条候选。 */
        public static final class ChoiceBuilder {
            final List<ChoiceOption> options = new ArrayList<>();

            /** 加一条选项，未指定目标 label 时选中即顺序推进到下一条。 */
            public ChoiceBuilder opt(String text) { return opt(text, null); }

            /** 加一条选项并指定跳转 label。 */
            public ChoiceBuilder opt(String text, String jumpLabel) {
                options.add(new ChoiceOption(Component.literal(text), jumpLabel));
                return this;
            }
        }
    }
}
