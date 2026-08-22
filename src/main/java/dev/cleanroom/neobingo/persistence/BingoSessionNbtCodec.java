package dev.cleanroom.neobingo.persistence;

import dev.cleanroom.neobingo.domain.BingoCard;
import dev.cleanroom.neobingo.domain.BingoGameSnapshot;
import dev.cleanroom.neobingo.domain.BingoSessionSnapshot;
import dev.cleanroom.neobingo.domain.GameMode;
import dev.cleanroom.neobingo.domain.ObjectiveId;
import dev.cleanroom.neobingo.domain.PlayerId;
import dev.cleanroom.neobingo.domain.SessionState;
import dev.cleanroom.neobingo.domain.TeamId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/** 在领域快照和版本化 NBT 数据之间转换。 */
public final class BingoSessionNbtCodec {
    private static final int SCHEMA_VERSION = 1;

    private BingoSessionNbtCodec() {}

    public static CompoundTag encode(BingoSessionSnapshot snapshot) {
        CompoundTag root = new CompoundTag();
        root.putInt("schema_version", SCHEMA_VERSION);
        root.putString("state", snapshot.state().name());
        root.put("assignments", encodeAssignments(snapshot.assignments()));
        snapshot.game().ifPresent(game -> root.put("game", encodeGame(game)));
        snapshot.seed().ifPresent(seed -> root.putLong("seed", seed));
        snapshot.winner().ifPresent(winner -> root.putString("winner", winner.value()));
        return root;
    }

    public static BingoSessionSnapshot decode(CompoundTag root) {
        require(root.contains("schema_version", Tag.TAG_INT), "Missing schema version");
        int version = root.getInt("schema_version");
        require(version == SCHEMA_VERSION, "Unsupported schema version: " + version);
        SessionState state = enumValue(SessionState.class, requiredString(root, "state"), "state");
        Map<PlayerId, TeamId> assignments = decodeAssignments(requiredList(root, "assignments", Tag.TAG_COMPOUND));

        Optional<BingoGameSnapshot> game = root.contains("game", Tag.TAG_COMPOUND)
                ? Optional.of(decodeGame(root.getCompound("game")))
                : Optional.empty();
        Optional<Long> seed = root.contains("seed", Tag.TAG_LONG)
                ? Optional.of(root.getLong("seed"))
                : Optional.empty();
        Optional<TeamId> winner = root.contains("winner", Tag.TAG_STRING)
                ? Optional.of(new TeamId(root.getString("winner")))
                : Optional.empty();
        return new BingoSessionSnapshot(state, assignments, game, seed, winner);
    }

    private static ListTag encodeAssignments(Map<PlayerId, TeamId> assignments) {
        ListTag list = new ListTag();
        assignments.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(player -> player.value().toString())))
                .forEach(entry -> {
                    CompoundTag assignment = new CompoundTag();
                    assignment.putUUID("player", entry.getKey().value());
                    assignment.putString("team", entry.getValue().value());
                    list.add(assignment);
                });
        return list;
    }

    private static Map<PlayerId, TeamId> decodeAssignments(ListTag list) {
        Map<PlayerId, TeamId> assignments = new LinkedHashMap<>();
        for (int index = 0; index < list.size(); index++) {
            CompoundTag assignment = list.getCompound(index);
            require(assignment.hasUUID("player"), "Assignment is missing a player UUID");
            TeamId previous = assignments.put(
                    new PlayerId(assignment.getUUID("player")),
                    new TeamId(requiredString(assignment, "team")));
            require(previous == null, "Player is assigned more than once");
        }
        return assignments;
    }

    private static CompoundTag encodeGame(BingoGameSnapshot snapshot) {
        CompoundTag game = new CompoundTag();
        game.putInt("card_size", snapshot.card().size());
        game.putString("mode", snapshot.mode().name());

        ListTag objectives = new ListTag();
        snapshot.card().objectives().forEach(objective -> objectives.add(StringTag.valueOf(objective.value())));
        game.put("objectives", objectives);

        ListTag claims = new ListTag();
        snapshot.claims().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    CompoundTag claim = new CompoundTag();
                    claim.putInt("tile", entry.getKey());
                    ListTag teams = new ListTag();
                    entry.getValue().stream()
                            .sorted(Comparator.comparing(TeamId::value))
                            .forEach(team -> teams.add(StringTag.valueOf(team.value())));
                    claim.put("teams", teams);
                    claims.add(claim);
                });
        game.put("claims", claims);
        return game;
    }

    private static BingoGameSnapshot decodeGame(CompoundTag game) {
        require(game.contains("card_size", Tag.TAG_INT), "Game is missing card size");
        int cardSize = game.getInt("card_size");
        GameMode mode = enumValue(GameMode.class, requiredString(game, "mode"), "mode");

        ListTag objectiveTags = requiredList(game, "objectives", Tag.TAG_STRING);
        var objectives = new ArrayList<ObjectiveId>(objectiveTags.size());
        for (int index = 0; index < objectiveTags.size(); index++) {
            objectives.add(new ObjectiveId(objectiveTags.getString(index)));
        }
        BingoCard card = new BingoCard(cardSize, objectives);

        Map<Integer, Set<TeamId>> claims = new LinkedHashMap<>();
        ListTag claimTags = requiredList(game, "claims", Tag.TAG_COMPOUND);
        for (int index = 0; index < claimTags.size(); index++) {
            CompoundTag claim = claimTags.getCompound(index);
            require(claim.contains("tile", Tag.TAG_INT), "Claim is missing tile index");
            int tileIndex = claim.getInt("tile");
            Set<TeamId> teams = new LinkedHashSet<>();
            ListTag teamTags = requiredList(claim, "teams", Tag.TAG_STRING);
            for (int teamIndex = 0; teamIndex < teamTags.size(); teamIndex++) {
                require(teams.add(new TeamId(teamTags.getString(teamIndex))), "Claim contains a duplicate team");
            }
            require(claims.put(tileIndex, teams) == null, "Tile is listed more than once");
        }
        return new BingoGameSnapshot(card, mode, claims);
    }

    private static String requiredString(CompoundTag tag, String key) {
        require(tag.contains(key, Tag.TAG_STRING), "Missing string field: " + key);
        return tag.getString(key);
    }

    private static ListTag requiredList(CompoundTag tag, String key, int elementType) {
        require(tag.contains(key, Tag.TAG_LIST), "Missing list field: " + key);
        ListTag list = tag.getList(key, elementType);
        require(list.isEmpty() || list.getElementType() == elementType, "Unexpected list type: " + key);
        return list;
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value, String field) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid " + field + ": " + value, exception);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
