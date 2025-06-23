package com.rae.formicapi.config;

import com.rae.formicapi.units.Pressure;
import com.rae.formicapi.units.RadiationFlux;
import com.rae.formicapi.units.Temperature;
import net.createmod.catnip.config.ConfigBase;

public class CROWNSUnits extends ConfigBase {
    public final ConfigEnum<Temperature> temperature = e(Temperature.CELSIUS,"temperature", Comments.temperature);
    public final ConfigEnum<Pressure> pressure = e(Pressure.ATMOSPHERES,"pressure", Comments.pressure);
    public final ConfigEnum<RadiationFlux> radiationFlux = e(RadiationFlux.MEGA_BECQUERELS,"radiation_flux", Comments.radiationFlux);


    @Override
    public String getName() {
        return "units";
    }
    private static class Comments {
        static String temperature ="unit used for temperature";
        static String pressure ="unit used for pressure";
        static String radiationFlux ="unit used for radiation activity";


    }
}
