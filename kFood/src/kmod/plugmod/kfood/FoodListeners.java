package kmod.plugmod.kfood;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;

/***
 * Class that houses all kFood listeners.
 * 
 * @author 7kasper
 */
public class FoodListeners implements Listener {
	private final kFood plugin;

	/**
	 * Instantiates a new FoodListeners class using our main class for all extra
	 * functions.
	 * 
	 * @param Plugin
	 */
	public FoodListeners(kFood Plugin) {
		this.plugin = Plugin;
		return;
	}
	
   //=====================================================\\
   //                    Giving Health                    \\
   //=====================================================\\

	/***
	 * When a player eats something, we want to check if the item he ate is in the
	 * list and apply health.
	 * 
	 * @param e
	 */
	@EventHandler(priority = EventPriority.HIGH)
	public void onItemConsume(PlayerItemConsumeEvent e) {
		Player p = e.getPlayer();
		if (!e.isCancelled()) {
			String eatName = plugin.getSafeFoodName(e.getItem());
			// Bukkit is a bit weird and doesn't apply health when it goes over the top.
			if (plugin.getFoodType(eatName) == FoodType.CONSUMABLE) {
				Double foodToAdd = plugin.getFoodWorth(eatName, FoodType.CONSUMABLE);
				plugin.addHealth(p, foodToAdd);
				plugin.debug(p.getName() + " ate " + eatName + ", which gave him " + foodToAdd + " halfhearts.");
			} else {
				plugin.debug(p.getName() + " ate " + eatName + " but that ain't a known food.");
			}
		}
	}

	/**
	 * We also want to handle instantFoods and cakes.
	 * 
	 * @param e
	 */
	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerInteract(PlayerInteractEvent e) {
		Player p = e.getPlayer();

		/*
		 * Handles the instant eating on right-click.
		 */
		if (e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
			if (e.getItem() != null) {
				String eatName = plugin.getSafeFoodName(e.getItem());
				if (plugin.getFoodType(eatName) == FoodType.INSTANT) {
					// We don't want other right click things to happen, so stop!
					e.setCancelled(true);
					// Takes one of the stack away, otherwise completely destroys the stack.
					if (e.getItem().getAmount() > 1) {
						if (plugin.returnsBowl(eatName))
							p.getInventory().addItem(plugin.bowl());
						e.getItem().setAmount(e.getItem().getAmount() - 1);
					} else {
						// When e.getHand() is set to EquipmentSlot.HAND it's in the main hand.
						if (e.getHand().equals(EquipmentSlot.HAND)) {
							if (plugin.returnsBowl(eatName))
								p.getInventory().setItemInMainHand(plugin.bowl());
							else
								p.getInventory().setItemInMainHand(null);
							plugin.debug(p.getName() + " instantly ate " + eatName + " from his main hand.");
						} else if (e.getHand().equals(EquipmentSlot.OFF_HAND)) {
							if (plugin.returnsBowl(eatName))
								p.getInventory().setItemInOffHand(plugin.bowl());
							else
								p.getInventory().setItemInOffHand(null);
							plugin.debug(p.getName() + " instantly ate " + eatName + " from his off hand.");
						}
					}
					Double foodToAdd = plugin.getFoodWorth(eatName, FoodType.INSTANT);
					plugin.addHealth(p, foodToAdd);
					// If set, plays the eating sound effect to all players within range (16 blocks)
					// and a little variation on the pitch (Hopefully just like you'd eat food
					// normally.
					if (plugin.burbEffect)
						p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_BURP, 1.0F, 1.0F);
					plugin.debug(p.getName() + " instantly ate " + eatName + ", wich gave him " + foodToAdd
							+ " halfhearts.");
				}
			}
		}

		/*
		 * The cake is a special case and needs to be checked separately.
		 */
		if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
			if (!e.useInteractedBlock().equals(Result.DENY)) {
				if (e.getClickedBlock().getType().equals(Material.CAKE)) {
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
	 * Forcefully disables the food event and makes sure that food levels stay the
	 * same.
	 * 
	 * @param e
	 */
	@EventHandler(priority = EventPriority.HIGH)
	public void foodChangeEvent(FoodLevelChangeEvent e) {
		if (e.getEntityType() == EntityType.PLAYER) {
			Player p = (Player) e.getEntity();
			// F*ck hunger, disable from the getgo.
			e.setCancelled(true);
			plugin.updateFoodLevel(p);
		}
	}

	/***
	 * Ensures when a player respawns, their food is updated.
	 * 
	 * @param e
	 */
	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent e) {
		Player p = e.getPlayer();
		plugin.debug(p.getName() + " respawned, resetting the food one tick later...");

		// This is kinda fired too early. For our changes to take effect, we want to
		// wait one tick.
		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			@Override
			public void run() {
				plugin.updateFoodLevel(p);
			}
		}, 1L);
	}

	/***
	 * Ensures when a player logs in, their food is updated.
	 * 
	 * @param e
	 */
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		plugin.debug(p.getName() + " logged on, resetting the food.");
		plugin.updateFoodLevel(p);
	}

	/***
	 * Ensures when a player's gamemode is changed (to survival or adventure), their
	 * food is updated.
	 * 
	 * @param e
	 */
	@EventHandler
	public void onGamemodeChange(PlayerGameModeChangeEvent e) {
		Player p = e.getPlayer();
		if (e.getNewGameMode().equals(GameMode.SURVIVAL) || e.getNewGameMode().equals(GameMode.ADVENTURE)) {
			plugin.debug(p.getName() + " changed into survival, resetting the food.");
			plugin.updateFoodLevel(p);
		}
	}

    //=====================================================\\
    //                    Swimming                         \\
    //=====================================================\\

	//TODO fix this sh*t when spigot gets good method for this.
	private List<Material> extraWaterlogged = Arrays.asList(Material.KELP, Material.KELP_PLANT, Material.SEAGRASS, Material.TALL_SEAGRASS);

	/***
	 * Ensures when a player moves the food level is updated.
	 * Also checks if when the overrideSwim option is true and
	 * the player is in a liquid that he is allowed some extra
	 * food when its low, so he can still properly swim.
	 * 
	 * @param e
	 */
	@EventHandler
	public void onMove(PlayerMoveEvent e) {
		Player p = e.getPlayer();
		if (plugin.overrideSwim && p.hasPermission(plugin.pPermissions.get(3)) && (p.getGameMode().equals(GameMode.SURVIVAL) || p.getGameMode().equals(GameMode.ADVENTURE))) {
			Block to = e.getTo().getBlock();
			if (to.isLiquid() || extraWaterlogged.contains(to.getType()) || (to.getBlockData() instanceof Waterlogged
					&& ((Waterlogged) to.getBlockData()).isWaterlogged())) {
				if (p.getFoodLevel() < 7) {
					p.setFoodLevel(7);
				}
				return;
			}
		}
		plugin.updateFoodLevel(p);
	}

}
