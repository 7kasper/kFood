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
public class Commands implements CommandExecutor, TabCompleter{
	private final kFood plugin;
	public final List<String> mainSubCmds = Arrays.asList(new String[]{"reload", "help", "base"});
	
	public Commands(kFood Plugin){
		this.plugin = Plugin;
	}
	
	@Override
	public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
		String cmdN = cmd.getName();
		if(args.length > 0){
			switch(args[0].toLowerCase()){
			case "reload":
			case "rl":
				plugin.loadConfig();
				s.sendMessage(plugin.pNameSend + "Reloaded from config.");
			break;
			case "base":
			case "b":
				if(args.length > 1){
					for(Player subject : Bukkit.getOnlinePlayers()){
						if(subject.getName().equalsIgnoreCase(args[1])){
							if(args.length > 2){
								try{
									plugin.setBaseFood(subject, Integer.parseInt(args[2]));
									s.sendMessage(plugin.pNameSend + ChatColor.GREEN + subject.getName() + ChatColor.RESET + " now has a basefood of: " + ChatColor.YELLOW + args[2] + ChatColor.RESET + ".");
								}catch (NumberFormatException e) {
									if(args[2].equalsIgnoreCase("clear")){
										plugin.resetBaseFood(subject);
										s.sendMessage(plugin.pNameSend + ChatColor.GREEN + subject.getName() + ChatColor.RESET + " has the default-basefood again.");
									}else{
							            s.sendMessage(plugin.pNameSend + "? " + ChatColor.YELLOW + args[2] + ChatColor.RESET + " is not a number!");	
									}
						        }
							}else{
								s.sendMessage(plugin.pNameSend + ChatColor.GREEN + subject.getName() + ChatColor.RESET + " has a basefood of: " + ChatColor.YELLOW + plugin.getBaseFood(subject) + ChatColor.RESET + ".");
							}
						}else{
							s.sendMessage(plugin.pNameSend + "? Player " + ChatColor.YELLOW + args[1] + ChatColor.RESET + " does not appear to be online.");
						}
					}
				}else{
					s.sendMessage(plugin.pNameSend + "? Use " + ChatColor.YELLOW + "/" + cmdN + " help set "+ ChatColor.RESET +" for options.");
				}
			break;
			case "help":
			case "?":
				s.sendMessage("=========== " + ChatColor.RED + plugin.pName + " Help" + ChatColor.RESET + " ===========");
				s.sendMessage("/" + cmdN + " help    : " + ChatColor.YELLOW + "Shows this help menu.");
				s.sendMessage("/" + cmdN + " reload : " + ChatColor.YELLOW + "Reloads from config.");
				s.sendMessage("/" + cmdN + " base    : " + ChatColor.YELLOW + "Handle base food per player.");
				s.sendMessage("=========== " + ChatColor.RED + "kFood Help" + ChatColor.RESET + " ===========");
			break;
			default:
				s.sendMessage(plugin.pNameSend + ChatColor.YELLOW + "? Use /" + cmdN + " help for options.");
			break;
			}
		}else{
			s.sendMessage("This server runs " + plugin.pNameSend + "version: " + ChatColor.YELLOW + plugin.pVersion + ChatColor.RESET + " by " + ChatColor.GREEN + plugin.pAuthors);
		}
	return false;
	}

	@Override
	public List<String> onTabComplete(CommandSender s, Command cmd, String alias, String[] args) {
		List<String> completeTo = new ArrayList<>();
		switch(args[0].toLowerCase()){
		case "base":
			if(args.length >= 3){
				//The player must now enter a number or clear.
				completeTo.add("clear");
				return completeTo;
			}else{
				//Null so default option (show player names) will be used.
				return null;
			}
		case "help":
			//Help must auto complete the commands again. This time using the second argument.
			StringUtil.copyPartialMatches(args[1], mainSubCmds, completeTo);
			Collections.sort(completeTo);
			return completeTo;
		default:
			//Auto complete, sorted using the first argument.
			StringUtil.copyPartialMatches(args[0], mainSubCmds, completeTo);
			Collections.sort(completeTo);
			return completeTo;
		}
	}

}
