package kmod.plugmod.kfood;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import net.md_5.bungee.api.ChatColor;

/***
 * Class that handles all kFood commands.
 * @author 7kasper
 *
 */
public class AdminCommand implements CommandExecutor, TabCompleter{
	private final kFood plugin;
	public final List<String> mainSubCmds = Arrays.asList(new String[]{"reload", "help", "level", "food"});
	
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
		if(args.length > 0){
			switch(args[0].toLowerCase()){
			case "reload":
				plugin.loadFromConfig();
				s.sendMessage(plugin.pNameSend + "Reloaded from config.");
			break;
			case "level":
				if(args.length > 1){
					for(Player subject : Bukkit.getOnlinePlayers()){
						if(subject.getName() == args[1]){
							if(args.length > 2){
								try{
									plugin.setFoodLevel(subject, Integer.parseInt(args[2]));
									plugin.updateFoodLevel(subject);
									s.sendMessage(plugin.pNameSend + ChatColor.GREEN + subject.getName() + ChatColor.RESET + " now has a food level of: " + ChatColor.YELLOW + args[2] + ChatColor.RESET + ".");
								}catch (NumberFormatException e) {
									if(args[2].equalsIgnoreCase("clear")){
										plugin.resetFoodLevel(subject);
										plugin.updateFoodLevel(subject);
										s.sendMessage(plugin.pNameSend + ChatColor.GREEN + subject.getName() + ChatColor.RESET + " now has the default-food level again. (" + ChatColor.YELLOW + plugin.defaultFood + ChatColor.RESET + ")");
									}else{
							            s.sendMessage(plugin.pNameSend + "? " + ChatColor.YELLOW + args[2] + ChatColor.RESET + " is not a number!");	
									}
						        }
							}else{
								plugin.updateFoodLevel(subject);
								s.sendMessage(plugin.pNameSend + ChatColor.GREEN + subject.getName() + ChatColor.RESET + " has a food level of: " + ChatColor.YELLOW + plugin.getFoodLevel(subject) + ChatColor.RESET + ".");
							}
							//Whoopsy, we don't want that message down there to fire if there actually is a player that matches our name.
							return true;
						}
					}
					s.sendMessage(plugin.pNameSend + "? Player " + ChatColor.YELLOW + args[1] + ChatColor.RESET + " does not appear to be online (or exist).");
				}else{
					s.sendMessage("=========== " + ChatColor.RED + plugin.pName + " Help Level" + ChatColor.RESET + " ===========");
					s.sendMessage("/" + cmdN + " level             :" + ChatColor.YELLOW + " Shows this help menu.");
					s.sendMessage("/" + cmdN + " level PLAYER      :" + ChatColor.YELLOW + " Shows current food of player.");
					s.sendMessage("/" + cmdN + " level PLAYER 1-20 :" + ChatColor.YELLOW + " Sets the food of player.");
					s.sendMessage("/" + cmdN + " level PLAYER clear:" + ChatColor.YELLOW + " Defaults the food of player.");
					s.sendMessage("=========== " + ChatColor.RED + plugin.pName + " Help Level" + ChatColor.RESET + " ===========");
				}
			break;
			case "food":
				if(args.length > 1){
					switch (args[1].toLowerCase()){
					case "set":
						if(args.length > 2){
							if(args.length > 3){
								try{
									if(plugin.setFood(args[2], Double.parseDouble(args[3]))){
										plugin.saveToConfig();
										s.sendMessage(plugin.pNameSend + ChatColor.YELLOW + args[2] + ChatColor.RESET + " now heals " + ChatColor.YELLOW + args[3] + ChatColor.RESET + " half heart(s).");
									}else{
										s.sendMessage(plugin.pNameSend + "? " + ChatColor.YELLOW + args[2] + ChatColor.RESET + " doesn't appear to be a food.");
									}
								}catch (NumberFormatException e) {
									s.sendMessage(plugin.pNameSend + "? " + ChatColor.YELLOW + args[3] + ChatColor.RESET + " is not a number!");	
								}
							}else{
								if (s instanceof Player){
									Player p = (Player) s;
									String itemInHand = plugin.getSafeFoodName(p.getInventory().getItemInMainHand());
									try{
										if(plugin.setFood(itemInHand, Double.parseDouble(args[2]))){
											plugin.saveToConfig();
											s.sendMessage(plugin.pNameSend + ChatColor.YELLOW + itemInHand + ChatColor.RESET + " now heals " + ChatColor.YELLOW + args[3] + ChatColor.RESET + " half heart(s).");
										}else{
											s.sendMessage(plugin.pNameSend + "? " + ChatColor.YELLOW + itemInHand + ChatColor.RESET + " doesn't appear to be a food.");
										}
									}catch (NumberFormatException e) {
										s.sendMessage(plugin.pNameSend + "? " + ChatColor.YELLOW + args[2] + ChatColor.RESET + " is not a number!");	
									}
								}else{
									s.sendMessage(plugin.pNameSend + "? You are not a player, so you need to specify a name aswell.");
								}
							}
						}else{
							s.sendMessage("=========== " + ChatColor.RED + plugin.pName + " Help Setting Food" + ChatColor.RESET + " ===========");
							s.sendMessage("/" + cmdN + " food set LEVEL");
							s.sendMessage(ChatColor.YELLOW + "Sets the halfHeartsToHeal of the item in hand.");
							s.sendMessage("/" + cmdN + " food set NAME LEVEL");
							s.sendMessage(ChatColor.YELLOW + "Sets the halfHeartsToHeal of an item based on name.");
							s.sendMessage("=========== " + ChatColor.RED + plugin.pName + " Help Setting Food" + ChatColor.RESET + " ===========");
						}
					break;
					case "add":
						if(args.length > 3){
							if(args.length > 4){
								try{
									if(plugin.addFood(args[2], FoodType.valueOf(args[3].toUpperCase().trim()), Double.parseDouble(args[4]))){
										plugin.saveToConfig();
										s.sendMessage(plugin.pNameSend + ChatColor.YELLOW + args[3] + "-food " + ChatColor.GREEN + args[2] + ChatColor.RESET + " is added and gives " + ChatColor.YELLOW + args[4] + ChatColor.RESET + " half heart(s).");
									}else{
										s.sendMessage(plugin.pNameSend + "? something went wrong... Enable debug and check the log?");
									}
								}catch (NumberFormatException e) {
									s.sendMessage(plugin.pNameSend + "? " + ChatColor.YELLOW + args[4] + ChatColor.RESET + " is not a number!");
								}catch (IllegalArgumentException e){
									s.sendMessage(plugin.pNameSend + "? " + ChatColor.YELLOW + args[3] + ChatColor.RESET + " is not a valid foodType!");
								}
							}else{
								if (s instanceof Player){
									Player p = (Player) s;
									String itemInHand = plugin.getSafeFoodName(p.getInventory().getItemInMainHand());
									try{
										if(plugin.addFood(itemInHand, FoodType.valueOf(args[2].toUpperCase().trim()), Double.parseDouble(args[3]))){
											plugin.saveToConfig();
											s.sendMessage(plugin.pNameSend + ChatColor.YELLOW + args[2] + "-food " + ChatColor.GREEN + itemInHand + ChatColor.RESET + " is added and gives " + ChatColor.YELLOW + args[3] + ChatColor.RESET + " half heart(s).");
										}else{
											s.sendMessage(plugin.pNameSend + "? something went wrong... Enable debug and check the log?");
										}
									}catch (NumberFormatException e) {
										s.sendMessage(plugin.pNameSend + "? " + ChatColor.YELLOW + args[3] + ChatColor.RESET + " is not a number!");
									}catch (IllegalArgumentException e){
										s.sendMessage(plugin.pNameSend + "? " + ChatColor.YELLOW + args[2] + ChatColor.RESET + " is not a valid foodType!");
									}
								}else{
									s.sendMessage(plugin.pNameSend + "? You are not a player, so you need to specify a name aswell.");
								}
							}
						}else{
							s.sendMessage("=========== " + ChatColor.RED + plugin.pName + " Help Adding Food" + ChatColor.RESET + " ===========");
							s.sendMessage("/" + cmdN + " food add TYPE LEVEL");
							s.sendMessage(ChatColor.YELLOW + "Adds the item in hand to either the foods or instantFoods with a a certain halfHeartsToHeal.");
							s.sendMessage("/" + cmdN + " food add NAME TYPE LEVEL");
							s.sendMessage(ChatColor.YELLOW + "Adds an item based on name to either the foods or instantFoods with a a certain halfHeartsToHeal.");
							s.sendMessage("=========== " + ChatColor.RED + plugin.pName + " Help Adding Food" + ChatColor.RESET + " ===========");
						}
					break;
					default:
						s.sendMessage("? Use " + ChatColor.YELLOW + "/" +cmdN + " food" + ChatColor.RESET + " for a list options");
					break;
					}
				}else{
					s.sendMessage("=========== " + ChatColor.RED + plugin.pName + " Help Food" + ChatColor.RESET + " ===========");
					s.sendMessage("/" + cmdN + " food       :" + ChatColor.YELLOW + " Shows this help menu.");
					s.sendMessage("/" + cmdN + " food set   :" + ChatColor.YELLOW + " Set the level of a food.");
					s.sendMessage("/" + cmdN + " food add   :" + ChatColor.YELLOW + " Add a food.");
					s.sendMessage("=========== " + ChatColor.RED + plugin.pName + " Help Food" + ChatColor.RESET + " ===========");
				}
			break;
			case "help":
			case "?":
				s.sendMessage("=========== " + ChatColor.RED + plugin.pName + " Help" + ChatColor.RESET + " ===========");
				s.sendMessage("/" + cmdN + " help    :" + ChatColor.YELLOW + " Shows this help menu.");
				s.sendMessage("/" + cmdN + " reload  :" + ChatColor.YELLOW + " Reloads from config.");
				s.sendMessage("/" + cmdN + " level   :" + ChatColor.YELLOW + " Handle food per player.");
				s.sendMessage("/" + cmdN + " config  :" + ChatColor.YELLOW + " Set config ingame.");
				s.sendMessage("=========== " + ChatColor.RED + plugin.pName + " Help" + ChatColor.RESET + " ===========");
			break;
			default:
				s.sendMessage(plugin.pNameSend + "? Use " + ChatColor.YELLOW + "/" + cmdN + " help" + ChatColor.RESET + " for options.");
			break;
			}
		}else{
			s.sendMessage("This server runs " + plugin.pNameSend + "version: " + ChatColor.YELLOW + plugin.pVersion + ChatColor.RESET + " by " + ChatColor.GREEN + plugin.pAuthors + ChatColor.RESET + ".");
		}
	return false;
	}

	/**
	 * Handles the useful tabComplete feature.
	 */
	@Override
	public List<String> onTabComplete(CommandSender s, Command cmd, String alias, String[] args) {
		List<String> completeTo = new ArrayList<>();
		if(args.length > 0){
			switch(args[0].toLowerCase()){
			case "level":
				if(args.length == 3){
					//The player must now enter a number or "clear".
					completeTo.add("clear");
					return completeTo;
				}else{
					//Null so default option (show player names) will be used.
					return null;
				}
			case "food":
				switch(args.length){
				case 2:
					//We either want the player to add or set. Might as well complete it for them.
					StringUtil.copyPartialMatches(args[1], Arrays.asList(new String[]{"add", "set"}), completeTo);
					Collections.sort(completeTo);
					return completeTo;
				case 3:
					if(args[1].toLowerCase() == "set"){
						//The sender is either going to pick a name or value. We can't guess values, so we should return all item names.
						StringUtil.copyPartialMatches(args[2], plugin.foods.keySet(), completeTo);
						StringUtil.copyPartialMatches(args[2], plugin.instantFoods.keySet(), completeTo);
						Collections.sort(completeTo);
						return completeTo;
					}else if (args[1].toLowerCase() == "add"){
						//The sender is either going to specify a name we can't know because its new, or a type. We'll tab out the type for them.
						StringUtil.copyPartialMatches(args[2], plugin.enumNameList(FoodType.class), completeTo);
						Collections.sort(completeTo);
						return completeTo;
					}
				}
				return null;
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
