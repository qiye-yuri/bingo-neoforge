package dev.cleanroom.neobingo.world;

import dev.cleanroom.neobingo.domain.SessionState;
import dev.cleanroom.neobingo.persistence.NeoBingoSavedData;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** 在胜利或排位倒计时结束后安全送回玩家并卸载比赛世界。 */
public final class MatchWorldLifecycleTicker {
    private MatchWorldLifecycleTicker() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (RuntimeMatchWorldManager.active().isEmpty()) {
            return;
        }
        NeoBingoSavedData.get(event.getServer()).restoreSession()
                .filter(session -> session.state() == SessionState.FINISHED)
                .ifPresent(session -> RuntimeMatchWorldManager.finish(event.getServer()));
    }
}
