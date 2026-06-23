package com.rae.crowns.content.nuclear.rod;

import com.rae.crowns.init.misc.BlockEntityInit;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.lwjgl.system.NonnullDefault;

@NonnullDefault
public class RodDriverBlock extends DirectionalAxisKineticBlock implements IBE<RodDriverBlockEntity> {
    public RodDriverBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<RodDriverBlockEntity> getBlockEntityClass() {
        return RodDriverBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends RodDriverBlockEntity> getBlockEntityType() {
        return BlockEntityInit.REACTOR_ROD_DRIVER.get();
    }
}
