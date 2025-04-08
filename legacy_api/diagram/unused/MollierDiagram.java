package com.rae.crowns.api.diagram.unused;

import java.util.HashMap;
import java.util.List;

/**
 *  they hold a map of IsoFunction

 */

// make a P-T diagram for now
public record MollierDiagram(
        //make something for the vapor quality ? -> for another diagram then this one isn't a mollier diagram
        HashMap<String, HashMap<Float, IsoStateFunction>> mapOfIsoFunction,
        String xAxisName,
        String yAxisName

) {
    //it doesn't work -> see the python test
    public MollierDiagram createFromDataSheet(List<String> header,List<List<Float>> data){

        Integer xIndex = 0;
        Integer yIndex = 1;
        HashMap<String, HashMap<Float, IsoStateFunction>> mapOfIsoFunction = makeRawIsoFunctions(data,header, xIndex,yIndex);

        return new MollierDiagram(mapOfIsoFunction, header.get(xIndex),header.get(yIndex));
    }

    private HashMap<String, HashMap<Float, IsoStateFunction>> makeRawIsoFunctions(List<List<Float>> data, List<String> header, Integer xIndex, Integer yIndex) {

        HashMap<String,HashMap<Float, IsoStateFunction>> listOfIsoFunction = new HashMap<>();
        //initalise the array
        for (int i = 0; i < header.size(); i++) {
            listOfIsoFunction.put(header.get(i), new HashMap<>());
        }
        //fill the functions
        for (List<Float> statePoint : data) {
            for (int i = 0; i < header.size(); i++) {
                fillIsoFunction(listOfIsoFunction.get(header.get(i)), header.get(i), i, xIndex, yIndex, statePoint);
            }
        }
        return listOfIsoFunction;
    }

    private static void fillIsoFunction(HashMap<Float, IsoStateFunction> isoFunctionMap, String functionName, int functionIndex, int xIndex, int yIndex, List<Float> statePoint) {
        Float X  = statePoint.get(xIndex);
        Float Y  = statePoint.get(yIndex);
        //make an interpolation ?

        IsoStateFunction partialIsoFunction = isoFunctionMap.get(statePoint.get(functionIndex));
        if (partialIsoFunction == null){
            partialIsoFunction = new IsoStateFunction(functionName,new HashMap<>());
        }
        partialIsoFunction.function().put(Y, X);
        isoFunctionMap.put(statePoint.get(functionIndex), partialIsoFunction);
    }
    private void interpolateIsoFunction(HashMap<Float, IsoStateFunction> isoFunctionMap, Float distance){

    }
}
