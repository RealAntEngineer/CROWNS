package com.rae.crowns.config;

import com.rae.colony_api.units.Pressure;
import com.rae.colony_api.units.RadiationFlux;
import com.rae.colony_api.units.Temperature;
import net.createmod.catnip.config.ConfigBase;
import org.jetbrains.annotations.NotNull;

public class CROWNSUnits extends ConfigBase {
    public final ConfigEnum<Temperature> temperature = e(Temperature.CELSIUS,"temperature", Comments.temperature);
    public final ConfigEnum<Pressure> pressure = e(Pressure.ATMOSPHERES,"pressure", Comments.pressure);
    public final ConfigEnum<RadiationFlux> radiationFlux = e(RadiationFlux.MEGA_BECQUERELS,"radiation_flux", Comments.radiationFlux);


    @Override
    public @NotNull String getName() {
        return "units";
    }
    private static class Comments {
        static String temperature ="unit used for temperature";
        static String pressure ="unit used for pressure";
        static String radiationFlux ="unit used for radiation activity";


    }
}
