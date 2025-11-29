package com.sitm.mio.datagram.master;

import DatagramProcessing.*;
import com.zeroc.Ice.Current;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementación del servicio Master para procesamiento distribuido de datagrams
 * Patrón: Master-Worker, Asynchronous Queuing
 */
public class DatagramMasterImpl implements DatagramMaster {
    
    private final Map<String, DatagramWorkerPrx> workers;
    private final Map<String, JobStatus> jobs;
    private final BlockingQueue<BatchTask> pendingBatches;
    private final ExecutorService executorService;
    private final AtomicInteger jobCounter;
    private volatile boolean running;
    
    public DatagramMasterImpl() {
        this.workers = new ConcurrentHashMap<>();
        this.jobs = new ConcurrentHashMap<>();
        this.pendingBatches = new LinkedBlockingQueue<>();
        this.executorService = Executors.newCachedThreadPool();
        this.jobCounter = new AtomicInteger(0);
        this.running = true;
        
        // Iniciar dispatcher de lotes
        startBatchDispatcher();
    }
    
    @Override
    public String registerWorker(DatagramWorkerPrx worker, Current current) {
        String workerId = "worker-" + UUID.randomUUID().toString().substring(0, 8);
        workers.put(workerId, worker);
        System.out.println("Worker registrado: " + workerId + " desde " + 
                          current.con.toString());
        return workerId;
    }
    
    @Override
    public void unregisterWorker(String workerId, Current current) {
        workers.remove(workerId);
        System.out.println("Worker desregistrado: " + workerId);
    }
    
    @Override
    public String processFile(String filePath, GraphNode[] nodes, int batchSize, Current current) {
        String jobId = "job-" + jobCounter.incrementAndGet();
        
        // Crear job
        JobStatus job = new JobStatus();
        job.jobId = jobId;
        job.filePath = filePath;
        job.totalBatches = 0;
        job.completedBatches = 0;
        job.status = "PROCESSING";
        job.results = new ConcurrentHashMap<>();
        jobs.put(jobId, job);
        
        System.out.println("Iniciando procesamiento distribuido: " + jobId + 
                         " (archivo: " + filePath + ", batchSize: " + batchSize + ")");
        
        // Procesar archivo en lotes de forma asíncrona
        executorService.submit(() -> {
            try {
                procesarArchivoEnLotes(job, filePath, nodes, batchSize);
            } catch (Exception e) {
                System.err.println("Error procesando archivo: " + e.getMessage());
                job.status = "FAILED";
                job.errorMessage = e.getMessage();
            }
        });
        
        return jobId;
    }
    
    @Override
    public int getJobProgress(String jobId, Current current) {
        JobStatus job = jobs.get(jobId);
        if (job == null) return -1;
        
        if (job.totalBatches == 0) return 0;
        return (int)((job.completedBatches * 100.0) / job.totalBatches);
    }
    
    @Override
    public SpeedStatistics[] getJobResults(String jobId, Current current) {
        JobStatus job = jobs.get(jobId);
        if (job == null || !"COMPLETED".equals(job.status)) {
            return new SpeedStatistics[0];
        }
        
        return job.results.values().toArray(new SpeedStatistics[0]);
    }
    
    @Override
    public int getWorkerCount(Current current) {
        return workers.size();
    }
    
