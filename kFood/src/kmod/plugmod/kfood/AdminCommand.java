package kmod.plugmod.kfood;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

/***
 * Class that handles all kFood commands.
 * @author 7kasper
 *
 */
public class AdminCommand implements CommandExecutor, TabCompleter{
	private final kFood plugin;
	
	public AdminCommand(kFood Plugin){
		this.plugin = Plugin;
	}
	
	/*
	 * Sorry, this class seems a bit messy...
	 * It is quite simply a chain of case and if statements to check against validity of entered commands and a function from the plugin.
	 */
	
	/**
	 * Handles all admin commands.
	 */
	@Override
	public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
		String cmdN = cmd.getName();
		boolean displayCredits = false;
		
		if(args.length > 0){
			switch(args[0].toLowerCase()){
			case "reload":
				if(s.hasPermission(plugin.pPermissions.get(0))){
					plugin.loadFromConfig();
					s.sendMessage(plugin.pNameSend + "Reloaded from config.");
					return true;
				}
				displayCredits = true;
			break;
			case "level":
				if(s.hasPermission(plugin.pPermissions.get(1))){
					if(args.length > 1){
						for(Player subject : Bukkit.getOnlinePlayers()){
							if(subject.getName() == args[1]){
								if(args.length > 2){
									try{
										plugin.setFoodLevel(subject, Integer.parseInt(args[2]));
										plugin.updateFoodLevel(subject);
										s.sendMessage(plugin.pNameSend + ChatColor.GREEN + subject.getName() + ChatColor.RESET + " now has a food level of: " + ChatColor.YELLOW + args[2] + ChatColor.RESET + ".");
										return true;
									}catch (NumberFormatException e) {
										if(args[2].equalsIgnoreCase("clear")){
											plugin.resetFoodLevel(subject);
											plugin.updateFoodLevel(subject);
											s.sendMessage(plugin.pNameSend + ChatColor.GREEN + subject.getName() + ChatColor.RESET + " now has the default-food level again. (" + ChatColor.YELLOW + plugin.defaultFood + ChatColor.RESET + ")");
											return true;
										}else{
								            s.sendMessage(plugin.pNameSend + "? " + ChatColor.YELLOW + args[2] + ChatColor.RESET + " is not a number!");
								            return true;
										}
							        }
								}else{
									plugin.updateFoodLevel(subject);
									s.sendMessage(plugin.pNameSend + ChatColor.GREEN + subject.getName() + ChatColor.RESET + " has a food level of: " + ChatColor.YELLOW + plugin.getFoodLevel(subject) + ChatColor.RESET + ".");
									return true;
								}
							}
						}
						s.sendMessage(plugin.pNameSend + "? Player " + ChatColor.YELLOW + args[1] + ChatColor.RESET + " does not appear to be online (or exist).");
						return true;
					}else{
						s.sendMessage("=========== " + ChatColor.RED + plugin.pName + " Help Level" + ChatColor.RESET + " ===========");
						s.sendMessage("/" + cmdN + " level              :" + ChatColor.YELLOW + " Shows this help menu.");
						s.sendMessage("/" + cmdN + " level PLAYER       :" + ChatColor.YELLOW + " Shows current food of player.");
						s.sendMessage("/" + cmdN + " level PLAYER 1-20  :" + ChatColor.YELLOW + " Sets the food of player.");
						s.sendMessage("/" + cmdN + " level PLAYER clear :" + ChatColor.YELLOW + " Defaults the food of player.");
						s.sendMessage("=========== " + ChatColor.RED + plugin.pName + " Help Level" + ChatColor.RESET + " ===========");
						return true;
					}
				}
				displayCredits = true;
			break;
			case "help":
			case "?":
				if(s.hasPermission(plugin.pPermissions.get(2))){
					s.sendMessage("=========== " + ChatColor.RED + plugin.pName + " Help" + ChatColor.RESET + " ===========");
					if(s.hasPermission(plugin.pPermissions.get(0))) s.sendMessage("/" + cmdN + " reload  :" + ChatColor.YELLOW + " Reloads from config.");
					if(s.hasPermission(plugin.pPermissions.get(1))) s.sendMessage("/" + cmdN + " level   :" + ChatColor.YELLOW + " Handle food per player.");
					if(s.hasPermission(plugin.pPermissions.get(2))) s.sendMessage("/" + cmdN + " help    :" + ChatColor.YELLOW + " Shows this help menu.");
					s.sendMessage("=========== " + ChatColor.RED + plugin.pName + " Help" + ChatColor.RESET + " ===========");
					return true;
				}
				displayCredits = true;
			break;	
			default:
				if(s.hasPermission(plugin.pPermissions.get(2))){
					s.sendMessage(plugin.pNameSend + "? Use " + ChatColor.YELLOW + "/" + cmdN + " help" + ChatColor.RESET + " for options.");
					return true;
				}else{
					displayCredits = true;
				}
			}
		}else{
			displayCredits = true;
		}
		
		if(displayCredits){
			s.sendMessage("This server runs " + plugin.pNameSend + "version: " + ChatColor.YELLOW + plugin.pVersion + ChatColor.RESET + " by " + ChatColor.GREEN + plugin.pAuthors + ChatColor.RESET + ".");
			return true;
		}
		return false;
	}

	/**
	 * Handles the useful tabComplete feature.
	 */
	@Override
	public List<String> onTabComplete(CommandSender s, Command cmd, String alias, String[] args) {
		//Only tab complete if the player has permission.
		List<String> mainSubCmds = new ArrayList<>();
		if(s.hasPermission(plugin.pPermissions.get(0))) mainSubCmds.add("reload");
		if(s.hasPermission(plugin.pPermissions.get(1))) mainSubCmds.add("level");
		if(s.hasPermission(plugin.pPermissions.get(2))) mainSubCmds.add("help");
		
		List<String> completeTo = new ArrayList<>();
		if(args.length > 0){
			switch(args[0].toLowerCase()){
			case "level":
				if(s.hasPermission(plugin.pPermissions.get(1))){
					if(args.length == 3){
						//The player must now enter a number or "clear".
						completeTo.add("clear");
						return completeTo;
					}else{
						//Null so default option (show player names) will be used.
						return null;
					}
				}
			default:
				//Auto complete, sorted using the first (uncomplete) argument.
				StringUtil.copyPartialMatches(args[0], mainSubCmds, completeTo);
				Collections.sort(completeTo);
				return completeTo;
			}
		}
		return mainSubCmds;
	}

}
