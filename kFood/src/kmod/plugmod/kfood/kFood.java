package kmod.plugmod.kfood;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
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
	public boolean potionEffects = true;
	public boolean potionMechanics = false;
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
	public String pCommand = "kfood";
	public kFood plugin;
	
	//Reflection stuff, will load onEnable()
	private Class<?> craftItemStack = null;
	private Class<?> minecraftItemStack = null;
	private Method craftItemStackAsNMSCopy = null;
	private Method craftItemStackGetName = null;
	
	//Scoreboard objective, to keep track of per player basefood.
	private Objective baseFoods = null;
	private Scoreboard mainScoreboard = null;
	
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
    	loadConfig();
    	//Prepare reflection for safe item names.
    	prepareReflection();
    	
		debug("Implementing " + pName + " listners...");
    	Bukkit.getPluginManager().registerEvents(new Listeners(this), this);
    	
    	debug("Implementing " + pName + " commands...");
    	Commands commands = new Commands(this);
    	getCommand(pCommand).setExecutor(commands);
    	getCommand(pCommand).setTabCompleter(commands);
    	
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
    public void loadConfig(){
		//Load the config file and all the values.
    	saveDefaultConfig();
    	reloadConfig();
		DEBUG = getConfig().getBoolean("debug");
		debug("DEBUG enabled!");
		
		debug("Getting main settings...");
		defaultFood = getConfig().getInt("default-food");
		debug("defaultFood", Integer.toString(defaultFood));
		potionEffects = getConfig().getBoolean("potion-effects");
		debug("potionEffects", Boolean.toString(potionEffects));
		potionMechanics = getConfig().getBoolean("potion-mechanics");
		debug("potionMechanics", Boolean.toString(potionMechanics));
		allowCustom = getConfig().getBoolean("allow-custom");
		debug("allowCustom", Boolean.toString(allowCustom));
		detectAnvil = getConfig().getBoolean("detect-anvil");
		debug("detectAnvil", Boolean.toString(detectAnvil));
		revertAnvil = getConfig().getBoolean("revert-anvil");
		debug("revertAnvil", Boolean.toString(revertAnvil));
		
		debug("Loading foods...");
		getConfig().getConfigurationSection("foods").getValues(false).forEach(
			(k,v) -> {
				foods.put(k.toString(), new Double(v.toString()));
				debug(k.toString(), v.toString());
			});
		debug("Loading instant foods...");
		getConfig().getConfigurationSection("instant-foods").getValues(false).forEach(
				(k,v) -> {
					instantFoods.put(k.toString(), new Double(v.toString()));
					debug(k.toString(), v.toString());
				});
		
		debug("Getting Misc values...");
		cakeHeal = getConfig().getDouble("cake");
		debug("cakeHeal", cakeHeal.toString());
    }
    
    /**
     * Load in plugin commands and name from the plugin.yml inside the jar.
     */
    private void loadPluginValues(){
    	pVersion = plugin.getDescription().getVersion();
    	pName = plugin.getDescription().getName();
    	pNameSend = "[" + ChatColor.DARK_RED + pName + ChatColor.RESET + "] ";
    	pCommand = plugin.getDescription().getCommands().entrySet().iterator().next().getKey();
    	pAuthors = StringUtils.join(plugin.getDescription().getAuthors(), ", ");
    }
    
    /***
     * Prepare reflection for save item names.
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
    	//If there is a method or class not found we just want to quit, because we cannot operate without getting the save item names.
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
    	mainScoreboard = scoreboardManager.getMainScoreboard();
    	for(Objective o : mainScoreboard.getObjectives()){
    		if(o.getName().matches("baseFood")){
    			baseFoods = o;
    			debug("Dummy objective baseFood found!");
    			return;
    		}
    	}
    	baseFoods = mainScoreboard.registerNewObjective("baseFood", "dummy");
    	debug("Created new dummy objective baseFood.");
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
    
    /**
     * Updates the player's food level.
     * @param p
     */
    public void updateFood(Player p){
    	int foodToUpdate = getBaseFood(p);
		if(p.getFoodLevel() != foodToUpdate || p.getSaturation() != foodToUpdate){
			p.setFoodLevel(foodToUpdate);
			p.setSaturation(foodToUpdate);
			debug("Updated food and saturation of " + p.getName() + " to " + foodToUpdate);
		}
    }
    
    /**
     * Sets the baseFood of a player.
     * @param p
     * @param baseFood
     */
    public void setBaseFood (Player p, int baseFood){
    	String playerName = p.getName();
    	baseFoods.getScore(playerName).setScore(baseFood);
    	debug(playerName + " now has a baseFood of " + baseFood + ".");
    	updateFood(p);
    }
    
    /***
     * Sets the player's baseFood to -1 to symbolize default. (Will read as default basefood again).
     * @param p
     */
    public void resetBaseFood (Player p){
    	String playerName = p.getName();
    	if(baseFoods.getScore(playerName) != null){
    		if(baseFoods.getScore(playerName).getScore() != -1){
        		baseFoods.getScore(playerName).setScore(-1);
        		debug(playerName + " now has a baseFood of -1, symbolizes default: " + defaultFood);
    		}
    	}
    }
    
    /**
     * Gets the baseFood of a player from the scoreboard.
     * Marks it with -1 to state: default if no score has been set.
     * @param p
     * @return
     */
    public int getBaseFood (Player p){
    	String playerName = p.getName();
    	if(baseFoods.getScore(playerName) != null){
    		int baseFood = baseFoods.getScore(playerName).getScore();
    		if(baseFood != -1){
    			if(baseFood >= -2 && baseFood <= 20){
            		return baseFoods.getScore(playerName).getScore();
    			}else{
    				cm(ChatColor.RED + "ERROR: " + playerName + " has an illigal score of " + baseFood + ", resetting it to -1!");
    				baseFoods.getScore(playerName).setScore(-1);
    			}
    		}
    	}else{
    		baseFoods.getScore(playerName).setScore(-1);
    	}
    	return plugin.defaultFood;
    }
    
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
    
    /***
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
     */
    public void damagePlayer(Player p, Double damage){
		if(!potionMechanics){
			//Poison I is applied just long enough for the awfull effect.
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
    
    /**
     * Gets the Item's display name safely with behavior based on config.
     * @param i
     * @return
     */
    public String getSaveName (ItemStack i){
    	if(allowCustom){
        	if(i.hasItemMeta()){
        		if(i.getItemMeta().hasDisplayName()){
        			if(!i.getItemMeta().getDisplayName().contains(ChatColor.RESET + "")){
        				if(detectAnvil){
        					if(revertAnvil){
        						//TODO: Add a revert when the list doesn't contain the named item. (set in config)
        						return "a_" + ChatColor.stripColor(i.getItemMeta().getDisplayName());
        					}else{
        						ItemMeta iM = i.getItemMeta();
        						iM.setDisplayName("");
        						i.setItemMeta(iM);
        						return craftDisplayName(i);
        					}
        				}
        			}
        			return ChatColor.stripColor(i.getItemMeta().getDisplayName());
        		}
        	}
    	}
    	return craftDisplayName(i);
    }  

}
