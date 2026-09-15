package xyz.voxensmp.voxentopup.service;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.EventSubscription;
import net.luckperms.api.event.node.NodeAddEvent;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import xyz.voxensmp.voxentopup.VoxenTopUp;
import xyz.voxensmp.voxentopup.config.TopUpConfig;
import xyz.voxensmp.voxentopup.util.MessageUtil;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** Correlates strict LuckPerms parent-set commands with actual global inheritance nodes. */
public final class RankTopUpService {
    private final VoxenTopUp plugin;
    private final LuckPerms luckPerms;
    private final ConcurrentMap<String, PendingTransaction> pendingByName = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, PendingTransaction> pendingByUuid = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> recentBroadcasts = new ConcurrentHashMap<>();
    private final EventSubscription<NodeAddEvent> nodeAddSubscription;
    private volatile TopUpConfig config;

    public RankTopUpService(VoxenTopUp plugin, LuckPerms luckPerms, TopUpConfig config) {
        this.plugin = plugin;
        this.luckPerms = luckPerms;
        this.config = config;
        // Registered once per plugin lifetime, not on config reload.
        this.nodeAddSubscription = luckPerms.getEventBus().subscribe(NodeAddEvent.class, this::onNodeAdded);
    }

    public void setConfig(TopUpConfig config) {
        this.config = config;
    }

    public void close() {
        nodeAddSubscription.close();
        pendingByName.clear();
        pendingByUuid.clear();
    }

    public void inspect(String rawCommand) {
        TopUpConfig current = config;
        if (!current.rankDetection()) return;

        String[] arguments = rawCommand.trim().replaceFirst("^/", "").split("\\s+");
        if (arguments.length != 6
                || !(arguments[0].equalsIgnoreCase("lp") || arguments[0].equalsIgnoreCase("luckperms"))
                || !arguments[1].equalsIgnoreCase("user")
                || !arguments[3].equalsIgnoreCase("parent")
                || !arguments[4].equalsIgnoreCase("set")) {
            return;
        }

        String playerName = arguments[2];
        String group = arguments[5];
        TopUpConfig.Product product = current.rank(group);
        if (product == null || !product.enabled()) return;

        PendingTransaction pending = new PendingTransaction(playerName, group, product, System.currentTimeMillis());
        pendingByName.put(pending.nameKey(), pending);
        if (current.debug()) plugin.getLogger().info("Detected candidate rank command: " + playerName + " -> " + group);

        // Resolve an offline user's UUID without blocking command processing. This also improves event correlation.
        luckPerms.getUserManager().lookupUniqueId(playerName).whenComplete((uuid, error) -> {
            if (error != null || uuid == null || pending.isExpired(pendingWindowMillis())) return;
            pending.uuid = uuid;
            pendingByUuid.put(pending.uuidKey(), pending);
        });
        Bukkit.getScheduler().runTaskLater(plugin, () -> verifyFallback(pending), current.delayTicks());
    }

    private void onNodeAdded(NodeAddEvent event) {
        if (!(event.getTarget() instanceof User user) || !(event.getNode() instanceof InheritanceNode node)) return;
        // Commands accepted above have no context arguments: only a global node may satisfy them.
        if (!node.getContexts().isEmpty()) return;

        PendingTransaction pending = pendingByUuid.get(key(user.getUniqueId().toString(), node.getGroupName()));
        if (pending == null) pending = pendingByName.get(key(user.getUsername(), node.getGroupName()));
        if (pending == null || pending.isExpired(pendingWindowMillis())
                || !node.getGroupName().equalsIgnoreCase(pending.group)) return;

        verifyAndBroadcast(pending, user);
    }

