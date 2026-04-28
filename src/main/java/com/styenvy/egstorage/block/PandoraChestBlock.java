package com.styenvy.egstorage.block;

import com.styenvy.egstorage.blockentity.PandoraChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public class PandoraChestBlock extends Block implements EntityBlock {

    public PandoraChestBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new PandoraChestBlockEntity(pos, state);
    }

    @Override
    public @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof PandoraChestBlockEntity chestEntity) {
                // Play mystical opening sound
                level.playSound(null, pos, SoundEvents.ENDER_CHEST_OPEN, SoundSource.BLOCKS, 0.5F,
                        level.random.nextFloat() * 0.1F + 0.9F);
                serverPlayer.openMenu((MenuProvider) chestEntity, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        // Add mystical particle effects
        for (int i = 0; i < 3; ++i) {
            if (random.nextInt(10) == 0) {
                double x = pos.getX() + 0.5D + (random.nextDouble() - 0.5D);
                double y = pos.getY() + 1.1D;
                double z = pos.getZ() + 0.5D + (random.nextDouble() - 0.5D);
                level.addParticle(ParticleTypes.PORTAL, x, y, z,
                        (random.nextDouble() - 0.5D) * 0.5D, -random.nextDouble(),
                        (random.nextDouble() - 0.5D) * 0.5D);
            }
        }
    }
}