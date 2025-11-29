module SITMMIO {
    
    // Estructuras de datos propias (independientes de otros archivos .ice)
    struct GraphNode {
        string id;
        string nombre;
        string tipo;
        double longitud;
        double latitud;
    };
    
    struct GraphEdge {
        string origenId;
        string destinoId;
        string ruta;
        double distancia;
        double tiempoPromedio;
        double velocidadPromedio;
    };
    
    sequence<GraphNode> GraphNodeList;
    sequence<GraphEdge> GraphEdgeList;
    
    // Excepciones
    exception NodeNotFoundException {
        string nodeId;
    };
    
    exception RouteNotFoundException {
        string routeId;
    };
    
    // Interfaz del servicio de grafos
    interface GraphService {
        // Obtener todos los nodos
        GraphNodeList obtenerTodosLosNodos();
        
        // Obtener un nodo por ID
        GraphNode obtenerNodo(string nodeId) throws NodeNotFoundException;
        
        // Obtener todos los arcos
        GraphEdgeList obtenerTodosLosArcos();
        
        // Obtener arcos de una ruta específica
        GraphEdgeList obtenerArcosPorRuta(string routeId) throws RouteNotFoundException;
        
        // Obtener estadísticas del grafo
        int obtenerNumeroNodos();
        int obtenerNumeroArcos();
    };
};


