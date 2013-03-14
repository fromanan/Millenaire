package org.millenaire.client.network;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet250CustomPayload;

import org.millenaire.client.MillClientUtilities;
import org.millenaire.client.gui.DisplayActions;
import org.millenaire.common.Building;
import org.millenaire.common.Culture;
import org.millenaire.common.MLN;
import org.millenaire.common.MillVillager;
import org.millenaire.common.MillWorldInfo.MillMapInfo;
import org.millenaire.common.Point;
import org.millenaire.common.TileEntityMillChest;
import org.millenaire.common.TileEntityPanel;
import org.millenaire.common.UserProfile;
import org.millenaire.common.core.MillCommonUtilities;
import org.millenaire.common.forge.CommonGuiHandler;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.network.ServerReceiver;
import org.millenaire.common.network.StreamReadWrite;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.IPacketHandler;
import cpw.mods.fml.common.network.Player;

public class ClientReceiver implements IPacketHandler
{
	/**
	 * Only packets sent from a server to this specific client arrive here
	 */
	@Override
	public void onPacketData(INetworkManager manager, Packet250CustomPayload packet, Player player)
	{

		if (FMLCommonHandler.instance().getSide().isServer() && (MLN.Network>=MLN.MAJOR)) {
			MLN.major(this, "Received a packet despite being server.");
			return;
		}


		final DataInputStream data = new DataInputStream(new ByteArrayInputStream(packet.data));

		ClientSender.networkManager=manager;

		try {
			final int packettype=data.read();

			Mill.clientWorld.millenaireEnabled=true;

			if (MLN.Network>=MLN.DEBUG) {
				MLN.debug(this, "Received client packet type: "+packettype);
			}

			final UserProfile profile=Mill.proxy.getClientProfile();

			if (packettype==ServerReceiver.PACKET_BUILDING) {
				Building.readBuildingPacket(Mill.clientWorld,data);
			} else if (packettype==ServerReceiver.PACKET_VILLAGER) {
				MillVillager.readVillagerPacket(data);
			} else if (packettype==ServerReceiver.PACKET_TRANSLATED_CHAT) {
				readTranslatedChatPackage(data);
			} else if (packettype==ServerReceiver.PACKET_MILLCHEST) {
				TileEntityMillChest.readUpdatePacket(data, Mill.clientWorld.world);
			} else if (packettype==ServerReceiver.PACKET_PROFILE) {
				profile.receiveProfilePacket(data);
			} else if (packettype==ServerReceiver.PACKET_QUESTINSTANCE) {
				profile.receiveQuestInstancePacket(data);
			} else if (packettype==ServerReceiver.PACKET_QUESTINSTANCEDELETE) {
				profile.receiveQuestInstanceDeletePacket(data);
			} else if (packettype==ServerReceiver.PACKET_OPENGUI) {
				readGUIPacket(data);
			} else if (packettype==ServerReceiver.PACKET_MAPINFO) {
				MillMapInfo.readPacket(data);
			} else if (packettype==ServerReceiver.PACKET_PANELUPDATE) {
				TileEntityPanel.readPacket(data);
			} else if (packettype==ServerReceiver.PACKET_VILLAGELIST) {
				Mill.clientWorld.receiveVillageListPacket(data);
			} else if (packettype==ServerReceiver.PACKET_SERVER_CONTENT) {
				readServerContentPacket(data);
			} else if (packettype==ServerReceiver.PACKET_ANIMALBREED) {
				readAnimalBreedPacket(data);
			}

		} catch (final Exception e) {
			MLN.printException("Error in ClientReceiver.onPacketData:", e);
		}
	}

	private void readAnimalBreedPacket(DataInputStream data) {

		try {
			Point p=StreamReadWrite.readNullablePoint(data);
			int endId=data.readInt();
			
			final List<Entity> animals=MillCommonUtilities.getEntitiesWithinAABB(Mill.clientWorld.world, EntityAnimal.class, p, 5, 5);

			for (final Entity ent : animals) {
				
				EntityAnimal animal=(EntityAnimal)ent;
				
				if (animal.entityId==endId) {
					animal.inLove=600;
					MillCommonUtilities.generateHearts(animal);
				}
			}

		} catch (final IOException e) {
			MLN.printException(e);
		}
	}

