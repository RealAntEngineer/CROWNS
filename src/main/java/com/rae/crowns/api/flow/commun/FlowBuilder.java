package com.rae.crowns.api.flow.commun;

import com.simibubi.create.foundation.utility.Color;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FlowBuilder {
    //todo implement Chan's algorithm for finding hull
    private final HashMap<Integer,FlowLine> flows;//ids are in order of construction
    private final Vec3 startPoint;
    private static final Color defaultColor = Color.WHITE;

    public FlowBuilder(Vec3 startPoint){
        flows = new HashMap<>();
        this.startPoint = startPoint;
    }
    public FlowBuilder wingtip(Vec3 direction,float length, float strength){
        //first point is at the tip of the blade, the

        //rotation :
        return this;
    }
    public  FlowBuilder turbulence(Vec3 flowSpeed,Vec3 rotationCenter,float length, float rotationSpeed){
        //first point is at the tip of the blade, the
        //rotation :
        ArrayList<Vec3> pointBuilder = new ArrayList<>();
        Vec3 axis = flowSpeed.normalize().scale(rotationSpeed);
        pointBuilder.add(startPoint);
        Vec3 tempPoint = startPoint;
        double dt = length/flowSpeed.length()/10;//10 points total
        for (int i = 0; i<10; i++) {
            tempPoint = tempPoint.add(flowSpeed.scale(dt)).add(startPoint.subtract(rotationCenter)
                    .xRot((float) (axis.x()*dt)).yRot((float)( axis.y()*dt)).zRot((float) (axis.z()*dt)));
            pointBuilder.add(tempPoint);
        }
        flows.put(flows.size(), new FlowLine(pointBuilder, List.of(flowSpeed.length()),List.of(defaultColor)));
        return this;
    }
    public ArrayList<FlowLine> build(){
        return new ArrayList<>(flows.values());
    }
}
