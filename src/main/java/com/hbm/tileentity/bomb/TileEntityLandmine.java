package com.hbm.tileentity.bomb;

import java.util.List;

import com.hbm.blocks.bomb.Landmine;
import com.hbm.main.MainRegistry;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;

public class TileEntityLandmine extends TileEntity {
	
	private boolean isPrimed = false;
	public boolean waitingForPlayer = false;

	public void updateEntity() {
		if(worldObj.isRemote) return;
		
		Block block = worldObj.getBlock(xCoord, yCoord, zCoord);

		if(!(block instanceof Landmine)) return;
		Landmine landmine = (Landmine) block;

		double range = landmine.range;
		double height = landmine.height;

		if(waitingForPlayer) {
			range = 25;
			height = 25;
		} else if(!isPrimed) {
			range *= 2;
			height *= 2;
		}

		@SuppressWarnings("unchecked")
		List<Object> list = worldObj.getEntitiesWithinAABBExcludingEntity(null,
			AxisAlignedBB.getBoundingBox(xCoord - range, yCoord - height, zCoord - range, xCoord + range + 1, yCoord + height, zCoord + range + 1));

		for(Object o : list) {
			if(o instanceof EntityBat) continue;
			if(waitingForPlayer) {
				// This mine has been generated by worldgen and is ignoring mobs until a player is close enough
				// This is to prevent worldgen mines from detonating well before they become gameplay relevant

				if(o instanceof EntityPlayer) {
					waitingForPlayer = false;
					return;
				}
			} else {
				if(o instanceof EntityLivingBase) {
					if(isPrimed) {
						//the explosion is part of the mine block so that the IBomb interface works, i remember now
						landmine.explode(worldObj, xCoord, yCoord, zCoord);
						if(o instanceof EntityPlayer) ((EntityPlayer) o).addStat(MainRegistry.statMines, 1);
					}
	
					return;
				}
			}
		}

		// After placing, the mine needs to prime itself in order to not immediately kill the placer
		// The mine will prime itself only after all entities have left its trigger radius * 2
		// I'm leaving this note because I made a dumb assumption on what this was meant to do
		if(!isPrimed && !waitingForPlayer) {
			this.worldObj.playSoundEffect(this.xCoord, this.yCoord, this.zCoord, "hbm:weapon.fstbmbStart", 3.0F, 1.0F);
			isPrimed = true;
		}
	}

	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		
		isPrimed = nbt.getBoolean("primed");
		waitingForPlayer = nbt.getBoolean("waiting");
	}

	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		
		nbt.setBoolean("primed", isPrimed);
		nbt.setBoolean("waiting", waitingForPlayer);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 65536.0D;
	}
}