	private void readGUIPacket(DataInputStream data) {

		try {
			final int guiId=data.read();

			if (guiId==CommonGuiHandler.GUI_QUEST) {
				final MillVillager v=Mill.clientWorld.villagers.get(data.readLong());

				if (v!=null) {
					DisplayActions.displayQuestGUI(Mill.proxy.getTheSinglePlayer(), v);
				} else {
					MLN.error(this, "Unknown villager id in readGUIPacket: "+guiId);
				}
			} else if (guiId==CommonGuiHandler.GUI_HIRE) {
				final MillVillager v=Mill.clientWorld.villagers.get(data.readLong());

				if (v!=null) {
					DisplayActions.displayHireGUI(Mill.proxy.getTheSinglePlayer(), v);
				} else {
					MLN.error(this, "Unknown villager id in readGUIPacket: "+guiId);
				}
			} else if (guiId==CommonGuiHandler.GUI_VILLAGECHIEF) {
				final MillVillager v=Mill.clientWorld.villagers.get(data.readLong());

				if (v!=null) {
					DisplayActions.displayVillageChiefGUI(Mill.proxy.getTheSinglePlayer(), v);
				} else {
					MLN.error(this, "Unknown villager id in readGUIPacket: "+guiId);
				}
			} else if (guiId==CommonGuiHandler.GUI_VILLAGEBOOK) {
				final Point p=StreamReadWrite.readNullablePoint(data);

				if (p!=null) {
					DisplayActions.displayVillageBookGUI(Mill.proxy.getTheSinglePlayer(), p);
				} else {
					MLN.error(this, "Unknown point in readGUIPacket: "+guiId);
				}
			} else if (guiId==CommonGuiHandler.GUI_NEGATIONWAND) {
				final Point p=StreamReadWrite.readNullablePoint(data);

				if (p!=null) {
					final Building building=Mill.clientWorld.getBuilding(p);
					if (building != null) {
						DisplayActions.displayNegationWandGUI(Mill.proxy.getTheSinglePlayer(), building);
					}
				} else {
					MLN.error(this, "Unknown point in readGUIPacket: "+guiId);
				}
			} else if (guiId==CommonGuiHandler.GUI_NEWBUILDING) {
				final Point thPos=StreamReadWrite.readNullablePoint(data);
				final Point pos=StreamReadWrite.readNullablePoint(data);

				if ((thPos!=null) && (pos!=null)) {
					final Building building=Mill.clientWorld.getBuilding(thPos);
					if (building != null) {
						DisplayActions.displayNewBuildingProjectGUI(Mill.proxy.getTheSinglePlayer(), building, pos);
					}
				} else {
					MLN.error(this, "Unknown point in readGUIPacket: "+guiId);
				}
			} else if (guiId==CommonGuiHandler.GUI_NEWVILLAGE) {
				final Point pos=StreamReadWrite.readNullablePoint(data);

				if (pos!=null) {
					DisplayActions.displayNewVillageGUI(Mill.proxy.getTheSinglePlayer(), pos);
				} else {
					MLN.error(this, "Unknown point in readGUIPacket: "+guiId);
				}
			} else if (guiId==CommonGuiHandler.GUI_CONTROLLEDPROJECTPANEL) {
				final Point thPos=StreamReadWrite.readNullablePoint(data);

				if (thPos!=null) {
					final Building building=Mill.clientWorld.getBuilding(thPos);
					if (building != null) {
						DisplayActions.displayControlledProjectGUI(Mill.proxy.getTheSinglePlayer(), building);
					}
				} else {
					MLN.error(this, "Unknown point in readGUIPacket: "+guiId);
				}
			} else if (guiId==CommonGuiHandler.GUI_PANEL) {
				final Point p=StreamReadWrite.readNullablePoint(data);

				if (p!=null) {
					MillClientUtilities.displayPanel(Mill.clientWorld.world, Mill.proxy.getTheSinglePlayer(),p);
				} else {
					MLN.error(this, "Unknown point in readGUIPacket: "+guiId);
				}
			} else if (guiId==CommonGuiHandler.GUI_TRADE) {
				final Point p=StreamReadWrite.readNullablePoint(data);

				if (p!=null) {
					Mill.proxy.getTheSinglePlayer().openGui(Mill.instance, CommonGuiHandler.GUI_TRADE, Mill.clientWorld.world,p.getiX(),p.getiY(),p.getiZ());
				} else {
					MLN.error(this, "Unknown point in readGUIPacket: "+guiId);
				}
			} else if (guiId==CommonGuiHandler.GUI_MERCHANT) {
				final int id1=data.readInt();
				final int id2=data.readInt();
				Mill.proxy.getTheSinglePlayer().openGui(Mill.instance, CommonGuiHandler.GUI_MERCHANT,Mill.clientWorld.world,id1,id2,0);
			} else if (guiId==CommonGuiHandler.GUI_MILL_CHEST) {
				final Point p=StreamReadWrite.readNullablePoint(data);

				if (p!=null) {
					final TileEntityMillChest chest=p.getMillChest(Mill.clientWorld.world);
					if ((chest!=null) && chest.loaded) {
						Mill.proxy.getTheSinglePlayer().openGui(Mill.instance, CommonGuiHandler.GUI_MILL_CHEST, Mill.clientWorld.world,p.getiX(),p.getiY(),p.getiZ());
					}
				} else {
					MLN.error(this, "Unknown point in readGUIPacket: "+guiId);
				}
			} else {
				MLN.error(null, "Unknown GUI: "+guiId);
			}

		} catch (final IOException e) {
			MLN.printException(e);
		}
	}


	private void readServerContentPacket(DataInputStream data) {
		int nbCultures;
		try {
			nbCultures = data.readShort();

			for (int i=0;i<nbCultures;i++) {
				Culture.readCultureMissingContentPacket(data);
			}

			Culture.refreshVectors();

		} catch (final IOException e) {
			MLN.printException("Error in readServerContentPacket:", e);
		}
	}

	private void readTranslatedChatPackage(DataInputStream data) {
		try {

			final char colour=data.readChar();

			String s=data.readUTF();

			final String[] values=new String[data.read()];

			for (int i=0;i<values.length;i++) {
				values[i]=MLN.unknownString(StreamReadWrite.readNullableString(data));
			}

			s=MLN.string(s,values);

			Mill.proxy.sendLocalChat(Mill.proxy.getTheSinglePlayer(),colour, s);
		} catch (final IOException e) {
			MLN.printException(e);
		}
	}


}