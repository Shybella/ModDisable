/*
    TekkitCustomizer Server Plugin for Minecraft
    Copyright (C) 2012 Ryan Hamshire

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.shybella;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.*;
import java.util.logging.*;


public class ModDisable extends JavaPlugin
{
	//for convenience, a reference to the instance of this plugin
	public static ModDisable instance;
	
	//for logging to the console and log file
	private static final Logger log;

	static {
		log = Logger.getLogger("ModDisable");
		log.setParent(Logger.getLogger("Minecraft"));
	}
		
	//where configuration data is kept
	private final static String dataLayerFolderPath = "plugins" + File.separator + "ModDisable";
	public final static String configFilePath = dataLayerFolderPath + File.separator + "config.yml";
	
	//user configuration, loaded/saved from a config.yml
	ArrayList<String> config_enforcementWorlds = new ArrayList<String>();
	MaterialCollection config_usageBanned = new MaterialCollection();
	MaterialCollection config_ownershipBanned = new MaterialCollection();
	MaterialCollection config_placementBanned = new MaterialCollection();
	MaterialCollection config_worldBanned = new MaterialCollection();
	MaterialCollection config_craftingBanned = new MaterialCollection();
	MaterialCollection config_recipesBanned = new MaterialCollection();

	boolean config_protectSurfaceFromExplosions;
	boolean config_removeUUMatterToNonRenewableRecipes;
	
	public static void AddLogEntry(String entry)
	{
		log.info(entry);
	}
	
	//initializes well...   everything
	public void onEnable()
	{ 		
		AddLogEntry("ModDisable enabled.");		
		
		instance = this;
		
		//register for events
		PluginManager pluginManager = this.getServer().getPluginManager();
		
		//player events
		PlayerEventHandler playerEventHandler = new PlayerEventHandler();
		pluginManager.registerEvents(playerEventHandler, this);
				
		//block events
		BlockEventHandler blockEventHandler = new BlockEventHandler();
		pluginManager.registerEvents(blockEventHandler, this);
		
		//entity events
		EntityEventHandler entityEventHandler = new EntityEventHandler();
		pluginManager.registerEvents(entityEventHandler, this);
		
		this.loadConfiguration();
		
		//start the repeating scan for banned items in player inventories and in the world
		//runs every minute and scans: 5% online players, 5% of loaded chunks
		Server server = this.getServer();
		ContrabandScannerTask task = new ContrabandScannerTask();
		server.getScheduler().scheduleSyncRepeatingTask(this, task, 20L * 60, 20L * 60);		
	}
	
	private void loadConfiguration()
	{
		//load the config if it exists
		FileConfiguration config = YamlConfiguration.loadConfiguration(new File(configFilePath));
		
		//read configuration settings
		
		//explosion protection for world surface
		this.config_protectSurfaceFromExplosions = config.getBoolean("ModDisable.ProtectSurfaceFromExplosives", true);
		config.set("ModDisable.ProtectSurfaceFromExplosives", this.config_protectSurfaceFromExplosions);
		
		//whether or not players can use UU matter to produce ore
		//this.config_removeUUMatterToNonRenewableRecipes = config.getBoolean("ModDisable.RemoveUUMatterToNonRenewableItemRecipes", true);
		//config.set("ModDisable.RemoveUUMatterToNonRenewableItemRecipes", this.config_removeUUMatterToNonRenewableRecipes);
		
		/*if(this.config_removeUUMatterToNonRenewableRecipes)
		{
			Server server = this.getServer();
			Iterator<Recipe> iterator = server.recipeIterator();
			while(iterator.hasNext())
			{
				Recipe recipe = iterator.next();
				if(recipe instanceof ShapedRecipe)
				{
					ShapedRecipe shapedRecipe = (ShapedRecipe)recipe;
					
					Map<Character, ItemStack> ingredients = shapedRecipe.getIngredientMap();
					for (ItemStack ingredient : ingredients.values()) {
						if (ingredient != null && ingredient.getTypeId() == 30188) //uu-matter ID
						{
							ItemStack result = shapedRecipe.getResult();
							if (result.getType() == Material.DIAMOND ||
									result.getType() == Material.COAL ||
									result.getType() == Material.IRON_ORE ||
									result.getType() == Material.GOLD_ORE ||
									result.getType() == Material.REDSTONE_ORE ||
									result.getType() == Material.OBSIDIAN ||
									result.getType() == Material.MYCEL ||
									result.getType() == Material.GRASS ||
									result.getType() == Material.REDSTONE ||
									result.getType() == Material.SULPHUR ||        //gun powder, from creepers
									result.getTypeId() == 140 ||                    //various tekkit ores
									result.getTypeId() == 30217)                    //sticky resin
							{
								iterator.remove();
								break;
							}
						}
					}
				}
			}
		}*/
		
		//default for worlds list
		ArrayList<String> defaultWorldNames = new ArrayList<String>();
		List<World> worlds = this.getServer().getWorlds();
		for (World world1 : worlds) {
			defaultWorldNames.add(world1.getName());
		}
		
		//get world names from the config file
		List<String> worldNames = config.getStringList("ModDisable.EnforcementWorlds");
		if(worldNames == null || worldNames.size() == 0)
		{			
			worldNames = defaultWorldNames;
		}
		
		//validate that list
		this.config_enforcementWorlds = new ArrayList<String>();
		for (String worldName : worldNames) {
			World world = this.getServer().getWorld(worldName);
			if (world == null) {
				AddLogEntry("Error: There's no world named \"" + worldName + "\".  Please update your config.yml.");
			} else {
				this.config_enforcementWorlds.add(world.getName());
			}
		}
		
		config.set("ModDisable.EnforcementWorlds", worldNames);
		
		//
		//USAGE BANS - players can have these, but they can't use their right-click ability
		//
		List<String> dontUseStrings = config.getStringList("ModDisable.Bans.UsageBanned");
		
		//default values
		if(dontUseStrings == null || dontUseStrings.size() == 0)
		{
			
		}
		
		//parse the strings from the config file
		this.parseMaterialListFromConfig(dontUseStrings, this.config_usageBanned);
		
		//write it back to the config entry for later saving
		config.set("ModDisable.Bans.UsageBanned", dontUseStrings);
		
		//
		//OWNERSHIP BANS - players can't have these at all (crafting is also blocked in this case)
		//
		List<String> dontOwnStrings = config.getStringList("ModDisable.Bans.OwnershipBanned");
		
		//default values
		if(dontOwnStrings == null || dontOwnStrings.size() == 0)
		{
			//these can potentially cause massive devastation, and/or totally ignore bukkit plugins (bypass anti grief and change logging)
			//also, the weapons and many rings/armors can injure players even when pvp is turned off
			dontOwnStrings.add(new MaterialInfo(99999, "Item name", "The reason for the item being disabled.").toString());
		}
		
		//parse the strings from the config file
		this.parseMaterialListFromConfig(dontOwnStrings, this.config_ownershipBanned);
		
		//write it back to the config entry for later saving
		config.set("ModDisable.Bans.OwnershipBanned", dontOwnStrings);
		
		//
		//PLACEMENT BANS - players can't place these in the world (crafting is also blocked in this case)
		//
		List<String> dontPlaceStrings = config.getStringList("ModDisable.Bans.PlacementBanned");
		
		//default values
		if(dontPlaceStrings == null || dontPlaceStrings.size() == 0)
		{
			
		}
		
		//parse the strings from the config file
		this.parseMaterialListFromConfig(dontPlaceStrings, this.config_placementBanned);
		
		//write it back to the config entry for later saving
		config.set("ModDisable.Bans.PlacementBanned", dontPlaceStrings);
		
		//
		//WORLD BANS - these aren't allowed anywhere in the enforcement worlds (see config variable) for any reason, and will be automatically removed
		//
		List<String> removeInWorldStrings = config.getStringList("ModDisable.Bans.WorldBanned");
		
		//default values
		if(removeInWorldStrings == null || removeInWorldStrings.size() == 0)
		{
			
		}
		
		//parse the strings from the config file
		this.parseMaterialListFromConfig(removeInWorldStrings, this.config_worldBanned);
		
		//write it back to the config entry for later saving
		config.set("ModDisable.Bans.WorldBanned", removeInWorldStrings);
		
		//
		//CRAFTING BANS - players aren't allowed to craft these items (only collect them from the world, or get through other means like admin gifts)
		//
		List<String> dontCraftStrings = config.getStringList("ModDisable.Bans.CraftingBanned");
		
		//default values
		if(dontCraftStrings == null || dontCraftStrings.size() == 0)
		{
			
		}
		
		//parse the strings from the config file
		this.parseMaterialListFromConfig(dontCraftStrings, this.config_craftingBanned);
		
		//write it back to the config entry for later saving
		config.set("ModDisable.Bans.CraftingBanned", dontCraftStrings);		
		
		//write all config data back to the config file
		try
		{
			config.save(configFilePath);
		}
		catch(IOException exception)
		{
			AddLogEntry("Unable to write to the configuration file at \"" + configFilePath + "\"");
		}
	}

	private void parseMaterialListFromConfig(List<String> stringsToParse, MaterialCollection materialCollection)
	{
		materialCollection.clear();
		
		//for each string in the list
		for(int i = 0; i < stringsToParse.size(); i++)
		{
			//try to parse the string value into a material info
			MaterialInfo materialInfo = MaterialInfo.fromString(stringsToParse.get(i));
			
			//null value returned indicates an error parsing the string from the config file
			if(materialInfo == null)
			{
				//show error in log
				ModDisable.AddLogEntry("ERROR: Unable to read a material entry from the config file.  Please update your config.yml.");
				
				//update string, which will go out to config file to help user find the error entry
				if(!stringsToParse.get(i).contains("can't"))
				{
					stringsToParse.set(i, stringsToParse.get(i) + "     <-- can't understand this entry");
				}
			}
			
			//otherwise store the valid entry in config data
			else
			{
				materialCollection.Add(materialInfo);
			}
		}		
	}

	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
	{
		Player player = null;
		if (sender instanceof Player)
		{
			player = (Player) sender;
		}
		
		if(cmd.getName().equalsIgnoreCase("blockinfo") && player != null)
		{			
			boolean messageSent = false;
			
			//info about in-hand block, if any
			MaterialInfo inHand;
			ItemStack handStack = player.getItemInHand();
			if(handStack.getType() != Material.AIR)
			{
				inHand = new MaterialInfo(handStack.getTypeId(), handStack.getData().getData(), null, null);
				player.sendMessage("In Hand: " + inHand.toString());
				messageSent = true;
			}
			
			HashSet<Byte> transparentMaterials = new HashSet<Byte>();
			transparentMaterials.add((byte)Material.AIR.getId());
			Block targetBlock = player.getTargetBlock(transparentMaterials, 50);
			if(targetBlock != null && targetBlock.getType() != Material.AIR)
			{
				player.sendMessage("Targeted: " + new MaterialInfo(targetBlock.getTypeId(), targetBlock.getData(), null, null).toString());
				messageSent = true;
			}
			
			if(!messageSent)
			{
				player.sendMessage("To get information about a material, either hold it in your hand or move close and point at it with your crosshair.");
			}
			
			return true;
		}
		
		if(cmd.getName().equalsIgnoreCase("ModDisablereload"))
		{			
			this.loadConfiguration();
			
			if(player != null)
			{
				player.sendMessage("Disabled item configuration reloaded.");
			}
			else
			{
				ModDisable.AddLogEntry("Disabled item configuration reloaded.");
			}
			
			return true;
		}

		return false;
	}
	
	
	
	public void onDisable()
	{
		AddLogEntry("ModDisable disabled.");
	}

	public MaterialInfo isBanned(ActionType actionType, Player player, int typeId, byte data, Location location) 
	{
		if(!this.config_enforcementWorlds.contains(location.getWorld().getName())) return null;
		
		if(player.hasPermission("ModDisable.*")) return null;

		MaterialCollection collectionToSearch;
		String permissionNode;
		if(actionType == ActionType.Usage)
		{
			collectionToSearch = this.config_usageBanned;
			permissionNode = "use";
		}
		else if(actionType == ActionType.Placement)
		{
			collectionToSearch = this.config_placementBanned;
			permissionNode = "place";
		}
		else if(actionType == ActionType.Crafting)
		{
			collectionToSearch = this.config_craftingBanned;
			permissionNode = "craft";
		}
		else
		{
			collectionToSearch = this.config_ownershipBanned;
			permissionNode = "own";
		}
		
		//if the item is banned, check permissions
		MaterialInfo bannedInfo = collectionToSearch.Contains(new MaterialInfo(typeId, data, null, null));
		if(bannedInfo != null)
		{
			if(player.hasPermission("ModDisable." + typeId + ".*.*")) return null;
			if(player.hasPermission("ModDisable." + typeId + ".*." + permissionNode)) return null;
			if(player.hasPermission("ModDisable." + typeId + "." + data + "." + permissionNode)) return null;			
			if(player.hasPermission("ModDisable." + typeId + "." + data + ".*")) return null;
			
			return bannedInfo;
		}
				
		return null;
	}
	
	public static String getFriendlyLocationString(Location location) 
	{
		return location.getWorld().getName() + "(" + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ() + ")";
	}
}
