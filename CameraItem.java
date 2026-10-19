package com.example.viewfinder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class CameraItem extends Item {
	public CameraItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		Vec3 eye = player.getEyePosition();
		List<String> palette = new ArrayList<>();
		Map<String, Integer> paletteIndex = new HashMap<>();
		int[] data = new int[Projection.W * Projection.H];

		for (int j = 0; j < Projection.H; j++) {
			for (int i = 0; i < Projection.W; i++) {
				Vec3 dir = Projection.dir(player, i, j);
				Vec3 end = eye.add(dir.scale(Projection.RANGE));
				BlockHitResult hit = level.clip(new ClipContext(eye, end,
						ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
				int idx = j * Projection.W + i;
				if (hit.getType() != HitResult.Type.BLOCK) {
					data[idx] = -1;
					continue;
				}

				BlockState state = level.getBlockState(hit.getBlockPos());
				String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
				int p = paletteIndex.computeIfAbsent(id, k -> {
					palette.add(k);
					return palette.size() - 1;
				});
				int distCm = (int) Math.round(hit.getLocation().distanceTo(eye) * 100.0);
				data[idx] = (p << 16) | Math.min(distCm, 0xFFFF);
			}
		}

		CompoundTag tag = new CompoundTag();
		tag.putString("palette", String.join(",", palette));
		tag.putIntArray("data", data);

		ItemStack photo = new ItemStack(ViewfinderMod.PHOTO);
		photo.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		photo.set(DataComponents.CUSTOM_NAME, Component.literal("Photograph"));
		if (!player.getInventory().add(photo)) {
			player.drop(photo, false);
		}

		level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
				SoundSource.PLAYERS, 1.0f, 1.5f);
		player.getCooldowns().addCooldown(player.getItemInHand(hand), 30);
		return InteractionResult.SUCCESS;
	}
}
