package com.rae.crowns.content.nuclear.rod;

import com.rae.crowns.init.misc.BlockEntityInit;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.lwjgl.system.NonnullDefault;

@NonnullDefault
public class RodBlock extends RotatedPillarBlock implements ProperWaterloggedBlock, IBE<RodBlockEntity> {

    // Radiation properties of this rod's material, expressed per meter of rod (i.e. per block it
    // fully occupies). These feed into the weighted totals computed by hollow blocks (sleeves,
    // drivers) via RodOccupancy.
    private final float absorption;
    private final float moderation;
    private final float reflection;

    public RodBlock(Properties properties, float absorption, float moderation, float reflection) {
        super(properties);
        this.absorption = absorption;
        this.moderation = moderation;
        this.reflection = reflection;
    }

    @Override
    public Class<RodBlockEntity> getBlockEntityClass() {
        return RodBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends RodBlockEntity> getBlockEntityType() {
        return BlockEntityInit.REACTOR_ROD.get();
    }

    /** @return the fraction of incoming radiation this rod absorbs, per meter of rod. */
    public float getAbsorption() {
        return absorption;
    }

    /** @return the fraction of incoming radiation this rod moderates, per meter of rod. */
    public float getModeration() {
        return moderation;
    }

    /** @return the fraction of incoming radiation this rod reflects, per meter of rod. */
    public float getReflection() {
        return reflection;
    }
}