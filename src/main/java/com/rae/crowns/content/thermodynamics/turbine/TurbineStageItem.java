package com.rae.crowns.content.thermodynamics.turbine;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

public class TurbineStageItem  extends BlockItem {
    public TurbineStageItem(Block block, Properties properties) {
        super(block, properties);
    }
    /*@Override
    protected boolean canPlace(BlockPlaceContext pContext, @NotNull BlockState pState) {
        TurbineStageBlock main = (TurbineStageBlock) getBlock();
        Level lvl = pContext.getLevel();
        Direction facing = pContext.getClickedFace();
        Vec3i offset = main.getOffset(facing, false);//nope this isn't the correct offset to know where to verify the blocks
        BlockPos mainPos = pContext.getClickedPos().offset(offset);
        boolean flag = true;
        Vec3i size = main.getSize(facing);
        for (int x = -offset.getX(); x < size.getX() - offset.getX();x++){
            for (int y = -offset.getY(); y < size.getY() - offset.getY();y++){
                for (int z = -offset.getZ(); z < size.getZ() - offset.getZ();z++){
                    if (!lvl.getBlockState(mainPos.offset(x,y,z)).isAir()){
                        flag = false;
                        break;
                    }
                }
                if (!flag){
                    break;
                }
            }
            if (!flag){
                break;
            }
        }
        return true;
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext pContext, @NotNull BlockState pState) {
        TurbineStageBlock main = (TurbineStageBlock) getBlock();
        Level lvl = pContext.getLevel();
        BlockPos mainPos = pContext.getClickedPos();
        lvl.setBlockAndUpdate(mainPos, main.getStateForPlacement(pContext));

        Player player = pContext.getPlayer();
        ItemStack itemstack = pContext.getItemInHand();
        BlockState blockstate1 = lvl.getBlockState(mainPos);
        blockstate1.getBlock().setPlacedBy(lvl, mainPos, blockstate1, player, itemstack);
        if (player instanceof ServerPlayer) {
            CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer) player, mainPos, itemstack);
        }

        return true;
    }
     */
}
