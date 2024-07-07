package com.lightning.northstar.block.tech.rocket_station;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.lightning.northstar.contraptions.RocketContraption;
import com.lightning.northstar.contraptions.RocketContraptionEntity;
import com.lightning.northstar.contraptions.RocketHandler;
import com.lightning.northstar.item.NorthstarItems;
import com.lightning.northstar.world.OxygenStuff;
import com.lightning.northstar.world.TemperatureStuff;
import com.lightning.northstar.world.dimension.NorthstarPlanets;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.ContraptionCollider;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.IControlContraption;
import com.simibubi.create.content.contraptions.IDisplayAssemblyExceptions;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.content.trains.station.StationBlock;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.WorldAttached;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;

public class RocketStationBlockEntity extends SmartBlockEntity implements IDisplayAssemblyExceptions, IControlContraption, MenuProvider {
	
	public boolean assembleNextTick;
	public Player owner;
	protected AssemblyException lastException;
	public TrackTargetingBehaviour<GlobalStation> edgePoint;
	protected ItemStackHandler inventory;
	public String name = "Bing Bong's Big Bonanza";
	
	protected int failedCarriageIndex;
	Direction assemblyDirection;
	int assemblyLength;
	int[] bogeyLocations;
	AbstractBogeyBlock<?>[] bogeyTypes;
	boolean[] upsideDownBogeys;
	int bogeyCount;
	
	public float offset;
	public int fuelCost;
	public boolean running;
	public boolean needsContraption;
	public AbstractContraptionEntity movedContraption;
	protected boolean forceMove;
	protected ScrollOptionBehaviour<MovementMode> movementMode;
	protected boolean waitingForSpeedChange;
	protected double sequencedOffsetLimit;
	
	
	int i = 0;

	// Custom position sync
	protected float clientOffsetDiff;
	public ResourceKey<Level> target;
	
	public final SimpleContainer container = new SimpleContainer(1) {
		public boolean canPlaceItem(int p_39066_, ItemStack itemstack) {
			return itemstack.is(NorthstarItems.STAR_MAP.get());
		}
		public int getMaxStackSize() {
			return 1;
		}
	};
	
	
	public ItemStack item = ItemStack.EMPTY;
	
	
	
	public static WorldAttached<Map<BlockPos, BoundingBox>> assemblyAreas = new WorldAttached<>(w -> new HashMap<>());

