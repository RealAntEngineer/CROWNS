package com.rae.crowns.api.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.rae.crowns.CROWNS;
import com.rae.crowns.api.data.managers.SingleFileCodecJsonDataManager;
import static com.rae.crowns.CROWNS.LOGGER;

import java.util.List;


public class VaporTableDataProcessor {
    public static final Codec<List<List<String>>> RAW_DATA_CODEC = Codec.list(Codec.list(Codec.STRING));
    public static final Codec<List<String>> HEADER_CODEC = Codec.list(Codec.STRING);
    public static Codec<RawDataTable> RAW_DATA_TABLE_CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    HEADER_CODEC.fieldOf("headers").forGetter(i->i.header),
                    RAW_DATA_CODEC.fieldOf("data").forGetter(i->i.raw_data)
            ).apply(instance, RawDataTable::new));

    public static final SingleFileCodecJsonDataManager<RawDataTable> DATA_TABLE_HOLDER = new SingleFileCodecJsonDataManager<>("thermal_fonctions", CROWNS.resource("compressed_liquid_and_superheated_steam_V1.3"), RAW_DATA_TABLE_CODEC, LOGGER);


    public record RawDataTable(List<String> header, List<List<String>> raw_data ){

    }

}
