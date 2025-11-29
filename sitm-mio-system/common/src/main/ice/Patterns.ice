module SITMMIO {
    
    // Tipos compartidos (también definidos en GraphService.ice para compatibilidad)
    // Estos tipos deben estar definidos antes de usarlos en las interfaces
    struct Node {
        string id;
        string nombre;
        string tipo;
        double longitud;
        double latitud;
    };
    
    struct Edge {
        string origenId;
        string destinoId;
        string ruta;
        double distancia;
        double tiempoPromedio;
        double velocidadPromedio;
    };
    
    sequence<Node> NodeList;
    sequence<Edge> EdgeList;
    
    // ============================================
    // PATRÓN: Publisher-Subscriber
    // ============================================
    
    interface GraphEventSubscriber {
        void onGraphUpdated(string eventType, string routeId);
        void onAnalysisComplete(string analysisId, double result);
    };
    
    interface GraphEventPublisher {
        void subscribe(GraphEventSubscriber* subscriber);
        void unsubscribe(GraphEventSubscriber* subscriber);
        void publishGraphUpdate(string eventType, string routeId);
    };
    
    // ============================================
    // PATRÓN: Master-Worker
    // ============================================
    
    struct WorkTask {
        string taskId;
        string routeId;
        string taskType; // "ANALYSIS", "REPORT", "VELOCITY"
        int priority;
    };
    
    struct WorkResult {
        string taskId;
        bool success;
        string resultData;
        double processingTime;
    };
    
    sequence<WorkTask> WorkTaskList;
    sequence<WorkResult> WorkResultList;
    
    interface Worker {
        string getWorkerId();
        bool isAvailable();
        WorkResult processTask(WorkTask task);
        void shutdown();
    };
    
    interface Master {
        string registerWorker(Worker* worker);
        void submitTask(WorkTask task);
        WorkResult getResult(string taskId);
        WorkResultList getAllResults();
        int getWorkerCount();
    };
    
    // ============================================
    // PATRÓN: Broker
    // ============================================
    
    struct ServiceInfo {
        string serviceName;
        string endpoint;
        string serviceType;
        int load;
    };
    
    sequence<ServiceInfo> ServiceInfoList;
    
    interface ServiceBroker {
        string routeRequest(string serviceType, string requestData);
        ServiceInfoList getAvailableServices(string serviceType);
        void registerService(ServiceInfo info);
        void unregisterService(string serviceName);
    };
    
    // ============================================
    // PATRÓN: Reliable Messaging
    // ============================================
    
    struct Message {
        string messageId;
        string senderId;
        string receiverId;
        string messageType;
        string payload;
        long timestamp;
        int retryCount;
    };
    
    sequence<Message> MessageList;
    
    interface ReliableMessaging {
        void sendMessage(Message msg);
        Message receiveMessage(string receiverId);
        void acknowledgeMessage(string messageId);
        MessageList getPendingMessages(string receiverId);
    };
    
    // ============================================
    // PATRÓN: Producer-Consumer
    // ============================================
    
    struct ReportJob {
        string jobId;
        string routeId;
        string reportType;
        string status; // "PENDING", "PROCESSING", "COMPLETED", "FAILED"
    };
    
    sequence<ReportJob> ReportJobList;
    
    interface ReportProducer {
        string createReportJob(string routeId, string reportType);
        ReportJob getJobStatus(string jobId);
        ReportJobList getPendingJobs();
    };
    
    interface ReportConsumer {
        ReportJob consumeJob();
        void completeJob(string jobId, string result);
        void failJob(string jobId, string error);
    };
    
    // ============================================
    // PATRÓN: Proxy (Virtual Proxy)
    // ============================================
    // Nota: Usa tipos básicos para evitar dependencias entre archivos .ice
    
    interface GraphProxy {
        // Retorna datos como strings para evitar dependencias
        string obtenerNodosJson();
        string obtenerArcosJson();
        string obtenerArcosPorRutaJson(string routeId);
        // Métodos lazy-loading
        bool isDataLoaded();
        void preloadData();
    };
    
    // ============================================
    // PATRÓN: Proxy-Cache
    // ============================================
    
    interface CachedGraphService {
        // Retorna datos como strings para evitar dependencias
        string obtenerNodosCachedJson();
        string obtenerArcosCachedJson();
        void invalidateCache();
        void setCacheTimeout(int seconds);
    };
};


