package xyz.voxensmp.voxentopup.service;
import org.bukkit.OfflinePlayer; import xyz.voxensmp.voxentopup.VoxenTopUp; import xyz.voxensmp.voxentopup.config.TopUpConfig; import xyz.voxensmp.voxentopup.util.*; import java.util.*;
public final class ContractTopUpService {
 private final VoxenTopUp plugin; private volatile TopUpConfig config; public ContractTopUpService(VoxenTopUp plugin,TopUpConfig config){this.plugin=plugin;this.config=config;} public void setConfig(TopUpConfig c){config=c;} public Set<String> contracts(){return config.enabledContracts();}
 public String announce(String key,String name){ TopUpConfig c=config; TopUpConfig.Product product=c.contract(key); if(product==null)return "unknown-contract"; if(!product.enabled())return "contract-disabled"; Optional<OfflinePlayer> target=PlayerUtil.find(name); if(target.isEmpty())return "player-not-found"; OfflinePlayer p=target.get(); if(c.requireOnline()&&!p.isOnline())return "player-not-found";
  Map<String,String> values=Map.of("player",p.getName()==null?name:p.getName(),"uuid",p.getUniqueId().toString(),"contract",product.displayName(),"price",c.price(product,false)); MessageUtil.broadcast(c.contractLines(),values); plugin.getLogger().info("Contract top-up: "+values.get("player")+" -> "+product.displayName()+" ("+values.get("price")+")"); return null; }
}
