<h1 align="center"><img src="https://raw.githubusercontent.com/7kasper/kFood/master/kFood/kFoodBanner.png" alt="kFood"/></h1>
kFood is a standalone plugmod (plugin) that abolishes minecraft's new food system and replaces it with a customizable system that resembles the way food used to work.

## Why?
I made this plugin to get myself familliar with bukkit again, it has been a loooonng while since I coded with it.
Oh and by the way, I just really didn't like the food bar :)

## Features
> - [X] Ensure players' foodLevels stay static.
> - [X] Give individual players different foodLevels.
> - [X] Make eating food increase health instead of the foodLevel.
> - [X] Set the halfHeartsToAdd for each food.
> - [X] Configure some effects when eated.
> - [X] Configure new (instant) foods.
> - [X] Set which foods can be eaten instantly.
> - [X] Set which instantFoods should return a bowl.
> 
> #### itemNames
> There are many plugins that reintroduce minecraft's old way of handling food.
> Most of them look to the material of the item to know how much food to apply; here kFood differs.
> 
> kFood's unique feature is to look at item names to identify which food it is.
> This has a few upsides:
> * More customization!
> * (better) support for items from other plugins.
> * (better) support for mods.
> * The ability to give different kinds of fish different amounts of halfHeartsToHeal.
> 
> To get every item's name kFood has to rely on NMS code. It uses reflection for this so it shouldn't break on a update.
> In fact, when a new food is added to the game you can add it to the config, no coding required. <br>
> The downside: if any major changes happen to bukkit's CraftItemStack class or NMS' ItemStack class this plugin will break and need updating.
> 
> #### anvilItems
> The previous feature raises some questions regarding anvils.
> What happens when a player takes a piece of Rotten Flash and renames it to Golden Carrot?
>
> On default if an anvilItem is eaten kFood will first check if an anvil name is specifically set.
> If not kFood will (for now) instead check against the original item's name.
> 
> #### foodLevels
> Even though this plugin renders the foodbar practically useless, the effects of it do still remain.
> This allows for some interesting features. If you set your foodLevel lower than 3 for instance, players cannot run anymore.
> The way the client works, it won't even try to call the PlayerToggleSprintEvent.
> If a player has a foodLevel higher than 17 it will slowly regenerate hearts.
>
> kFood stores the foodLevel of all players in the main scoreboard under the "_food_" objective.
> Having a score_food of 0 (default) means that you have the default-food.
> This way you can use commandblocks or other plugins to give individual players different foodlevels, for instance to make a certain area where players can or cannot run.
