package com.sitm.mio.grafos;

/**
 * Clase que representa un nodo (estación) en el grafo del SITM-MIO
 */
public class Nodo {
    private String id;
    private String nombre;
    private String tipo; // Ejemplo: "Estación", "Paradero", etc.
    private double longitud; // Longitud GPS (DECIMALLONGITUDE)
    private double latitud;  // Latitud GPS (DECIMALLATITUDE)
    
    /**
     * Constructor de la clase Nodo
     * @param id Identificador único del nodo
     * @param nombre Nombre de la estación o paradero
     * @param tipo Tipo de nodo (Estación, Paradero, etc.)
     */
    public Nodo(String id, String nombre, String tipo) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.longitud = 0.0;
        this.latitud = 0.0;
    }
    
    /**
     * Constructor de la clase Nodo con coordenadas GPS
     * @param id Identificador único del nodo
     * @param nombre Nombre de la estación o paradero
     * @param tipo Tipo de nodo (Estación, Paradero, etc.)
     * @param longitud Longitud GPS
     * @param latitud Latitud GPS
     */
    public Nodo(String id, String nombre, String tipo, double longitud, double latitud) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.longitud = longitud;
        this.latitud = latitud;
    }
    
    // Getters y Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    
    public String getTipo() {
        return tipo;
    }
    
    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
    
    public double getLongitud() {
        return longitud;
    }
    
    public void setLongitud(double longitud) {
        this.longitud = longitud;
    }
    
    public double getLatitud() {
        return latitud;
    }
    
    public void setLatitud(double latitud) {
        this.latitud = latitud;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Nodo nodo = (Nodo) obj;
        return id != null ? id.equals(nodo.id) : nodo.id == null;
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return "Nodo{" +
                "id='" + id + '\'' +
                ", nombre='" + nombre + '\'' +
                ", tipo='" + tipo + '\'' +
                '}';
    }
}

