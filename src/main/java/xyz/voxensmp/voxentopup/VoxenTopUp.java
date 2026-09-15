package xyz.voxensmp.voxentopup;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.voxensmp.voxentopup.command.TopUpCommand;
import xyz.voxensmp.voxentopup.config.TopUpConfig;
import xyz.voxensmp.voxentopup.listener.LuckPermsCommandListener;
import xyz.voxensmp.voxentopup.service.ContractTopUpService;
import xyz.voxensmp.voxentopup.service.RankTopUpService;

public final class VoxenTopUp extends JavaPlugin {
    private TopUpConfig topUpConfig;
    private RankTopUpService rankService;
    private ContractTopUpService contractService;

    @Override public void onEnable() {
        try { LuckPermsProvider.get(); } catch (IllegalStateException ex) {
            getLogger().severe("LuckPerms API is unavailable; VoxenTopUp requires LuckPerms and will disable.");
            getServer().getPluginManager().disablePlugin(this); return;
        }
        saveDefaultConfig();
        try { topUpConfig = TopUpConfig.load(this); } catch (IllegalArgumentException ex) {
            getLogger().severe("Invalid config.yml: " + ex.getMessage()); getServer().getPluginManager().disablePlugin(this); return;
        }
        LuckPerms luckPerms = LuckPermsProvider.get();
        rankService = new RankTopUpService(this, luckPerms, topUpConfig);
        contractService = new ContractTopUpService(this, topUpConfig);
        getServer().getPluginManager().registerEvents(new LuckPermsCommandListener(rankService), this);
        TopUpCommand command = new TopUpCommand(this, contractService);
        PluginCommand topup = getCommand("topup"); PluginCommand root = getCommand("voxentopup");
        if (topup == null || root == null) throw new IllegalStateException("Commands missing from plugin.yml");
        topup.setExecutor(command); topup.setTabCompleter(command); root.setExecutor(command); root.setTabCompleter(command);
    }
    public boolean reloadTopUpConfig() {
        try { reloadConfig(); TopUpConfig candidate = TopUpConfig.load(this); topUpConfig = candidate; rankService.setConfig(candidate); contractService.setConfig(candidate); return true; }
        catch (IllegalArgumentException ex) { getLogger().warning("Configuration reload rejected: " + ex.getMessage()); return false; }
    }
    public TopUpConfig config() { return topUpConfig; }
}
