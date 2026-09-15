package xyz.voxensmp.voxentopup.listener;
import org.bukkit.event.*; import org.bukkit.event.player.PlayerCommandPreprocessEvent; import org.bukkit.event.server.ServerCommandEvent; import xyz.voxensmp.voxentopup.service.RankTopUpService;
public final class LuckPermsCommandListener implements Listener {
 private final RankTopUpService service; public LuckPermsCommandListener(RankTopUpService service){this.service=service;}
 @EventHandler(ignoreCancelled=true) public void playerCommand(PlayerCommandPreprocessEvent e){service.inspect(e.getMessage());}
 @EventHandler public void consoleCommand(ServerCommandEvent e){service.inspect(e.getCommand());}
}
