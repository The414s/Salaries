package com.github.TeamRA.Salaries;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

public class Salaries extends JavaPlugin {
private final Logger log = Logger.getLogger("Minecraft.Salaries");

private PluginDescriptionFile pdfFile;
private String logPrefix;
private String chatPrefix;

private static PermissionHandler Permissions;

private final static String groupsFilename = "groups.yml";
private final static String lastTimesFilename = "lasttimes.yml";
private String folder;
private Configuration groups;
private Configuration lastTimes;

private long usageInterval = (8 * 60 * 60);

private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, d MMM 'at' HH:mm:ss");

    public void onEnable() {
     pdfFile = this.getDescription();
     logPrefix = "[" + pdfFile.getName() + "] ";
     chatPrefix = "§e[" + pdfFile.getName() + "]§f ";
    
     logInfo("v" + pdfFile.getVersion() + " is now enabled.");
        
folder = this.getDataFolder().toString().replace("\\", "/") + "/";

        setupPermissions();

try {
loadGroupPrefs();
loadLastTimes();
} catch (Exception e) {
logWarning("Auto-disabling...");
getServer().getPluginManager().disablePlugin(this);
return;
}
        
    }
    
public void onDisable() {
logInfo("v" + pdfFile.getVersion() + " is now disabled.");
}
    
public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {

if (Permissions == null) {
sender.sendMessage(chatPrefix + "This plugin cannot work if Permissions is not loaded.");
return true;
}
    
if (!(sender instanceof Player)) {
sender.sendMessage(chatPrefix + "This command is only available to players.");
return true;
}

if (args.length != 0) {
return false;
}

Player player = (Player)sender;

if (!hasPermission((Player)sender, "salaries.salary")) {
player.sendMessage(chatPrefix + "Your group doesn't have any salary !");
return true;
}

Date currentDate = new Date();
String lastUse = lastTimes.getString(player.getName(), "");

if (!lastUse.equals("")) {
Date lastDate;
try {
lastDate = new Date(Long.valueOf(lastUse));
} catch (NumberFormatException e) {
logWarning("The last usage time of " + player.getName() + " couldn't be parsed : " + e);
return true;
}

player.sendMessage(chatPrefix + "Last salary received " + simpleDateFormat.format(lastDate) + ".");

long secondsElapsed = (currentDate.getTime() - lastDate.getTime()) / 1000;

if (secondsElapsed < usageInterval) {
long secondsRemaining = usageInterval - secondsElapsed;
int hours = (int) (secondsRemaining / 3600);
int minutes = (int) ((secondsRemaining % 3600) / 60);
int seconds = (int) (secondsRemaining % 60);
player.sendMessage(chatPrefix + "You must wait " + hours + " hours, " + minutes + " mns and " + seconds + " seconds.");
return true;
}
}

String playerGroup = "";
playerGroup = Permissions.getGroup(player.getWorld().getName(), player.getName());

Integer spongeQuantity = groups.getInt(playerGroup, 0);

if (spongeQuantity == 0) {
player.sendMessage(chatPrefix + "Your salary could not be retrieved. Contact an administrator.");
logWarning("The player " + player.getName() + " (Group : " + playerGroup + ") has the 'salaries.salary' permission but no sponge quantity could be retrieved from '" + groupsFilename + "'.");
return true;
}

player.sendMessage(chatPrefix + "You received your salary !");

HashMap<Integer, ItemStack> remainingItems = player.getInventory().addItem(new ItemStack(Material.SPONGE, spongeQuantity));
if (!remainingItems.isEmpty()) {
if (remainingItems.get(0) != null) {
player.sendMessage(chatPrefix + "Your inventory being full, some of it has been dropped on the floor.");
player.getWorld().dropItemNaturally(player.getLocation(), remainingItems.get(0));
}
}

lastTimes.setProperty(player.getName(), currentDate.getTime());

try {
saveLastTimes();
} catch (Exception e) {
logWarning("Error while saving the last times in the file !");
}

     return true;
}
    
    private void setupPermissions() {
        Plugin test = this.getServer().getPluginManager().getPlugin("Permissions");

        if (Salaries.Permissions == null) {
            if (test != null) {
                Salaries.Permissions = ((Permissions)test).getHandler();
            } else {
             logInfo("Permissions plugin not detected, defaulting to OP.");
            }
        }
    }
    
    public boolean hasPermission(Player player, String permission) {
     if ((player == null) || (permission == null)) {
         return false;
        }

        if ((Permissions != null) && (Permissions.has(player, permission))) {
         return true;
        } else if ((Permissions == null) && (player.isOp())) {
         return true;
        } else {
         return false;
        }
}
    
    private void loadGroupPrefs() throws Exception {
File file = new File(folder + groupsFilename);
groups = new Configuration(file);
if (file.exists()) {
try {
groups.load();
} catch (Exception e) {
logWarning("Could not load " + groupsFilename + " : " + e);
throw new Exception();
}
} else {
logInfo(groupsFilename + " file not found, creating a default one.");
try {
(new File(folder)).mkdir();
} catch (SecurityException e) {
logWarning("Could not create the plugin's folder : " + e);
throw new SecurityException();
}

groups.setProperty("groupExample", 1);

try {
groups.save();
} catch (Exception e) {
logWarning("Could not create " + groupsFilename + " : " + e);
throw new Exception();
}
}
    }
    
private void loadLastTimes() throws Exception {
File file = new File(folder + lastTimesFilename);
lastTimes = new Configuration(file);
if (file.exists()) {
try {
lastTimes.load();
} catch (Exception e) {
logWarning("Could not load " + lastTimesFilename + " : " + e);
throw new Exception();
}
} else {
logInfo(lastTimesFilename + " file not found, creating a default one.");
try {
(new File(folder)).mkdir();
} catch (SecurityException e) {
logWarning("Could not create the plugin's folder : " + e);
throw new SecurityException();
}

saveLastTimes();
}
}

private void saveLastTimes() throws Exception {
try {
lastTimes.save();
} catch (Exception e) {
logWarning("Could not save last times into " + lastTimesFilename + " : " + e);
throw new Exception();
}
}

private void logInfo(String info) {
log.info(logPrefix + info);
}

private void logWarning(String warning) {
log.warning(logPrefix + warning);
}

}