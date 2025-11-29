package com.sitm.mio.common;

import java.io.Serializable;
import java.util.Objects;

/**
 * Modelo común para representar un nodo (parada) del grafo
 */
public class GraphNode implements Serializable {
    private String id;
    private String nombre;
    private String tipo;
    private double longitud;
    private double latitud;
    
    public GraphNode() {}
    
    public GraphNode(String id, String nombre, String tipo, double longitud, double latitud) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.longitud = longitud;
        this.latitud = latitud;
    }
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    
    public double getLongitud() { return longitud; }
    public void setLongitud(double longitud) { this.longitud = longitud; }
    
    public double getLatitud() { return latitud; }
    public void setLatitud(double latitud) { this.latitud = latitud; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphNode graphNode = (GraphNode) o;
        return Objects.equals(id, graphNode.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "GraphNode{id='" + id + "', nombre='" + nombre + "'}";
    }
}



