package org.dyndns.pamelloes.ExtraFurnaces.data;

import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.dyndns.pamelloes.ExtraFurnaces.ExtraFurnaces;
import org.getspout.spoutapi.event.spout.ServerTickEvent;
import org.getspout.spoutapi.material.CustomBlock;

public class GoldFurnaceData extends CustomFurnaceData {
	private static final long serialVersionUID = 6470491086115887831L;

	public GoldFurnaceData(UUID world, int x, int y, int z) {
		super(13, world, x, y, z);
	}

	@Override
	protected int getFuelIndex() {
		return 7;
	}

	@Override
	protected int getBurnIndex() {
		return 3;
	}

	@Override
	protected int getResultIndex() {
		return 8;
	}
	
	@Override
	protected int getItemSmeltTime() {
		return 80;
	}

	@Override
	@EventHandler
	public void onServerTick(ServerTickEvent e) {
		boolean update = false;
		if(update()) {
			updateRange(getFuelIndex(), getFuelIndex() - 3);
			updateRange(getBurnIndex(), getBurnIndex() - 3);
			update = true;
		}
		if (adjustResult(5)) update = true;
		if (update) updateInventory();
		updateData();
	}

	@Override
	protected CustomBlock getBlock(boolean burning) {
		return burning ? ExtraFurnaces.goldfurnaceon : ExtraFurnaces.goldfurnaceoff;
	}

	public String getName() {
		return "Gold Furnace";
	}
}
