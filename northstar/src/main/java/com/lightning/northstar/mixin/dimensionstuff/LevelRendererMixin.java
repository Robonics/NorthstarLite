package com.lightning.northstar.mixin.dimensionstuff;

import javax.annotation.Nullable;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.lightning.northstar.config.NorthstarConfigs;
import com.lightning.northstar.world.dimension.NorthstarRenderOverride;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Camera;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
	
	@Nullable
	private VertexBuffer darkBuffer;
	@Shadow
	private VertexBuffer skyBuffer;
	@Shadow
	private VertexBuffer starBuffer;
	private VertexBuffer starBuffer2;
	private VertexBuffer starBuffer3;
	
	@Nullable
	@Shadow
	private VertexBuffer cloudBuffer;
	@Nullable
	private VertexBuffer cloudBuffer2;
	private boolean generateClouds = true;
	private int prevCloudX = Integer.MIN_VALUE;
	private int prevCloudY = Integer.MIN_VALUE;
	private int prevCloudZ = Integer.MIN_VALUE;
	private Vec3 prevCloudColor = Vec3.ZERO;
	@Nullable
	private CloudStatus prevCloudsType;
	
	@Nullable
	@Shadow
	private ClientLevel level;
	@Shadow
	private Minecraft minecraft;
	private float f_alpha = 1;
	private int ticks;
	private int rainSoundTime;
	private double dust_bounce = 0.01;
    float sc = 1;
	
	private final float[] rainSizeX = new float[1024];
	private final float[] rainSizeZ = new float[1024];
	
//	@Inject(method = "renderLevel", at = @At("HEAD"), cancellable = true)
//	public void renderLevel(PoseStack pPoseStack, float pPartialTick, long pFinishNanoTime, boolean pRenderBlockOutline, Camera pCamera, GameRenderer pGameRenderer, LightTexture pLightTexture, Matrix4f pProjectionMatrix, CallbackInfo info) {
//		 if(this.level.dimension() == NorthstarDimensions.VENUS_DIM_KEY)
//		 {RenderSystem.setupLevelDiffuseLighting(VENUS_DIFFUSE_1, VENUS_DIFFUSE_2, pPoseStack.last().pose());}
//	}
	@Inject(method = "renderSnowAndRain", at = @At("HEAD"), cancellable = true)
	private void renderWeather(LightTexture pLightTexture, float pPartialTick, double pCamX, double pCamY, double pCamZ, CallbackInfo info) {
		if( NorthstarConfigs.RENDER_WEATHER.get() ) {
			NorthstarRenderOverride.renderWeather(pLightTexture, pPartialTick, pCamX, pCamY, pCamZ, info,
			this.minecraft, this.ticks, this.rainSizeX, this.rainSizeZ, this.dust_bounce);
		}
	}
	
	@Inject(method = "tickRain", at = @At("HEAD"), cancellable = true)
	private void tickRain(Camera pCamera, CallbackInfo info) {
		if( NorthstarConfigs.RENDER_WEATHER.get() ) {
			NorthstarRenderOverride.tickRain(pCamera, info,
			this.minecraft, this.level, this.ticks, this.rainSoundTime);
		}
	}
	@Inject(method = "createStars", at = @At("TAIL"), cancellable = true)
	private void createStars(CallbackInfo ci) {
		if( NorthstarConfigs.RENDER_SKY.get() ) {
			NorthstarRenderOverride.createStars( ci, this.starBuffer2, this.starBuffer3 );
		}
	}
	
	
	@Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
    private void renderSky(PoseStack pPoseStack, Matrix4f pProjectionMatrix, float pPartialTick, Camera camera, boolean thing, Runnable runnable, CallbackInfo info) {
		if( NorthstarConfigs.RENDER_SKY.get() ) {
			NorthstarRenderOverride.renderSky(pPoseStack, pProjectionMatrix, pPartialTick, camera, thing, runnable, info, minecraft, level, f_alpha, skyBuffer, pPartialTick, starBuffer, starBuffer2, starBuffer3);
		}
   }
	
	//THIS IS FOR THE OVERWORLD ONLY, OTHERWISE IT (probably) WONT BE CALLED
	// THIS IS FOR WHEN THE PLAYER IS **NOT** LEAVING THE PLANET
	@Inject(method = "renderSky", at = @At("TAIL"), cancellable = true)
    private void renderSky2(PoseStack pPoseStack, Matrix4f pProjectionMatrix, float pPartialTick, Camera camera, boolean thing, Runnable runnable, CallbackInfo info) {
		if( NorthstarConfigs.RENDER_SKY.get() ) {
			NorthstarRenderOverride.renderSky2(pPoseStack, pProjectionMatrix, pPartialTick, camera, thing, runnable, info, minecraft, level);
		}
	}
	
	@Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
   public void renderClouds(PoseStack pPoseStack, Matrix4f pProjectionMatrix, float pPartialTick, double pCamX, double pCamY, double pCamZ, CallbackInfo info) {
		if( NorthstarConfigs.RENDER_WEATHER.get() ) {
			NorthstarRenderOverride.renderClouds(pPoseStack, pProjectionMatrix, pPartialTick, pCamX, pCamY, pCamZ, info, minecraft, level, ticks, cloudBuffer, prevCloudsType, prevCloudX, prevCloudY, prevCloudZ, prevCloudColor, generateClouds);
		}
	}
}
	
	
	