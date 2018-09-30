package kmod.plugmod.kfood;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

/**
 * Main class of kFood, extends JavaPlugin.
 * Houses all API functions.
 * @author 7kasper
 */
public class kFood extends JavaPlugin{
	
    //=====================================================\\
    //						Variables					   \\
    //=====================================================\\
	
	//Options, will load from config.yml
	public boolean DEBUG = false;
	public int defaultFood = 17;
	public boolean potionMechanics = false;
	public boolean potionEffects = true;
	public boolean burbEffect = true; 
	public boolean allowCustom = true;
	public boolean detectAnvil = true;
	public boolean revertAnvil = true;
	public Map<String, Double> foods = new HashMap<>();
	public Map<String, Double> instantFoods = new HashMap<>();
	public Double cakeHeal = 2.0;
	public List<String> returnBowl = new ArrayList<>();
	
	//Plugin values, will load from plugin.yml
	public String pName = "";
	public String pNameSend = "";
	public String pVersion = "";
	public String pAuthors = "";
	public String pAdminCommand = "";
	public List<Permission> pPermissions = new ArrayList<>();
	public kFood plugin;
	
	//Reflection stuff, will load onEnable()
	private Function<ItemStack, String> getRealName;
	
	//Scoreboard objective, to keep track of per player basefood.
	private Objective foodObjective = null;
	
    //=====================================================\\
    //				    Plugin Functions				   \\
    //=====================================================\\
	
	/**
	 * Called whenever kFood initializes.
	 */
    @Override
    public void onEnable() {
    	//Set up for future reference.
    	plugin = this;
    	
    	//Get some data from the plugin.yml
    	if(!loadPluginValues()){
    		cm(ChatColor.RED + "Fatal error reading from plugin.yml! Please reinstall the plugin.");
    		Bukkit.getPluginManager().disablePlugin(plugin);
    		return;
    	};
    	//Load in the config.yml and its values.
    	if(!loadFromConfig()){
    		cm(ChatColor.RED + "Fatal error reading config.yml! Please run in debug and check the config for anomalies.");
    		Bukkit.getPluginManager().disablePlugin(plugin);
    		return;
    	};
    	//Prepare reflection for safe item names.
    	if(!prepareReflection()){
    		cm(ChatColor.RED + "Fatal error during reflecting! Are the names of some core classes or methods changed?");
    		Bukkit.getPluginManager().disablePlugin(plugin);
    		return;
    	};
    	//Setup the foods Scoreboard.
    	if(!setupScoreboard()){
    		cm(ChatColor.RED + "Fatal error when setting up scoreboard. Are any other plugins messing with the foods objective?");
    		Bukkit.getPluginManager().disablePlugin(plugin);
    		return;
    	}
    	
		debug("Implementing FoodListners...");
    	Bukkit.getPluginManager().registerEvents(new FoodListeners(this), this);
    	
    	if(pAdminCommand != ""){
        	debug("Implementing adminCommands...");
        	AdminCommand adminCommands = new AdminCommand(this);
        	getCommand(pAdminCommand).setExecutor(adminCommands);
        	getCommand(pAdminCommand).setTabCompleter(adminCommands);
    	}
    	
        try {
            Metrics metrics = new Metrics(this);
            metrics.start();
        } catch (IOException e) {
            debug("[" + ChatColor.RED + "MCSTATS" + ChatColor.RESET + "] Failed to submit the stats :-(");
        }
    		
    	
    	//Done!
    	cm("Hunger is now a thing of the past!");
    }
    
    /**
     * Called when kFood stops.
     */
    @Override
    public void onDisable() {
    	cm("Don't forget your sandwich!");
    }
    
    //=====================================================\\
    //				  Preparing Functions				   \\
    //=====================================================\\
    
