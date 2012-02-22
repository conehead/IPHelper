package com.connor.iphelper;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPreLoginEvent;

public class IPHelperLoginListener implements Listener {
    private IPHelper plugin;
    
    public IPHelperLoginListener(IPHelper plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerLogin(PlayerPreLoginEvent event) {
        if (plugin.isIPBanned(event.getAddress().getHostAddress())) {
            plugin.log.info("[IP] " + event.getName() + " has a banned IP; disconnecting.");
            event.disallow(PlayerPreLoginEvent.Result.KICK_WHITELIST, "You are not whitelisted on this server!");
        }
        plugin.registerPlayer(event.getName(), event.getAddress());
    }
}
