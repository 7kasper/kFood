package kmod.plugmod.kfood;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import net.md_5.bungee.api.ChatColor;

public class kFood extends JavaPlugin{
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
	
	//Plugin values, will load from plugin.yml
	public String pName = "kFood";
	public String pNameSend = "kFood";
	public String pVersion = "0.0";
	public String pAuthors = "7kasper";
	public String pAdminCommand = "kfood";
	public kFood plugin;
	
	//Reflection stuff, will load onEnable()
	private Class<?> craftItemStack = null;
	private Class<?> minecraftItemStack = null;
	private Method craftItemStackAsNMSCopy = null;
	private Method craftItemStackGetName = null;
	
	//Scoreboard objective, to keep track of per player basefood.
	private Objective foodObjective = null;
	
	/**
	 * Called whenever kFood initializes.
	 */
    @Override
    public void onEnable() {
    	//Set up for future reference.
    	plugin = this;
    	
    	//Get some data from the plugin.yml
    	loadPluginValues();
    	//Load in the config.yml and its values.
    	loadFromConfig();
    	//Prepare reflection for safe item names.
    	prepareReflection();
    	
		debug("Implementing " + pName + " listners...");
    	Bukkit.getPluginManager().registerEvents(new FoodListeners(this), this);
    	
    	debug("Implementing " + pName + " commands...");
    	AdminCommand adminCommands = new AdminCommand(this);
    	getCommand(pAdminCommand).setExecutor(adminCommands);
    	getCommand(pAdminCommand).setTabCompleter(adminCommands);
    	
    	debug("Preparing the baseFood scoreboard...");
    	setupScoreboard();
    	//Done!
    	cm("Hunger is now a thing of the past!");
    }
    
    @Override
    public void onDisable() {
    	cm("Don't forget your sandwich!");
    }
    
    //=====================================================\\
    //				  Preparing Functions				   \\
    //=====================================================\\
    
    /***
     * (re)load config file.
     */
    public void loadFromConfig(){
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
    }
    
    /**
     * Flushes the foods and values in memory to the config.yml.
     */
    public void saveToConfig(){
    	debug("Saving foods to file...");
    	//We don't need to do casting here, yay!
    	getConfig().createSection("foods", foods);
    	debug("Consumable foods set.");
    	getConfig().createSection("instant-foods", instantFoods);
    	debug("instantFoods saved.");
    	saveConfig();
    	debug("Config saved!");
    }
    
    /**
     * Load in plugin commands and name from the plugin.yml inside the jar.
     */
    private void loadPluginValues(){
    	pVersion = plugin.getDescription().getVersion();
    	pName = plugin.getDescription().getName();
    	pNameSend = "[" + ChatColor.DARK_RED + pName + ChatColor.RESET + "] ";
    	pAdminCommand = plugin.getDescription().getCommands().entrySet().iterator().next().getKey();
    	pAuthors = StringUtils.join(plugin.getDescription().getAuthors(), ", ");
    }
    
    /***
     * Prepare reflection for safe item names.
     */
    private void prepareReflection(){
    	debug("Preparing reflection...");
    	try {
    		//CraftItem class is easy to get because we can call getServer();
    		String classToGet = getServer().getClass().getPackage().getName() + ".inventory.CraftItemStack";
    		debug("Looking for CraftItemStack class at: " + classToGet + "...");
			craftItemStack = Class.forName(classToGet);
			debug("Reflected class, " + craftItemStack.getName() + ", successfully.");
	        for (Method m : craftItemStack.getMethods()) {
	            if (m.getName().equals("asNMSCopy") && m.getParameterTypes().length == 1) {
	            	craftItemStackAsNMSCopy = m;
	            	debug("Reflected method, " + m.getName() + ", successfully.");
	            }
	        }
	        //Accurately gets the right formatted server version.
	        String minecraftVersion = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
	        //Points to the class we want.
	        classToGet = "net.minecraft.server." + minecraftVersion + ".ItemStack";
	        debug("Looking for ItemStack class at: " + classToGet + "...");
	        //Get the class.
	        minecraftItemStack = Class.forName(classToGet);
	        debug("Reflected class, " + minecraftItemStack.getName() + ", successfully.");
	        for(Method m : minecraftItemStack.getMethods()){
	            if (m.getName().equals("getName") && m.getParameterTypes().length == 0){
	            	craftItemStackGetName = m;
	            	debug("Reflected method, " + m.getName() + ", successfully.");
	            }
	        }
	        
		} catch (ClassNotFoundException e) {}
    	//If there is a method or class not found we just want to quit, because we cannot operate without getting the safe item names.
    	if(craftItemStackAsNMSCopy == null || craftItemStackGetName == null){
    		cm(ChatColor.RED + "Fatal error during reflecting! Are the names of some core classes or methods changed?");
    		Bukkit.getPluginManager().disablePlugin(plugin);
    	}
    }
    
    /**
     * Gets or creates the baseFood dummy objective.
     */
    private void setupScoreboard(){
    	ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
    	Scoreboard mainScoreboard = scoreboardManager.getMainScoreboard();
    	for(Objective o : mainScoreboard.getObjectives()){
    		if(o.getName().matches("food")){
    			foodObjective = o;
    			debug("Dummy objective baseFood found!");
    			return;
    		}
    	}
    	foodObjective = mainScoreboard.registerNewObjective("food", "dummy");
    	debug("Created new dummy objective: food.");
    }
    
