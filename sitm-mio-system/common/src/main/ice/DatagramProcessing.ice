module DatagramProcessing {
    
    /**
     * Representa un datagram (registro GPS de bus)
     */
    struct Datagram {
        string busId;
        string routeId;
        string stopId;
        double latitude;
        double longitude;
        long timestamp;
        int sequenceNumber;
    };
    
    /**
     * Lote de datagrams para procesamiento distribuido
     */
    sequence<Datagram> DatagramBatch;
    
    /**
     * Estadísticas de velocidad calculadas
     */
    struct SpeedStatistics {
        string routeId;
        string origenStopId;
        string destinoStopId;
        double distancia;
        double tiempoPromedio;
        double velocidadPromedio;
        int numMuestras;
    };
    
    /**
     * Lista de estadísticas de velocidad
     */
    sequence<SpeedStatistics> SpeedStatisticsList;
    
    /**
     * Resultado del procesamiento de un lote
     */
    struct BatchResult {
        string batchId;
        SpeedStatisticsList statistics;
        int processedCount;
        double processingTime;
        bool success;
        string errorMessage;
    };
    
    /**
     * Nodo del grafo para cálculo de distancias
     */
    struct GraphNode {
        string id;
        string nombre;
        string tipo;
        double longitud;
        double latitud;
    };
    
    // Alias de tipo para GraphNodeArray (array)
    sequence<GraphNode> GraphNodeArray;
    
    /**
     * Servicio Worker para procesar lotes de datagrams
     */
    interface DatagramWorker {
        /**
         * Procesa un lote de datagrams y retorna estadísticas de velocidad
         * @param batch Lote de datagrams a procesar
         * @param nodes Lista de nodos del grafo para cálculo de distancias
         * @return Resultado del procesamiento
         */
        BatchResult processBatch(DatagramBatch batch, GraphNodeArray nodes);
        
        /**
         * Verifica si el worker está disponible
         */
        bool isAvailable();
        
        /**
         * Obtiene el ID del worker
         */
        string getWorkerId();
        
        /**
         * Obtiene estadísticas del worker (carga, tareas procesadas, etc.)
         */
        string getStatistics();
    };
    
    /**
     * Servicio Master para coordinar procesamiento distribuido
     */
    interface DatagramMaster {
        /**
         * Registra un worker en el sistema
         */
        string registerWorker(DatagramWorker* worker);
        
        /**
         * Desregistra un worker
         */
        void unregisterWorker(string workerId);
        
        /**
         * Procesa un archivo completo de datagrams de forma distribuida
         * @param filePath Ruta al archivo datagrams.csv
         * @param nodes Lista de nodos del grafo
         * @param batchSize Tamaño de cada lote
         * @return ID del job de procesamiento
         */
        string processFile(string filePath, GraphNodeArray nodes, int batchSize);
        
        /**
         * Inicia un nuevo job de procesamiento (sin leer archivo)
         * El cliente enviará los lotes directamente usando submitBatch
         * @param nodes Lista de nodos del grafo
         * @param totalBatches Número total de lotes que se enviarán
         * @return ID del job de procesamiento
         */
        string startJob(GraphNodeArray nodes, int totalBatches);
        
        /**
         * Envía un lote de datagrams para procesamiento
         * @param jobId ID del job iniciado con startJob
         * @param batch Lote de datagrams a procesar
         * @param batchNumber Número de lote (0-based)
         * @return true si el lote fue aceptado
         */
        bool submitBatch(string jobId, DatagramBatch batch, int batchNumber);
        
        /**
         * Marca un job como completado (todos los lotes han sido enviados)
         * @param jobId ID del job
         */
        void completeJob(string jobId);
        
        /**
         * Obtiene el progreso de un job
         * @param jobId ID del job
         * @return Porcentaje completado (0-100)
         */
        int getJobProgress(string jobId);
        
        /**
         * Obtiene los resultados de un job completado
         * @param jobId ID del job
         * @return Lista de estadísticas de velocidad
         */
        SpeedStatisticsList getJobResults(string jobId);
        
        /**
         * Obtiene el número de workers disponibles
         */
        int getWorkerCount();
    };
};

