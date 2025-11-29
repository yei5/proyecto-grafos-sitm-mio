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
    
    @Override
    public String startJob(GraphNode[] nodes, int totalBatches, Current current) {
        String jobId = "job-" + jobCounter.incrementAndGet();
        
        // Crear job
        JobStatus job = new JobStatus();
        job.jobId = jobId;
        job.filePath = null; // No hay archivo, el cliente envía los lotes
        job.totalBatches = totalBatches;
        job.completedBatches = 0;
        job.status = "PROCESSING";
        job.results = new ConcurrentHashMap<>();
        // Guardar los nodos del grafo para usar en los lotes
        job.nodes = nodes;
        jobs.put(jobId, job);
        
        System.out.println("Iniciando job sin archivo: " + jobId + 
                         " (totalBatches: " + totalBatches + ")");
        
        return jobId;
    }
    
    @Override
    public boolean submitBatch(String jobId, Datagram[] batch, int batchNumber, Current current) {
        JobStatus job = jobs.get(jobId);
        if (job == null) {
            System.err.println("Job no encontrado: " + jobId);
            return false;
        }
        
        if (!"PROCESSING".equals(job.status)) {
            System.err.println("Job no está en estado PROCESSING: " + jobId);
            return false;
        }
        
        // Usar los nodos guardados en el job
        GraphNode[] nodesArray = job.nodes;
        if (nodesArray == null) {
            System.err.println("Job no tiene nodos del grafo: " + jobId);
            return false;
        }
        
        // Convertir Datagram[] a List<Datagram>
        List<Datagram> batchList = new ArrayList<>();
        for (Datagram dg : batch) {
            batchList.add(dg);
        }
        
        // Crear tarea de lote
        String batchId = jobId + "-batch-" + batchNumber;
        BatchTask task = new BatchTask(batchId, jobId, batchList, nodesArray);
        
        // Enviar a la cola de procesamiento
        pendingBatches.offer(task);
        
        System.out.println("Lote recibido: " + batchId + " (" + batch.length + " datagrams)");
        
        return true;
    }
    
    @Override
    public void completeJob(String jobId, Current current) {
        JobStatus job = jobs.get(jobId);
        if (job == null) {
            System.err.println("Job no encontrado: " + jobId);
            return;
        }
        
        System.out.println("Job marcado como completado (esperando procesamiento): " + jobId);
        
        // Iniciar monitoreo del job en un hilo separado
        executorService.submit(() -> {
            try {
                // Esperar a que todos los lotes se completen
                while (job.completedBatches < job.totalBatches) {
                    Thread.sleep(1000);
                }
                
                job.status = "COMPLETED";
                System.out.println("Job completado: " + jobId + 
                                 " (" + job.totalBatches + " lotes procesados)");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                job.status = "FAILED";
                job.errorMessage = "Interrumpido";
            }
        });
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
        // Soporta rutas locales, UNC (\\server\share) y unidades de red
        java.io.File archivo = new java.io.File(filePath);
        if (!archivo.exists() && !filePath.startsWith("\\\\")) {
            // Si no existe y no es una ruta UNC, verificar si es accesible
            System.out.println("Advertencia: Archivo no encontrado localmente: " + filePath);
            System.out.println("Intentando acceder como ruta de red...");
        }
        
        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(
                    new java.io.FileInputStream(filePath), 
                    java.nio.charset.StandardCharsets.UTF_8))) {
            
            String primeraLinea = br.readLine();
            if (primeraLinea == null) {
                throw new Exception("Archivo vacío");
            }
            
            // Detectar si la primera línea es encabezados o datos
            String[] primeraLineaCampos = parsearCSV(primeraLinea);
            boolean tieneEncabezados = detectarSiTieneEncabezados(primeraLineaCampos);
            
            Map<String, Integer> indices;
            String lineaDatos;
            
            if (tieneEncabezados) {
                // La primera línea son encabezados
                indices = mapearEncabezados(primeraLineaCampos);
                lineaDatos = null; // Leeremos la siguiente línea
            } else {
                // No hay encabezados, usar orden predefinido
                System.out.println("⚠ Archivo sin encabezados detectado. Usando orden predefinido de columnas.");
                indices = crearIndicesSinEncabezados(primeraLineaCampos.length);
                lineaDatos = primeraLinea; // La primera línea ya es un dato
            }
            
            // Leer datos
            if (lineaDatos != null) {
                String[] campos = parsearCSV(lineaDatos);
                try {
                    Datagram dg = parsearDatagram(campos, indices);
                    if (dg != null) {
                        currentBatch.add(dg);
                        if (currentBatch.size() >= batchSize) {
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
            
            String linea;
            while ((linea = br.readLine()) != null) {
                if (linea.trim().isEmpty()) continue;
                
                String[] campos = parsearCSV(linea);
                
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
    
    /**
     * Detecta si la primera línea del CSV contiene encabezados o datos
     */
    private boolean detectarSiTieneEncabezados(String[] primeraLinea) {
        if (primeraLinea == null || primeraLinea.length == 0) {
            return false;
        }
        
        String[] palabrasClave = {"bus", "route", "line", "stop", "date", "time", "timestamp", 
                                  "lat", "lon", "lng", "longitude", "latitude", "id", "trip"};
        
        int coincidencias = 0;
        for (String campo : primeraLinea) {
            String campoLower = campo.replace("\"", "").trim().toLowerCase();
            for (String clave : palabrasClave) {
                if (campoLower.contains(clave)) {
                    coincidencias++;
                    break;
                }
            }
        }
        
        // Si todos son números, definitivamente son datos
        boolean todosSonNumeros = true;
        for (String campo : primeraLinea) {
            String campoLimpio = campo.replace("\"", "").trim();
            if (!campoLimpio.isEmpty()) {
                try {
                    Double.parseDouble(campoLimpio);
                } catch (NumberFormatException e) {
                    todosSonNumeros = false;
                    break;
                }
            }
        }
        
        if (todosSonNumeros) {
            return false;
        }
        
        return coincidencias >= 2;
    }
    
    /**
     * Crea un mapa de índices cuando no hay encabezados, usando un orden predefinido
     * Orden por defecto basado en estructura real del CSV:
     * 0: eventType, 1: registerdate, 2: stopId, 3: odometer, 4: latitude, 
     * 5: longitude, 6: taskId, 7: lineId, 8: tripId, 9: unknown1, 
     * 10: datagramDate, 11: busId
     */
    private Map<String, Integer> crearIndicesSinEncabezados(int numColumnas) {
        Map<String, Integer> indices = new HashMap<>();
        
        // Orden por defecto basado en estructura real
        int busIdIdx = Integer.parseInt(System.getProperty("datagram.csv.column.busId", "11"));
        int routeIdIdx = Integer.parseInt(System.getProperty("datagram.csv.column.routeId", "7"));
        int stopIdIdx = Integer.parseInt(System.getProperty("datagram.csv.column.stopId", "2"));
        int latitudeIdx = Integer.parseInt(System.getProperty("datagram.csv.column.latitude", "4"));
        int longitudeIdx = Integer.parseInt(System.getProperty("datagram.csv.column.longitude", "5"));
        int timestampIdx = Integer.parseInt(System.getProperty("datagram.csv.column.timestamp", "10"));
        int sequenceIdx = Integer.parseInt(System.getProperty("datagram.csv.column.sequence", "8"));
        
        if (busIdIdx >= 0 && busIdIdx < numColumnas) {
            indices.put("bus_id", busIdIdx);
        }
        if (routeIdIdx >= 0 && routeIdIdx < numColumnas) {
            indices.put("route_id", routeIdIdx);
        }
        if (stopIdIdx >= 0 && stopIdIdx < numColumnas) {
            indices.put("stop_id", stopIdIdx);
        }
        if (latitudeIdx >= 0 && latitudeIdx < numColumnas) {
            indices.put("latitude", latitudeIdx);
        }
        if (longitudeIdx >= 0 && longitudeIdx < numColumnas) {
            indices.put("longitude", longitudeIdx);
        }
        if (timestampIdx >= 0 && timestampIdx < numColumnas) {
            indices.put("timestamp", timestampIdx);
        }
        if (sequenceIdx >= 0 && sequenceIdx < numColumnas) {
            indices.put("sequence", sequenceIdx);
        }
        
        System.out.println("⚠ Archivo sin encabezados - Orden de columnas:");
        System.out.println("  Estructura esperada: eventType, registerdate, stopId, odometer, latitude, longitude, taskId, lineId, tripId, unknown1, datagramDate, busId");
        System.out.println("  busId: " + busIdIdx + ", routeId/lineId: " + routeIdIdx + 
                         ", stopId: " + stopIdIdx + ", lat: " + latitudeIdx + 
                         ", lon: " + longitudeIdx + ", timestamp/datagramDate: " + timestampIdx + 
                         ", sequence/tripId: " + sequenceIdx);
        
        return indices;
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
        GraphNode[] nodes; // Nodos del grafo (para jobs sin archivo)
        int totalBatches;
        int completedBatches;
        String status; // PROCESSING, COMPLETED, FAILED
        String errorMessage;
        Map<String, SpeedStatistics> results;
    }
}

