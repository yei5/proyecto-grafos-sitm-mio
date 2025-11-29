package com.sitm.mio.grafos;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Clase que representa un grafo dirigido para el sistema SITM-MIO
 */
public class Grafo {
    private Map<String, Nodo> nodos;
    private List<Arco> arcos;
    private Map<String, List<Arco>> listaAdyacencia;
    
    
    /**
     * Constructor de la clase Grafo
     */
    public Grafo() {
        this.nodos = new HashMap<>();
        this.arcos = new ArrayList<>();
        this.listaAdyacencia = new HashMap<>();
    }
    
    /**
     * Agrega un nodo al grafo
     * @param nodo Nodo a agregar
     */
    public void agregarNodo(Nodo nodo) {
        nodos.put(nodo.getId(), nodo);
        listaAdyacencia.putIfAbsent(nodo.getId(), new ArrayList<>());
    }
    
    /**
     * Agrega un arco al grafo, evitando duplicados dentro de la misma ruta
     * Un arco es duplicado si tiene el mismo origen, destino y ruta
     * @param arco Arco a agregar
     */
    public void agregarArco(Arco arco) {
        if (!nodos.containsKey(arco.getOrigen().getId())) {
            agregarNodo(arco.getOrigen());
        }
        if (!nodos.containsKey(arco.getDestino().getId())) {
            agregarNodo(arco.getDestino());
        }
        
        // Verificar si ya existe un arco con el mismo origen, destino y ruta
        String origenId = arco.getOrigen().getId();
        String destinoId = arco.getDestino().getId();
        String ruta = arco.getRuta();
        
        // Solo agregar si no existe un arco duplicado en la misma ruta
        // (diferentes rutas pueden tener el mismo arco, eso está permitido)
        boolean esDuplicado = false;
        List<Arco> arcosOrigen = listaAdyacencia.get(origenId);
        if (arcosOrigen != null) {
            for (Arco arcoExistente : arcosOrigen) {
                if (arcoExistente.getDestino().getId().equals(destinoId) && 
                    arcoExistente.getRuta().equals(ruta)) {
                    esDuplicado = true;
                    break;
                }
            }
        }
        
        if (!esDuplicado) {
            arcos.add(arco);
            listaAdyacencia.get(origenId).add(arco);
        }
    }
    
    /**
     * Obtiene un nodo por su ID
     * @param id Identificador del nodo
     * @return Nodo encontrado o null si no existe
     */
    public Nodo obtenerNodo(String id) {
        return nodos.get(id);
    }
    
    /**
     * Obtiene todos los nodos del grafo
     * @return Colección de nodos
     */
    public Collection<Nodo> obtenerNodos() {
        return nodos.values();
    }
    
    /**
     * Obtiene todos los arcos del grafo
     * @return Lista de arcos
     */
    public List<Arco> obtenerArcos() {
        return new ArrayList<>(arcos);
    }
    
    /**
     * Obtiene los arcos adyacentes a un nodo
     * @param nodoId Identificador del nodo
     * @return Lista de arcos adyacentes
     */
    public List<Arco> obtenerArcosAdyacentes(String nodoId) {
        return listaAdyacencia.getOrDefault(nodoId, new ArrayList<>());
    }
    
    /**
     * Calcula la velocidad promedio de todos los arcos
     * @return Velocidad promedio general
     */
    public double calcularVelocidadPromedioGeneral() {
        if (arcos.isEmpty()) {
            return 0.0;
        }
        
        double sumaVelocidades = 0.0;
        for (Arco arco : arcos) {
            sumaVelocidades += arco.getVelocidadPromedio();
        }
        
        return sumaVelocidades / arcos.size();
    }
    
    /**
     * Calcula la velocidad promedio de los arcos de una ruta específica
     * @param rutaId Identificador de la ruta
     * @return Velocidad promedio de la ruta
     */
    public double calcularVelocidadPromedioPorRuta(String rutaId) {
        List<Arco> arcosRuta = new ArrayList<>();
        for (Arco arco : arcos) {
            if (arco.getRuta().equals(rutaId)) {
                arcosRuta.add(arco);
            }
        }
        
        if (arcosRuta.isEmpty()) {
            return 0.0;
        }
        
        double sumaVelocidades = 0.0;
        for (Arco arco : arcosRuta) {
            sumaVelocidades += arco.getVelocidadPromedio();
        }
        
        return sumaVelocidades / arcosRuta.size();
    }
    