    //=====================================================\\
    //				  Reflection Functions				   \\
    //=====================================================\\
    
    /**
     * Uses reflection to get the display name. (Cause only craftbukkit has it :S)
     * @param i
     * @return
     */
    public String craftDisplayName(ItemStack i){
    	try {
			return craftItemStackGetName.invoke(getReflectCraftItemStack(i)).toString();
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
    	return "";
    }
    
    /**
     * Use reflection to get a CraftItemStack as object.
     * @param i
     * @return
     */
    public Object getReflectCraftItemStack(ItemStack i) {
    	try {
			return craftItemStackAsNMSCopy.invoke(this, i);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
    	return null;
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
     * @return
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
     */
    public void updateFoodLevel(Player p){
    	int foodToUpdate = getFoodLevel(p);
		if(p.getFoodLevel() != foodToUpdate || p.getSaturation() != foodToUpdate){
			p.setFoodLevel(foodToUpdate);
			p.setSaturation(foodToUpdate);
			debug("Updated food and saturation of " + p.getName() + " to " + foodToUpdate);
		}
    }
    
    /**
     * Sets the food of a player.
     * @param p
     * @param baseFood
     */
    public void setFoodLevel (Player p, int food){
    	String playerName = p.getName();
    	foodObjective.getScore(playerName).setScore(food);
    	debug(playerName + " now has a food of " + food + ".");
    }
    
    /***
     * Resets the player's food to 0 to symbolize the default-food.
     * @param p
     */
    public void resetFoodLevel (Player p){
    	String playerName = p.getName();
    	foodObjective.getScore(playerName).setScore(0);
    	debug(playerName + " now has a food of 0; symbolizes default: " + defaultFood + ".");
    }
    
    /**
     * Gets the food of a player from the scoreboard.
     * Marks it with 0 to symbolize default-food if no score has been set.
     * @param p
     * @return the foodLevel of a player.
     */
    public int getFoodLevel (Player p){
    	String playerName = p.getName();
    	int food = foodObjective.getScore(playerName).getScore();
    	
    	//Since commandBlocks or other plugins may use the scoreboard to alter this at any time, check for faults.
		if(food < 0 || food > 20){
			cm(ChatColor.RED + "ERROR: " + playerName + " has an illigal food of " + food + ", resetting it to 0...");
			resetFoodLevel(p);
			food = foodObjective.getScore(playerName).getScore();
		}
		
		//The 0 symbolizes the defaultFood.
		if(food == 0){
			return plugin.defaultFood;
		}
    	return foodObjective.getScore(playerName).getScore();
    }
    
    /*
     * === Giving / Taking Health ===
     */
    
    /**
     * Changes the health of a player with effect based on the config.
     * Damage can be dealt by add negatively.
     * @param p
     * @param healthToAdd
     */
    public void addHealth (Player p, Double healthToAdd){
    	if(healthToAdd > 0){
    		regenPlayer(p, healthToAdd);
    	}else if(healthToAdd < 0){
    		//-healthToAdd is health to subtract! :)
    		damagePlayer(p, -healthToAdd);
    	}
    }
    
    /**
     * Heals a player with effect based on the config. 
     * @param p
     * @param toHeal
     */
    public void regenPlayer(Player p, Double toHeal){
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
    	}else{
    		//Regeneration II heals half a heart approximately 1.25 seconds (25 ticks).
    		int ticksToHeal = (int) Math.ceil(toHeal * 25); 
    		debug("Applying regeneration to " + p.getName() + " for " + ticksToHeal + " ticks.");
    		p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, ticksToHeal, 1));
    	}
    }
    
    /**
     * Damages a player with effect based on config.
     * @param p
     * @param damage
     */
    public void damagePlayer(Player p, Double damage){
		if(!potionMechanics){
			//Poison I is applied just long enough for the awful effect.
	    	if(potionEffects) p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20, 0));
	    	//Damage the player.
	    	p.damage(damage);
	    	debug("Applied " + damage + " to " + p.getName());
		}else{
			//Poison II damages half a heart every 0.5 seconds (10 ticks). Round it up above, make sure we get the full damage.
			int ticksToPoison = (int) Math.ceil(damage * 12); 
			debug("Applying poisen to " + p.getName() + " for " + ticksToPoison + " ticks.");
			p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, ticksToPoison, 1));
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
     * Adds a new food using a (safe) name.<br>
     * <br>
     * If you want to register a new food,
     * it's better to check if getFoodType == FoodType.NON in onEnable()
     * and call the saveConfig function.
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
     * and call {@link saveFoodsToConfig()},
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
     * {@link saveFoodsToConfig()}.
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
     * {@link saveFoodsToConfig()}.
     * @param i
     * @param foodType
     * @param halfHeartsToHeal
     * @return if the operation was successful.
     */
    public boolean setFood (ItemStack i, Double halfHeartsToHeal){
    	return setFood(getSafeFoodName(i), halfHeartsToHeal);
    }
    
    
    
}
