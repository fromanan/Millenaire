package org.millenaire.common.block;

import java.util.Random;

import net.minecraft.block.BlockSign;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import org.millenaire.common.Building;
import org.millenaire.common.Point;
import org.millenaire.common.TileEntityPanel;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.network.ServerSender;


public class BlockPanel extends BlockSign {

	@SuppressWarnings("rawtypes")
	public BlockPanel(int i, Class class1, boolean flag) {
		super(i, class1, flag);
	}

	@Override
	public boolean onBlockActivated(World world, int i, int j, int k, EntityPlayer entityplayer, int par6, float par7, float par8, float par9) {


		if (world.isRemote)
			return true;

		final TileEntityPanel panel=(TileEntityPanel) world.getBlockTileEntity(i, j, k);

		if ((panel==null) || (panel.panelType==0))
			return false;

		final Building building=Mill.getMillWorld(world).getBuilding(panel.buildingPos);

		if (building==null)
			return false;

		if ((panel.panelType==TileEntityPanel.controlledProjects) && building.controlledBy(entityplayer.username)) {
			ServerSender.displayControlledProjectGUI(entityplayer,building);
			return true;
		}

		ServerSender.displayPanel(entityplayer, new Point(i,j,k));

		return true;
	}

	@Override
	public int quantityDropped(Random random)
	{
		return 0;
	}

}