    /**
     * Procesa un archivo dividiéndolo en lotes
     */
    private void procesarArchivoEnLotes(JobStatus job, String filePath, 
                                       GraphNode[] nodes, int batchSize) throws java.lang.Exception {
        List<Datagram> currentBatch = new ArrayList<>(batchSize);
        int batchNumber = 0;
        
        // Convertir GraphNodeList a mapa para DatagramProcessor
        Map<String, com.sitm.mio.common.GraphNode> nodeMap = convertirNodes(nodes);
        
        // Leer archivo y crear lotes
        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(
                    new java.io.FileInputStream(filePath), 
                    java.nio.charset.StandardCharsets.UTF_8))) {
            
            String linea = br.readLine(); // Encabezados
            if (linea == null) {
                throw new Exception("Archivo vacío");
            }
            
            // Parsear encabezados
            String[] encabezados = parsearCSV(linea);
            Map<String, Integer> indices = mapearEncabezados(encabezados);
            
            // Leer datos
            while ((linea = br.readLine()) != null) {
                if (linea.trim().isEmpty()) continue;
                
                String[] campos = parsearCSV(linea);
                if (campos.length < encabezados.length) continue;
                
                try {
                    Datagram dg = parsearDatagram(campos, indices);
                    if (dg != null) {
                        currentBatch.add(dg);
                        
                        if (currentBatch.size() >= batchSize) {
                            // Enviar lote para procesamiento
                            String batchId = job.jobId + "-batch-" + batchNumber++;
                            BatchTask task = new BatchTask(batchId, job.jobId, 
                                                          new ArrayList<>(currentBatch), nodes);
                            pendingBatches.offer(task);
                            job.totalBatches++;
                            currentBatch.clear();
                        }
                    }
                } catch (Exception e) {
                    // Ignorar líneas con errores
                }
            }
            
            // Procesar último lote
            if (!currentBatch.isEmpty()) {
                String batchId = job.jobId + "-batch-" + batchNumber++;
                BatchTask task = new BatchTask(batchId, job.jobId, 
                                              currentBatch, nodes);
                pendingBatches.offer(task);
                job.totalBatches++;
            }
        }
        
        // Esperar a que todos los lotes se completen
        while (job.completedBatches < job.totalBatches) {
            Thread.sleep(1000);
        }
        
        job.status = "COMPLETED";
        System.out.println("Job completado: " + job.jobId + 
                         " (" + job.totalBatches + " lotes procesados)");
    }
    
    /**
     * Dispatcher de lotes a workers disponibles
     */
    private void startBatchDispatcher() {
        executorService.submit(() -> {
            while (running) {
                try {
                    BatchTask task = pendingBatches.take();
                    dispatchBatch(task);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }
    
    /**
     * Distribuye un lote a un worker disponible
     */
    private void dispatchBatch(BatchTask task) {
        DatagramWorkerPrx worker = selectAvailableWorker();
        
        if (worker != null) {
            executorService.submit(() -> {
                try {
                    BatchResult result = worker.processBatch(
                        task.batch.toArray(new Datagram[0]), 
                        task.nodes
                    );
                    
                    // Procesar resultado
                    JobStatus job = jobs.get(task.jobId);
                    if (job != null) {
                        for (SpeedStatistics stats : result.statistics) {
                            String key = stats.routeId + "-" + stats.origenStopId + "-" + stats.destinoStopId;
                            // Combinar estadísticas si ya existen
                            if (job.results.containsKey(key)) {
                                SpeedStatistics existing = job.results.get(key);
                                // Combinar muestras
                                int totalMuestras = existing.numMuestras + stats.numMuestras;
                                double tiempoPromedio = (existing.tiempoPromedio * existing.numMuestras + 
                                                        stats.tiempoPromedio * stats.numMuestras) / totalMuestras;
                                double velocidadPromedio = stats.distancia > 0 && tiempoPromedio > 0 ?
                                    stats.distancia / (tiempoPromedio / 60.0) : 0.0;
                                
                                job.results.put(key, new SpeedStatistics(
                                    stats.routeId, stats.origenStopId, stats.destinoStopId,
                                    stats.distancia, tiempoPromedio, velocidadPromedio, totalMuestras
                                ));
                            } else {
                                job.results.put(key, stats);
                            }
                        }
                        job.completedBatches++;
                    }
                } catch (Exception e) {
                    System.err.println("Error procesando lote " + task.batchId + ": " + e.getMessage());
                    // Reintentar
                    pendingBatches.offer(task);
                }
            });
        } else {
            // No hay workers disponibles, reintentar más tarde
            pendingBatches.offer(task);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Selecciona un worker disponible (round-robin)
     */
    private DatagramWorkerPrx selectAvailableWorker() {
        for (DatagramWorkerPrx worker : workers.values()) {
            try {
                if (worker.isAvailable()) {
                    return worker;
                }
            } catch (Exception e) {
                // Worker no disponible, continuar
            }
        }
        return null;
    }
    
    /**
     * Convierte GraphNodeList a mapa
     */
    private Map<String, com.sitm.mio.common.GraphNode> convertirNodes(GraphNode[] nodes) {
        Map<String, com.sitm.mio.common.GraphNode> map = new HashMap<>();
        for (GraphNode node : nodes) {
            map.put(node.id, new com.sitm.mio.common.GraphNode(
                node.id, node.nombre, node.tipo, node.longitud, node.latitud
            ));
        }
        return map;
    }
    
    // Métodos auxiliares para parsear CSV (simplificados)
    private String[] parsearCSV(String linea) {
        return linea.split(",");
    }
    
    private Map<String, Integer> mapearEncabezados(String[] encabezados) {
        Map<String, Integer> indices = new HashMap<>();
        for (int i = 0; i < encabezados.length; i++) {
            String enc = encabezados[i].replace("\"", "").trim().toLowerCase();
            if (enc.equals("busid") || enc.equals("bus_id") || enc.matches(".*bus.*id.*")) {
                indices.put("bus_id", i);
            }
            if (enc.equals("lineid") || enc.equals("line_id") || 
                enc.matches(".*line.*id.*|.*route.*id.*")) {
                indices.put("route_id", i); // lineId se mapea a route_id
            }
            if (enc.equals("stopid") || enc.equals("stop_id") || enc.matches(".*stop.*id.*")) {
                indices.put("stop_id", i);
            }
            if (enc.equals("latitude") || enc.equals("lat")) {
                indices.put("latitude", i);
            }
            if (enc.equals("longitude") || enc.equals("lon") || enc.equals("lng")) {
                indices.put("longitude", i);
            }
            if (enc.equals("datagramdate") || enc.equals("datagram_date") ||
                enc.matches(".*datagram.*date.*|.*timestamp.*|.*time.*")) {
                indices.put("timestamp", i); // datagramDate se mapea a timestamp
            }
            if (enc.equals("tripid") || enc.equals("trip_id") ||
                enc.matches(".*trip.*id.*|.*sequence.*|.*order.*")) {
                indices.put("sequence", i); // tripId se usa como secuencia
            }
        }
        return indices;
    }
    
    private Datagram parsearDatagram(String[] campos, Map<String, Integer> indices) {
        try {
            String busId = campos[indices.get("bus_id")].replace("\"", "").trim();
            String routeId = campos[indices.get("route_id")].replace("\"", "").trim();
            String stopId = campos[indices.get("stop_id")].replace("\"", "").trim();
            
            // Las coordenadas están en microgrados (ej: 34761183 = 34.761183)
            double lat = Double.parseDouble(campos[indices.get("latitude")].replace("\"", "").trim());
            double lon = Double.parseDouble(campos[indices.get("longitude")].replace("\"", "").trim());
            
            // Convertir de microgrados a grados decimales
            if (Math.abs(lat) > 90) {
                lat = lat / 1000000.0;
            }
            if (Math.abs(lon) > 180) {
                lon = lon / 1000000.0;
            }
            
            // Parsear timestamp desde datagramDate
            long timestamp = parsearTimestamp(campos, indices);
            int seq = indices.containsKey("sequence") ? 
                Integer.parseInt(campos[indices.get("sequence")].replace("\"", "").trim()) : 0;
            
            if (busId != null && routeId != null && stopId != null && timestamp > 0) {
                return new Datagram(busId, routeId, stopId, lat, lon, timestamp, seq);
            }
        } catch (Exception e) {
            // Ignorar errores de parseo
        }
        return null;
    }
    
    /**
     * Parsea timestamp desde datagramDate (formato: "2019-05-27 20:14:43")
     */
    private long parsearTimestamp(String[] campos, Map<String, Integer> indices) {
        String valor = campos[indices.get("timestamp")].replace("\"", "").trim();
        if (valor == null || valor.isEmpty()) {
            return 0;
        }
        
        try {
            // Intentar como milisegundos
            return Long.parseLong(valor);
        } catch (NumberFormatException e) {
            try {
                // Intentar como segundos
                return (long)(Double.parseDouble(valor) * 1000);
            } catch (NumberFormatException e2) {
                // Parsear como fecha "2019-05-27 20:14:43"
                return parsearFechaHora(valor);
            }
        }
    }
    
    private long parsearFechaHora(String fechaHora) {
        try {
            String[] partes = fechaHora.trim().split(" ");
            if (partes.length != 2) return 0;
            
            String[] fechaPartes = partes[0].split("-");
            String[] horaPartes = partes[1].split(":");
            
            if (fechaPartes.length != 3 || horaPartes.length != 3) return 0;
            
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.set(Integer.parseInt(fechaPartes[0]), 
                   Integer.parseInt(fechaPartes[1]) - 1, 
                   Integer.parseInt(fechaPartes[2]),
                   Integer.parseInt(horaPartes[0]),
                   Integer.parseInt(horaPartes[1]),
                   Integer.parseInt(horaPartes[2]));
            cal.set(java.util.Calendar.MILLISECOND, 0);
            
            return cal.getTimeInMillis();
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Clase interna para representar un lote pendiente
     */
    private static class BatchTask {
        String batchId;
        String jobId;
        List<Datagram> batch;
        GraphNode[] nodes;
        
        BatchTask(String batchId, String jobId, List<Datagram> batch, GraphNode[] nodes) {
            this.batchId = batchId;
            this.jobId = jobId;
            this.batch = batch;
            this.nodes = nodes;
        }
    }
    
    /**
     * Clase interna para estado de un job
     */
    private static class JobStatus {
        String jobId;
        String filePath;
        int totalBatches;
        int completedBatches;
        String status; // PROCESSING, COMPLETED, FAILED
        String errorMessage;
        Map<String, SpeedStatistics> results;
    }
}

