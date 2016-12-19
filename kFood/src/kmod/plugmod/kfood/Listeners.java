package kmod.plugmod.kfood;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;

/***
 * Class that houses all kFood listeners.
 * @author 7kasper
 */
public class Listeners implements Listener{
	private final kFood plugin;
	
	public Listeners(kFood Plugin){
		this.plugin = Plugin;
		return;
	}
	
	   //=====================================================\\
	   //                    Giving Health                    \\
	   //=====================================================\\
	
    /***
     * When a player eats something, we want to check if the item he ate is in the list and apply health.
     * @param e
     */
   @EventHandler (priority = EventPriority.HIGH)
   public void onItemConsume(PlayerItemConsumeEvent e){
	   Player p = e.getPlayer();
	   if(!e.isCancelled()){
		   String eatName = plugin.getSaveName(e.getItem());
		   //Bukkit is a bit weird and doesn't apply health when it goes over the top.
		   if(plugin.foods.containsKey(eatName)){
			   plugin.addHealth(p, plugin.foods.get(eatName));
			   plugin.debug(p.getName() + " ate " + eatName + ".");
		   }else{
			   plugin.debug(p.getName() + " ate " + eatName + " but that ain't a known food.");
		   }
	   }
   }
   
   /**
    * We also want to handle cakes.
    * @param e
    */
   @EventHandler (priority = EventPriority.HIGH)
   public void onPlayerInteract(PlayerInteractEvent e){
   Player p = e.getPlayer();
	   
	   /*
	    * Handles the instant eating on right-click.
	    */
	   if(e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK)){
		   if(e.getItem() != null){
			   String eatName = plugin.getSaveName(e.getItem());
			   if(plugin.instantFoods.containsKey(eatName)){
				   e.setCancelled(true);
				   //Takes one of the stack away, otherwise completely destroys the stack.
				   if(e.getItem().getAmount() > 1){
					   e.getItem().setAmount(e.getItem().getAmount() -1);
				   }else{
					   //When e.getHand() is set to EquipmentSlot.HAND it's in the main hand.
					   if(e.getHand().equals(EquipmentSlot.HAND)){
						   e.getPlayer().getInventory().setItemInMainHand(null);
						   plugin.debug(p.getName() + " instantly ate " + eatName + " from his main hand.");
					   }else if(e.getHand().equals(EquipmentSlot.OFF_HAND)){
						   e.getPlayer().getInventory().setItemInOffHand(null);
						   plugin.debug(p.getName() + " instantly ate " + eatName + " from his off hand.");
					   }
				   }
				   plugin.addHealth(p, plugin.instantFoods.get(eatName));
				   plugin.debug(p.getName() + " instantly ate " + eatName + ".");
			   }
		   }
	   }
	   
	   /*
	    * The cake is a special case and needs to be checked separately.
	    */
	   if(e.getAction().equals(Action.RIGHT_CLICK_BLOCK)){
		   if(!e.isCancelled()){
			   if(e.getClickedBlock().getType().equals(Material.CAKE_BLOCK)){
				   plugin.debug(p.getName() + " ate a piece of cake.");
				   plugin.addHealth(p, plugin.cakeHeal);
			   }  
		   }
	   }   
   }
   
   
   //=====================================================\\
   //                    Stopping Food                    \\
   //=====================================================\\
    
   
    /***
     * Forcefully disables the food event and makes sure that food levels stay the same.
     * @param e
     */
    @EventHandler (priority = EventPriority.HIGHEST)
    public void foodChangeEvent (FoodLevelChangeEvent e){
    	if(e.getEntityType() == EntityType.PLAYER){
    		Player p = (Player)e.getEntity();
        	//F*ck hunger, disable from the getgo.
        	e.setCancelled(true);
    		plugin.updateFood(p);
    	}
    }
    
    /***
     * Ensures when a player respawns, their food is updated.
     * @param e
     */
    @EventHandler
    public void onPlayerRespawn (PlayerRespawnEvent e){
    	Player p = e.getPlayer();
    	plugin.updateFood(p);
    }
    
    /***
     * Ensures when a player logs in, their food is updated.
     * @param e
     */
    @EventHandler
    public void onPlayerLogin (PlayerLoginEvent e){
    	Player p = e.getPlayer();
    	plugin.updateFood(p);
    }
    
    /***
     * Ensures when a player's gamemode is changed (to survival or adventure), their food is updated.
     * @param e
     */
    @EventHandler
    public void onGamemodeChange (PlayerGameModeChangeEvent e){
    	if(e.getNewGameMode().equals(GameMode.SURVIVAL) || e.getNewGameMode().equals(GameMode.ADVENTURE)){
    		plugin.updateFood(e.getPlayer());
    	}
    }
}
