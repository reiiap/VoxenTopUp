package xyz.voxensmp.voxentopup.command;
import org.bukkit.command.*; import org.bukkit.entity.Player; import xyz.voxensmp.voxentopup.VoxenTopUp; import xyz.voxensmp.voxentopup.service.ContractTopUpService; import xyz.voxensmp.voxentopup.util.MessageUtil; import java.util.*;
public final class TopUpCommand implements CommandExecutor, TabCompleter {
 private final VoxenTopUp plugin; private final ContractTopUpService contracts; public TopUpCommand(VoxenTopUp plugin,ContractTopUpService contracts){this.plugin=plugin;this.contracts=contracts;}
 public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
  if(command.getName().equalsIgnoreCase("voxentopup")){if(!sender.hasPermission("voxentopup.reload")){send(sender,"no-permission");return true;} if(args.length==1&&args[0].equalsIgnoreCase("reload")){if(plugin.reloadTopUpConfig())send(sender,"reload-success");else sender.sendMessage("§c[VoxenTopUp] Reload gagal; konfigurasi sebelumnya tetap aktif. Periksa console.");}else send(sender,"invalid-usage"); return true;}
  if(!sender.hasPermission("voxentopup.topup")){send(sender,"no-permission");return true;} if(args.length!=2){send(sender,"invalid-usage");return true;} String result=contracts.announce(args[0],args[1]); if(result!=null)send(sender,result); return true;
 }
 private void send(CommandSender s,String key){s.sendMessage(MessageUtil.render(plugin.config().message(key),Map.of()));}
 public List<String> onTabComplete(CommandSender s,Command c,String l,String[] a){ if(c.getName().equalsIgnoreCase("voxentopup"))return a.length==1?List.of("reload"):List.of(); if(a.length==1)return contracts.contracts().stream().filter(x->x.toLowerCase(Locale.ROOT).startsWith(a[0].toLowerCase(Locale.ROOT))).toList(); if(a.length==2)return plugin.getServer().getOnlinePlayers().stream().map(Player::getName).filter(n->n.toLowerCase(Locale.ROOT).startsWith(a[1].toLowerCase(Locale.ROOT))).toList(); return List.of(); }
}
