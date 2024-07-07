package com.lightning.northstar.contraptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;import org.apache.commons.lang3.mutable.MutableInt;

import com.lightning.northstar.Northstar;
import com.lightning.northstar.world.dimension.NorthstarDimensions;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.portal.PortalForcer;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.level.storage.LevelData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = Northstar.MOD_ID, bus = Bus.FORGE)
public class RocketHandler {
	public static List<RocketContraptionEntity> ROCKETS = new ArrayList<>();
	static int pp = 0;
	
	@SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event){
//		long l = event.level.getGameTime();
		if(event.level.isClientSide)
			return;
		if(ROCKETS.size() != 0) {
			pp++;
			for(int p = 0; p < ROCKETS.size(); p++) {
				if( pp % 800 == 0) {
					System.out.println("YOO THSI IS ESFPKC" + pp);
				if(ROCKETS.get(p).level.dimension() != ROCKETS.get(p).destination && ROCKETS.get(p).getY() > 1750) {
					changeDim(ROCKETS.get(p), event.level);
					ROCKETS.remove(ROCKETS.get(p));
				}
				}
			}
		}
    	
    }
	
	public static void changeDim(RocketContraptionEntity entity, Level level) {
		if (entity == null)
			return;
		entity.startLanding();
		ResourceKey<Level> dest;
		if(entity.destination == null) {
			dest = NorthstarDimensions.MOON_DIM_KEY;
		}else {dest = entity.destination;}
		ServerLevel destLevel = entity.getLevel().getServer().getLevel(dest);
		HashMap<Entity,Integer> seatMap = new HashMap<Entity,Integer>();
		UUID controller = null;
		Map<Entity, MutableInt> colliders = new HashMap<Entity, MutableInt>();
		System.out.println("Pre Travel Seat Map: " + entity.getContraption().getSeatMapping());
		for(Entity passengers : entity.entitiesInContraption) {
			if(passengers.level.getServer().getLevel(passengers.level.dimension()) != destLevel && !passengers.level.isClientSide) {
				System.out.println("PASSERNGSER ARE REALS");
				if(passengers instanceof ServerPlayer) {
					changePlayerDimension(destLevel, (ServerPlayer) passengers, new PortalForcer(destLevel), seatMap, entity.getContraption(), entity, controller);		
					continue;
				}
				changeDimensionCustom(destLevel, passengers, new PortalForcer(destLevel), seatMap, colliders, entity.getContraption(), entity, controller);
			}
		}
		changeDimensionCustom(destLevel, entity, new PortalForcer(destLevel), seatMap, colliders, entity.getContraption(), entity, controller);
		System.out.println(entity.getContraption().getSeatMapping() + "Please for the love of all that is holy work");
	}
	
	public static Entity changeDimensionCustom(ServerLevel pDestination, Entity entity, net.minecraftforge.common.util.ITeleporter teleporter,
			HashMap<Entity,Integer> seatMap, Map<Entity, MutableInt> colliders,RocketContraption contrap, RocketContraptionEntity contrapEnt, UUID controller) {
	      if (!net.minecraftforge.common.ForgeHooks.onTravelToDimension(entity, pDestination.dimension())) return null;
	      if (entity.level instanceof ServerLevel && !entity.isRemoved()) {
	    	  entity.level.getProfiler().push("changeDimension");
	    	  int seatNumber = -12345;
	    	  if(contrap.getSeatOf(entity.getUUID()) != null) {
	    		  Map<UUID, Integer> seatMapping = contrap.getSeatMapping();
	    		  for (Map.Entry<UUID, Integer> entry : seatMapping.entrySet()) {
	    			  if(entry.getKey() == entity.getUUID()) {
	    				  seatNumber = entry.getValue();
	    			  }
	    		  }
	    	  }	    	  
	    	  entity.level.getProfiler().push("reposition");

	    	  System.out.println(entity);

	    	  Entity transportedEntity = teleporter.placeEntity(entity, (ServerLevel) entity.level, pDestination, entity.getYRot(), spawnPortal -> {
	            	
	            entity.level.getProfiler().popPush("reloading");
	            Entity newentity = entity.getType().create(pDestination);
	            System.out.println(newentity);
	            System.out.println(entity.getType());
	            
	            if (newentity != null) {
	            	newentity.restoreFrom(entity);
	            	newentity.moveTo(entity.position().x, entity.position().y, entity.position().z, entity.getYRot(), entity.getXRot());
	            	newentity.setDeltaMovement(entity.getDeltaMovement());
	               pDestination.addDuringTeleport(newentity);
	            }
	            
	            return newentity;
	            });
	    	  	if(entity instanceof RocketContraptionEntity rce) {
	    	  		
	    	  		for(Integer entint : seatMap.values()) {
	    	  			for(Entity ents : seatMap.keySet()) {
	    	  				if(seatMap.get(ents) == entint)
	    	  				((RocketContraptionEntity)transportedEntity).addSittingPassenger(ents, entint);;
	    	  			}
	    	  		}
	    	  		for(MutableInt entint : colliders.values()) {
	    	  			for(Entity ents : colliders.keySet()) {
	    	  				if(colliders.get(ents) == entint)
	    	  				((RocketContraptionEntity)transportedEntity).registerColliding(ents);
	    	  			}
	    	  		}
	    	  		if(controller == null) {
	    	  			((RocketContraptionEntity)transportedEntity).setControllingPlayer(controller);
	    	  		}
	    	  		
	    	  		
	    	  		System.out.println(seatMap);
	    	  		System.out.println("Seat Mapping: " + ((RocketContraptionEntity)transportedEntity).getContraption().getSeatMapping());
	    	  	}
	    	  	if(seatNumber != -12345 ) {
	    	  		seatMap.put(transportedEntity, seatNumber);
	    	  	}
		    	if(contrapEnt.collidingEntities.containsKey(entity)) {
		    		colliders.put(transportedEntity, contrapEnt.collidingEntities.get(entity));
		    		System.out.println("TRUCK NUTS!!!!!!!!!!!!!!!!!!!!");
		    	}
	    	  
	    	  
	            	System.out.println("Errrrmmmm, did that just happen??   " + transportedEntity);
	            
	            entity.setRemoved(Entity.RemovalReason.CHANGED_DIMENSION);
	            entity.level.getProfiler().pop();
	            ((ServerLevel)entity.level).resetEmptyTime();
	            pDestination.resetEmptyTime();
	            entity.level.getProfiler().pop();
	            return transportedEntity;
	      } else {
	         return null;
	      }
	}
	public static Entity changePlayerDimension(ServerLevel pDestination, ServerPlayer entity, net.minecraftforge.common.util.ITeleporter teleporter, HashMap<Entity,Integer> seatMap, RocketContraption contrap, RocketContraptionEntity contrapEnt, UUID controller) {
	      if (!net.minecraftforge.common.ForgeHooks.onTravelToDimension(entity, pDestination.dimension())) return null;
	      ((ServerPlayer) entity).isChangingDimension();
	      ServerLevel serverlevel = entity.getLevel();
	          LevelData leveldata = pDestination.getLevelData();
	          entity.connection.send(new ClientboundRespawnPacket(pDestination.dimensionTypeId(), pDestination.dimension(), BiomeManager.obfuscateSeed(pDestination.getSeed()), entity.gameMode.getGameModeForPlayer(), entity.gameMode.getPreviousGameModeForPlayer(), pDestination.isDebug(), pDestination.isFlat(), true, entity.getLastDeathLocation()));
	          entity.connection.send(new ClientboundChangeDifficultyPacket(leveldata.getDifficulty(), leveldata.isDifficultyLocked()));
	          PlayerList playerlist = entity.server.getPlayerList();
	          int seatNumber = -12345;
	    	  if(contrap.getSeatOf(entity.getUUID()) != null) {
	    		  Map<UUID, Integer> seatMapping = contrap.getSeatMapping();
	    		  for (Map.Entry<UUID, Integer> entry : seatMapping.entrySet()) {
	    			  if(entry.getKey() == entity.getUUID()) {
	    				  seatNumber = entry.getValue();
	    			  }
	    		  }
	    	  }
	    	  if(contrapEnt.hasControllingPassenger() && contrapEnt.getControllingPlayer() != null) {
	    		  controller = contrapEnt.getControllingPlayer().get();
	    	  }
	          playerlist.sendPlayerPermissionLevel(entity);
	          pDestination.removePlayerImmediately(entity, Entity.RemovalReason.CHANGED_DIMENSION);
	          entity.revive();
	          PortalInfo portalinfo = new PortalInfo(entity.position(), entity.getDeltaMovement(), entity.getYRot(), entity.getXRot());
	             Entity e = teleporter.placeEntity(entity, serverlevel, pDestination, entity.getYRot(), spawnPortal -> {//Forge: Start vanilla logic
	             serverlevel.getProfiler().push("moving");
	             serverlevel.getProfiler().pop();
	             serverlevel.getProfiler().push("placing");
	             entity.setLevel(pDestination);
	             pDestination.addDuringPortalTeleport(entity);
	             entity.setXRot(portalinfo.xRot);
	             entity.setYRot(portalinfo.yRot);
	             entity.moveTo(portalinfo.pos.x, portalinfo.pos.y + 1, portalinfo.pos.z);
	             serverlevel.getProfiler().pop();
	             CriteriaTriggers.CHANGED_DIMENSION.trigger(entity, entity.level.dimension(), pDestination.dimension());
	             return entity;//forge: this is part of the ITeleporter patch
	             });//Forge: End vanilla logic
	             if (e != entity) throw new java.lang.IllegalArgumentException(String.format(java.util.Locale.ENGLISH, "Teleporter %s returned not the player entity but instead %s, expected PlayerEntity %s", teleporter, e, entity));
	             entity.connection.send(new ClientboundPlayerAbilitiesPacket(entity.getAbilities()));
	             playerlist.sendLevelInfo(entity, pDestination);
	             playerlist.sendAllPlayerInfo(entity);

	             for(MobEffectInstance mobeffectinstance : entity.getActiveEffects()) {
	                entity.connection.send(new ClientboundUpdateMobEffectPacket(entity.getId(), mobeffectinstance));
	             
	          }
	          if(seatNumber != -12345 ) {
	        	  System.out.println("WE HAVE A SEAT YEAHHHHH WOOOOO!!!!!" + seatNumber + "     " + entity.getUUID());
	        	  seatMap.put(entity, seatNumber);
			  }
	          
	             
	          return entity;
	       
	}
	
	public static boolean isInRocket(Entity entity) {
		for(RocketContraptionEntity rockets : ROCKETS) {
			if(rockets.entitiesInContraption.contains(entity)) {
				return true;
			}
		}
		return false;
	}
	
	public static void register() {
		System.out.println("Handling rockets for" + Northstar.MOD_ID);
	}

}
