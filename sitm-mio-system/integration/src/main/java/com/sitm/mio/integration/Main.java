package com.sitm.mio.integration;

import com.sitm.mio.grafos.ConstructorGrafo;
import com.sitm.mio.grafos.Grafo;
import com.sitm.mio.common.DatagramProcessor;
import com.sitm.mio.common.GraphAdapter;
import com.sitm.mio.common.GraphNode;
import com.sitm.mio.integration.DatagramDistributedClient;

import java.io.File;
import java.util.Map;

/**
 * Cliente principal para procesamiento distribuido de datagrams
 * Solo soporta procesamiento distribuido usando IceGrid
 */
public class Main {
    
    public static void main(String[] args) {
        String rutaDatos = args.length > 0 ? args[0] : "datos";
        
        try {
            System.out.println("=== SISTEMA SITM-MIO - PROCESAMIENTO DISTRIBUIDO ===\n");
            
            // 1. Construir el grafo desde los datos CSV
            System.out.println("1. Construyendo grafo desde datos CSV...");
            Grafo grafoGeneral = ConstructorGrafo.construirGrafoDesdeCSV(rutaDatos);
            System.out.println("   ✓ Grafo construido: " + grafoGeneral.getNumeroNodos() + 
                             " nodos, " + grafoGeneral.getNumeroArcos() + " arcos\n");
            
            // 2. Procesar datagrams de forma distribuida
            String rutaDatagrams = rutaDatos + File.separator + "datagrams.csv";
            
            // Si la ruta es una ruta UNC (\\server\share) o una unidad de red, usar directamente
            // Java soporta rutas UNC nativamente
            if (rutaDatos.startsWith("\\\\") || rutaDatos.matches("^[A-Za-z]:\\\\.*")) {
                // Es una ruta UNC o una unidad de red, usar directamente
                rutaDatagrams = rutaDatos + (rutaDatos.endsWith("\\") || rutaDatos.endsWith("/") ? "" : File.separator) + "datagrams.csv";
            } else {
                // Ruta local normal
                File archivoDatagrams = new File(rutaDatagrams);
                if (!archivoDatagrams.exists()) {
                    System.err.println("ERROR: Archivo datagrams.csv no encontrado en: " + rutaDatagrams);
                    System.err.println("El procesamiento distribuido requiere el archivo datagrams.csv");
                    System.err.println("\nNOTA: Si el archivo está en otro computador, puedes usar:");
                    System.err.println("  - Ruta UNC: \\\\servidor\\carpeta\\datagrams.csv");
                    System.err.println("  - Unidad de red: Z:\\datagrams.csv");
                    System.exit(1);
                }
            }
            
            System.out.println("2. Procesando datagrams de forma distribuida...");
            
            // Obtener configuración desde propiedades del sistema o archivo .cfg
            String locatorEndpoint = System.getProperty("Ice.Default.Locator");
            if (locatorEndpoint == null || locatorEndpoint.isEmpty()) {
                locatorEndpoint = "IceGrid/Locator:tcp -h localhost -p 4061";
            }
            
            int batchSize = Integer.parseInt(
                System.getProperty("datagram.batch.size", "1000"));
            
            System.out.println("   Locator: " + locatorEndpoint);
            System.out.println("   Batch Size: " + batchSize);
            
            // Mostrar configuración de filtros básicos
            String tiempoMin = System.getProperty("datagram.filter.tiempoMinimo", "0.1");
            String tiempoMax = System.getProperty("datagram.filter.tiempoMaximo", "120.0");
            String velMin = System.getProperty("datagram.filter.velocidadMinima", "1.0");
            String velMax = System.getProperty("datagram.filter.velocidadMaxima", "120.0");
            String distMin = System.getProperty("datagram.filter.distanciaMinima", "0.01");
            
            System.out.println("   Filtros básicos activos:");
            System.out.println("     Tiempo: " + tiempoMin + " - " + tiempoMax + " min");
            System.out.println("     Velocidad: " + velMin + " - " + velMax + " km/h");
            System.out.println("     Distancia mínima: " + distMin + " km");
            
            // Conectar con Master usando IceGrid
            DatagramDistributedClient client = new DatagramDistributedClient(locatorEndpoint);
            
            int workerCount = client.getWorkerCount();
            if (workerCount == 0) {
                System.err.println("   ⚠ ADVERTENCIA: No hay workers disponibles");
            } else {
                System.out.println("   ✓ " + workerCount + " workers disponibles");
            }
            
            // Procesar archivo
            // Usar processFileLocal para que el Cliente lea el archivo localmente
            // y el Master NO requiera acceso al archivo
            Map<String, DatagramProcessor.SpeedStatistics> velocidadesReales = 
                client.processFileLocal(rutaDatagrams, grafoGeneral, batchSize);
            
            client.close();
            
            System.out.println("\n3. Resultados:");
            System.out.println("   ✓ " + velocidadesReales.size() + 
                             " velocidades por arco calculadas");
            System.out.println("   ✓ Procesamiento completado exitosamente\n");
            
            // Calcular velocidades promedio por ruta
            Map<String, DatagramProcessor.RouteStatistics> velocidadesPorRuta = 
                DatagramProcessor.calcularVelocidadesPorRuta(velocidadesReales);
            
            // Mostrar resumen general con información de depuración
            if (!velocidadesReales.isEmpty()) {
                // Contar arcos con velocidad válida
                long arcosConVelocidad = velocidadesReales.values().stream()
                    .filter(s -> s.getVelocidadPromedio() > 0)
                    .count();
                long arcosConDistancia = velocidadesReales.values().stream()
                    .filter(s -> s.getDistancia() > 0)
                    .count();
                long arcosConTiempo = velocidadesReales.values().stream()
                    .filter(s -> s.getTiempoPromedio() > 0)
                    .count();
                
                System.out.println("   Información de depuración:");
                System.out.println("     Total de arcos: " + velocidadesReales.size());
                System.out.println("     Arcos con distancia > 0: " + arcosConDistancia);
                System.out.println("     Arcos con tiempo > 0: " + arcosConTiempo);
                System.out.println("     Arcos con velocidad > 0: " + arcosConVelocidad);
                
                double velocidadPromedioGeneral = velocidadesReales.values().stream()
                    .filter(s -> s.getVelocidadPromedio() > 0)
                    .mapToDouble(DatagramProcessor.SpeedStatistics::getVelocidadPromedio)
                    .average()
                    .orElse(0.0);
                
                System.out.println("   Velocidad promedio general (arcos con velocidad > 0): " + 
                                 String.format("%.2f", velocidadPromedioGeneral) + " km/h");
                
                // Mostrar algunos ejemplos de arcos
                if (velocidadesReales.size() > 0 && arcosConVelocidad == 0) {
                    System.out.println("\n   ⚠ ADVERTENCIA: Ningún arco tiene velocidad > 0");
                    System.out.println("   Primeros 3 arcos (para depuración):");
                    velocidadesReales.entrySet().stream()
                        .limit(3)
                        .forEach(e -> {
                            DatagramProcessor.SpeedStatistics s = e.getValue();
                            System.out.println(String.format(
                                "     %s: distancia=%.4f km, tiempo=%.2f min, velocidad=%.2f km/h",
                                e.getKey(), s.getDistancia(), s.getTiempoPromedio(), s.getVelocidadPromedio()
                            ));
                        });
                }
            }
            
            // Mostrar estadísticas por ruta
            if (!velocidadesPorRuta.isEmpty()) {
                System.out.println("\n4. Velocidades promedio por ruta:");
                System.out.println("   ✓ " + velocidadesPorRuta.size() + " rutas con datos\n");
                
                // Ordenar por velocidad promedio (descendente)
                velocidadesPorRuta.entrySet().stream()
                    .sorted((e1, e2) -> Double.compare(
                        e2.getValue().getVelocidadPromedio(), 
                        e1.getValue().getVelocidadPromedio()))
                    .forEach(entry -> {
                        DatagramProcessor.RouteStatistics stats = entry.getValue();
                        System.out.println(String.format(
                            "   Ruta: %s | Velocidad: %.2f km/h | Distancia: %.2f km | Tiempo: %.2f min | Arcos: %d | Muestras: %d",
                            stats.getRouteId(),
                            stats.getVelocidadPromedio(),
                            stats.getDistanciaTotal(),
                            stats.getTiempoTotal(),
                            stats.getNumArcos(),
                            stats.getNumMuestrasTotal()
                        ));
                    });
                
                // Calcular velocidad promedio de todas las rutas (ponderada por distancia)
                double distanciaTotalRutas = velocidadesPorRuta.values().stream()
                    .mapToDouble(DatagramProcessor.RouteStatistics::getDistanciaTotal)
                    .sum();
                
                double velocidadPromedioRutas = 0.0;
                if (distanciaTotalRutas > 0) {
                    double sumaPonderada = velocidadesPorRuta.values().stream()
                        .mapToDouble(s -> s.getDistanciaTotal() * s.getVelocidadPromedio())
                        .sum();
                    velocidadPromedioRutas = sumaPonderada / distanciaTotalRutas;
                }
                
                System.out.println("\n   Velocidad promedio ponderada (todas las rutas): " + 
                                 String.format("%.2f", velocidadPromedioRutas) + " km/h");
            }
            
            System.out.println("\n=== PROCESO COMPLETADO ===\n");
            
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
