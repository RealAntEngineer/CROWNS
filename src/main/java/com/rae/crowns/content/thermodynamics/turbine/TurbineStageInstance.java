package com.rae.crowns.content.thermodynamics.turbine;

import com.jozufozu.flywheel.api.Instancer;
import com.jozufozu.flywheel.api.MaterialManager;
import com.rae.crowns.init.PartialModelInit;
import com.rae.crowns.content.thermodynamics.turbine.TurbineStageBlockEntity;
import com.rae.crowns.content.thermodynamics.turbine.TurbineStageBlock;
import com.simibubi.create.content.kinetics.base.SingleRotatingInstance;
import com.simibubi.create.content.kinetics.base.flwdata.RotatingData;

public class TurbineStageInstance  extends SingleRotatingInstance<TurbineStageBlockEntity> {
    public TurbineStageInstance(MaterialManager materialManager, TurbineStageBlockEntity blockEntity) {
        super(materialManager, blockEntity);
    }

    @Override
    protected Instancer<RotatingData> getModel() {
        return getRotatingMaterial().getModel(PartialModelInit.TURBINE_STAGE, blockState,blockState.getValue(TurbineStageBlock.FACING));
    }
}