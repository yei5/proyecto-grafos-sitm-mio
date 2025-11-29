module SITMMIO {
    
    struct VelocityAnalysis {
        string routeId;
        double velocidadPromedio;
        double velocidadMaxima;
        double velocidadMinima;
        int numeroArcos;
    };
    
    struct ArcVelocity {
        string origenId;
        string destinoId;
        string ruta;
        double distancia;
        double tiempo;
        double velocidad;
    };
    
    sequence<VelocityAnalysis> VelocityAnalysisList;
    sequence<ArcVelocity> ArcVelocityList;
    
    exception RouteNotFoundException {
        string routeId;
    };
    
    interface AnalysisService {
        // Calcular velocidad promedio general
        double calcularVelocidadPromedioGeneral();
        
        // Calcular velocidad promedio por ruta
        double calcularVelocidadPromedioRuta(string routeId) throws RouteNotFoundException;
        
        // Obtener análisis de velocidades por ruta
        VelocityAnalysisList obtenerAnalisisVelocidadesPorRuta();
        
        // Obtener arco más rápido
        ArcVelocity obtenerArcoMasRapido();
        
        // Obtener arco más lento
        ArcVelocity obtenerArcoMasLento();
        
        // Obtener velocidades de todos los arcos de una ruta
        ArcVelocityList obtenerVelocidadesArcosRuta(string routeId) throws RouteNotFoundException;
    };
};



