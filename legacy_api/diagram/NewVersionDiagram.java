package com.rae.crowns.api.diagram;

import com.mojang.math.Vector3d;
import com.rae.crowns.api.math.DerivationHelper;
import com.rae.crowns.api.thermal_utilities.SpecificFluidState;
import com.simibubi.create.foundation.utility.Couple;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

import static com.rae.crowns.api.math.DerivationHelper.getDerivative;

public record NewVersionDiagram(HashMap<Float, HashMap<Float, SpecificFluidState>> data, ArrayList<Float> xKeys, ArrayList<Float> yKeys) {
    public static String xAxisName ="specific_enthalpy", yAxisName="pressure";
    public void addPoint(Float x,Float y, SpecificFluidState fluidState){
        HashMap<Float, SpecificFluidState> oldPartialMap = this.data.get(x);
        if (oldPartialMap==null){
            oldPartialMap = new HashMap<>();
        }
        else {
            this.xKeys.add(x);
            this.xKeys.sort(Comparator.naturalOrder());
        }
        oldPartialMap.put(y,fluidState);
        this.yKeys.add(y);
        this.xKeys.sort(Comparator.naturalOrder());
        this.data.put(x,oldPartialMap);
    }



    public SpecificFluidState interpolation(SpecificFluidState fluidState, @Nonnull String firstKnownVariable, @Nonnull String secondKnownVariable){
        if (firstKnownVariable.equals(xAxisName)){
            if (secondKnownVariable.equals(yAxisName)){
                return normalInterpolation(fluidState);
            }
            if (secondKnownVariable.equals("specific_entropy")){
                return entropyInterpolation(fluidState);
            }
        }
        return fluidState;
    }
    /**
     * interpolate the fluid state from pressure and entropy
     * @param fluidState the partial fluidState
     * @return the complete fluidState
     */
    private SpecificFluidState entropyInterpolation(SpecificFluidState fluidState) {
        Couple<Float> firstPoint = null; // low left
        Couple<Float> secondPoint = null; // low right
        Couple<Float> thirdPoint = null; // up left
        Float distance1 = null;
        Float distance2 = null;
        Float distance3 = null;

        //TODO make this smarter + work with cache so near point will be found quicker
        for (Float x:
                xKeys) {
            for (Float y:data.get(x).keySet()){
                Float s = get(Couple.create(x,y)).specific_entropy();
                Float currentDistance = distance(s, y, fluidState.specific_entropy(), fluidState.pressure());
                if (y < fluidState.pressure()) {
                    if (s < fluidState.specific_enthalpy()) {
                        if (distance1 == null || currentDistance > distance1) {
                            distance1 = currentDistance;
                            firstPoint = Couple.create(s, y);
                        }
                    }
                    else {
                        if (distance2 == null || currentDistance > distance2) {
                            distance2 = currentDistance;
                            secondPoint = Couple.create(s, y);
                        }
                    }
                }
                else  {
                    if (s < fluidState.specific_enthalpy()) {
                        if (distance3 == null || distance(Couple.create(s, y), Couple.create(fluidState.specific_enthalpy(), fluidState.pressure())) > distance3) {
                            distance3 = distance(Couple.create(s, y), Couple.create(fluidState.specific_enthalpy(), fluidState.pressure()));
                            thirdPoint = Couple.create(s, y);
                        }
                    }
                }
            }
        }
        assert firstPoint != null;
        SpecificFluidState firstPointValue = get(firstPoint);
        assert secondPoint != null;
        SpecificFluidState secondPointValue = get(firstPoint);
        assert thirdPoint != null;
        SpecificFluidState thirdPointValue = get(thirdPoint);

        assert firstPointValue != null;
        assert secondPointValue != null;
        assert thirdPointValue != null;

        DerivationHelper.Plane localTemperaturePlane =
                getDerivative(
                        new Vector3d(
                                firstPointValue.specific_entropy(),
                                firstPointValue.pressure(),
                                firstPointValue.temperature())
                        ,new Vector3d(
                                secondPointValue.specific_entropy(),
                                secondPointValue.pressure(),
                                secondPointValue.temperature()),
                        new Vector3d(
                                thirdPointValue.specific_entropy(),
                                thirdPointValue.pressure(),
                                thirdPointValue.temperature()));
        DerivationHelper.Plane localVolumePlane =
                getDerivative(
                        new Vector3d(
                                firstPointValue.specific_entropy(),
                                firstPointValue.pressure(),
                                firstPointValue.specific_volume())
                        ,new Vector3d(
                                secondPointValue.specific_entropy(),
                                secondPointValue.pressure(),
                                secondPointValue.specific_volume()),
                        new Vector3d(
                                thirdPointValue.specific_entropy(),
                                thirdPointValue.pressure(),
                                thirdPointValue.specific_volume()));
        DerivationHelper.Plane localEnthalpyPlane =
                getDerivative(
                        new Vector3d(
                                firstPointValue.specific_entropy(),
                                firstPointValue.pressure(),
                                firstPointValue.specific_volume())
                        ,new Vector3d(
                                secondPointValue.specific_entropy(),
                                secondPointValue.pressure(),
                                secondPointValue.specific_volume()),
                        new Vector3d(
                                thirdPointValue.specific_entropy(),
                                thirdPointValue.pressure(),
                                thirdPointValue.specific_volume()));
        Couple<Double> point = Couple.create((double)fluidState.pressure(),(double)fluidState.specific_enthalpy());
        return new SpecificFluidState(
                (float) localTemperaturePlane.getPoint(point),
                fluidState.pressure(), (float) localEnthalpyPlane.getPoint(point),
                (float) localVolumePlane.getPoint(point),
                fluidState.specific_entropy());
    }

