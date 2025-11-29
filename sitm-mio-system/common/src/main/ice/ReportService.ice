module SITMMIO {
    
    // Alias de tipo para lista de strings
    sequence<string> StringList;
    
    struct RouteReport {
        string routeId;
        string nombre;
        int numeroArcos;
        double distanciaTotal;
        double tiempoTotal;
        double velocidadPromedio;
        StringList paradas; // IDs de paradas en orden
    };
    
    struct ArcReport {
        string origenId;
        string origenNombre;
        string destinoId;
        string destinoNombre;
        string ruta;
        int secuencia;
        double distancia;
        double tiempo;
        double velocidad;
    };
    
    // Definir sequences antes de usarlas
    sequence<RouteReport> RouteReportList;
    sequence<ArcReport> ArcReportList;
    
    struct SystemReport {
        int totalNodos;
        int totalArcos;
        int totalRutas;
        double velocidadPromedioGeneral;
        RouteReportList reportesPorRuta;
    };
    
    exception RouteNotFoundException {
        string routeId;
    };
    
    interface ReportService {
        // Generar reporte completo del sistema
        SystemReport generarReporteCompleto();
        
        // Generar reporte de una ruta específica
        RouteReport generarReporteRuta(string routeId) throws RouteNotFoundException;
        
        // Generar reporte de arcos de una ruta
        ArcReportList generarReporteArcosRuta(string routeId) throws RouteNotFoundException;
        
        // Exportar reporte a texto
        string exportarReporteTexto(string routeId) throws RouteNotFoundException;
    };
};


