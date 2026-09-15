package xyz.voxensmp.voxentopup.config;

import org.bukkit.configuration.ConfigurationSection;
import xyz.voxensmp.voxentopup.VoxenTopUp;
import java.util.*;

/** Immutable, validated runtime configuration. */
public final class TopUpConfig {
    public record Product(String key, String displayName, long price, boolean enabled) {}
    private final boolean debug, rankDetection, requireOnline;
    private final long delayTicks, dedupeMillis;
    private final String rankPriceFormat, contractPriceFormat;
    private final Map<String, Product> ranks, contracts;
    private final List<String> rankLines, contractLines;
    private final Map<String, String> messages;
    private TopUpConfig(boolean debug, boolean rankDetection, boolean requireOnline, long delayTicks, long dedupeMillis, String rankPriceFormat, String contractPriceFormat, Map<String, Product> ranks, Map<String, Product> contracts, List<String> rankLines, List<String> contractLines, Map<String, String> messages) {
        this.debug=debug; this.rankDetection=rankDetection; this.requireOnline=requireOnline; this.delayTicks=delayTicks; this.dedupeMillis=dedupeMillis; this.rankPriceFormat=rankPriceFormat; this.contractPriceFormat=contractPriceFormat; this.ranks=ranks; this.contracts=contracts; this.rankLines=rankLines; this.contractLines=contractLines; this.messages=messages;
    }
    public static TopUpConfig load(VoxenTopUp plugin) {
        var c=plugin.getConfig();
        long delay=c.getLong("settings.rank-detection.verification-delay-ticks",2); if(delay<0) throw new IllegalArgumentException("verification-delay-ticks cannot be negative");
        long window=c.getLong("settings.rank-detection.deduplication-window-seconds",10); if(window<0) throw new IllegalArgumentException("deduplication-window-seconds cannot be negative");
        return new TopUpConfig(c.getBoolean("settings.debug"),c.getBoolean("settings.rank-detection.enabled",true),c.getBoolean("settings.contract.require-online-player",false),delay,Math.multiplyExact(window,1000L),c.getString("settings.price-format.rank","Rp%price%"),c.getString("settings.price-format.contract","Rp%price%"),products(c,"ranks"),products(c,"contracts"),lines(c,"broadcast.rank"),lines(c,"broadcast.contract"),messageMap(c.getConfigurationSection("messages")));
    }
    private static Map<String,Product> products(org.bukkit.configuration.file.FileConfiguration c,String path) {
        ConfigurationSection s=c.getConfigurationSection(path); Map<String,Product> out=new HashMap<>(); if(s==null) return Map.of();
        for(String key:s.getKeys(false)) { ConfigurationSection p=s.getConfigurationSection(key); if(p==null) continue; long price=p.getLong("price",-1); if(price<0) throw new IllegalArgumentException(path+"."+key+" has invalid non-negative price"); out.put(key.toLowerCase(Locale.ROOT),new Product(key,p.getString("display-name",key),price,p.getBoolean("enabled",true))); }
        return Collections.unmodifiableMap(out);
    }
    private static List<String> lines(org.bukkit.configuration.file.FileConfiguration c,String path) { return c.getBoolean(path+".enabled", true) ? Collections.unmodifiableList(c.getStringList(path+".lines")) : List.of(); }
    private static Map<String,String> messageMap(ConfigurationSection s) { Map<String,String> out=new HashMap<>(); if(s!=null) for(String k:s.getKeys(false)) out.put(k,s.getString(k,"")); return Collections.unmodifiableMap(out); }
    public Product rank(String key){return ranks.get(key.toLowerCase(Locale.ROOT));} public Product contract(String key){return contracts.get(key.toLowerCase(Locale.ROOT));}
    public Set<String> enabledContracts(){Set<String> s=new TreeSet<>(); contracts.values().stream().filter(Product::enabled).forEach(p->s.add(p.key())); return s;}
    public boolean debug(){return debug;} public boolean rankDetection(){return rankDetection;} public boolean requireOnline(){return requireOnline;} public long delayTicks(){return delayTicks;} public long dedupeMillis(){return dedupeMillis;}
    public List<String> rankLines(){return rankLines;} public List<String> contractLines(){return contractLines;} public String message(String key){return messages.getOrDefault(key,key);} public String price(Product p,boolean rank){return (rank?rankPriceFormat:contractPriceFormat).replace("%price%",String.format(Locale.forLanguageTag("id-ID"),"%,d",p.price()).replace(',', '.'));}
}