    /***
     * (re)load config file.
     * @return true, if all values were correctly loaded in.
     */
    public boolean loadFromConfig(){
    	try{
    		//Load the config file and all the values.
        	saveDefaultConfig();
        	reloadConfig();
    		DEBUG = getConfig().getBoolean("debug");
    		debug("DEBUG enabled!");
    		
    		debug("Getting main settings...");
    		defaultFood = getConfig().getInt("default-food");
    		debug("defaultFood", Integer.toString(defaultFood));
    		potionMechanics = getConfig().getBoolean("potion-mechanics");
    		debug("potionMechanics", Boolean.toString(potionMechanics));
    		
    		debug("Getting Custom Item settings...");
    		allowCustom = getConfig().getBoolean("allow-custom");
    		debug("allowCustom", Boolean.toString(allowCustom));
    		detectAnvil = getConfig().getBoolean("detect-anvil");
    		debug("detectAnvil", Boolean.toString(detectAnvil));
    		revertAnvil = getConfig().getBoolean("revert-anvil");
    		debug("revertAnvil", Boolean.toString(revertAnvil));
    		
    		debug("Getting Effects settings...");
    		potionEffects = getConfig().getBoolean("potion-effects");
    		debug("potionEffects", Boolean.toString(potionEffects));
    		burbEffect = getConfig().getBoolean("burb-effect");
    		debug("burbEffect", Boolean.toString(burbEffect));
    		
    		debug("Loading foods...");
    		getConfig().getConfigurationSection("foods").getValues(false).forEach(
    			(k,v) -> {
    				addFood(k.toString(), FoodType.CONSUMABLE, new Double(v.toString()));
    				debug(k.toString(), v.toString());
    			});
    		
    		debug("Loading instant foods...");
    		getConfig().getConfigurationSection("instant-foods").getValues(false).forEach(
    				(k,v) -> {
    					addFood(k.toString(), FoodType.INSTANT, new Double(v.toString()));
    					debug(k.toString(), v.toString());
    				});
    		
    		debug("Getting Misc values...");
    		cakeHeal = getConfig().getDouble("cake");
    		debug("cakeHeal", cakeHeal.toString());
    		returnBowl = getConfig().getStringList("return-bowl");
    		debug("returnBowl", returnBowl.toString());
    		
    		return true;
    	}catch (Exception e){
    		return false;
    	}
    }
    
    /**
     * Deletes all foods in the config.yml and saves all foods present in {@link #foods} and {@link #instantFoods} to config. 
     * @return true, if all operations were successful.
     */
    public boolean saveFoodsToConfig(){
    	debug("Saving foods to config...");
    	int operationsSuccessful = 0;
    	try {
        	//Read the config in.
        	Path configPath = Paths.get(getDataFolder() + "/config.yml");
			List<String> configLines = new ArrayList<>(Files.readAllLines(configPath));
			debug("Read " + configLines.size() + " lines of config.");
			
			debug("Removing all foods currently in config...");
			//Operation 0: remove all foods present (only the foods start with just a " ").
			for(Iterator<String> lineIterator = configLines.iterator(); lineIterator.hasNext();){
				String line = lineIterator.next();
				if(line.startsWith(" ") && line.contains(":")) {
					debug("Removed line: '" + line + "'.");
					lineIterator.remove();
				}
			}
			
			//Operation 1: add all foods.
			String foodLine = "foods:";
			if (configLines.contains(foodLine)){
				int startIndex = configLines.indexOf(foodLine) + 1;
				debug("Adding foods from line " + startIndex + ".");
				//We want the hashmap's optimization, but when going to print we'd like the order of a TreeMap.
				Map<String, Double> sortedFoods = new TreeMap<>(foods);
				List<String> sortedFoodKeys = new ArrayList<>(sortedFoods.keySet());
				//We cycle through it the other way around because adding will shift previous elements down.
				for(int i = sortedFoodKeys.size() - 1;  i >= 0; i--){
					String currentKey = sortedFoodKeys.get(i);
					String lineToAdd = " " + currentKey + ": " + sortedFoods.get(currentKey);
					debug("Added line: '" + lineToAdd + "'");
					configLines.add(startIndex, lineToAdd);
				}operationsSuccessful++;
			}
			
			//Operation 2: add all instantFoods
			String instantFoodLine = "instant-foods:";
			if(configLines.contains(instantFoodLine)){
				int startIndex = configLines.indexOf(instantFoodLine) + 1;
				debug("Adding instantFoods from line " + startIndex + ".");
				//We want the hashmap's optimization, but when going to print we'd like the order of a TreeMap.
				Map<String, Double> sortedInstantFoods = new TreeMap<>(instantFoods);
				List<String> sortedInstantFoodKeys = new ArrayList<>(sortedInstantFoods.keySet());
				//We cycle through it the other way around because adding will shift previous elements down.
				for (int i = sortedInstantFoodKeys.size() - 1; i >= 0; i--){
					String currentKey = sortedInstantFoodKeys.get(i);
					String lineToAdd = " " + currentKey + ": " + sortedInstantFoods.get(currentKey);
					debug("Added: '" + lineToAdd + "'");
					configLines.add(startIndex, lineToAdd);
				}operationsSuccessful++;
			}
			
			//Finally, rewrite config.yml
			Files.write(configPath, configLines); operationsSuccessful++;
			debug("Config saved!");
			
		} catch (Exception e) {}
    	
    	//If everything went according to plan, operationsSuccessful should be 2.
    	if(operationsSuccessful == 3){
    		return true;
    	}
    	
    	return false;
    }
    
