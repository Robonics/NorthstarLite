package com.lightning.northstar.world.temperature;

import java.util.HashMap;

import com.lightning.northstar.world.TemperatureStuff;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * The job of this class is to provide an abstract framework to enable the ability
 * to load different temeprature loaders, allowing Northstar to adapt itself to different temperature mods while not affecting the base mod.
 * This will also enable mod authors to tell Northstar to load their own custom provider by simply calling a function.
 * 
 * IMPORTANT: Because Java does not support the idea of static + abstract, each provider must be an opject, and must be garbage collected if changed.
 * The connector class, {@link TemperatureStuff}, will handle that, therefore.
 */
public abstract class TemperatureProvider {
	abstract public void markTemp(BlockPos pos, Level level, HashMap<BlockPos, Integer> map, int temp, int sizeX, int sizeY, int sizeZ, int offsetX, int offsetY, int offsetZ);
	abstract public void removeSource(BlockPos pos, Level level, HashMap<BlockPos, Integer> map, HashMap<BlockPos, Integer> truemap);
	abstract public HashMap<BlockPos, Integer> spreadTemp(Level level, HashMap<BlockPos, Integer> list, int maxSize, int temp);
	abstract public int getTemp(BlockPos pos, Level level);
	abstract public int getTempForEntity(Entity ent);
	abstract public boolean canSpread(BlockState state);
	abstract public boolean combustable(FluidState state);
	// This stuff needs to all be updated to use data
	abstract public int combustionTemp(FluidState state);
	abstract public int getBoilingPoint(FluidState state);
	abstract public int getFreezingPoint(FluidState state);
	// --
	abstract public boolean hasInsultation(LivingEntity ent);
	// TODO: Add tag for heat resitant entities
	abstract public boolean hasHeatProtection(LivingEntity ent);

	// This stuff needs to all be update to use data x2
	abstract public double getHeatRating(ResourceKey<Level> level);
	abstract public double getHeatConstant(ResourceKey<Level> level);
	// -- 
}
