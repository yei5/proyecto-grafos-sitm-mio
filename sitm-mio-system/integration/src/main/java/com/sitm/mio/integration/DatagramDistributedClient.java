package com.sitm.mio.integration;

import DatagramProcessing.*;
import com.sitm.mio.common.GraphAdapter;
import com.sitm.mio.common.GraphNode;
import com.sitm.mio.common.DatagramProcessor;
import com.zeroc.Ice.*;
import com.zeroc.IceGrid.QueryPrx;

import java.util.*;

/**
 * Cliente para procesamiento distribuido de datagrams
 * Conecta con el servicio Master y coordina el procesamiento
 */
public class DatagramDistributedClient {
    
    private final DatagramMasterPrx master;
    private final Communicator communicator;
    
    public DatagramDistributedClient(String locatorEndpoint) throws java.lang.Exception {
        communicator = Util.initialize();
        
        // Configurar Locator
        LocatorPrx locator = LocatorPrx.checkedCast(
            communicator.stringToProxy(locatorEndpoint)
        );
        
        if (locator == null) {
            throw new java.lang.Exception("No se pudo conectar al Locator en: " + locatorEndpoint);
        }
        
        communicator.setDefaultLocator(locator);
        
        // Usar IceGrid QueryPrx para buscar el Master (método recomendado)
        QueryPrx query = QueryPrx.checkedCast(
            communicator.stringToProxy("IceGrid/Query")
        );
        
        if (query == null) {
            throw new java.lang.Exception("No se pudo obtener QueryPrx de IceGrid. Verifica que el Registry esté corriendo.");
        }
        
        System.out.println("Buscando Master en IceGrid usando QueryPrx...");
        
        // Buscar Master usando QueryPrx
        ObjectPrx masterProxy = null;
        int maxRetries = 5;
        for (int i = 0; i < maxRetries; i++) {
            try {
                masterProxy = query.findObjectById(Util.stringToIdentity("DatagramMaster"));
                if (masterProxy != null) {
                    break;
                }
            } catch (java.lang.Exception e) {
                if (i < maxRetries - 1) {
                    System.out.println("  Intento " + (i+1) + "/" + maxRetries + " falló, reintentando...");
                    Thread.sleep(1000);
                } else {
                    throw new java.lang.Exception("No se pudo encontrar Master después de " + maxRetries + " intentos: " + e.getMessage());
                }
            }
        }
        
        if (masterProxy == null) {
            // Fallback: intentar con Locator
            System.out.println("⚠ QueryPrx no encontró Master, intentando con Locator...");
            masterProxy = locator.findObjectById(Util.stringToIdentity("DatagramMaster"));
        }
        
        if (masterProxy == null) {
            throw new java.lang.Exception("Master no encontrado en IceGrid. Verifica que el Master esté ejecutándose y registrado.");
        }
        
        master = DatagramMasterPrx.checkedCast(masterProxy);
        
        if (master == null) {
            throw new java.lang.Exception("No se pudo hacer cast del Master proxy");
        }
        
        System.out.println("✓ Conectado al Master distribuido vía IceGrid QueryPrx");
    }
    
