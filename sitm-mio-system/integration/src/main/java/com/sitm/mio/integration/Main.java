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
            File archivoDatagrams = new File(rutaDatagrams);
            
            if (!archivoDatagrams.exists()) {
                System.err.println("ERROR: Archivo datagrams.csv no encontrado en: " + rutaDatagrams);
                System.err.println("El procesamiento distribuido requiere el archivo datagrams.csv");
                System.exit(1);
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
            
            // Conectar con Master usando IceGrid
            DatagramDistributedClient client = new DatagramDistributedClient(locatorEndpoint);
            
            int workerCount = client.getWorkerCount();
            if (workerCount == 0) {
                System.err.println("   ⚠ ADVERTENCIA: No hay workers disponibles");
            } else {
                System.out.println("   ✓ " + workerCount + " workers disponibles");
            }
            
            // Procesar archivo
            Map<String, DatagramProcessor.SpeedStatistics> velocidadesReales = 
                client.processFile(rutaDatagrams, grafoGeneral, batchSize);
            
            client.close();
            
            System.out.println("\n3. Resultados:");
            System.out.println("   ✓ " + velocidadesReales.size() + 
                             " velocidades reales calculadas");
            System.out.println("   ✓ Procesamiento completado exitosamente\n");
            
            // Mostrar resumen de velocidades
            if (!velocidadesReales.isEmpty()) {
                double velocidadPromedio = velocidadesReales.values().stream()
                    .mapToDouble(DatagramProcessor.SpeedStatistics::getVelocidadPromedio)
                    .average()
                    .orElse(0.0);
                
                System.out.println("   Velocidad promedio: " + 
                                 String.format("%.2f", velocidadPromedio) + " km/h");
            }
            
            System.out.println("\n=== PROCESO COMPLETADO ===\n");
            
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
