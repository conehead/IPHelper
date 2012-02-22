
package com.connor.iphelper;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class IPHelper extends JavaPlugin implements Listener {
    public Logger log = Logger.getLogger("Minecraft");
    private YamlConfiguration yamlIPTable;
    private List<String> players = null;
    private HashMap<String, List<String>> ipTable = null;
    private List<String> bannedIPs = null;

    public void onEnable() {
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        initConfig();
        initIPTable();
        
        getServer().getPluginManager().registerEvents(new IPHelperLoginListener(this), this);
        getCommand("ip").setExecutor(this);
    }

    private void initConfig() {
        //Never mind, maybe later
    }

    private void initIPTable() {
        this.yamlIPTable = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "iptable.yml"));
        bannedIPs = new ArrayList<String>();
        ipTable = new HashMap<String, List<String>>();

        MemorySection section = (MemorySection)yamlIPTable.get("players");
        if (section != null) {
            Set<String> keys = section.getKeys(false);
            players = new ArrayList<String>(keys);
        }

        if (this.players == null) {
            players = new ArrayList<String>();
        }

        for (String playerName : this.players) {
            List<String> playerIPs = yamlIPTable.getStringList("players." + playerName);
            if (playerIPs != null) {
                ipTable.put(playerName, playerIPs);
            }
        }
        
        this.bannedIPs = yamlIPTable.getStringList("banned");
        if (this.bannedIPs == null) {
            bannedIPs = new ArrayList<String>();
        }
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (args.length > 1) {
            if (args[0].equalsIgnoreCase("lookup") && args.length > 2) {
                if (!sender.hasPermission("ip.lookup")) return true;
                if (args[1].equalsIgnoreCase("name") || args[1].equalsIgnoreCase("player") || args[1].equalsIgnoreCase("user")) {
                    return lookupName(sender, args[2]);
                } else if (args[1].equalsIgnoreCase("ip")) {
                    return lookupIP(sender, args[2]);
                }
            } else if (args[0].equalsIgnoreCase("ban") && args.length > 2) {
                if (!sender.hasPermission("ip.ban")) return true;
                if (args[1].equalsIgnoreCase("name") || args[1].equalsIgnoreCase("player") || args[1].equalsIgnoreCase("user")) {
                    return banName(sender, args[2]);
                } else if (args[1].equalsIgnoreCase("ip")) {
                    return banIP(sender, args[2]);
                }
            } else if (args[0].equalsIgnoreCase("unban") && args.length > 2) {
                if (!sender.hasPermission("ip.ban")) return true;
                if (args[1].equalsIgnoreCase("name") || args[1].equalsIgnoreCase("player") || args[1].equalsIgnoreCase("user")) {
                    return unbanName(sender, args[2]);
                } else if (args[1].equalsIgnoreCase("ip")) {
                    return unbanIP(sender, args[2]);
                }
            } else if (args[0].equalsIgnoreCase("clear")) {
                if (!(sender instanceof ConsoleCommandSender)) {
                    sender.sendMessage(ChatColor.GRAY + "Only console can clear IPs");
                } else {
                    return clearIPsFromName(sender, args[1]);
                }
            }
        } else if (args.length == 1 && args[0].equalsIgnoreCase("save")) {

        }
        return showHelp(sender);
    }
    
    private boolean showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "[IP] " + ChatColor.DARK_GREEN + "Help");
        sender.sendMessage(ChatColor.GRAY + "| " + ChatColor.YELLOW + "/ip <lookup|ban|unban> <(user/player/name)|ip> NameOrIP");
        sender.sendMessage(ChatColor.GRAY + "| For example: ");
        sender.sendMessage(ChatColor.GRAY + "| " + ChatColor.YELLOW + "/ip ban name ConnorJames");
        sender.sendMessage(ChatColor.GRAY + "| " + ChatColor.YELLOW + "/ip unban ip 127.0.0.1");
        sender.sendMessage(ChatColor.GRAY + "| " + ChatColor.YELLOW + "/ip lookup player ConnorJames");
        return true;
    }

    private boolean lookupName(CommandSender sender, String arg) {
        //Check for online players too
        String playerEntry = null;
        if (players.contains(arg)) { //Check in the list first for an exact match
            playerEntry = arg;
        } else { //Check on the server for players
            Player p = getServer().getPlayer(arg);
            if (p == null) {
                p = getServer().getPlayerExact(arg);
            }
            if (p != null && players.contains(p.getName())) playerEntry = p.getName();
        }
        
        if (playerEntry == null) {
            sender.sendMessage(ChatColor.GRAY + "No results found for \"" + arg + "\"");
            return true;
        }
        
        sender.sendMessage(ChatColor.GOLD + "[IP] " + ChatColor.DARK_GREEN + playerEntry + ":");

        String ipList = "";
        for (String s : ipTable.get(playerEntry)) {
            if (!ipList.equals("")) ipList += ", ";
            if (isIPBanned(s)) {
                ipList += ChatColor.DARK_RED;
            } else {
                ipList += ChatColor.DARK_GREEN;
            }
            ipList += s + ChatColor.GRAY;
        }
        sender.sendMessage(ChatColor.GRAY + "Logged in from: " + ipList);
        

        String sharedIPList = "";
        for (String playerName : players) {
            String toAppend = "";
            boolean banned = false;
            boolean foundMatch = false;
            if (playerName.equals(playerEntry)) continue;
            for (String ip : ipTable.get(playerName)) {
                if (isIPBanned(ip)) {
                    banned = true;
                }
                if (foundMatch) continue;
                for (String entryIP : ipTable.get(playerEntry)) {
                    if (ip.equals(entryIP)) {
                        foundMatch = true;
                    }
                }
            }
            if (foundMatch) {
                if (banned) {
                    toAppend += ChatColor.DARK_RED;
                } else {
                    toAppend += ChatColor.DARK_GREEN;
                }
                toAppend += playerName;
                
                if (sharedIPList.equals("")) {
                    sharedIPList += toAppend;
                } else {
                    sharedIPList += ChatColor.GRAY + ", " + toAppend;
                }
            }
        }

        sender.sendMessage(ChatColor.GRAY + "Shares IPs with: " + sharedIPList);
        return true;
    }
    
    private boolean lookupIP(CommandSender sender, String arg) {
        sender.sendMessage(ChatColor.GOLD + "[IP] " + ChatColor.DARK_GREEN + arg + ":");
        
        String playersWithIP = "";
        for (String playerName : players) {
            String toAppend = "";
            boolean banned = false;
            boolean foundMatch = false;
            for (String ip : ipTable.get(playerName)) {
                if (isIPBanned(ip)) {
                    banned = true;
                }
                if (foundMatch) continue;
                if (ip.equals(arg)) {
                    foundMatch = true;
                }
            }
            if (foundMatch) {
                if (banned) {
                    toAppend += ChatColor.DARK_RED;
                } else {
                    toAppend += ChatColor.DARK_GREEN;
                }
                toAppend += playerName;
                
                if (playersWithIP.equals("")) {
                    playersWithIP += toAppend;
                } else {
                    playersWithIP += ChatColor.GRAY + ", " + toAppend;
                }
            }
        }
        
        sender.sendMessage(ChatColor.GRAY + "Players: " + playersWithIP);
        return true;
    }
    
    private boolean banName(CommandSender sender, String arg) {
        //Check for online players too
        String playerEntry = null;
        if (players.contains(arg)) { //Check in the list first for an exact match
            playerEntry = arg;
        } else { //Check on the server for players
            Player p = getServer().getPlayer(arg);
            if (p == null) {
                p = getServer().getPlayerExact(arg);
            }
            if (p != null && players.contains(p.getName())) playerEntry = p.getName();
        }

        if (playerEntry == null) {
            sender.sendMessage(ChatColor.GRAY + "No results found for \"" + arg + "\"");
            return true;
        }
        
        sender.sendMessage(ChatColor.GOLD + "[IP] " + ChatColor.DARK_GREEN + "Banning player " + ChatColor.DARK_RED + playerEntry);
        if (!players.contains(playerEntry)) players.add(playerEntry);
        
        String bannedIPList = "";
        for (String ip : ipTable.get(playerEntry)) {
            if (!bannedIPs.contains(ip)) {
                bannedIPs.add(ip);
                if (bannedIPList.equals("")) {
                    bannedIPList += ChatColor.DARK_RED + ip;
                } else {
                    bannedIPList += ChatColor.GRAY + ", " + ChatColor.DARK_RED + ip;
                }
            }
        }
        
        if (getServer().getPlayerExact(playerEntry) != null) {
            getServer().getPlayerExact(playerEntry).kickPlayer("End of stream");
        }
        
        sender.sendMessage(ChatColor.GRAY + "Banned IPs: " + bannedIPList);
        save();
        return true;
    }
    
    private boolean banIP(CommandSender sender, String arg) {
        sender.sendMessage(ChatColor.GOLD + "[IP] " + ChatColor.DARK_GREEN + "Banning IP " + ChatColor.DARK_RED + arg);
        bannedIPs.add(arg);
        
        String bannedPlayerList = "";
        for (String playerName : players) {
            boolean match = false;
            
            for (String ip : ipTable.get(playerName)) {
                if (ip.equals(arg)) {
                    match = true;
                    break;
                }
            }
            
            if (match) {
                if (getServer().getPlayerExact(playerName) != null) {
                    getServer().getPlayerExact(playerName).kickPlayer("End of stream");
                }
                if (bannedPlayerList.equals("")) {
                    players.add(playerName);
                    bannedPlayerList += ChatColor.DARK_RED + playerName;
                } else {
                    bannedPlayerList += ChatColor.GRAY + ", " + ChatColor.DARK_RED + playerName;
                }
            }
        }
        sender.sendMessage(ChatColor.GRAY + "Associated players: " + bannedPlayerList);
        save();
        return true;
    }

    private boolean unbanName(CommandSender sender, String arg) {
        sender.sendMessage(ChatColor.GOLD + "[IP] " + ChatColor.DARK_GREEN + "Unbanning player " + arg);

        String bannedIPList = "";
        List<String> ips = ipTable.get(arg);
        if (ips == null) return true;
        for (String ip : ips) {
            bannedIPs.remove(ip);
            if (bannedIPList.equals("")) {
                bannedIPList += ChatColor.DARK_GREEN + ip;
            } else {
                bannedIPList += ChatColor.GRAY + ", " + ChatColor.DARK_GREEN + ip;
            }
        }

        sender.sendMessage(ChatColor.GRAY + "Unbanned IPs: " + bannedIPList);
        save();
        return true;
    }

    private boolean unbanIP(CommandSender sender, String arg) {
        sender.sendMessage(ChatColor.GOLD + "[IP] " + ChatColor.DARK_GREEN + "Unbanning IP " + arg);
        bannedIPs.remove(arg);

        String bannedPlayerList = "";
        for (String playerName : players) {
            boolean match = false;

            for (String ip : ipTable.get(playerName)) {
                if (ip.equals(arg)) {
                    match = true;
                    break;
                }
            }

            if (match) {
                if (bannedPlayerList.equals("")) {
                    bannedPlayerList += ChatColor.DARK_GREEN + playerName;
                } else {
                    bannedPlayerList += ChatColor.GRAY + ", " + ChatColor.DARK_GREEN + playerName;
                }
            }
        }
        sender.sendMessage(ChatColor.GRAY + "Associated players: " + bannedPlayerList);
        save();
        return true;
    }
    
    private boolean clearIPsFromName(CommandSender sender, String arg) {
        ipTable.remove(arg);
        players.remove(arg);
        sender.sendMessage(ChatColor.GOLD + "[IP] " + ChatColor.DARK_GREEN + "Data removed for " + arg);
        save();
        return true;
    }
    

    public void registerPlayer(String name, InetAddress address) {
        if (!players.contains(name)) {
            ArrayList<String> ips = new ArrayList<String>();
            ips.add(address.getHostAddress());
            players.add(name);
            ipTable.put(name, ips);
        } else {
            List<String> ips = ipTable.get(name);
            if (!ips.contains(address.getHostAddress())) {
                ips.add(address.getHostAddress());
            }
        }
        save();
    }
    
    public void save() {
        for (String playerName : players) {
            yamlIPTable.set("players." + playerName, ipTable.get(playerName));
        }
        yamlIPTable.set("banned", bannedIPs);
        try {
            yamlIPTable.save(new File(getDataFolder(), "iptable.yml"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isIPBanned(String address) {
        return bannedIPs.contains(address);
    }
}