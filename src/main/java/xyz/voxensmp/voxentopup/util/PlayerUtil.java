package xyz.voxensmp.voxentopup.util;
import org.bukkit.Bukkit; import org.bukkit.OfflinePlayer; import java.util.Optional;
public final class PlayerUtil { private PlayerUtil(){} public static Optional<OfflinePlayer> find(String name){ for(OfflinePlayer p:Bukkit.getOfflinePlayers()) if(p.getName()!=null&&p.getName().equalsIgnoreCase(name)) return Optional.of(p); return Optional.empty(); } }
