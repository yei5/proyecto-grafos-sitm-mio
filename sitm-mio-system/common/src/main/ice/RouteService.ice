module SITMMIO {
    
    // RouteSummary (también definido en GraphService.ice para compatibilidad)
    struct RouteSummary {
        string rutaId;
        int numeroArcos;
        double distanciaTotal;
        double tiempoTotal;
        double velocidadPromedio;
    };
    
    struct RouteInfo {
        string routeId;
        string nombre;
        string descripcion;
        int numeroParadas;
        double distanciaTotal;
        double tiempoTotal;
        double velocidadPromedio;
    };
    
    struct ArcDetail {
        string origenId;
        string origenNombre;
        string destinoId;
        string destinoNombre;
        int secuencia;
        double distancia;
        double tiempo;
        double velocidad;
    };
    
    sequence<RouteInfo> RouteInfoList;
    sequence<ArcDetail> ArcDetailList;
    
    exception RouteNotFoundException {
        string routeId;
    };
    
    interface RouteService {
        // Obtener información de todas las rutas
        RouteInfoList obtenerTodasLasRutas();
        
        // Obtener información de una ruta específica
        RouteInfo obtenerInformacionRuta(string routeId) throws RouteNotFoundException;
        
        // Obtener detalles de arcos de una ruta
        ArcDetailList obtenerArcosDeRuta(string routeId) throws RouteNotFoundException;
        
        // Obtener resumen de una ruta
        RouteSummary obtenerResumenRuta(string routeId) throws RouteNotFoundException;
    };
};


