package com.sitm.mio.common;

import com.sitm.mio.grafos.Grafo;
import com.sitm.mio.grafos.Nodo;
import com.sitm.mio.grafos.Arco;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adaptador para convertir entre el modelo del grafo existente y los modelos comunes
 * Patrón: Adapter
 */
public class GraphAdapter {
    
    /**
     * Convierte un Grafo del proyecto original a una representación común
     */
    public static Map<String, GraphNode> convertirNodos(Grafo grafo) {
        Map<String, GraphNode> nodosComunes = new HashMap<>();
        
        for (Nodo nodo : grafo.obtenerNodos()) {
            GraphNode graphNode = new GraphNode(
                nodo.getId(),
                nodo.getNombre(),
                nodo.getTipo(),
                nodo.getLongitud(),
                nodo.getLatitud()
            );
            nodosComunes.put(nodo.getId(), graphNode);
        }
        
        return nodosComunes;
    }
    
    /**
     * Convierte los arcos del grafo a GraphEdge con distancias calculadas
     * 
     * @param grafo Grafo original
     * @param nodosComunes Mapa de nodos comunes
     * @param velocidadesReales Mapa opcional de velocidades reales calculadas desde datagrams
     *                          (key: "routeId-origenId-destinoId", value: SpeedStatistics)
     */
    public static List<GraphEdge> convertirArcos(Grafo grafo, Map<String, GraphNode> nodosComunes,
                                                   Map<String, DatagramProcessor.SpeedStatistics> velocidadesReales) {
        List<GraphEdge> edges = new ArrayList<>();
        Map<String, GraphNode> nodosMap = nodosComunes;
        
        for (Arco arco : grafo.obtenerArcos()) {
            String origenId = arco.getOrigen().getId();
            String destinoId = arco.getDestino().getId();
            
            GraphNode origen = nodosMap.get(origenId);
            GraphNode destino = nodosMap.get(destinoId);
            
            if (origen != null && destino != null) {
                // Calcular distancia si no está disponible
                double distancia = arco.getDistancia();
                if (distancia <= 0) {
                    distancia = DistanceCalculator.calcularDistancia(origen, destino);
                }
                
                // Calcular tiempo: priorizar datos reales, luego estimados
                double tiempo = arco.getTiempoPromedio();
                
                if (tiempo <= 0 && distancia > 0) {
                    // Buscar velocidad real en datos de datagrams
                    String claveArco = generarClaveArco(arco.getRuta(), origenId, destinoId);
                    DatagramProcessor.SpeedStatistics stats = null;
                    
                    if (velocidadesReales != null) {
                        stats = velocidadesReales.get(claveArco);
                    }
                    
                    if (stats != null && stats.getTiempoPromedio() > 0) {
                        // Usar tiempo real calculado desde datagrams
                        tiempo = stats.getTiempoPromedio();
                        System.out.println("Usando tiempo real para " + claveArco + 
                                         ": " + String.format("%.2f", tiempo) + " min " +
                                         "(basado en " + stats.getNumMuestras() + " muestras)");
                    } else {
                        // Usar estimador de velocidad basado en contexto de la ruta
                        tiempo = VelocityEstimator.calcularTiempoEstimado(arco.getRuta(), distancia);
                    }
                }
                
                GraphEdge edge = new GraphEdge(origenId, destinoId, arco.getRuta(), 
                                              distancia, tiempo);
                edges.add(edge);
            }
        }
        
        return edges;
    }
    
    /**
     * Convierte los arcos del grafo a GraphEdge (método sin velocidades reales, para compatibilidad)
     */
    public static List<GraphEdge> convertirArcos(Grafo grafo, Map<String, GraphNode> nodosComunes) {
        return convertirArcos(grafo, nodosComunes, null);
    }
    
    /**
     * Genera una clave única para un arco
     */
    private static String generarClaveArco(String routeId, String origenId, String destinoId) {
        return routeId + "-" + origenId + "-" + destinoId;
    }
}


