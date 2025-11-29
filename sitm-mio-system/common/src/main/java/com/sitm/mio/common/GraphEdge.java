package com.sitm.mio.common;

import java.io.Serializable;
import java.util.Objects;

/**
 * Modelo común para representar un arco (edge) del grafo
 */
public class GraphEdge implements Serializable {
    private String origenId;
    private String destinoId;
    private String ruta;
    private double distancia; // en kilómetros
    private double tiempoPromedio; // en minutos
    private double velocidadPromedio; // en km/h
    
    public GraphEdge() {}
    
    public GraphEdge(String origenId, String destinoId, String ruta, 
                     double distancia, double tiempoPromedio) {
        this.origenId = origenId;
        this.destinoId = destinoId;
        this.ruta = ruta;
        this.distancia = distancia;
        this.tiempoPromedio = tiempoPromedio;
        this.velocidadPromedio = calcularVelocidad();
    }
    
    private double calcularVelocidad() {
        if (tiempoPromedio <= 0) return 0.0;
        double tiempoEnHoras = tiempoPromedio / 60.0;
        return distancia / tiempoEnHoras;
    }
    
    public String getOrigenId() { return origenId; }
    public void setOrigenId(String origenId) { this.origenId = origenId; }
    
    public String getDestinoId() { return destinoId; }
    public void setDestinoId(String destinoId) { this.destinoId = destinoId; }
    
    public String getRuta() { return ruta; }
    public void setRuta(String ruta) { this.ruta = ruta; }
    
    public double getDistancia() { return distancia; }
    public void setDistancia(double distancia) { 
        this.distancia = distancia;
        this.velocidadPromedio = calcularVelocidad();
    }
    
    public double getTiempoPromedio() { return tiempoPromedio; }
    public void setTiempoPromedio(double tiempoPromedio) { 
        this.tiempoPromedio = tiempoPromedio;
        this.velocidadPromedio = calcularVelocidad();
    }
    
    public double getVelocidadPromedio() { return velocidadPromedio; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphEdge graphEdge = (GraphEdge) o;
        return Objects.equals(origenId, graphEdge.origenId) &&
               Objects.equals(destinoId, graphEdge.destinoId) &&
               Objects.equals(ruta, graphEdge.ruta);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(origenId, destinoId, ruta);
    }
    
    @Override
    public String toString() {
        return String.format("GraphEdge{origen='%s', destino='%s', ruta='%s', " +
                           "distancia=%.2f km, tiempo=%.2f min, velocidad=%.2f km/h}",
                           origenId, destinoId, ruta, distancia, tiempoPromedio, velocidadPromedio);
    }
}



