package org.dyndns.pamelloes.ExtraFurnaces.block;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map.Entry;

import net.minecraft.server.EnumToolMaterial;
import net.minecraft.server.Item;
import net.minecraft.server.ItemTool;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.dyndns.pamelloes.ExtraFurnaces.ExtraFurnaces;
import org.dyndns.pamelloes.ExtraFurnaces.data.CustomFurnaceData;
import org.dyndns.pamelloes.ExtraFurnaces.gui.FurnaceGui;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.block.SpoutBlock;
import org.getspout.spoutapi.block.design.GenericCubeBlockDesign;
import org.getspout.spoutapi.block.design.Texture;
import org.getspout.spoutapi.event.screen.ButtonClickEvent;
import org.getspout.spoutapi.gui.Color;
import org.getspout.spoutapi.gui.GenericButton;
import org.getspout.spoutapi.gui.GenericGradient;
import org.getspout.spoutapi.gui.GenericLabel;
import org.getspout.spoutapi.gui.GenericPopup;
import org.getspout.spoutapi.gui.GenericTexture;
import org.getspout.spoutapi.gui.PopupScreen;
import org.getspout.spoutapi.gui.RenderPriority;
import org.getspout.spoutapi.material.CustomBlock;
import org.getspout.spoutapi.material.MaterialData;
import org.getspout.spoutapi.material.block.GenericCubeCustomBlock;
import org.getspout.spoutapi.player.SpoutPlayer;

public abstract class CustomFurnace extends GenericCubeCustomBlock implements Listener {
	protected ExtraFurnaces plugin;
	
	public CustomFurnace(ExtraFurnaces plugin, String name, Texture tex, int[] ids, boolean on) {
		super(plugin, (on ? "Burning " : "") + name, on ? Material.BURNING_FURNACE.getId() : Material.FURNACE.getId(), 2, new GenericCubeBlockDesign(plugin, tex, ids), true);
		this.plugin=plugin;
		setFriction(on ? MaterialData.burningfurnace.getFriction() : MaterialData.furnace.getFriction());
		setLightLevel(on ? MaterialData.burningfurnace.getLightLevel() : MaterialData.furnace.getLightLevel());
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	public abstract FurnaceGui getGui(SpoutPlayer player);

	@Override
    public boolean onBlockInteract(World world, int x, int y, int z, SpoutPlayer player) {
		if(!validateClient(player)) return true;
		CustomFurnaceData dat = (CustomFurnaceData) SpoutManager.getChunkDataManager().getBlockData("ExtraFurnaces", world, x, y, z);
		ExtraFurnaces.datamap.put(player, dat);
		FurnaceGui gui = getGui(player);
		ExtraFurnaces.guimap.put(player, gui);
		if(gui.makeGui()) {
			dat.onPlayerOpenFurnace(player);
		} else {
			ExtraFurnaces.guimap.remove(player);
			ExtraFurnaces.datamap.remove(player);
		}
		return true;
    }

	@Override
    public void onBlockDestroyed(World world, int x, int y, int z) {
		((CraftWorld) world).getHandle().q(x, y, z);
		CustomFurnaceData dat = (CustomFurnaceData) SpoutManager.getChunkDataManager().getBlockData("ExtraFurnaces", world, x, y, z);
		if(dat==null)  return;
		if(dat.replace) return;
		Iterator<Entry<SpoutPlayer,CustomFurnaceData>> iter = ExtraFurnaces.datamap.entrySet().iterator();
		while(iter.hasNext()) {
			Entry<SpoutPlayer,CustomFurnaceData> entry = iter.next();
			if(entry.getValue().equals(dat)) {
				entry.getKey().getMainScreen().closePopup();
				ExtraFurnaces.guimap.remove(entry.getKey());
				iter.remove();
			}
		}
		SpoutManager.getChunkDataManager().removeBlockData("ExtraFurnaces", world, x, y, z);
		Location loc = new Location(world,x,y,z);
		for(int i = 0; i < dat.getSize(); i++) if(dat.getStackInSlot(i)!=null) world.dropItem(loc, dat.getStackInSlot(i));
        HandlerList.unregisterAll(dat);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.isCancelled()) {
			return;
		}
		
		SpoutBlock block = (SpoutBlock) event.getBlock();

		SpoutPlayer player = (SpoutPlayer)event.getPlayer();

		CustomBlock material = block.getCustomBlock();
		if (material != null && material instanceof CustomFurnace) {
			if (player.getGameMode() == GameMode.SURVIVAL && usingToolLevel(player, 2)) {
				block.getWorld().dropItem(block.getLocation(), material.getItemDrop());
			}
			block.setTypeId(0);
			SpoutManager.getMaterialManager().removeBlockOverride(block);
			event.setCancelled(true);
		}
	}

	
	public boolean usingToolLevel(LivingEntity living, int level) {
		if(!(living instanceof HumanEntity)) return false;
		CraftItemStack is =  (CraftItemStack) ((HumanEntity) living).getItemInHand();
		if(is == null) return false;
		Item item = is.getHandle().getItem();
		if(!(item instanceof ItemTool)) return false;
		ItemTool it = (ItemTool) item;
		EnumToolMaterial tm = null;
		try {
			Field f = ItemTool.class.getDeclaredField("b");
			f.setAccessible(true);
			tm = (EnumToolMaterial) f.get(it);
		} catch (Exception e) { e.printStackTrace(); }
		if(tm == null) return false;
		return tm.d() >= level;
	}
	
