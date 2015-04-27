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

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class PlayerEventHandler implements Listener {
	
	// when a player successfully joins the server...
	@EventHandler(ignoreCancelled = true)
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		Player player = event.getPlayer();
		PlayerInventory inventory = player.getInventory();
		
		for(int i = 0; i < inventory.getSize(); i++)
		{
			ItemStack item = inventory.getItem(i);
			if(item == null) continue;
			
			if(ModDisable.instance.isBanned(ActionType.Ownership, player, item.getTypeId(), item.getData().getData(), player.getLocation()) != null)
			{
				inventory.setItem(i, new ItemStack(Material.AIR));
			}
		}
	}
	
	//when something is crafted (may not be a player crafting)
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	void onItemCrafted(CraftItemEvent event)
	{
		Player player = (Player)event.getWhoClicked();
		ItemStack item = event.getRecipe().getResult();
		
		MaterialInfo bannedInfo = ModDisable.instance.isBanned(ActionType.Ownership, player, item.getTypeId(), item.getData().getData(), player.getLocation());
		if(bannedInfo == null)
		{
			bannedInfo = ModDisable.instance.isBanned(ActionType.Crafting, player, item.getTypeId(), item.getData().getData(), player.getLocation());
		}
		if(bannedInfo != null)
		{
			event.setCancelled(true);
			player.sendMessage("Sorry, that item is disabled.  Reason: " + bannedInfo.reason);
		}
	}
	
	//when something is clicked in an inventory
	@EventHandler(priority = EventPriority.LOWEST)
	void onItemClicked(InventoryClickEvent event)
	{
		Player player = (Player)event.getWhoClicked();
		ItemStack item = event.getCurrentItem();
		if(item == null) return;
		
		MaterialInfo bannedInfo = ModDisable.instance.isBanned(ActionType.Ownership, player, item.getTypeId(), item.getData().getData(), player.getLocation());
		if(bannedInfo != null)
		{
			event.setCancelled(true);
			
			if(event.getInventory() instanceof PlayerInventory)
			{
				item.setType(Material.AIR);
				player.sendMessage("disabled item confiscated.  Reason: " + bannedInfo.reason);
			}
			else
			{
				player.sendMessage("Sorry, that item is disabled.  Reason: " + bannedInfo.reason);
			}
		}
	}
	
	//when a player interacts with the world
	@EventHandler(priority = EventPriority.LOWEST)
	void onPlayerInteract(PlayerInteractEvent event)
	{
		Player player = event.getPlayer();
		
		//ignore pressure plates for this
		if(event.getAction() == Action.PHYSICAL) return;

		MaterialInfo bannedInfo = ModDisable.instance.isBanned(ActionType.Ownership, player, player.getItemInHand().getTypeId(), player.getItemInHand().getData().getData(), player.getLocation());
		if(bannedInfo != null)
		{
			event.setCancelled(true);
			player.getInventory().setItemInHand(new ItemStack(Material.AIR));
			ModDisable.AddLogEntry("Confiscated " + bannedInfo.toString() + " from " + player.getName() + ".");
			player.sendMessage("disabled item confiscated.  Reason: " + bannedInfo.reason);
			return;
		}
		
		bannedInfo = ModDisable.instance.isBanned(ActionType.Usage, player, player.getItemInHand().getTypeId(), player.getItemInHand().getData().getData(), player.getLocation());
		if(bannedInfo != null)
		{
			event.setCancelled(true);						
			player.sendMessage("Sorry, usage of that item has been disabled.  Reason: " + bannedInfo.reason);
		}
		
		if(event.getAction() == Action.RIGHT_CLICK_BLOCK)
		{
			Block block = event.getClickedBlock();
			
			bannedInfo = ModDisable.instance.isBanned(ActionType.Usage, player, block.getTypeId(), block.getData(), block.getLocation());
			if(bannedInfo != null)
			{
				event.setCancelled(true);						
				player.sendMessage("Sorry, usage of that item has been disabled.  Reason: " + bannedInfo.reason);
			}
			else
			{
				bannedInfo = ModDisable.instance.isBanned(ActionType.Ownership, player, block.getTypeId(), block.getData(), block.getLocation());
				if(bannedInfo != null)
				{
					event.setCancelled(true);
					player.sendMessage("Sorry, usage of that item has been disabled.  Reason: " + bannedInfo.reason);
				}
			}
		}
	}
	
	//when a player interacts with an entity
	@EventHandler(priority = EventPriority.LOWEST)
	void onPlayerInteractEntity(PlayerInteractEntityEvent event)
	{
		Player player = event.getPlayer();

		MaterialInfo usageBannedInfo = ModDisable.instance.isBanned(ActionType.Usage, player, player.getItemInHand().getTypeId(), player.getItemInHand().getData().getData(), player.getLocation());
		if(usageBannedInfo != null)
		{
			event.setCancelled(true);
			
			MaterialInfo bannedInfo = ModDisable.instance.isBanned(ActionType.Ownership, player, player.getItemInHand().getTypeId(), player.getItemInHand().getData().getData(), player.getLocation());
			if(bannedInfo != null)
			{
				player.sendMessage("disabled item confiscated.  Reason: " + bannedInfo.reason);
				ModDisable.AddLogEntry("Confiscated " + bannedInfo.toString() + " from " + player.getName() + ".");
				player.getInventory().setItemInHand(new ItemStack(Material.AIR));
			}
			else
			{
				player.sendMessage("Sorry, usage of that item has been disabled.  Reason: " + usageBannedInfo.reason);				
			}			
		}
	}
	
	//when a player picks up an item
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	void onPlayerPickupItem(PlayerPickupItemEvent event)
	{
		Player player = event.getPlayer();
		ItemStack item = event.getItem().getItemStack();

		if(ModDisable.instance.isBanned(ActionType.Ownership, player, item.getTypeId(), item.getData().getData(), player.getLocation()) != null)
		{
			event.setCancelled(true);						
		} 
	}
	
	//when a player switches the item he's holding in hand...
	@EventHandler(priority = EventPriority.LOWEST)
	void onPlayerSwitchInHand(PlayerItemHeldEvent event)
	{
		//plan to check if the new item in hand is banned, if it's still equipped after half a second
		//(20L = 1 second)
		InHandContrabandScanTask task = new InHandContrabandScanTask(event.getPlayer(), event.getNewSlot());
		ModDisable.instance.getServer().getScheduler().scheduleSyncDelayedTask(ModDisable.instance, task, 10L);
	}
}