	public RocketStationBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
		super(typeIn, pos, state);
	}
	public boolean hasItem() {
		return this.item.is(NorthstarItems.STAR_MAP.get());
	}
	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}
	
	@Override
	public void initialize() {
		super.initialize();
		if (!getBlockState().canSurvive(level, worldPosition))
			level.destroyBlock(worldPosition, true);
	}
	
	public ItemStack getItem() {
		return this.item;
	}
	
	public void queueAssembly(Player player) {
		owner = player;
		assembleNextTick = true;
	}
	
	
	public void enterAssembly() {
		assembleNextTick = true;
	}
	public void exitAssembly() {
		assembleNextTick = false;
	}
	
	@Override
	public void tick() {
		super.tick();
		i++;
		if (i % 20 == 0 && !level.isClientSide) {}
		item = container.getItem(0);
		if (item.getItem() == NorthstarItems.STAR_MAP.get()) {
			if(item.getTagElement("Planet") != null)
			target = NorthstarPlanets.getPlanetDimension(NorthstarPlanets.targetGetter(item.getTagElement("Planet").toString()));
		}
		fuelCost = fuelCalc();
		

		if (level.isClientSide)
			return;

		if (assembleNextTick == true) {
			System.out.println("BAINZGEA 2!!!!!!");
			tryAssemble();
			assembleNextTick = false;
		}
	}
	
	
	
	private void tryAssemble() {
		System.out.println("HUH");
		if (level.isClientSide)
			return;
		BlockState blockState = getBlockState();
		System.out.println("im tryin over here man :(");
		if (!(blockState.getBlock() instanceof RocketStationBlock))
			{System.out.println("what"); return;}

		RocketContraption contraption = new RocketContraption();

		BlockEntity blockEntity = level.getBlockEntity(worldPosition);
		if (!(blockEntity instanceof RocketStationBlockEntity))
			{System.out.println("ruh roh raggy"); return; }
		Direction movementDirection = Direction.UP;
		
		int engines = 0;
		boolean hasStation = false;
		boolean hasFuel = false;
		int fuelAmount = 0;
		int requiredJets = 0;
		int heatShielding = 0;
		double heatCost = 0;
		double heatCostHome = 0;
		try {
			lastException = null;
			if (!contraption.assemble(level, worldPosition))
				{System.out.println("IT DOESNT WORK AHHHHHH");return;} 
			engines = contraption.hasJetEngine();
			hasFuel = contraption.hasFuel();
			fuelAmount = contraption.fuelAmount();
			heatShielding = contraption.heatShielding();
			hasStation |= contraption.hasRocketStation();
			contraption.owner = owner;
			contraption.fuelCost = fuelCost;
			System.out.println(this.container);
			heatCost = (TemperatureStuff.getHeatRating(target) * (contraption.blockCount)) + TemperatureStuff.getHeatConstant(target);
			heatCostHome = (TemperatureStuff.getHeatRating(level.dimension()) * (contraption.blockCount)) + TemperatureStuff.getHeatConstant(level.dimension());
			if(heatCostHome > heatCost) {
				heatCost = heatCostHome;
			}
			requiredJets = fuelCost / 800;

			sendData();
		} catch (AssemblyException e) {
			lastException = e;
			sendData();
			return;
		}
		if (ContraptionCollider.isCollidingWithWorld(level, contraption, worldPosition.relative(movementDirection),
			movementDirection))
			{System.out.println("Many such cases! Sad!"); return;}else {System.out.println("Obamna");}
		
		Set<BlockPos> oxyCheck = new HashSet<BlockPos>();
		boolean oxygenSealed = true;
		System.out.println("SPREAD THE DISEASE SPREAD THE DISEASE");
		if(!oxyCheck.contains(this.getBlockPos().above()))
			oxyCheck.add(this.getBlockPos().above());
		if(oxyCheck.size() < OxygenStuff.maximumOxy) {		  
			OxygenStuff.spreadOxy(this.level, oxyCheck, OxygenStuff.maximumOxy);
			System.out.println(oxyCheck.size());
		}
		if(oxyCheck.size() >= OxygenStuff.maximumOxy) {
			oxygenSealed = false;
		}
		
		if (engines >= requiredJets && hasStation && hasFuel && fuelAmount > (fuelCost + contraption.weightCost) && heatShielding >= heatCost && oxygenSealed) {
		System.out.println(engines);
		contraption.removeBlocksFromWorld(level, BlockPos.ZERO);
		RocketContraptionEntity movedContraption =
				RocketContraptionEntity.create(level, contraption);
		BlockPos anchor = worldPosition;
		movedContraption.setPos(anchor.getX(), anchor.getY(), anchor.getZ());
		AllSoundEvents.CONTRAPTION_ASSEMBLE.playOnServer(level, worldPosition);
		movedContraption.destination = target;
		level.addFreshEntity(movedContraption);
		RocketHandler.ROCKETS.add(movedContraption);
		System.out.println("Heat Cost: " + heatCost + "     Heat Shielding: " + heatShielding);
		System.out.println("Weight Cost: " + contraption.weightCost + "      Fuel Cost: " + fuelCost);}else
		{
			contraption.owner.displayClientMessage(Component.literal
			("Full Fuel Cost: " + (contraption.weightCost + contraption.fuelCost)).withStyle(ChatFormatting.GOLD), false);
			contraption.owner.displayClientMessage(Component.literal
			("Current Fuel Supply: " + contraption.fuelAmount()).withStyle(ChatFormatting.GOLD), false);
			contraption.owner.displayClientMessage(Component.literal
			("Required Heat Shielding: " + heatCost).withStyle(ChatFormatting.YELLOW), false);
			contraption.owner.displayClientMessage(Component.literal
			("Current Heat Shielding: " + contraption.heatShielding()).withStyle(ChatFormatting.YELLOW), false);
			contraption.owner.displayClientMessage(Component.literal
			("Required Engines: " + requiredJets).withStyle(ChatFormatting.BLUE), false);
			contraption.owner.displayClientMessage(Component.literal
			("Current Engine Count: " + contraption.hasJetEngine()).withStyle(ChatFormatting.BLUE), false);
			contraption.owner.displayClientMessage(Component.literal
			("Oxygen Size: " + oxyCheck.size()).withStyle(ChatFormatting.AQUA), false);
			if(!oxygenSealed) {
				contraption.owner.displayClientMessage(Component.literal
				("Cockpit is not sealed, or too large!").withStyle(ChatFormatting.AQUA), false);
			}
			contraption.owner.displayClientMessage(Component.literal
			("Rocket failed to assemble!").withStyle(ChatFormatting.RED), false);
			System.out.println("No station or jet engine, Bruh!");
			System.out.println("Heat Cost: " + heatCost + "     Heat Shielding: " + heatShielding);
			System.out.println("Weight Cost: " + contraption.weightCost + "      Fuel Cost: " + fuelCost);
			exception(new AssemblyException(Lang.translateDirect("train_assembly.no_controls")), -1);
		}
	}
	
	
	@Nullable
	public GlobalStation getStation() {
		return edgePoint.getEdgePoint();
	}
	
	public boolean isAssembling() {
		BlockState state = getBlockState();
		return state.hasProperty(StationBlock.ASSEMBLING) && state.getValue(StationBlock.ASSEMBLING);
	}
	public int fuelCalc() {
		String home = NorthstarPlanets.getPlanetName(this.level.dimension());
		String targ = NorthstarPlanets.getPlanetName(target);
		
		int home_x = (int) NorthstarPlanets.getPlanetX(home);
		int home_y = (int) NorthstarPlanets.getPlanetY(home);
		
		int targ_x = (int) NorthstarPlanets.getPlanetX(targ);
		int targ_y = (int) NorthstarPlanets.getPlanetY(targ);
		
		int dif = (int) (Math.pow(home_x - targ_x, 2) + Math.pow(home_y - targ_y, 2));
		dif = Mth.roundToward(dif, 100) / 20;
		int cost = dif + NorthstarPlanets.getPlanetAtmosphereCost(this.level.dimension()) + 1000;
		if (dif != 0) {
	//		System.out.println(dif);
		}
		return cost * 8;
	}
	
	@Override
	protected void write(CompoundTag compound, boolean clientPacket) {
		compound.put("map", this.container.getItem(0).save(new CompoundTag()));
		compound.put("item", this.item.save(new CompoundTag()));
		super.write(compound, clientPacket);
	}
	
	@Override
	public void writeSafe(CompoundTag compound) {
		super.writeSafe(compound);
		compound.put("map", this.container.getItem(0).save(new CompoundTag()));
		compound.put("item", this.item.save(new CompoundTag()));
	}

	@Override
	protected void read(CompoundTag compound, boolean clientPacket) {
		this.container.setItem(0, ItemStack.of(compound.getCompound("map")));
		this.item = ItemStack.of(compound.getCompound("item"));
		super.read(compound, clientPacket);
	}
	
	
	public ItemStackHandler getInventoryOfBlock() {
		return inventory;
	}

	public void applyInventoryToBlock(ItemStackHandler handler) {
		for (int i = 0; i < inventory.getSlots(); i++)
			inventory.setStackInSlot(i, i < handler.getSlots() ? handler.getStackInSlot(i) : ItemStack.EMPTY);
	}
	
	public boolean hasInventory() { return true; }
	
	private void exception(AssemblyException exception, int carriage) {
		failedCarriageIndex = carriage;
		lastException = exception;
		sendData();
	}
	
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		return super.getCapability(cap, side);
	}
	
	
	@SuppressWarnings("unused")
	private boolean shouldAssemble() {
		BlockState blockState = getBlockState();
		if (!(blockState.getBlock() instanceof RocketStationBlock))
			return false;
		return true;
	}
	@Override
	public AssemblyException getLastAssemblyException() {
		return lastException;
	}
	
	public Direction getAssemblyDirection() {
		if (assemblyDirection != null)
			return assemblyDirection;
		if (!edgePoint.hasValidTrack())
			return null;
		BlockPos targetPosition = edgePoint.getGlobalPosition();
		BlockState trackState = edgePoint.getTrackBlockState();
		ITrackBlock track = edgePoint.getTrack();
		AxisDirection axisDirection = edgePoint.getTargetDirection();
		Vec3 axis = track.getTrackAxes(level, targetPosition, trackState)
			.get(0)
			.normalize()
			.scale(axisDirection.getStep());
		return assemblyDirection = Direction.getNearest(axis.x, axis.y, axis.z);
	}

	@Override
	public boolean isValid() {
		return !isRemoved();
	}

	@Override
	public void attach(ControlledContraptionEntity contraption) {
		this.movedContraption = contraption;
		if (!level.isClientSide) {
			this.running = true;
			sendData();
		}
	}

	@Override
	public boolean isAttachedTo(AbstractContraptionEntity contraption) {
		return movedContraption == contraption;
	}

	@Override
	public BlockPos getBlockPosition() {
		return worldPosition;
	}
	@Override
	public void onStall() {
		if (!level.isClientSide) {
			forceMove = true;
			sendData();
		}
	}
	@Override
	public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
		return new RocketStationMenu(pContainerId, pPlayerInventory, this);
	}
	@Override
	public Component getDisplayName() {
		return Component.literal("Rocket Station");
	}

}