	private boolean validateClient(SpoutPlayer player) {
		if(!player.isSpoutCraftEnabled()) {
			player.sendMessage(ChatColor.RED + "");
			player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + " This is a special furnace and requires Spoutcraft");
			player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + " for you to use it. You can get Spoutcraft at ");
			player.sendMessage(ChatColor.BLUE + "" + ChatColor.BOLD + " http://get.spout.org/index.php");
			player.sendMessage(ChatColor.RED + "");
			player.sendMessage(ChatColor.RED + "" + ChatColor.ITALIC + " If you are using Spoutcraft, please inform your server admin");
			player.sendMessage(ChatColor.RED + "" + ChatColor.ITALIC + " that they need to have " + ChatColor.GRAY + "\"AuthenticateSpoutcraft: true\"" + ChatColor.RED + "" + ChatColor.ITALIC + " in the");
			player.sendMessage(ChatColor.RED + "" + ChatColor.ITALIC + " server's Spout configuration.");
			player.sendMessage(ChatColor.RED + "");
			return false;
		}
		int build = -1;
		try {
			build = Integer.parseInt(player.getVersionString());
		} catch(NumberFormatException e) {}
		if(build < ExtraFurnaces.MINIMUM_SPOUTCRAFT_VERSION && build >= 0) {
			// Window Title
			GenericLabel wintitle = new GenericLabel("Spoutcraft outdated!");
			wintitle.setX(158).setY(25);
			wintitle.setPriority(RenderPriority.Lowest);
			wintitle.setWidth(-1).setHeight(-1);
			
			// Content Title
			GenericLabel contitle = new GenericLabel("You need to have Spoutcraft " + ExtraFurnaces.MINIMUM_SPOUTCRAFT_VERSION + " or newer!");
			contitle.setX(105).setY(55);
			contitle.setPriority(RenderPriority.Lowest);
			contitle.setWidth(-1).setHeight(-1);
			contitle.setTextColor(new Color(1.0f,0.3f,0.3f));
			
			// Content
			GenericLabel content1 = new GenericLabel("The ExtraFurnaces addon (which allows you to");
			content1.setX(91).setY(80);
			content1.setPriority(RenderPriority.Lowest);
			content1.setWidth(-1).setHeight(-1);
			GenericLabel content2 = new GenericLabel("use the furnace) requires that you are using");
			content2.setX(91).setY(93);
			content2.setPriority(RenderPriority.Lowest);
			content2.setWidth(-1).setHeight(-1);
			GenericLabel content3 = new GenericLabel(" Spoutcraft " + ExtraFurnaces.MINIMUM_SPOUTCRAFT_VERSION + " or newer. If you are using an");
			content3.setX(91).setY(106);
			content3.setPriority(RenderPriority.Lowest);
			content3.setWidth(-1).setHeight(-1);
			GenericLabel content4 = new GenericLabel("older build then your client will be missing");
			content4.setX(91).setY(119);
			content4.setPriority(RenderPriority.Lowest);
			content4.setWidth(-1).setHeight(-1);
			GenericLabel content5 = new GenericLabel("important features that will render the furnace unusable.");
			content5.setX(91).setY(132);
			content5.setPriority(RenderPriority.Lowest);
			content5.setWidth(-1).setHeight(-1);
			GenericLabel content6 = new GenericLabel("You can update Spoutcraft in the \"Options\"");
			content6.setX(91).setY(158);
			content6.setPriority(RenderPriority.Lowest);
			content6.setWidth(-1).setHeight(-1);
			content6.setTextColor(new Color(1.0f,0.3f,0.3f));
			GenericLabel content7 = new GenericLabel("section of the launcher!");
			content7.setX(91).setY(171);
			content7.setPriority(RenderPriority.Lowest);
			content7.setWidth(-1).setHeight(-1);
			content7.setTextColor(new Color(1.0f,0.3f,0.3f));

			// Border
			GenericTexture border = new GenericTexture("http://dl.dropbox.com/u/27507830/GuildCraft/Images/HUD/blue.png");
			border.setX(65).setY(20);
			border.setPriority(RenderPriority.High);
			border.setWidth(300).setHeight(200);
			border.setDrawAlphaChannel(true);

			// Background gradient
			GenericGradient gradient = new GenericGradient();
			gradient.setTopColor(new Color(0.25F, 0.25F, 0.25F, 1.0F));
			gradient.setBottomColor(new Color(0.35F, 0.35F, 0.35F, 1.0F));
			gradient.setWidth(300).setHeight(200);
			gradient.setX(65).setY(20);
			gradient.setPriority(RenderPriority.Highest);

			// Close button
			GenericButton close = new GenericButton("Close") {
				@Override
				public void onButtonClick(ButtonClickEvent event) {
					event.getPlayer().getMainScreen().getActivePopup().close();
				}
			};
			close.setX(188).setY(195);
			close.setWidth(60).setHeight(20);
			close.setPriority(RenderPriority.Lowest);
			
			player.getMainScreen().attachPopupScreen((PopupScreen) new GenericPopup().attachWidgets(plugin,wintitle,contitle,content1,content2,content3,content4,content5,content6,content7,border,gradient,close));
			return false;
		}
		return true;
	}
}
