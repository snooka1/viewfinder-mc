package com.example.viewfinder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class PhotoItem extends Item {
	public PhotoItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		ItemStack stack = player.getItemInHand(hand);
		CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
		if (custom == null) {
			return InteractionResult.PASS;
		}
		CompoundTag tag = custom.copyTag();
		Optional<int[]> maybeData = tag.getIntArray("data");
		if (maybeData.isEmpty() || maybeData.get().length != Projection.W * Projection.H) {
			return InteractionResult.PASS;
		}
		int[] data = maybeData.get();

		String paletteStr = tag.getStringOr("palette", "");
		String[] ids = paletteStr.isEmpty() ? new String[0] : paletteStr.split(",");
		BlockState[] states = new BlockState[ids.length];
		for (int k = 0; k < ids.length; k++) {
			Identifier id = Identifier.tryParse(ids[k]);
			states[k] = id == null ? null
					: BuiltInRegistries.BLOCK.getOptional(id).map(Block::defaultBlockState).orElse(null);
		}

		Vec3 eye = player.getEyePosition();
		BlockPos eyePos = BlockPos.containing(eye);
		BlockPos feetPos = player.blockPosition();
		Set<BlockPos> done = new HashSet<>();

		for (int j = 0; j < Projection.H; j++) {
			for (int i = 0; i < Projection.W; i++) {
				int v = data[j * Projection.W + i];
				if (v == -1) continue;
				int p = v >>> 16;
				double dist = (v & 0xFFFF) / 100.0;
				if (p >= states.length || states[p] == null) continue;

				Vec3 dir = Projection.dir(player, i, j);
				// Nudge slightly past the surface so the point lands inside the block.
				BlockPos pos = BlockPos.containing(eye.add(dir.scale(dist + 0.01)));
				if (pos.equals(eyePos) || pos.equals(feetPos) || !done.add(pos)) continue;
				if (!level.isInWorldBounds(pos)) continue;
				if (!level.getBlockState(pos).canBeReplaced()) continue; // never destroy existing blocks
				level.setBlock(pos, states[p], Block.UPDATE_CLIENTS);
			}
		}

		level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE,
				SoundSource.PLAYERS, 0.8f, 1.8f);
		if (!player.isCreative()) {
			stack.shrink(1);
		}
		return InteractionResult.SUCCESS;
	}
}
