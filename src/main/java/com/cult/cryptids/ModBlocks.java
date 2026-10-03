package com.cult.cryptids;

import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, "cult_cryptids");

    // 🧸 Плюшевая игрушка Хэттера — поворачивается лицом к игроку
    public static final RegistryObject<Block> TOY_HATTER =
            BLOCKS.register("toy_hatter", () -> new ToyHatterBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOL)
                            .strength(0.8F)
                            .sound(SoundType.WOOL)
                            .noOcclusion()
                            .isViewBlocking((s, l, p) -> false)
                            .isSuffocating((s, l, p) -> false)
            ));

    // 🧸 Предмет
    public static final RegistryObject<Item> TOY_HATTER_ITEM =
            ModItems.ITEMS.register("toy_hatter", () -> new BlockItem(
                    TOY_HATTER.get(), new Item.Properties()
            ));

    // =========================================================
    //  ВНУТРЕННИЙ КЛАСС: блок с поворотом (как печь/сундук)
    // =========================================================
    public static class ToyHatterBlock extends HorizontalDirectionalBlock {
        public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

        public ToyHatterBlock(Properties properties) {
            super(properties);
            this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            // Лицом к игроку
            return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }
    }
}