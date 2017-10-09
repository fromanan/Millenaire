package org.millenaire.items;

import org.millenaire.PlayerTracker;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemSeeds;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public class ItemMillSeeds extends ItemSeeds {

	public ItemMillSeeds(Block crops) {
		super(crops, Blocks.farmland);

	}

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer playerIn, World worldIn, BlockPos pos, EnumFacing side,
			float hitX, float hitY, float hitZ) {
		if (!worldIn.isRemote) {
			if (PlayerTracker.get(playerIn).canPlayerUseCrop(stack.getItem()))
				return super.onItemUse(stack, playerIn, worldIn, pos, side, hitX, hitY, hitZ);
			else
				return false;
		} else
			return false;
	}
}
