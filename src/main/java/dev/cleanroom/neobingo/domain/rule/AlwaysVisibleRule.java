package dev.cleanroom.neobingo.domain.rule;

/** 始终公开卡片目标的可见性规则。 */
public enum AlwaysVisibleRule implements VisibilityRule {
    INSTANCE;

    @Override
    public boolean isVisible(boolean claimedByViewer) {
        return true;
    }
}
