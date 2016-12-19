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
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import net.md_5.bungee.api.ChatColor;

public class kFood extends JavaPlugin{
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
	
	public String pName = "kFood";
	public String pNameSend = "kFood";
	public String pVersion = "0.0";
	public String pAuthors = "7kasper";
	public String pCommand = "kfood";
	public kFood plugin;
	
	//Reflection shit:
	private Class<?> craftItemStack = null;
	private Class<?> minecraftItemStack = null;
	private Method craftItemStackAsNMSCopy = null;
	private Method craftItemStackGetName = null;
	
    @Override
    public void onEnable() {
    	//Set up for future reference.
    	plugin = this;
    	
    	//Get some data from the plugin.yml
    	pVersion = plugin.getDescription().getVersion();
    	pName = plugin.getDescription().getName();
    	pNameSend = "[" + ChatColor.DARK_RED + pName + ChatColor.RESET + "] ";
    	pCommand = plugin.getDescription().getCommands().entrySet().iterator().next().getKey();
    	pAuthors = StringUtils.join(plugin.getDescription().getAuthors(), ", ");
    	
    	//Load in the config.yml and its values.
    	loadConfig();
    	//Prepare reflection for safe item names.
    	prepareReflection();
    	
		debug("Implementing kFood listners...");
    	Bukkit.getPluginManager().registerEvents(new listeners(this), this);
    	
    	debug("Implementing kFood commands...");
    	commands commands = new commands(this);
    	getCommand(pCommand).setExecutor(commands);
    	getCommand(pCommand).setTabCompleter(commands);
    	
    	//Done!
    	cm("Hunger is now a thing of the past!");
    }
    
    @Override
    public void onDisable() {
    	cm("Don't forget your sandwich!");
    }
    
    /***
     * Prepare reflection for save item names.
     */
    private void prepareReflection(){
    	debug("Preparing reflection...");
    	try {
    		//CraftItem class is easy to get because we can call getServer();
			craftItemStack = Class.forName(getServer().getClass().getPackage().getName() + ".inventory.CraftItemStack");
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
	        String classToGet = "net.minecraft.server." + minecraftVersion + ".ItemStack";
	        debug("Looking for ItemStack class at: " + classToGet);
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
    	
    	if(craftItemStackAsNMSCopy == null || craftItemStackGetName == null){
    		cm(ChatColor.RED + "Fatal error during reflecting! Have classnames changed?");
    		Bukkit.getPluginManager().disablePlugin(plugin);
    	}
    }
    
    /**
     * Adds health to the player.
     * Since bukkit doesn't seem to want to give health over the maximum,
     * a save check is implemented in this function.
     * @param p
     * @param healthToAdd
     */
    public void addHealth (Player p, Double healthToAdd){
    	if(healthToAdd > 0){
    		regenPlayer(p, healthToAdd);
    	}else if(healthToAdd < 0){
    		//-healthToAdd is health to subtract! :)
    		poisonPlayer(p, -healthToAdd);
    	}
    }
    
    /***
     * Heals a player.
     * @param p
     * @param toHeal
     */
    public void regenPlayer(Player p, Double toHeal){
    	if(!potionMechanics){
        	Double pHealth = p.getHealth();
        	Double pMaxHealth = p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        	if(potionEffects) p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20, 0));
        	if(pHealth + toHeal < pMaxHealth){
        		p.setHealth(pHealth + toHeal);
        	}else{
        		p.setHealth(pMaxHealth);
        	}
    	}else{
    		//Regeneration II heals half a heart every 0.6 seconds (12 ticks)
    		int ticksToHeal = (int) Math.ceil(toHeal * 25); 
    		debug("Applying regeneration to " + p.getName() + " for " + ticksToHeal + " ticks.");
    		p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, ticksToHeal, 1));
    	}
    }
    
    /**
     * Poisons a player.
     */
    public void poisonPlayer(Player p, Double damage){
		if(!potionMechanics){
			//When enabled, color the hearts.
	    	if(potionEffects) p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20, 0));
	    	//Damage the player.
	    	p.damage(damage);
		}else{
			//Poison II damages half a heart every 0.5 seconds (10 ticks). Round it up above, make sure we get the full damage.
			int ticksToPoison = (int) Math.ceil(damage * 12); 
			debug("Applying poisen to " + p.getName() + " for " + ticksToPoison + " ticks.");
			p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, ticksToPoison, 1));
		}
    }
    
    /**
     * Gets the Item's display name safely, since bukkit won't do it for us.
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
    
    /**
     * Stores the baseFood of a player in their metadata!
     * @param p
     * @param baseFood
     */
    public void setBaseFood (Player p, int baseFood){
    	p.setMetadata("baseFood", new FixedMetadataValue (plugin, baseFood));
    	plugin.updateFood(p);
    }
    
    /**
     * Gets the baseFood of a player from their metadata!
     * @param p
     * @return
     */
    public int getBaseFood (Player p){
    	if(p.hasMetadata("baseFood")){
        	return p.getMetadata("baseFood").get(0).asInt();
    	}else{
    		return plugin.defaultFood;
    	}
    }
    
    /**
     * Updates the player's food level.
     * @param p
     */
    public void updateFood(Player p){
    	int foodToUpdate = getBaseFood(p);
		if(p.getFoodLevel() != foodToUpdate || p.getSaturation() != foodToUpdate){
			p.setFoodLevel(foodToUpdate);
			p.setSaturation(foodToUpdate);
		}
    }
    
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

}
