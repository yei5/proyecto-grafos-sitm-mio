package com.sitm.mio.common;

/**
 * Calculadora de distancias usando la fórmula de Haversine
 * Patrón: Utility Class (Singleton implícito)
 */
public class DistanceCalculator {
    
    private static final double EARTH_RADIUS_KM = 6371.0;
    
    /**
     * Calcula la distancia entre dos puntos geográficos usando la fórmula de Haversine
     * @param lat1 Latitud del primer punto
     * @param lon1 Longitud del primer punto
     * @param lat2 Latitud del segundo punto
     * @param lon2 Longitud del segundo punto
     * @return Distancia en kilómetros
     */
    public static double calcularDistancia(double lat1, double lon1, double lat2, double lon2) {
        // Convertir grados a radianes
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS_KM * c;
    }
    
    /**
     * Calcula la distancia entre dos nodos
     */
    public static double calcularDistancia(GraphNode nodo1, GraphNode nodo2) {
        return calcularDistancia(nodo1.getLatitud(), nodo1.getLongitud(),
                                nodo2.getLatitud(), nodo2.getLongitud());
    }
    
    /**
     * Calcula el tiempo estimado basado en distancia y velocidad promedio
     * @param distancia Distancia en kilómetros
     * @param velocidadPromedio Velocidad promedio en km/h
     * @return Tiempo en minutos
     */
    public static double calcularTiempoEstimado(double distancia, double velocidadPromedio) {
        if (velocidadPromedio <= 0) return 0.0;
        double tiempoEnHoras = distancia / velocidadPromedio;
        return tiempoEnHoras * 60.0; // Convertir a minutos
    }
}