    /**
     * Obtiene estadísticas del grafo
     * @return String con estadísticas
     */
    public String obtenerEstadisticas() {
        StringBuilder stats = new StringBuilder();
        stats.append("=== ESTADÍSTICAS DEL GRAFO SITM-MIO ===\n");
        stats.append("Total de nodos: ").append(nodos.size()).append("\n");
        stats.append("Total de arcos: ").append(arcos.size()).append("\n");
        stats.append("Velocidad promedio general: ")
              .append(String.format("%.2f", calcularVelocidadPromedioGeneral()))
              .append(" km/h\n");
        return stats.toString();
    }
    
    /**
     * Obtiene el número de nodos en el grafo
     * @return Número de nodos
     */
    public int getNumeroNodos() {
        return nodos.size();
    }
    
    /**
     * Obtiene el número de arcos en el grafo
     * @return Número de arcos
     */
    public int getNumeroArcos() {
        return arcos.size();
    }
    
    /**
     * Obtiene los arcos agrupados y ordenados por ruta
     * @return Mapa con ruta como clave y lista de arcos ordenados como valor
     */
    public Map<String, List<Arco>> obtenerArcosPorRuta() {
        Map<String, List<Arco>> arcosPorRuta = new TreeMap<>();
        
        for (Arco arco : arcos) {
            arcosPorRuta.putIfAbsent(arco.getRuta(), new ArrayList<>());
            arcosPorRuta.get(arco.getRuta()).add(arco);
        }
        
        return arcosPorRuta;
    }
    
    /**
     * Obtiene una representación en texto de los arcos ordenados por ruta y secuencia
     * @return String con la lista de arcos ordenados
     */
    public String obtenerArcosOrdenadosPorRuta() {
        StringBuilder resultado = new StringBuilder();
        resultado.append("=== ARCOS ORDENADOS POR RUTA Y SECUENCIA ===\n\n");
        
        Map<String, List<Arco>> arcosPorRuta = obtenerArcosPorRuta();
        
        for (Map.Entry<String, List<Arco>> entrada : arcosPorRuta.entrySet()) {
            String ruta = entrada.getKey();
            List<Arco> arcosRuta = entrada.getValue();
            
            resultado.append("Ruta: ").append(ruta).append("\n");
            resultado.append("Número de arcos: ").append(arcosRuta.size()).append("\n");
            resultado.append("Arcos en secuencia:\n");
            
            int numeroArco = 1;
            for (Arco arco : arcosRuta) {
                resultado.append("  ").append(numeroArco).append(". ")
                        .append(arco.getOrigen().getNombre())
                        .append(" (").append(arco.getOrigen().getId()).append(")")
                        .append(" -> ")
                        .append(arco.getDestino().getNombre())
                        .append(" (").append(arco.getDestino().getId()).append(")");
                
                if (arco.getDistancia() > 0 || arco.getTiempoPromedio() > 0) {
                    resultado.append(" [Distancia: ").append(String.format("%.2f", arco.getDistancia()))
                            .append(" km, Tiempo: ").append(String.format("%.2f", arco.getTiempoPromedio()))
                            .append(" min, Velocidad: ").append(String.format("%.2f", arco.getVelocidadPromedio()))
                            .append(" km/h]");
                }
                
                resultado.append("\n");
                numeroArco++;
            }
            resultado.append("\n");
        }
        
        return resultado.toString();
    }


    public void exportarGrafoComoTexto(String rutaArchivo) {
    try (PrintWriter pw = new PrintWriter(new FileWriter(rutaArchivo))) {

        pw.println("=== GRAFO SITM-MIO ===");
        pw.println("\nNodos:");
        for (Nodo nodo : nodos.values()) {
            pw.println(nodo.getId() + " - " + nodo.getNombre());
        }

        pw.println("\nArcos:");
        for (Arco arco : arcos) {
            pw.println(
                arco.getOrigen().getId() + " -> " +
                arco.getDestino().getId() +
                " | Ruta: " + arco.getRuta() +
                " | Distancia: " + arco.getDistancia()
            );
        }

        System.out.println("Archivo exportado correctamente en: " + rutaArchivo);

    } catch (IOException e) {
        System.err.println("Error al exportar grafo: " + e.getMessage());
    }
}


}

