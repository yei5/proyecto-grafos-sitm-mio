package com.sitm.mio.grafos;

import java.util.*;

/**
 * Clase utilitaria para calcular velocidades promedio en el grafo SITM-MIO
 */
public class CalculadoraVelocidad {
    
    /**
     * Calcula la velocidad promedio de todos los arcos en el grafo
     * @param grafo Grafo a analizar
     * @return Velocidad promedio en km/h
     */
    public static double calcularVelocidadPromedioGeneral(Grafo grafo) {
        return grafo.calcularVelocidadPromedioGeneral();
    }
    
    /**
     * Calcula la velocidad promedio de una ruta específica
     * @param grafo Grafo a analizar
     * @param rutaId Identificador de la ruta
     * @return Velocidad promedio en km/h
     */
    public static double calcularVelocidadPromedioRuta(Grafo grafo, String rutaId) {
        return grafo.calcularVelocidadPromedioPorRuta(rutaId);
    }
    
    /**
     * Encuentra el arco con mayor velocidad promedio
     * @param grafo Grafo a analizar
     * @return Arco con mayor velocidad
     */
    public static Arco encontrarArcoMasRapido(Grafo grafo) {
        List<Arco> arcos = grafo.obtenerArcos();
        if (arcos.isEmpty()) {
            return null;
        }
        
        Arco masRapido = arcos.get(0);
        for (Arco arco : arcos) {
            if (arco.getVelocidadPromedio() > masRapido.getVelocidadPromedio()) {
                masRapido = arco;
            }
        }
        
        return masRapido;
    }
    
    /**
     * Encuentra el arco con menor velocidad promedio
     * @param grafo Grafo a analizar
     * @return Arco con menor velocidad
     */
    public static Arco encontrarArcoMasLento(Grafo grafo) {
        List<Arco> arcos = grafo.obtenerArcos();
        if (arcos.isEmpty()) {
            return null;
        }
        
        Arco masLento = arcos.get(0);
        for (Arco arco : arcos) {
            if (arco.getVelocidadPromedio() < masLento.getVelocidadPromedio()) {
                masLento = arco;
            }
        }
        
        return masLento;
    }
    
    /**
     * Obtiene un mapa con las velocidades promedio por ruta
     * @param grafo Grafo a analizar
     * @return Mapa con ruta como clave y velocidad promedio como valor
     */
    public static Map<String, Double> obtenerVelocidadesPorRuta(Grafo grafo) {
        Map<String, Double> velocidadesPorRuta = new HashMap<>();
        Map<String, List<Arco>> arcosPorRuta = new HashMap<>();
        
        // Agrupar arcos por ruta
        for (Arco arco : grafo.obtenerArcos()) {
            arcosPorRuta.putIfAbsent(arco.getRuta(), new ArrayList<>());
            arcosPorRuta.get(arco.getRuta()).add(arco);
        }
        
        // Calcular velocidad promedio por ruta
        for (Map.Entry<String, List<Arco>> entry : arcosPorRuta.entrySet()) {
            String ruta = entry.getKey();
            List<Arco> arcos = entry.getValue();
            
            double sumaVelocidades = 0.0;
            for (Arco arco : arcos) {
                sumaVelocidades += arco.getVelocidadPromedio();
            }
            
            double velocidadPromedio = sumaVelocidades / arcos.size();
            velocidadesPorRuta.put(ruta, velocidadPromedio);
        }
        
        return velocidadesPorRuta;
    }
    
    /**
     * Genera un reporte de velocidades del grafo
     * @param grafo Grafo a analizar
     * @return String con el reporte
     */
    public static String generarReporteVelocidades(Grafo grafo) {
        StringBuilder reporte = new StringBuilder();
        reporte.append("=== REPORTE DE VELOCIDADES SITM-MIO ===\n\n");
        
        // Velocidad promedio general
        double velocidadGeneral = calcularVelocidadPromedioGeneral(grafo);
        reporte.append("Velocidad promedio general: ")
              .append(String.format("%.2f", velocidadGeneral))
              .append(" km/h\n\n");
        
        // Arco más rápido y más lento
        Arco masRapido = encontrarArcoMasRapido(grafo);
        Arco masLento = encontrarArcoMasLento(grafo);
        
        if (masRapido != null) {
            reporte.append("Arco más rápido:\n").append(masRapido.toString()).append("\n\n");
        }
        
        if (masLento != null) {
            reporte.append("Arco más lento:\n").append(masLento.toString()).append("\n\n");
        }
        
        // Velocidades por ruta
        reporte.append("Velocidades promedio por ruta:\n");
        Map<String, Double> velocidadesPorRuta = obtenerVelocidadesPorRuta(grafo);
        for (Map.Entry<String, Double> entry : velocidadesPorRuta.entrySet()) {
            reporte.append("Ruta ").append(entry.getKey())
                  .append(": ").append(String.format("%.2f", entry.getValue()))
                  .append(" km/h\n");
        }
        
        return reporte.toString();
    }
}

