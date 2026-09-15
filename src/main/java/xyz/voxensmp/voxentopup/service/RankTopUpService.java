package xyz.voxensmp.voxentopup.service;
import net.luckperms.api.LuckPerms; import net.luckperms.api.model.user.User; import org.bukkit.Bukkit; import xyz.voxensmp.voxentopup.VoxenTopUp; import xyz.voxensmp.voxentopup.config.TopUpConfig; import xyz.voxensmp.voxentopup.util.MessageUtil;
import java.util.*; import java.util.concurrent.*;
public final class RankTopUpService {
 private final VoxenTopUp plugin; private final LuckPerms lp; private volatile TopUpConfig config; private final ConcurrentMap<String,Long> recent=new ConcurrentHashMap<>();
 public RankTopUpService(VoxenTopUp plugin,LuckPerms lp,TopUpConfig config){this.plugin=plugin;this.lp=lp;this.config=config;} public void setConfig(TopUpConfig config){this.config=config;}
 public void inspect(String raw){
  TopUpConfig c=config; if(!c.rankDetection())return; String[] a=raw.trim().replaceFirst("^/","").split("\\s+");
  if(a.length!=6 || !(a[0].equalsIgnoreCase("lp")||a[0].equalsIgnoreCase("luckperms")) || !a[1].equalsIgnoreCase("user") || !a[3].equalsIgnoreCase("parent") || !a[4].equalsIgnoreCase("set")) return;
  String target=a[2], group=a[5]; TopUpConfig.Product product=c.rank(group); if(product==null||!product.enabled())return;
  if(c.debug())plugin.getLogger().info("Detected candidate rank command: "+target+" -> "+group);
  Bukkit.getScheduler().runTaskLater(plugin,()->verify(target,group,product),c.delayTicks());
 }
 private void verify(String name,String expected,TopUpConfig.Product product){
  lp.getUserManager().lookupUniqueId(name).thenCompose(id-> id==null?CompletableFuture.completedFuture(null):lp.getUserManager().loadUser(id)).whenComplete((user,error)->{
   if(error!=null){plugin.getLogger().warning("Failed to verify LuckPerms group for player "+name+": "+error.getMessage());return;} if(user==null)return;
   String primary=user.getPrimaryGroup(); if(!primary.equalsIgnoreCase(expected)){if(config.debug())plugin.getLogger().info("Rank verification did not match for "+name);return;}
   String prefix=Optional.ofNullable(user.getCachedData().getMetaData().getPrefix()).orElse(product.displayName());
   Bukkit.getScheduler().runTask(plugin,()->broadcast(user,expected,product,prefix));
  });
 }
 private void broadcast(User user,String group,TopUpConfig.Product product,String prefix){
  TopUpConfig c=config; String key=user.getUniqueId()+":"+group.toLowerCase(Locale.ROOT); long now=System.currentTimeMillis(); Long prior=recent.put(key,now); recent.entrySet().removeIf(e->now-e.getValue()>c.dedupeMillis()); if(prior!=null&&now-prior<c.dedupeMillis())return;
  Map<String,String> v=Map.of("player",user.getUsername()==null?user.getUniqueId().toString():user.getUsername(),"uuid",user.getUniqueId().toString(),"group",group,"rank",product.displayName(),"prefix",prefix,"price",c.price(product,true)); MessageUtil.broadcast(c.rankLines(),v); plugin.getLogger().info("Rank top-up detected: "+v.get("player")+" -> "+product.displayName()+" ("+v.get("price")+")");
 }
}
