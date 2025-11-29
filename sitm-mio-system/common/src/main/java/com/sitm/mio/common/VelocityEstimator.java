package com.sitm.mio.common;

/**
 * Estimador de velocidades para transporte público
 * Calcula velocidades más realistas basadas en el contexto de la ruta
 */
public class VelocityEstimator {
    
    // Velocidades base por tipo de ruta (km/h)
    private static final double VELOCIDAD_TRONCAL = 30.0;      // Rutas T (Troncales)
    private static final double VELOCIDAD_ALIMENTADOR = 22.0;   // Rutas A (Alimentadores)
    private static final double VELOCIDAD_PREALIMENTADOR = 20.0; // Rutas P (Pre-alimentadores)
    private static final double VELOCIDAD_DEFAULT = 25.0;      // Valor por defecto
    
    // Factores de ajuste
    private static final double DISTANCIA_CORTA = 0.5;  // km - distancias menores son más lentas
    private static final double FACTOR_DISTANCIA_CORTA = 0.7; // 70% de la velocidad base
    
    /**
     * Estima la velocidad promedio para un arco basado en el contexto
     * 
     * @param rutaId Identificador de la ruta (ej: "T31", "A12", "P80")
     * @param distancia Distancia en kilómetros
     * @return Velocidad estimada en km/h
     */
    public static double estimarVelocidad(String rutaId, double distancia) {
        // Extraer el tipo de ruta del ID
        String tipoRuta = extraerTipoRuta(rutaId);
        double velocidadBase;
        
        // Determinar velocidad base según tipo de ruta
        switch (tipoRuta.toUpperCase()) {
            case "T":
                velocidadBase = VELOCIDAD_TRONCAL;
                break;
            case "A":
                velocidadBase = VELOCIDAD_ALIMENTADOR;
                break;
            case "P":
                velocidadBase = VELOCIDAD_PREALIMENTADOR;
                break;
            default:
                velocidadBase = VELOCIDAD_DEFAULT;
        }
        
        // Ajustar por distancia: tramos muy cortos son más lentos
        // (más paradas, más tiempo de aceleración/desaceleración)
        if (distancia < DISTANCIA_CORTA) {
            velocidadBase *= FACTOR_DISTANCIA_CORTA;
        }
        
        // Asegurar un mínimo razonable (no menos de 10 km/h)
        return Math.max(velocidadBase, 10.0);
    }
    
    /**
     * Extrae el tipo de ruta del identificador
     * Ejemplos: "T31-0" -> "T", "A12A-1" -> "A", "P80-0" -> "P"
     */
    private static String extraerTipoRuta(String rutaId) {
        if (rutaId == null || rutaId.isEmpty()) {
            return "";
        }
        
        // El tipo es el primer carácter
        String primerChar = rutaId.substring(0, 1);
        
        // Verificar si es un tipo válido
        if (primerChar.matches("[TAP]")) {
            return primerChar;
        }
        
        return "";
    }
    
    /**
     * Calcula el tiempo estimado basado en distancia y velocidad estimada
     * 
     * @param rutaId Identificador de la ruta
     * @param distancia Distancia en kilómetros
     * @return Tiempo estimado en minutos
     */
    public static double calcularTiempoEstimado(String rutaId, double distancia) {
        double velocidad = estimarVelocidad(rutaId, distancia);
        return DistanceCalculator.calcularTiempoEstimado(distancia, velocidad);
    }
}