    /**
     * Load in plugin commands and name from the plugin.yml inside the jar.
     * @return true, if the operation was successful.
     */
    private boolean loadPluginValues(){
    	try{
        	pName = plugin.getDescription().getName();
        	pNameSend = "[" + ChatColor.DARK_RED + pName + ChatColor.RESET + "] ";
        	pVersion = plugin.getDescription().getVersion();
        	pPermissions = plugin.getDescription().getPermissions();
        	pAuthors = String.join(", ", plugin.getDescription().getAuthors());
        	pAdminCommand = plugin.getDescription().getCommands().entrySet().iterator().next().getKey();
        	return true;
    	}catch (Exception e){
    		return false;
    	}
    }
    
    /***
     * Prepare reflection for safe item names.
     * @return true, if all methods have successfully been reflected. 
     */
    private boolean prepareReflection(){
    	debug("Preparing reflection...");
		Class<?> minecraftItemStack = null;
		Class<?> chatComponent = null;
    	try {
	        //Accurately gets the right formatted server version.
	        String minecraftVersion = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
	        //Target the nms itemstack class.
	        String classToGet = "net.minecraft.server." + minecraftVersion + ".ItemStack";
	        debug("Looking for ItemStack class at: " + classToGet + "...");
	        //Get the class.
	        minecraftItemStack = Class.forName(classToGet);
	        debug("Reflected class, " + minecraftItemStack.getName() + ", successfully.");
	        //Get the proper methods.
	        Method getNMSItem = minecraftItemStack.getMethod("fromBukkitCopy", ItemStack.class);
	        Method getComponentName = minecraftItemStack.getDeclaredMethod("getName");
	        //Target the nms chatComponent class.
	        classToGet = "net.minecraft.server." + minecraftVersion + ".IChatBaseComponent";
	        debug("Looking for ChatComponent class at: " + classToGet + "...");
	        //Get the class.
	        chatComponent = Class.forName(classToGet);
	        debug("Reflected class, " + chatComponent.getName() + ", successfully.");
	        //Get the proper method.
	        Method getTextName = chatComponent.getDeclaredMethod("getText");
    		getRealName = (item) -> {
    			try {
					return getTextName.invoke(getComponentName.invoke((getNMSItem.invoke(this, item)))).toString();
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					e.printStackTrace();
				}
    			return "ohCrap";
    		};
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {}
    	//If there is a method or class not found we just want to quit, because we cannot operate without getting the safe item names.
    	if(getRealName == null){
    		return false;
    	}else{
    		return true;
    	}
    }
    
    /**
     * Gets or creates the baseFood dummy objective.
     * @return if the food objective in the mainScoreboard has either been found or created.
     */
    private boolean setupScoreboard(){
    	debug("Preparing the baseFood scoreboard...");
    	ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
    	Scoreboard mainScoreboard = scoreboardManager.getMainScoreboard();
    	for(Objective o : mainScoreboard.getObjectives()){
    		if(o.getName().matches("food")){
    			foodObjective = o;
    			debug("Dummy objective baseFood found!");
    			return true;
    		}
    	}
    	foodObjective = mainScoreboard.registerNewObjective("food", "dummy", "food");
    	debug("Created new dummy objective: food.");
    	if(mainScoreboard.getObjectives().contains(foodObjective)) return true;
    	
    	return false;
    }
    
    //=====================================================\\
    //				  Reflection Functions				   \\
    //=====================================================\\
    
    /**
     * Uses reflection to get the display name. (Cause only craftbukkit has it :S)
     * @param i
     * @return the displayName as NMS sees it.
     */
    public String craftDisplayName(ItemStack i){
    	return getRealName.apply(i);
    }
    
    //=====================================================\\
    //				   Console Functions				   \\
    //=====================================================\\
    
    /***
     * Displays a debug message if DEBUG = true;
     * @param msg
     */
    public void debug(String msg){
    	if(DEBUG){
    		cm("DEBUG: " + msg);
    	}
    }
    
    /**
     * Displays a debug message with a custom sayer if DEBUG = true;
     * @param sayer
     * @param msg
     */
    public void debug(String sayer, String msg){
    	if(DEBUG){
    		cm(sayer + " = " + msg);
    	}
    }
    
    /***
     * Sends a console message as the plugin.
     * @param msg
     */
    public void cm(String msg){
    	Bukkit.getConsoleSender().sendMessage(pNameSend + msg);
    }
    
    //=====================================================\\
    //				       API Functions				   \\
    //=====================================================\\
    
    /*
     * === MISC ===
     */
    
    /**
     * Crude (java 8) function to get a string List of the names in an Enum.
     * @param e
     * @return a list from String[] from the names of an Enum.
     */
    public List<String> enumNameList(Class<? extends Enum<?>> e) {
        return Arrays.asList(Arrays.stream(e.getEnumConstants()).map(Enum::name).toArray(String[]::new));
    }
    
    /**
     * Gets the Item's display name safely with behavior based on config.
     * @param i
     * @return the ItemName 
     */
    public String getSafeFoodName (ItemStack i){
    	if(allowCustom){
        	if(i.hasItemMeta()){
        		if(i.getItemMeta().hasDisplayName()){
        			debug("Custom item detected!");
        			if(!i.getItemMeta().getDisplayName().contains(ChatColor.RESET + "")){
        				if(detectAnvil){
        					debug("Is anvil-item!");
        					String anvilName = "a_" + ChatColor.stripColor(i.getItemMeta().getDisplayName());
        					//If a player names an item and its not on our list, we still want to check against the name of the original item name.
        					if(revertAnvil){
        						if(getFoodType(anvilName) == FoodType.NON){
        							String originalName = getOriginalItemName(i);
        							debug("Anvil-item " + anvilName + " wasn't in the list, reverting back to " + originalName + ".");
        							return originalName;
        						}
        					}
    						return anvilName;
        				}
        			}
        			return ChatColor.stripColor(i.getItemMeta().getDisplayName());
        		}
        	}
    	}
    	return craftDisplayName(i);
    }
    
    /**
     * Gets the ItemName of i before it was renamed.
     * @param i
     * @return the original itemName.
     */
    public String getOriginalItemName (ItemStack i){
		ItemStack copy = i;
		ItemMeta copyMeta = copy.getItemMeta();
		copyMeta.setDisplayName("");
		copy.setItemMeta(copyMeta);
		return craftDisplayName(copy);
    }
    
    /*
     * === Food Level ===
     */
    
    /**
     * Updates the player's food level.
     * @param p
     * @return true, if the player's foodLevel or saturation had to be updated.
     */
    public boolean updateFoodLevel(Player p){
    	int foodToUpdate = getFoodLevel(p);
		if(p.getFoodLevel() != foodToUpdate || p.getSaturation() != foodToUpdate){
			p.setFoodLevel(foodToUpdate);
			p.setSaturation(foodToUpdate);
			debug("Updated food and saturation of " + p.getName() + " to " + foodToUpdate);
			return true;
		}
		return false;
    }
    
    /**
     * Sets the food of a player using playerName.
     * @param p
     * @param baseFood
     * @return true, if the player's food level has changed.
     */
    public boolean setFoodLevel (String playerName, int foodLevel){
    	if(foodObjective.getScore(playerName).getScore() != foodLevel){
        	foodObjective.getScore(playerName).setScore(foodLevel);
        	debug(playerName + " now has a food of " + foodLevel + ".");
        	return true;
    	}
    	debug(playerName + " still has a food of " + foodLevel + ".");
    	return false;
    }
    
    /**
     * Sets the food of a player using player.
     * @param p
     * @param baseFood
     * @return true, if the player's food level has changed.
     */
    public boolean setFoodLevel (Player p, int foodLevel){
    	return setFoodLevel(p.getName(), foodLevel);
    }
    
    /***
     * Resets the player's food to 0 to symbolize the default-food using playerName.
     * @param playerName
     * @return true, if the player's food level is changed.
     */
    public boolean resetFoodLevel (String playerName){
    	debug("To symbolize default: " + defaultFood + ",");
    	return setFoodLevel(playerName, 0);
    }
    
    /***
     * Resets the player's food to 0 to symbolize the default-food using player.
     * @param playerName
     * @return true, if the player's food level is changed.
     */
    public boolean resetFoodLevel (Player p){
    	return resetFoodLevel(p.getName());
    }
    
    /**
     * Gets the foodLevel of a player from the scoreboard using playerName.
     * Marks it with 0 to symbolize default-food if no score has been set.
     * @param playerName
     * @return the foodLevel of a player.
     */
    public int getFoodLevel (String playerName){
    	int food = foodObjective.getScore(playerName).getScore();
    	
    	//Since commandBlocks or other plugins may use the scoreboard to alter this at any time, check for faults.
		if(food < 0 || food > 20){
			cm(ChatColor.RED + "ERROR: " + playerName + " has an illigal food of " + food + ", resetting it to 0...");
			resetFoodLevel(playerName);
			food = foodObjective.getScore(playerName).getScore();
		}
		
		//The 0 symbolizes the defaultFood.
		if(food == 0){
			return plugin.defaultFood;
		}
    	return foodObjective.getScore(playerName).getScore();
    }
    
    /**
     * Gets the foodLevel of a player from the scoreboard using player.
     * Marks it with 0 to symbolize default-food if no score has been set.
     * @param p
     * @return the foodLevel of a player.
     */
    public int getFoodLevel (Player p){
    	return getFoodLevel(p.getName());
    }
    
    /*
     * === Giving / Taking Health ===
     */
    
    /**
     * Changes the health of a player with effect based on the config.
     * Damage can be dealt by add negatively.
     * @param p
     * @param healthToAdd
     * @return true, if the player's health has been changed.
     */
    public boolean addHealth (Player p, Double healthToAdd){
    	if(healthToAdd > 0){
    		regenPlayer(p, healthToAdd);
    		return true;
    	}else if(healthToAdd < 0){
    		//-healthToAdd is health to subtract! :)
    		damagePlayer(p, -healthToAdd);
    		return true;
    	}
    	return false;
    }
    
    /**
     * Heals a player with effect based on the config. 
     * @param p
     * @param toHeal
     * @return the ticks Regeneration II is given. <br>
     * 0 if the config doesn't call for potionMechanics.
     */
    public int regenPlayer(Player p, Double toHeal){
    	if(!potionMechanics){
        	Double pHealth = p.getHealth();
        	Double pMaxHealth = p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        	//Regeneration I is applied just long enough for a nice effect.
        	if(potionEffects) p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20, 0));
        	//Bukkit doesn't want us to go over the maximum, so we won't.
        	if(pHealth + toHeal < pMaxHealth){
        		p.setHealth(pHealth + toHeal);
        		debug("Healed " + p.getName() + " with " + toHeal + " halfhearts.");
        	}else{
        		p.setHealth(pMaxHealth);
        		debug("Healed " + p.getName() + " to maximum health.");
        	}
        	return 0;
    	}else{
    		//Regeneration II heals half a heart approximately 1.25 seconds (25 ticks).
    		int ticksToHeal = (int) Math.ceil(toHeal * 25); 
    		p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, ticksToHeal, 1));
    		debug("Applied Regeneration II to " + p.getName() + " for " + ticksToHeal + " ticks.");
    		return ticksToHeal;
    	}
    }
    
    /**
     * Damages a player with effect based on config.
     * @param p
     * @param damage
     * @return the ticks Poison II is given. <br>
     * 0 if the config doesn't call for potionMechanics
     */
    public int damagePlayer(Player p, Double damage){
		if(!potionMechanics){
			//Poison I is applied just long enough for the awful effect.
	    	if(potionEffects) p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20, 0));
	    	//Damage the player.
	    	p.damage(damage);
	    	debug("Applied " + damage + " damage to " + p.getName());
	    	return 0;
		}else{
			//Poison II damages about half a heart every 0.6 seconds (12 ticks). Round it up above, make sure we get the full damage.
			int ticksToPoison = (int) Math.ceil(damage * 12); 
			p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, ticksToPoison, 1));
			debug("Applied Poison II to " + p.getName() + " for " + ticksToPoison + " ticks.");
			return ticksToPoison;
		}
    }
    
    /*
     * === Foods ===
     */
    
    
    /**
     * Gets foodType based on a (safe) ItemName.
     * @param name
     * @return the food type of a certain food.
     */
    public FoodType getFoodType (String name){
    	if(foods.containsKey(name)) return FoodType.CONSUMABLE;
    	if(instantFoods.containsKey(name)) return FoodType.INSTANT;
    	return FoodType.NON;
    }
    
    /**
     * Gets foodType based on an ItemStack.
     * @param i
     * @return the food type of a certain food.
     */
    public FoodType getFoodType (ItemStack i){
    	return getFoodType(getSafeFoodName(i));
    }
    
    /***
     * Gets the worth of a food based on a (safe) ItemName and FoodType.
     * @param name
     * @param type
     * @return the halfHeartsToHeal of a certain food.
     */
    public Double getFoodWorth (String name, FoodType type){
    	if(type == FoodType.CONSUMABLE) return foods.get(name);
    	if(type == FoodType.INSTANT) return instantFoods.get(name);
    	return 0.0;
    }
    
    /***
     * Gets the worth of a food based on an ItemStack and FoodType.
     * @param i
     * @param type
     * @return the halfHeartToHeal of a certain food.
     */
    public Double getFoodWorth (ItemStack i, FoodType type){
    	return getFoodWorth (getSafeFoodName(i), type);
    }
    
    /***
     * Gets the worth of a food based on a (safe) ItemName.
     * @param name
     * @return the halfHeartsToHeal of a certain food.
     */
    public Double getFoodWorth (String name){
    	return getFoodWorth(name, getFoodType(name));
    }
    
    /***
     * Gets the worth of a food based on a ItemStack.
     * @param i
     * @return the halfHeartsToHeal of a certain food.
     */
    public Double getFoodWorth (ItemStack i){
    	return getFoodWorth(getSafeFoodName(i));
    }
    
    /**
     * Check if an item should give back a bowl.
     * @param name The ItemName to check.
     * @return if the food should return a bowl.
     */
    public boolean returnsBowl (String name){
    	for(String returnBowlName : returnBowl){
    		if(name.contains(returnBowlName)) return true;
    	}
    	return false;
    }
    
    /**
     * Check if an item should give back a bowl.
     * @param i The ItemStack to check.
     * @return if the food should return a bowl.
     */
    public boolean returnsBowl (ItemStack i){
    	return returnsBowl(getSafeFoodName(i));
    }
    
    /**
     * Simple function to get a bowl.
     * @return a new simple bowl ItemStack.
     */
    public ItemStack bowl(){
    	return new ItemStack(Material.BOWL);
    }
    
    /**
     * Adds a new food using a (safe) name.<br>
     * <br>
     * If you want to register a new food,
     * it's better to check if getFoodType == FoodType.NON in onEnable()
     * and call {@link #saveFoodsToConfig()},
     * so the food is saved on file and can be edited by the serverowner. <br>
     * <br>
     * If you want to forcefully decide the halfHeartsToHeal or foodType you need to call this function every time in when your plugin is enabled.
     * using appendToConfig = false.
     * @param name
     * @param halfHeartsToHeal
     * @param appendToConfig
     * @return if the operation was successful.
     */
    public boolean addFood (String name, FoodType foodType, Double halfHeartsToHeal){
    	//If the food isn't already here...
    	if(getFoodType(name) == FoodType.NON){
    		if(foodType == FoodType.CONSUMABLE){
    			foods.put(name, halfHeartsToHeal);
    			return true;
    		}
        	if(foodType == FoodType.INSTANT){
        		instantFoods.put(name, halfHeartsToHeal);
        		return true;
        	}
    	}
    	return false;
    }
    
    /**
     * Adds a new food using an ItemStack. <br>
     * <br>
     * If you want to register a new food,
     * it's better to check if getFoodType == FoodType.NON in onEnable()
     * and call {@link #saveFoodsToConfig()},
     * so the food is saved on file and can be edited by the serverowner. <br>
     * <br>
     * If you want to forcefully decide the halfHeartsToHeal you need to call this function every time in when your plugin is enabled.
     * using appendToConfig = false.
     * @param i
     * @param halfHeartsToHeal
     * @param appendToConfig
     * @return if the operation was successful.
     */
    public boolean addFood (ItemStack i, FoodType foodType, Double halfHeartsToHeal){
    	return addFood(getSafeFoodName(i), foodType, halfHeartsToHeal);
    }
    
    /**
     * Sets the value of a food using a (safe) name. <br>
     * <br>
     * If you want this value to stick (save to config), be sure to also call
     * {@link #saveFoodsToConfig()}.
     * @param i
     * @param foodType
     * @param halfHeartsToHeal
     * @return if the operation was successful.
     */
    public boolean setFood (String name, Double halfHeartsToHeal){
    	if(getFoodType(name) == FoodType.CONSUMABLE){
    		foods.replace(name, halfHeartsToHeal);
    		return true;
    	}
    	if(getFoodType(name) == FoodType.INSTANT){
    		instantFoods.replace(name, halfHeartsToHeal);
    		return true;
    	}
    	return false;
    }
    
    /**
     * Sets the value of a food using an ItemStack. <br>
     * <br>
     * If you want this value to stick (save to config), be sure to also call
     * {@link #saveFoodsToConfig()}.
     * @param i
     * @param foodType
     * @param halfHeartsToHeal
     * @return if the operation was successful.
     */
    public boolean setFood (ItemStack i, Double halfHeartsToHeal){
    	return setFood(getSafeFoodName(i), halfHeartsToHeal);
    }
    
}
