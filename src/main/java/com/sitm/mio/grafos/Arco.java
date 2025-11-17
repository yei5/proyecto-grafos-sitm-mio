package com.sitm.mio.grafos;

/**
 * Clase que representa un arco (ruta) entre dos nodos en el grafo del SITM-MIO
 */
public class Arco {
    private Nodo origen;
    private Nodo destino;
    private double distancia; // Distancia en kilómetros
    private double tiempoPromedio; // Tiempo promedio en minutos
    private String ruta; // Identificador de la ruta
    private double velocidadPromedio; // Velocidad promedio calculada (km/h)
    
    /**
     * Constructor de la clase Arco
     * @param origen Nodo de origen
     * @param destino Nodo de destino
     * @param distancia Distancia en kilómetros
     * @param tiempoPromedio Tiempo promedio en minutos
     * @param ruta Identificador de la ruta
     */
    public Arco(Nodo origen, Nodo destino, double distancia, double tiempoPromedio, String ruta) {
        this.origen = origen;
        this.destino = destino;
        this.distancia = distancia;
        this.tiempoPromedio = tiempoPromedio;
        this.ruta = ruta;
        this.velocidadPromedio = calcularVelocidadPromedio();
    }
    
    /**
     * Calcula la velocidad promedio del arco
     * @return Velocidad promedio en km/h
     */
    private double calcularVelocidadPromedio() {
        if (tiempoPromedio <= 0) {
            return 0.0;
        }
        // Convertir minutos a horas y calcular velocidad
        double tiempoEnHoras = tiempoPromedio / 60.0;
        return distancia / tiempoEnHoras;
    }
    
    // Getters y Setters
    public Nodo getOrigen() {
        return origen;
    }
    
    public void setOrigen(Nodo origen) {
        this.origen = origen;
    }
    
    public Nodo getDestino() {
        return destino;
    }
    
    public void setDestino(Nodo destino) {
        this.destino = destino;
    }
    
    public double getDistancia() {
        return distancia;
    }
    
    public void setDistancia(double distancia) {
        this.distancia = distancia;
        this.velocidadPromedio = calcularVelocidadPromedio();
    }
    
    public double getTiempoPromedio() {
        return tiempoPromedio;
    }
    
    public void setTiempoPromedio(double tiempoPromedio) {
        this.tiempoPromedio = tiempoPromedio;
        this.velocidadPromedio = calcularVelocidadPromedio();
    }
    
    public String getRuta() {
        return ruta;
    }
    
    public void setRuta(String ruta) {
        this.ruta = ruta;
    }
    
    public double getVelocidadPromedio() {
        return velocidadPromedio;
    }
    
    @Override
    public String toString() {
        return "Arco{" +
                "origen=" + origen.getNombre() +
                ", destino=" + destino.getNombre() +
                ", distancia=" + distancia + " km" +
                ", tiempoPromedio=" + tiempoPromedio + " min" +
                ", ruta='" + ruta + '\'' +
                ", velocidadPromedio=" + String.format("%.2f", velocidadPromedio) + " km/h" +
                '}';
    }
}

