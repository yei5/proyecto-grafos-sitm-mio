package com.sitm.mio.datagram.worker;

import DatagramProcessing.*;
import com.sitm.mio.common.DatagramProcessor;
import com.zeroc.Ice.Current;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementación del servicio Worker para procesar lotes de datagrams
 * Patrón: Master-Worker
 */
public class DatagramWorkerImpl implements DatagramWorker {
    
    private final String workerId;
    private volatile boolean available;
    private final AtomicInteger tasksProcessed;
    private final AtomicLong totalProcessingTime;
    
    public DatagramWorkerImpl() {
        this.workerId = "worker-" + UUID.randomUUID().toString().substring(0, 8);
        this.available = true;
        this.tasksProcessed = new AtomicInteger(0);
        this.totalProcessingTime = new AtomicLong(0);
    }
    
    @Override
    public BatchResult processBatch(Datagram[] batch, GraphNode[] nodes, Current current) {
        long startTime = System.currentTimeMillis();
        available = false;
        
        try {
            // Convertir arrays Ice a tipos Java
            List<DatagramProcessor.Datagram> datagrams = convertirDatagrams(batch);
            Map<String, com.sitm.mio.common.GraphNode> nodeMap = convertirNodes(nodes);
            
            // Procesar lote usando DatagramProcessor optimizado
            Map<String, DatagramProcessor.SpeedStatistics> statistics = 
                DatagramProcessor.calcularVelocidadesParaLote(datagrams, nodeMap);
            
            // Convertir resultados a tipos Ice
            SpeedStatistics[] iceStats = convertirStatistics(statistics);
            
            BatchResult result = new BatchResult();
            result.batchId = "batch-" + System.currentTimeMillis();
            result.statistics = iceStats;
            result.processedCount = batch.length;
            result.processingTime = (System.currentTimeMillis() - startTime) / 1000.0;
            result.success = true;
            result.errorMessage = "";
            
            tasksProcessed.incrementAndGet();
            totalProcessingTime.addAndGet(System.currentTimeMillis() - startTime);
            
            System.out.println("Worker " + workerId + " procesó lote: " + 
                             batch.length + " datagrams, " + statistics.size() + 
                             " estadísticas (" + String.format("%.2f", result.processingTime) + "s)");
            
            return result;
            
        } catch (Exception e) {
            BatchResult result = new BatchResult();
            result.batchId = "batch-" + System.currentTimeMillis();
            result.statistics = new SpeedStatistics[0];
            result.processedCount = 0;
            result.processingTime = (System.currentTimeMillis() - startTime) / 1000.0;
            result.success = false;
            result.errorMessage = e.getMessage();
            
            System.err.println("Error procesando lote en worker " + workerId + ": " + e.getMessage());
            return result;
            
        } finally {
            available = true;
        }
    }
    
    @Override
    public boolean isAvailable(Current current) {
        return available;
    }
    
    @Override
    public String getWorkerId(Current current) {
        return workerId;
    }
    
    @Override
    public String getStatistics(Current current) {
        int tasks = tasksProcessed.get();
        long totalTime = totalProcessingTime.get();
        double avgTime = tasks > 0 ? totalTime / (1000.0 * tasks) : 0.0;
        
        return String.format(
            "Worker ID: %s | Tareas procesadas: %d | Tiempo promedio: %.2f s | Disponible: %s",
            workerId, tasks, avgTime, available
        );
    }
    
    /**
     * Convierte array de Datagram Ice a lista de DatagramProcessor.Datagram
     */
    private List<DatagramProcessor.Datagram> convertirDatagrams(Datagram[] batch) {
        List<DatagramProcessor.Datagram> result = new ArrayList<>(batch.length);
        for (Datagram dg : batch) {
            result.add(new DatagramProcessor.Datagram(
                dg.busId, dg.routeId, dg.stopId,
                dg.latitude, dg.longitude, dg.timestamp, dg.sequenceNumber
            ));
        }
        return result;
    }
    
    /**
     * Convierte array de GraphNode Ice a mapa de GraphNode común
     */
    private Map<String, com.sitm.mio.common.GraphNode> convertirNodes(GraphNode[] nodes) {
        Map<String, com.sitm.mio.common.GraphNode> map = new HashMap<>();
        for (GraphNode node : nodes) {
            // Los campos de struct en Ice son públicos
            map.put(node.id, new com.sitm.mio.common.GraphNode(
                node.id, node.nombre, node.tipo, node.longitud, node.latitud
            ));
        }
        return map;
    }
    
    /**
     * Convierte mapa de SpeedStatistics a array Ice
     */
    private SpeedStatistics[] convertirStatistics(
            Map<String, DatagramProcessor.SpeedStatistics> stats) {
        List<SpeedStatistics> result = new ArrayList<>();
        for (DatagramProcessor.SpeedStatistics s : stats.values()) {
            SpeedStatistics ice = new SpeedStatistics();
            ice.routeId = s.getRouteId();
            ice.origenStopId = s.getOrigenStopId();
            ice.destinoStopId = s.getDestinoStopId();
            ice.distancia = s.getDistancia();
            ice.tiempoPromedio = s.getTiempoPromedio();
            ice.velocidadPromedio = s.getVelocidadPromedio();
            ice.numMuestras = s.getNumMuestras();
            result.add(ice);
        }
        return result.toArray(new SpeedStatistics[0]);
    }
}

