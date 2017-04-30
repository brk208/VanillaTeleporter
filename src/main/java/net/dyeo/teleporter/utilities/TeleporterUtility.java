package net.dyeo.teleporter.utilities;

import com.google.common.base.Throwables;

import net.dyeo.teleporter.capabilities.CapabilityTeleporterEntity;
import net.dyeo.teleporter.capabilities.ITeleporterEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;

public class TeleporterUtility
{

	// transfers the entity to the selected location in the selected dimension
	public static boolean transferToDimensionLocation(Entity srcEntity, int dstDimension, double x, double y, double z,
			float yaw, float pitch)
	{
		if (srcEntity != null)
		{
			// if the dimensions are the same, we can fall back to the transfer to location teleport
			if (srcEntity.dimension == dstDimension)
			{
				return TeleporterUtility.transferToLocation(srcEntity, x, y, z, yaw, pitch);
			}
			else
			{
				if (srcEntity instanceof EntityPlayerMP)
				{
					System.out.println("EntityPlayerMp");
					return transferPlayerToDimension((EntityPlayerMP) srcEntity, dstDimension, x, y, z, yaw, pitch);
				}
				else if (srcEntity instanceof EntityLivingBase)
				{
					System.out.println("EntityLivingBase");
					return TeleporterUtility._transferEntityToDimension(srcEntity, dstDimension, x, y, z, yaw, pitch);
				}
			}
		}
		System.out.println("Non Entity");
		return false;
	}

	// transfers entity to a location in the same dimension
	public static boolean transferToLocation(Entity srcEntity, double x, double y, double z, float yaw, float pitch)
	{
		try
		{
			srcEntity.setPositionAndUpdate(x, y, z);
			srcEntity.rotationYaw = yaw;
			srcEntity.rotationPitch = pitch;
		}
		catch (Exception e)
		{
			Throwables.propagate(e);
			return false;
		}
		return true;
	}

	// transfer player to dimension, retaining all information and not dying
	public static boolean transferPlayerToDimension(EntityPlayerMP srcPlayer, int dstDimension, double x, double y, double z,
			float yaw, float pitch)
	{

		// get the server configuration manager for the player
		ServerConfigurationManager serverConfigurationManager = srcPlayer.mcServer.getConfigurationManager();

		// get the world server for the player's current dimension
		WorldServer srcWorldServer = srcPlayer.mcServer.worldServerForDimension(srcPlayer.dimension);
		// get the world server for the destination dimension
		WorldServer dstWorldServer = srcPlayer.mcServer.worldServerForDimension(dstDimension);

		// fire player change dimension event and check that action is valid before continuing
		PlayerChangedDimensionEvent playerChangedDimensionEvent = new PlayerChangedDimensionEvent(srcPlayer,
				srcPlayer.dimension, dstDimension);
		if (MinecraftForge.EVENT_BUS.post(playerChangedDimensionEvent) == true)
		{
			return false;
		}

		// (hard) set the player's dimension to the destination dimension
		srcPlayer.dimension = dstDimension;

		// send a player respawn packet to the destination dimension so the player respawns there
		srcPlayer.playerNetServerHandler.sendPacket(
				new S07PacketRespawn(
						srcPlayer.dimension,
						srcPlayer.worldObj.getDifficulty(), 
						srcPlayer.worldObj.getWorldInfo().getTerrainType(),
						srcPlayer.theItemInWorldManager.getGameType()
						)
				);

		// remove the original player entity
		srcWorldServer.removeEntity(srcPlayer);
		// make sure the player isn't dead (removeEntity sets player as dead)
		srcPlayer.isDead = false;

		srcPlayer.mountEntity((Entity) null);
		if (srcPlayer.riddenByEntity != null)
		{
			srcPlayer.riddenByEntity.mountEntity((Entity) null);
		}

		// spawn the player in the new world
		dstWorldServer.spawnEntityInWorld(srcPlayer);
		// update the entity (do not force)
		dstWorldServer.updateEntityWithOptionalForce(srcPlayer, false);

		// set the player's world to the new world
		srcPlayer.setWorld(dstWorldServer);

		// serverConfigurationManager.func_72375_a(sourcePlayer, sourceWorldServer);
		serverConfigurationManager.preparePlayer(srcPlayer, srcWorldServer);

		// set player's location (net server handler)
		srcPlayer.playerNetServerHandler.setPlayerLocation(x, y, z, yaw, pitch);

		// set item in world manager's world to the same as the player
		srcPlayer.theItemInWorldManager.setWorld(dstWorldServer);

		// update time and weather for the player so that it's the same as the world
		srcPlayer.mcServer.getConfigurationManager().updateTimeAndWeatherForPlayer(srcPlayer, dstWorldServer);
		srcPlayer.mcServer.getConfigurationManager().syncPlayerInventory(srcPlayer);

		// add no experience (syncs experience)
		srcPlayer.addExperience(0);
		// update player's health
		srcPlayer.setPlayerHealthUpdated();

		// fire the dimension changed event so that minecraft swithces dimensions properly
		FMLCommonHandler.instance().firePlayerChangedDimensionEvent(
				srcPlayer, 
				srcWorldServer.provider.getDimensionId(),
				dstWorldServer.provider.getDimensionId()
				);

		TeleporterUtility.transferToLocation(srcPlayer, x, y, z, srcPlayer.rotationYaw, srcPlayer.rotationPitch);

		return true;
	}

	// transfer entity to dimension. do not transfer player using this method! use _transferPlayerToDimension
	static boolean _transferEntityToDimension(Entity srcEntity, int dstDimension, 
			double x, double y, double z,
			float yaw, float pitch)
	{
		int srcDimension = srcEntity.worldObj.provider.getDimensionId();

		MinecraftServer minecraftServer = MinecraftServer.getServer();

		WorldServer srcWorldServer = minecraftServer.worldServerForDimension(srcDimension);
		WorldServer dstWorldServer = minecraftServer.worldServerForDimension(dstDimension);

		if (dstWorldServer != null)
		{
			NBTTagCompound tagCompound = new NBTTagCompound();

			srcEntity.writeToNBT(tagCompound);

			Class<? extends Entity> entityClass = srcEntity.getClass();

			srcWorldServer.removeEntity(srcEntity);

			try
			{
				Entity dstEntity = entityClass.getConstructor(World.class).newInstance((World) dstWorldServer);

				TeleporterUtility.transferToLocation(dstEntity, x, y, z, yaw, pitch);

				dstEntity.forceSpawn = true;
				dstWorldServer.spawnEntityInWorld(dstEntity);
				dstEntity.forceSpawn = false;

				ITeleporterEntity ite = dstEntity.getCapability(CapabilityTeleporterEntity.INSTANCE, null);
				ite.setOnTeleporter(true);
				ite.setTeleported(true);

				dstWorldServer.updateEntityWithOptionalForce(dstEntity, false);
			}
			catch (Exception e)
			{
				// teleport unsuccessful
				Throwables.propagate(e);
				return false;
			}

			// teleport successful
			return true;
		}
		else
		{
			// teleport unsuccessful
			System.out.println("Destination world server does not exist.");
			return false;
		}
	}
}