    /**
     * Procesa un archivo de datagrams de forma distribuida
     * El Master lee el archivo directamente (requiere acceso al archivo)
     * 
     * @param filePath Ruta al archivo datagrams.csv
     * @param grafo Grafo con nodos para cálculo de distancias
     * @param batchSize Tamaño de cada lote (recomendado: 500-2000)
     * @return Mapa de estadísticas de velocidad
     */
    public Map<String, com.sitm.mio.common.DatagramProcessor.SpeedStatistics> 
            processFile(String filePath, com.sitm.mio.grafos.Grafo grafo, int batchSize) throws java.lang.Exception {
        
        // Convertir nodos del grafo a formato Ice
        Map<String, com.sitm.mio.common.GraphNode> nodos = GraphAdapter.convertirNodos(grafo);
        DatagramProcessing.GraphNode[] nodeList = convertirNodesToIce(nodos);
        
        // Iniciar procesamiento distribuido
        System.out.println("Iniciando procesamiento distribuido de: " + filePath);
        String jobId = master.processFile(filePath, nodeList, batchSize);
        System.out.println("Job ID: " + jobId);
        
        // Monitorear progreso
        int lastProgress = -1;
        while (true) {
            int progress = master.getJobProgress(jobId);
            
            if (progress == -1) {
                throw new java.lang.Exception("Job no encontrado: " + jobId);
            }
            
            if (progress != lastProgress) {
                System.out.println("Progreso: " + progress + "%");
                lastProgress = progress;
            }
            
            if (progress >= 100) {
                break;
            }
            
            Thread.sleep(2000); // Esperar 2 segundos antes de verificar de nuevo
        }
        
        // Obtener resultados
        System.out.println("Obteniendo resultados...");
        SpeedStatistics[] results = master.getJobResults(jobId);
        
        // Convertir a formato común
        Map<String, com.sitm.mio.common.DatagramProcessor.SpeedStatistics> estadisticas = 
            new HashMap<>();
        
        for (SpeedStatistics stats : results) {
            String key = stats.routeId + "-" + stats.origenStopId + "-" + stats.destinoStopId;
            estadisticas.put(key, new com.sitm.mio.common.DatagramProcessor.SpeedStatistics(
                stats.routeId, stats.origenStopId, stats.destinoStopId,
                stats.distancia, stats.tiempoPromedio, 
                stats.velocidadPromedio, stats.numMuestras
            ));
        }
        
        System.out.println("Procesamiento completado: " + estadisticas.size() + 
                         " velocidades calculadas");
        
        return estadisticas;
    }
    
    /**
     * Procesa un archivo de datagrams de forma distribuida
     * El Cliente lee el archivo localmente y envía los lotes al Master
     * (El Master NO requiere acceso al archivo)
     * 
     * @param filePath Ruta al archivo datagrams.csv (debe ser accesible desde el cliente)
     * @param grafo Grafo con nodos para cálculo de distancias
     * @param batchSize Tamaño de cada lote (recomendado: 500-2000)
     * @return Mapa de estadísticas de velocidad
     */
    public Map<String, com.sitm.mio.common.DatagramProcessor.SpeedStatistics> 
            processFileLocal(String filePath, com.sitm.mio.grafos.Grafo grafo, int batchSize) throws java.lang.Exception {
        
        // Convertir nodos del grafo a formato Ice
        Map<String, com.sitm.mio.common.GraphNode> nodos = GraphAdapter.convertirNodos(grafo);
        DatagramProcessing.GraphNode[] nodeList = convertirNodesToIce(nodos);
        
        System.out.println("Leyendo archivo localmente: " + filePath);
        System.out.println("El Master NO requiere acceso al archivo");
        
        // Contar total de lotes primero (para estimar)
        int totalBatches = estimarTotalBatches(filePath, batchSize);
        
        // Iniciar job en el Master
        String jobId = master.startJob(nodeList, totalBatches);
        System.out.println("Job ID: " + jobId);
        
        // Leer archivo y enviar lotes
        com.sitm.mio.common.DatagramProcessor.BatchCallback callback = (batch, num) -> {
            try {
                // Convertir Datagram comunes a Datagram de Ice
                DatagramProcessing.Datagram[] iceBatch = convertirDatagramsToIce(batch);
                
                // Enviar lote al Master
                boolean accepted = master.submitBatch(jobId, iceBatch, num);
                if (!accepted) {
                    throw new java.io.IOException("El Master rechazó el lote " + num);
                }
                
                if (num % 10 == 0) {
                    System.out.println("  Lotes enviados: " + (num + 1));
                }
            } catch (java.lang.Exception e) {
                throw new java.io.IOException("Error enviando lote " + num + ": " + e.getMessage(), e);
            }
        };
        
        // Cargar datagrams en lotes y enviarlos
        com.sitm.mio.common.DatagramProcessor.cargarDatagrams(filePath, batchSize, callback);
        
        // Marcar job como completado (todos los lotes enviados)
        System.out.println("Todos los lotes han sido enviados. Marcando job como completado...");
        master.completeJob(jobId);
        
        // Monitorear progreso
        int lastProgress = -1;
        while (true) {
            int progress = master.getJobProgress(jobId);
            
            if (progress == -1) {
                throw new java.lang.Exception("Job no encontrado: " + jobId);
            }
            
            if (progress != lastProgress) {
                System.out.println("Progreso: " + progress + "%");
                lastProgress = progress;
            }
            
            if (progress >= 100) {
                break;
            }
            
            Thread.sleep(2000); // Esperar 2 segundos antes de verificar de nuevo
        }
        
        // Obtener resultados
        System.out.println("Obteniendo resultados...");
        SpeedStatistics[] results = master.getJobResults(jobId);
        
        // Convertir a formato común
        Map<String, com.sitm.mio.common.DatagramProcessor.SpeedStatistics> estadisticas = 
            new HashMap<>();
        
        for (SpeedStatistics stats : results) {
            String key = stats.routeId + "-" + stats.origenStopId + "-" + stats.destinoStopId;
            estadisticas.put(key, new com.sitm.mio.common.DatagramProcessor.SpeedStatistics(
                stats.routeId, stats.origenStopId, stats.destinoStopId,
                stats.distancia, stats.tiempoPromedio, 
                stats.velocidadPromedio, stats.numMuestras
            ));
        }
        
        System.out.println("Procesamiento completado: " + estadisticas.size() + 
                         " velocidades calculadas");
        
        return estadisticas;
    }
    
