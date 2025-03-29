package com.rae.crowns.config;

import com.rae.crowns.api.units.Pressure;
import com.rae.crowns.api.units.Temperature;
import net.createmod.catnip.config.ConfigBase;

public class CROWNSUnits extends ConfigBase {
    public final ConfigEnum<Temperature> temperature = e(Temperature.CELSIUS,"temperature", Comments.temperature);
    public final ConfigEnum<Pressure> pressure = e(Pressure.ATMOSPHERES,"pressure", Comments.pressure);

    @Override
    public String getName() {
        return "units";
    }
    private static class Comments {
        static String temperature ="unit used for temperature";
        static String pressure ="unit used for pressure";

    }
}
