package dev.cleanroom.neobingo.domain.rule;

/** 决定某个格子的目标是否应向查看者显示。 */
@FunctionalInterface
public interface VisibilityRule {
    boolean isVisible(boolean claimedByViewer);
}