    /**
     * interpolate the fluid state from pressure and enthalpy
     * @param fluidState the partial fluidState
     * @return the complete fluidState
     */

    public SpecificFluidState normalInterpolation(SpecificFluidState fluidState){
        Couple<Float> firstPoint = null; // low left
        Couple<Float> secondPoint = null; // low right
        Couple<Float> thirdPoint = null; // up left
        Float distance1 = null;
        Float distance2 = null;
        Float distance3 = null;

        //TODO make this smarter + work with cache so near point will be found quicker
        for (Float x:
             xKeys) {
            for (Float y:data.get(x).keySet()){
                Float currentDistance = distance(x, y, fluidState.specific_enthalpy(), fluidState.pressure());
                if (y < fluidState.pressure()) {
                    if (x < fluidState.specific_enthalpy()) {
                        if (distance1 == null || currentDistance > distance1) {
                            distance1 = currentDistance;
                            firstPoint = Couple.create(x, y);
                        }
                    }
                    else {
                        if (distance2 == null || currentDistance > distance2) {
                            distance2 = currentDistance;
                            secondPoint = Couple.create(x, y);
                        }
                    }
                }
                else  {
                    if (x < fluidState.specific_enthalpy()) {
                        if (distance3 == null || distance(Couple.create(x, y), Couple.create(fluidState.specific_enthalpy(), fluidState.pressure())) > distance3) {
                            distance3 = distance(Couple.create(x, y), Couple.create(fluidState.specific_enthalpy(), fluidState.pressure()));
                            thirdPoint = Couple.create(x, y);
                        }
                    }
                }
            }
        }
        assert firstPoint != null;
        SpecificFluidState firstPointValue = get(firstPoint);
        assert secondPoint != null;
        SpecificFluidState secondPointValue = get(firstPoint);
        assert thirdPoint != null;
        SpecificFluidState thirdPointValue = get(thirdPoint);

        assert firstPointValue != null;
        assert secondPointValue != null;
        assert thirdPointValue != null;

        DerivationHelper.Plane localTemperaturePlane =
                getDerivative(
                        new Vector3d(
                                firstPointValue.specific_enthalpy(),
                                firstPointValue.pressure(),
                                firstPointValue.temperature())
                        ,new Vector3d(
                                secondPointValue.specific_enthalpy(),
                                secondPointValue.pressure(),
                                secondPointValue.temperature()),
                        new Vector3d(
                                thirdPointValue.specific_enthalpy(),
                                thirdPointValue.pressure(),
                                thirdPointValue.temperature()));
        DerivationHelper.Plane localVolumePlane =
                getDerivative(
                        new Vector3d(
                                firstPointValue.specific_enthalpy(),
                                firstPointValue.pressure(),
                                firstPointValue.specific_volume())
                        ,new Vector3d(
                                secondPointValue.specific_enthalpy(),
                                secondPointValue.pressure(),
                                secondPointValue.specific_volume()),
                        new Vector3d(
                                thirdPointValue.specific_enthalpy(),
                                thirdPointValue.pressure(),
                                thirdPointValue.specific_volume()));
        DerivationHelper.Plane localEntropyPlane =
                getDerivative(
                        new Vector3d(
                                firstPointValue.specific_enthalpy(),
                                firstPointValue.pressure(),
                                firstPointValue.specific_entropy())
                        ,new Vector3d(
                                secondPointValue.specific_enthalpy(),
                                secondPointValue.pressure(),
                                secondPointValue.specific_entropy()),
                        new Vector3d(
                                thirdPointValue.specific_enthalpy(),
                                thirdPointValue.pressure(),
                                thirdPointValue.specific_entropy()));
        Couple<Double> point = Couple.create((double)fluidState.pressure(),(double)fluidState.specific_enthalpy());
        return new SpecificFluidState(
                (float) localTemperaturePlane.getPoint(point),
                fluidState.pressure(), fluidState.specific_enthalpy(),
                (float) localVolumePlane.getPoint(point),
                (float) localEntropyPlane.getPoint(point));
    }

    public Float distance(Couple<Float> a, Couple<Float> b){
        return distance(a.getFirst(),a.getSecond(), b.getFirst(), b.getSecond());
    }
    public Float distance(Float x1,Float y1,Float x2,Float y2){
        return (float) Math.sqrt((x1 - x2)*(x1 - x2) + (y1 - y2)*(y1 - y2));
    }

    /**
     *
     * @param coordinate couple with x and y axis
     * @return the data point at that coordinate or null if non-existent
     */
    public SpecificFluidState get(Couple<Float> coordinate){
        if (data.get(coordinate.getFirst())== null){
            return null;
        }
        return data.get(coordinate.getFirst()).get(coordinate.getSecond());
    }
}
