package dev.cleanroom.neobingo.domain.rule;

/** 仅在查看者所属队伍认领格子后公开目标的规则。 */
public enum HiddenUntilClaimedRule implements VisibilityRule {
    INSTANCE;

    @Override
    public boolean isVisible(boolean claimedByViewer) {
        return claimedByViewer;
    }
}
