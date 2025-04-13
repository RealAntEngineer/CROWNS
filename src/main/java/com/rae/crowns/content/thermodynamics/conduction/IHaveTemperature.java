package com.rae.crowns.content.thermodynamics.conduction;

public interface IHaveTemperature {
    //put the initialise here ?
    int getThermalCapacity();
    int getThermalConductivity();
    float getTemperature();
    void addTemperature(float dT);
}