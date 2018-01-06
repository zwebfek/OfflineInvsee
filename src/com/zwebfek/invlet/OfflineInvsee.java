package com.zwebfek.invlet;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.Map.Entry;

public class OfflineInvsee extends JavaPlugin implements Listener {

    public void onDisable() {
        System.out.print("[OfflineInvsee] disabled");
    }

    public void onEnable() {
        loadConfiguration();
        PluginManager pm = Bukkit.getServer().getPluginManager();
        pm.registerEvents(this, this);
        System.out.print("[OfflineInvsee] enabled");
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String usage = "Usage: /offlineinvsee <player>";
        if (cmd.getName().equalsIgnoreCase("offlineinvsee")) {
            if (args.length != 1) {
                sender.sendMessage(usage);
                return false;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.BLUE + "[OfflineInvsee] " + ChatColor.RESET + "Only players can execute this command.");
                return false;
            }
            if (sender.hasPermission("OfflineInvsee.basic") || sender.isOp()) {
                Player player = (Player) sender;
                player.closeInventory();
                String invString = getConfig().getString("inventories." + args[0]);
                System.out.println(invString);
                player.openInventory(stringToInventory(invString));
                return true;
            } else {
                sender.sendMessage(ChatColor.BLUE + "[OfflineInvsee] " + ChatColor.RESET + "No permission.");
                return false;
            }
        }
        return false;
    }

    public static String inventoryToString(Inventory invInventory) {
        String serialization = invInventory.getSize() + ";";
        for (int i = 0; i < invInventory.getSize(); i++) {
            ItemStack is = invInventory.getItem(i);
            if (is != null) {
                String serializedItemStack = new String();
                String isType = String.valueOf(is.getType().getId());
                serializedItemStack += "t@" + isType;
                if (is.getDurability() != 0) {
                    String isDurability = String.valueOf(is.getDurability());
                    serializedItemStack += ":d@" + isDurability;
                }
                if (is.getAmount() != 1) {
                    String isAmount = String.valueOf(is.getAmount());
                    serializedItemStack += ":a@" + isAmount;
                }
                Map<Enchantment,Integer> isEnch = is.getEnchantments();
                if (isEnch.size() > 0) {
                    for (Entry<Enchantment,Integer> ench : isEnch.entrySet()) {
                        serializedItemStack += ":e@" + ench.getKey().getId() + "@" + ench.getValue();
                    }
                }
                serialization += i + "#" + serializedItemStack + ";";
            }
        }
        return serialization;
    }

    public static Inventory stringToInventory(String invString) {
        String[] serializedBlocks = invString.split(";");
        String invInfo = serializedBlocks[0];
        Inventory deserializedInventory = Bukkit.getServer().createInventory(null, Integer.valueOf(invInfo));
        for (int i = 1; i < serializedBlocks.length; i++) {
            String[] serializedBlock = serializedBlocks[i].split("#");
            int stackPosition = Integer.valueOf(serializedBlock[0]);
            if (stackPosition >= deserializedInventory.getSize()) {
                continue;
            }
            ItemStack is = null;
            Boolean createdItemStack = false;
            String[] serializedItemStack = serializedBlock[1].split(":");
            for (String itemInfo : serializedItemStack) {
                String[] itemAttribute = itemInfo.split("@");
                if (itemAttribute[0].equals("t")) {
                    is = new ItemStack(Material.getMaterial(Integer.valueOf(itemAttribute[1])));
                    createdItemStack = true;
                } else if (itemAttribute[0].equals("d") && createdItemStack) {
                    is.setDurability(Short.valueOf(itemAttribute[1]));
                } else if (itemAttribute[0].equals("a") && createdItemStack) {
                    is.setAmount(Integer.valueOf(itemAttribute[1]));
                } else if (itemAttribute[0].equals("e") && createdItemStack) {
                    is.addEnchantment(Enchantment.getById(Integer.valueOf(itemAttribute[1])), Integer.valueOf(itemAttribute[2]));
                }
            }
            deserializedInventory.setItem(stackPosition, is);
        }
        return deserializedInventory;
    }

    public void loadConfiguration(){
        String path = "init.first";
        getConfig().addDefault(path, "successful");
        getConfig().options().copyDefaults(true);
        saveConfig();
        reloadConfig();
    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        Inventory playerInventory = player.getInventory();
        getConfig().set("inventories." + playerName, inventoryToString(playerInventory));
        saveConfig();
        reloadConfig();
        System.out.println("[OfflineInvsee] inventory contents of " + playerName + " have been saved");
    }

}