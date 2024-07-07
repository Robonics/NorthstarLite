package com.lightning.northstar.entity;

import com.lightning.northstar.NorthstarTags.NorthstarBlockTags;
import com.lightning.northstar.entity.projectiles.LunargradeSpit;
import com.lightning.northstar.sound.NorthstarSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.IAnimationTickable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.builder.ILoopType.EDefaultLoopTypes;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.util.GeckoLibUtil;

public class MoonLunargradeEntity extends Monster implements IAnimatable, IAnimationTickable, RangedAttackMob{
	AnimationFactory factory = GeckoLibUtil.createFactory(this);

	boolean didSpit;
	int spitTimer = 0;

	protected MoonLunargradeEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
		this.maxUpStep = 1;
	}

	protected void registerGoals() {
		this.goalSelector.addGoal(3, new RangedAttackGoal(this, 1.25D, 40, 20.0F));
		this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.7D));
		this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6.0F));
		this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
	    this.targetSelector.addGoal(1, new MoonLunargradeEntity.LunargradeHurtByTargetGoal(this));
	}
	
	void setDidSpit(boolean bool) {
		this.didSpit = bool;
	}
	
	public static boolean lunargradeSpawnRules(EntityType<MoonLunargradeEntity> lunargrade, LevelAccessor level, MobSpawnType spawntype, BlockPos pos, RandomSource rando) {
		int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING,(int) pos.getX(),(int) pos.getZ());
		BlockState state = level.getBlockState(pos.below());
		if(level.getEntitiesOfClass(Monster.class, new AABB(pos).inflate(92)).size() >= 24) {
			return false;
		}else if (pos.getY() >= surfaceY) {
			return false;
		} else {
			return state.is(NorthstarBlockTags.NATURAL_MOON_BLOCKS.tag);
		}
	}
	@Override
	public boolean removeWhenFarAway(double pDistanceToClosestPlayer) {
		return false;
	}
	
	@Override
	protected SoundEvent getAmbientSound() {
		super.getAmbientSound();
		return NorthstarSounds.MOON_LUNARGRADE_IDLE.get();
	}
	@Override
	protected SoundEvent getHurtSound(DamageSource pDamageSource) {
		return NorthstarSounds.MOON_LUNARGRADE_HURT.get();
	}
	@Override
	protected SoundEvent getDeathSound() {
		return NorthstarSounds.MOON_LUNARGRADE_DIE.get();
	}
	
	private void spit(LivingEntity pTarget) {
		LunargradeSpit lunargradespit = new LunargradeSpit(this.level, this);
		double d0 = pTarget.getX() - this.getX();
		double d1 = pTarget.getY(0.3333333333333333D) - lunargradespit.getY();
		double d2 = pTarget.getZ() - this.getZ();
		double d3 = Math.sqrt(d0 * d0 + d2 * d2) * (double)0.2F;
		lunargradespit.shoot(d0, d1 + d3, d2, 1.5F, 10.0F);
		if (!this.isSilent()) {
			this.level.playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.LLAMA_SPIT, this.getSoundSource(), 1.0F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
		}

		this.level.addFreshEntity(lunargradespit);
		this.didSpit = true;
		this.spitTimer = 10;
		this.level.broadcastEntityEvent(this, (byte)4);
	}
	
	@Override
	public void performRangedAttack(LivingEntity target, float dist) {
		this.spit(target);
	}
	
	static class LunargradeHurtByTargetGoal extends HurtByTargetGoal {
		public LunargradeHurtByTargetGoal(MoonLunargradeEntity lunargrade) {
			super(lunargrade);
		}

		/**
		 * Returns whether an in-progress EntityAIBase should continue executing
		 */
		public boolean canContinueToUse() {
			if (this.mob instanceof MoonLunargradeEntity) {
				MoonLunargradeEntity lunargrade = (MoonLunargradeEntity)this.mob;
				if (lunargrade.didSpit) {
					lunargrade.setDidSpit(false);
					return false;
				}
			}

			return super.canContinueToUse();
		}
	}

	@Override
	public int tickTimer() {
		return tickCount;
	}
	
	@Override
	public void tick() {
		super.tick();
		if(spitTimer > 0) {
			spitTimer = Mth.clamp(spitTimer, 0, spitTimer - 1);
		}
	}

	@Override
	public void registerControllers(AnimationData data) {
		data.addAnimationController(new AnimationController<MoonLunargradeEntity>(this, "controller", 2, this::predicate));
	}
	
	@Override
	public void handleEntityEvent(byte pId) {
		if (pId == 4) {
			this.spitTimer = 10;
		}
		super.handleEntityEvent(pId);
	}
	
	private <P extends IAnimatable> PlayState predicate(AnimationEvent<P> event) {
		if (!(event.getLimbSwingAmount() > -0.15F && event.getLimbSwingAmount() < 0.15F) && spitTimer == 0 ) {
			event.getController().setAnimation(new AnimationBuilder().addAnimation("walk", EDefaultLoopTypes.LOOP));
			event.getController().animationSpeed = event.getLimbSwingAmount();
		} else if (spitTimer > 0) {
			System.out.println("AAAAAAAAA!!!!!!! SPIT TIMER !!!!! " + spitTimer);
			event.getController().setAnimation(new AnimationBuilder().addAnimation("spit", EDefaultLoopTypes.PLAY_ONCE));
			event.getController().animationSpeed = 1;
		} else {
			event.getController().setAnimation(new AnimationBuilder().addAnimation("idle", EDefaultLoopTypes.LOOP));
			event.getController().animationSpeed = 1;
		}

		return PlayState.CONTINUE;
	}

	@Override
	public AnimationFactory getFactory() {
		return factory;
	}
}