    private void verifyFallback(PendingTransaction pending) {
        if (pending.isExpired(pendingWindowMillis())) {
            discard(pending);
            return;
        }
        CompletableFuture<User> userFuture = pending.uuid == null
                ? luckPerms.getUserManager().lookupUniqueId(pending.playerName)
                    .thenCompose(uuid -> uuid == null ? CompletableFuture.completedFuture(null) : luckPerms.getUserManager().loadUser(uuid))
                : luckPerms.getUserManager().loadUser(pending.uuid);
        userFuture.whenComplete((user, error) -> {
            if (error != null) {
                plugin.getLogger().warning("Failed to verify LuckPerms parent for player " + pending.playerName + ": " + error.getMessage());
                discard(pending);
            } else if (user != null) {
                pending.uuid = user.getUniqueId();
                pendingByUuid.put(pending.uuidKey(), pending);
                verifyAndBroadcast(pending, user);
            } else {
                discard(pending);
            }
        });
    }

    private void verifyAndBroadcast(PendingTransaction pending, User user) {
        if (!hasGlobalParent(user, pending.group)) {
            if (config.debug()) plugin.getLogger().info("Rank verification did not match for " + pending.playerName + " -> " + pending.group);
            return;
        }
        if (!pending.claimed.compareAndSet(false, true)) return;
        discard(pending);
        if (config.debug()) plugin.getLogger().info("Rank verification succeeded: " + pending.playerName + " -> " + pending.group);

        String prefix = Optional.ofNullable(user.getCachedData().getMetaData().getPrefix()).orElse(pending.product.displayName());
        Bukkit.getScheduler().runTask(plugin, () -> broadcast(user, pending, prefix));
    }

    private static boolean hasGlobalParent(User user, String expectedGroup) {
        return user.getNodes().stream()
                .filter(InheritanceNode.class::isInstance)
                .map(InheritanceNode.class::cast)
                .anyMatch(node -> node.getContexts().isEmpty() && node.getGroupName().equalsIgnoreCase(expectedGroup));
    }

    private void broadcast(User user, PendingTransaction pending, String prefix) {
        TopUpConfig current = config;
        long now = System.currentTimeMillis();
        String key = user.getUniqueId() + ":" + normalize(pending.group);
        Long previous = recentBroadcasts.put(key, now);
        recentBroadcasts.entrySet().removeIf(entry -> now - entry.getValue() > current.dedupeMillis());
        if (previous != null && now - previous < current.dedupeMillis()) return;

        String player = user.getUsername() == null ? user.getUniqueId().toString() : user.getUsername();
        Map<String, String> values = Map.of("player", player, "uuid", user.getUniqueId().toString(), "group", pending.group,
                "rank", pending.product.displayName(), "prefix", prefix, "price", current.price(pending.product, true));
        MessageUtil.broadcast(current.rankLines(), values);
        plugin.getLogger().info("Rank top-up detected: " + player + " -> " + pending.product.displayName() + " (" + values.get("price") + ")");
    }

    private long pendingWindowMillis() {
        // Pending state is not the broadcast suppression window; retain it long enough for LP event timing.
        return Math.max(config.dedupeMillis(), 30_000L);
    }

    private void discard(PendingTransaction pending) {
        pendingByName.remove(pending.nameKey(), pending);
        if (pending.uuid != null) pendingByUuid.remove(pending.uuidKey(), pending);
    }

    private static String key(String player, String group) {
        return normalize(player) + ":" + normalize(group);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static final class PendingTransaction {
        private final String playerName;
        private final String group;
        private final TopUpConfig.Product product;
        private final long createdAt;
        private final AtomicBoolean claimed = new AtomicBoolean();
        private volatile UUID uuid;

        private PendingTransaction(String playerName, String group, TopUpConfig.Product product, long createdAt) {
            this.playerName = playerName;
            this.group = group;
            this.product = product;
            this.createdAt = createdAt;
        }
        private String nameKey() { return key(playerName, group); }
        private String uuidKey() { return key(uuid.toString(), group); }
        private boolean isExpired(long windowMillis) { return System.currentTimeMillis() - createdAt > windowMillis; }
    }
}
