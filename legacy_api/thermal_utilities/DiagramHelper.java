package com.rae.crowns.api.thermal_utilities;

import com.rae.crowns.api.diagram.NewVersionDiagram;
import com.simibubi.create.foundation.utility.Couple;

public class DiagramHelper {

    NewVersionDiagram diagram;

    public DiagramHelper(NewVersionDiagram diagram){
        this.diagram = diagram;
    }

    /**
     *
     * @param fluidState : should have temperature, pressure and enthalpy not null otherwise return null
     * @return the vapor quality : 0 if it's a liquid, 1 if it's a gaz
     */
    public Float vaporQuality(SpecificFluidState fluidState){

        return 0f;
    }

    public SpecificFluidState fillState(SpecificFluidState fluidState){
        Float temperature = fluidState.temperature();
        Float pressure = fluidState.pressure();
        Float specific_enthalpy = fluidState.specific_enthalpy();
        Float specific_volume = fluidState.specific_volume();
        Float specific_entropy = fluidState.specific_entropy();
        if (pressure != null){
            //only the case that appear in the water transformation helper
            if (specific_enthalpy!=null){
                return diagram.interpolation(new SpecificFluidState(null,pressure,specific_enthalpy,null,null),"specific_enthalpy","pressure");
            }
            if (specific_entropy!=null){
                return diagram.interpolation(new SpecificFluidState(null,pressure,null,null,specific_entropy),"specific_enthalpy","pressure");
            }
        }
        return new SpecificFluidState(temperature,pressure,specific_enthalpy,specific_volume,specific_entropy);
    }
    public SpecificFluidState normalPointGetter(Float pressure,Float specific_enthalpy){
        return diagram.get(Couple.create(pressure,specific_enthalpy));
    }

}
