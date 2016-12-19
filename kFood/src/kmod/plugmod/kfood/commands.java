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
public class commands implements CommandExecutor, TabCompleter{
	private final kFood plugin;
	public final List<String> toComplete = Arrays.asList(new String[]{"reload", "help", "set"});
	
	public commands(kFood Plugin){
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
			case "set":
			case "s":
				if(args.length > 1){
					for(Player subject : Bukkit.getOnlinePlayers()){
						if(subject.getName().equalsIgnoreCase(args[1])){
							if(args.length > 2){
								try{
									plugin.setBaseFood(subject, Integer.parseInt(args[2]));
									s.sendMessage(plugin.pNameSend + ChatColor.GREEN + subject.getName() + ChatColor.RESET + " now has a base food of: " + ChatColor.YELLOW + args[2] + ChatColor.RESET + ".");
								}catch (NumberFormatException e) {
						            s.sendMessage(plugin.pNameSend + "? " + ChatColor.YELLOW + args[2] + ChatColor.RESET + " is not a number!");
						        }
							}else{
								s.sendMessage(plugin.pNameSend + ChatColor.GREEN + subject.getName() + ChatColor.RESET + " has a base food of: " + ChatColor.YELLOW + plugin.getBaseFood(subject) + ChatColor.RESET + ".");
							}
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
				s.sendMessage("/" + cmdN + " set     : " + ChatColor.YELLOW + "Sets base food per player.");
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
		case "set":
			if(args.length >= 3){
				//The player must now enter a number.
				completeTo.add("num");
				return completeTo;
			}else{
				//Null so default option (show player names) will be used.
				return null;
			}
		case "help":
			//Help must auto complete the commands again. This time using the second argument.
			StringUtil.copyPartialMatches(args[1], toComplete, completeTo);
			Collections.sort(completeTo);
			return completeTo;
		default:
			//Auto complete, sorted using the first argument.
			StringUtil.copyPartialMatches(args[0], toComplete, completeTo);
			Collections.sort(completeTo);
			return completeTo;
		}
	}

}