    /**
     * Estima el número total de lotes basándose en el tamaño del archivo
     */
    private int estimarTotalBatches(String filePath, int batchSize) throws java.io.IOException {
        java.io.File file = new java.io.File(filePath);
        if (!file.exists()) {
            // Si no existe localmente, podría ser una ruta de red
            // Retornar una estimación conservadora
            return 1000; // Estimación por defecto
        }
        
        long fileSize = file.length();
        // Estimar ~100 bytes por línea (muy aproximado)
        long estimatedLines = fileSize / 100;
        int estimatedBatches = (int)(estimatedLines / batchSize) + 1;
        
        return Math.max(estimatedBatches, 1);
    }
    
    /**
     * Convierte lista de Datagram comunes a array de Datagram de Ice
     */
    private DatagramProcessing.Datagram[] convertirDatagramsToIce(
            List<DatagramProcessor.Datagram> datagrams) {
        DatagramProcessing.Datagram[] iceDatagrams = new DatagramProcessing.Datagram[datagrams.size()];
        
        for (int i = 0; i < datagrams.size(); i++) {
            DatagramProcessor.Datagram dg = datagrams.get(i);
            DatagramProcessing.Datagram iceDg = new DatagramProcessing.Datagram();
            iceDg.busId = dg.getBusId();
            iceDg.routeId = dg.getRouteId();
            iceDg.stopId = dg.getStopId();
            iceDg.latitude = dg.getLatitude();
            iceDg.longitude = dg.getLongitude();
            iceDg.timestamp = dg.getTimestamp();
            iceDg.sequenceNumber = dg.getSequence();
            iceDatagrams[i] = iceDg;
        }
        
        return iceDatagrams;
    }
    
    /**
     * Convierte mapa de GraphNode común a GraphNode[] de Ice
     */
    private DatagramProcessing.GraphNode[] convertirNodesToIce(Map<String, com.sitm.mio.common.GraphNode> nodos) {
        List<DatagramProcessing.GraphNode> list = new ArrayList<>();
        for (com.sitm.mio.common.GraphNode node : nodos.values()) {
            // Crear GraphNode de DatagramProcessing (Ice) - los campos son públicos
            DatagramProcessing.GraphNode iceNode = new DatagramProcessing.GraphNode();
            iceNode.id = node.getId();
            iceNode.nombre = node.getNombre();
            iceNode.tipo = node.getTipo();
            iceNode.longitud = node.getLongitud();
            iceNode.latitud = node.getLatitud();
            list.add(iceNode);
        }
        return list.toArray(new DatagramProcessing.GraphNode[0]);
    }
    
    /**
     * Verifica si hay workers disponibles
     */
    public int getWorkerCount() {
        try {
            return master.getWorkerCount();
        } catch (java.lang.Exception e) {
            return 0;
        }
    }
    
    /**
     * Cierra la conexión
     */
    public void close() {
        if (communicator != null) {
            communicator.destroy();
        }
    }
}

