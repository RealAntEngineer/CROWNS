package com.rae.crowns.content.thermodynamics.compressor;

import com.jozufozu.flywheel.api.Instancer;
import com.jozufozu.flywheel.api.MaterialManager;
import com.rae.crowns.content.thermodynamics.turbine.TurbineStageBlock;
import com.rae.crowns.init.PartialModelInit;
import com.simibubi.create.content.kinetics.base.SingleRotatingInstance;
import com.simibubi.create.content.kinetics.base.flwdata.RotatingData;

public class CompressorStageInstance extends SingleRotatingInstance<CompressorStageBlockEntity> {
    public CompressorStageInstance(MaterialManager materialManager, CompressorStageBlockEntity blockEntity) {
        super(materialManager, blockEntity);
    }

    @Override
    protected Instancer<RotatingData> getModel() {
        return getRotatingMaterial().getModel(PartialModelInit.COMPRESSOR_STAGE, blockState,blockState.getValue(TurbineStageBlock.FACING));
    }
}