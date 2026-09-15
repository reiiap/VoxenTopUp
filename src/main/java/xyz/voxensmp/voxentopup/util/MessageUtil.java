package xyz.voxensmp.voxentopup.util;
import net.kyori.adventure.text.Component; import net.kyori.adventure.text.minimessage.MiniMessage; import org.bukkit.Bukkit; import java.util.*;
/** Centralized placeholder and safe MiniMessage/legacy formatting. */
public final class MessageUtil {
 private static final MiniMessage MINI=MiniMessage.miniMessage(); private static final Map<Character,String> LEGACY=Map.ofEntries(Map.entry('0',"black"),Map.entry('1',"dark_blue"),Map.entry('2',"dark_green"),Map.entry('3',"dark_aqua"),Map.entry('4',"dark_red"),Map.entry('5',"dark_purple"),Map.entry('6',"gold"),Map.entry('7',"gray"),Map.entry('8',"dark_gray"),Map.entry('9',"blue"),Map.entry('a',"green"),Map.entry('b',"aqua"),Map.entry('c',"red"),Map.entry('d',"light_purple"),Map.entry('e',"yellow"),Map.entry('f',"white"),Map.entry('l',"bold"),Map.entry('o',"italic"),Map.entry('n',"underlined"),Map.entry('m',"strikethrough"),Map.entry('r',"reset")); private MessageUtil(){}
 public static Component render(String line,Map<String,String> values){String text=line;for(var e:values.entrySet())text=text.replace("%"+e.getKey()+"%",e.getValue()); try{return MINI.deserialize(legacyToMini(text));}catch(Exception ex){return Component.text(text);}}
 private static String legacyToMini(String text){StringBuilder out=new StringBuilder();for(int i=0;i<text.length();i++){char ch=text.charAt(i);if(ch=='&'&&i+1<text.length()){String tag=LEGACY.get(Character.toLowerCase(text.charAt(i+1)));if(tag!=null){out.append('<').append(tag).append('>');i++;continue;}}out.append(ch);}return out.toString();}
 public static void broadcast(List<String> lines,Map<String,String> values){for(String line:lines)Bukkit.broadcast(render(line,values));}
}
