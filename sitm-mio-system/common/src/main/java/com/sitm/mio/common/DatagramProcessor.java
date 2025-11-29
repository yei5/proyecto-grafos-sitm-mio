package com.sitm.mio.common;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Procesador de datos de datagrams (GPS de buses)
 * Calcula velocidades reales basadas en datos de tracking de buses
 */
public class DatagramProcessor {
    
    /**
     * Representa un datagram (registro de posición de bus)
     */
    public static class Datagram {
        private String busId;
        private String routeId;
        private String stopId;
        private double latitude;
        private double longitude;
        private long timestamp; // Timestamp en milisegundos
        private int sequence; // Secuencia en la ruta
        
        public Datagram(String busId, String routeId, String stopId,
                       double latitude, double longitude, long timestamp, int sequenceNumber) {
            this.busId = busId;
            this.routeId = routeId;
            this.stopId = stopId;
            this.latitude = latitude;
            this.longitude = longitude;
            this.timestamp = timestamp;
            this.sequence = sequenceNumber;
        }
        
        // Getters
        public String getBusId() { return busId; }
        public String getRouteId() { return routeId; }
        public String getStopId() { return stopId; }
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public long getTimestamp() { return timestamp; }
        public int getSequence() { return sequence; }
    }
    
    /**
     * Representa estadísticas de velocidad entre dos paradas
     */
    public static class SpeedStatistics {
        private String routeId;
        private String origenStopId;
        private String destinoStopId;
        private double distancia; // km
        private double tiempoPromedio; // minutos
        private double velocidadPromedio; // km/h
        private int numMuestras; // Número de mediciones
        
        public SpeedStatistics(String routeId, String origenStopId, String destinoStopId,
                             double distancia, double tiempoPromedio, double velocidadPromedio, int numMuestras) {
            this.routeId = routeId;
            this.origenStopId = origenStopId;
            this.destinoStopId = destinoStopId;
            this.distancia = distancia;
            this.tiempoPromedio = tiempoPromedio;
            this.velocidadPromedio = velocidadPromedio;
            this.numMuestras = numMuestras;
        }
        
        // Getters
        public String getRouteId() { return routeId; }
        public String getOrigenStopId() { return origenStopId; }
        public String getDestinoStopId() { return destinoStopId; }
        public double getDistancia() { return distancia; }
        public double getTiempoPromedio() { return tiempoPromedio; }
        public double getVelocidadPromedio() { return velocidadPromedio; }
        public int getNumMuestras() { return numMuestras; }
    }
    
    /**
     * Carga datagrams desde un archivo CSV de forma eficiente usando streaming
     * OPTIMIZACIÓN: Lee línea por línea para no cargar todo en memoria
     * 
     * @param rutaArchivo Ruta al archivo datagrams.csv
     * @param batchSize Tamaño del lote para procesamiento (0 = cargar todo)
     * @param batchCallback Callback para procesar cada lote (null = cargar todo)
     * @return Lista de datagrams cargados (solo si batchCallback es null)
     */
    public static List<Datagram> cargarDatagrams(String rutaArchivo, int batchSize, 
                                                   BatchCallback batchCallback) throws IOException {
        if (batchSize > 0 && batchCallback != null) {
            // Modo streaming: procesar en lotes
            cargarDatagramsEnLotes(rutaArchivo, batchSize, batchCallback);
            return new ArrayList<>(); // Retornar lista vacía en modo streaming
        } else {
            // Modo tradicional: cargar todo en memoria
            return cargarDatagramsCompleto(rutaArchivo);
        }
    }
    
    /**
     * Carga datagrams desde un archivo CSV (método tradicional - compatibilidad)
     */
    public static List<Datagram> cargarDatagrams(String rutaArchivo) throws IOException {
        return cargarDatagramsCompleto(rutaArchivo);
    }
    
    /**
     * Interfaz para callback de procesamiento por lotes
     */
    public interface BatchCallback {
        void processBatch(List<Datagram> batch, int batchNumber) throws IOException;
    }
    
    /**
     * Carga datagrams en lotes usando streaming (OPTIMIZADO para grandes archivos)
     */
    private static void cargarDatagramsEnLotes(String rutaArchivo, int batchSize, 
                                                 BatchCallback callback) throws IOException {
        List<Datagram> currentBatch = new ArrayList<>(batchSize);
        int batchNumber = 0;
        
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(rutaArchivo), StandardCharsets.UTF_8))) {
            
            String primeraLinea = br.readLine();
            if (primeraLinea == null) {
                throw new IOException("El archivo datagrams.csv está vacío");
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
                
                // Validar columnas requeridas
                if (!indices.containsKey("bus_id") || !indices.containsKey("route_id") || 
                    !indices.containsKey("stop_id") || !indices.containsKey("timestamp")) {
                    throw new IOException("El archivo datagrams.csv no tiene las columnas requeridas. " +
                                        "Esperadas: busId (o bus_id), lineId (o line_id), stopId (o stop_id), " +
                                        "datagramDate (o timestamp). " +
                                        "Columnas encontradas: " + String.join(", ", primeraLineaCampos));
                }
            } else {
                // No hay encabezados, usar orden predefinido
                System.out.println("⚠ Archivo sin encabezados detectado. Usando orden predefinido de columnas.");
                indices = crearIndicesSinEncabezados(primeraLineaCampos.length);
                lineaDatos = primeraLinea; // La primera línea ya es un dato
            }
            
            // Leer datos en lotes
            // Si no hay encabezados, ya tenemos la primera línea como dato
            if (lineaDatos != null) {
                String[] campos = parsearCSV(lineaDatos);
                procesarLineaComoDato(campos, indices, currentBatch, batchSize, callback, batchNumber++);
            }
            
            String linea;
            while ((linea = br.readLine()) != null) {
                if (linea.trim().isEmpty()) continue;
                
                String[] campos = parsearCSV(linea);
                procesarLineaComoDato(campos, indices, currentBatch, batchSize, callback, batchNumber);
                if (currentBatch.size() >= batchSize) {
                    batchNumber++;
                }
            }
            
            // Procesar último lote si no está vacío
            if (!currentBatch.isEmpty()) {
                callback.processBatch(currentBatch, batchNumber);
            }
        }
    }
    
    /**
     * Procesa una línea como dato (helper para evitar duplicación)
     */
    private static void procesarLineaComoDato(String[] campos, Map<String, Integer> indices,
                                               List<Datagram> currentBatch, int batchSize,
                                               BatchCallback callback, int batchNumber) {
        try {
            Datagram dg = parsearDatagram(campos, indices);
            if (dg != null) {
                currentBatch.add(dg);
                
                // Procesar lote cuando alcance el tamaño
                if (currentBatch.size() >= batchSize) {
                    callback.processBatch(new ArrayList<>(currentBatch), batchNumber);
                    currentBatch.clear();
                }
            }
        } catch (Exception e) {
            // Ignorar líneas con errores
        }
    }
    
    /**
     * Carga todos los datagrams en memoria (método tradicional)
     */
    private static List<Datagram> cargarDatagramsCompleto(String rutaArchivo) throws IOException {
        List<Datagram> datagrams = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(rutaArchivo), StandardCharsets.UTF_8))) {
            
            String primeraLinea = br.readLine();
            if (primeraLinea == null) {
                throw new IOException("El archivo datagrams.csv está vacío");
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
                
                // Validar columnas requeridas
                if (!indices.containsKey("bus_id") || !indices.containsKey("route_id") || 
                    !indices.containsKey("stop_id") || !indices.containsKey("timestamp")) {
                    throw new IOException("El archivo datagrams.csv no tiene las columnas requeridas. " +
                                        "Esperadas: busId (o bus_id), lineId (o line_id), stopId (o stop_id), " +
                                        "datagramDate (o timestamp). " +
                                        "Columnas encontradas: " + String.join(", ", primeraLineaCampos));
                }
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
                        datagrams.add(dg);
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
                        datagrams.add(dg);
                    }
                } catch (Exception e) {
                    // Ignorar líneas con errores
                }
            }
        }
        
        return datagrams;
    }
    
    /**
     * Calcula velocidades promedio entre paradas basadas en datos reales de datagrams
     * OPTIMIZADO: Usa Fork/Join para procesamiento paralelo en múltiples cores
     * 
     * @param datagrams Lista de datagrams cargados
     * @param nodos Mapa de nodos (paradas) con coordenadas
     * @return Mapa de estadísticas de velocidad por arco (origen-destino-ruta)
     */
    public static Map<String, SpeedStatistics> calcularVelocidadesReales(
            List<Datagram> datagrams, Map<String, GraphNode> nodos) {
        
        // Para grandes volúmenes, usar procesamiento paralelo
        if (datagrams.size() > 1000) {
            return calcularVelocidadesRealesParalelo(datagrams, nodos);
        } else {
            return calcularVelocidadesRealesSecuencial(datagrams, nodos);
        }
    }
    
    /**
     * Calcula velocidades usando Fork/Join para procesamiento paralelo
     */
    private static Map<String, SpeedStatistics> calcularVelocidadesRealesParalelo(
            List<Datagram> datagrams, Map<String, GraphNode> nodos) {
        
        // Dividir datagrams en chunks para procesamiento paralelo
        int chunkSize = Math.max(100, datagrams.size() / (Runtime.getRuntime().availableProcessors() * 2));
        List<List<Datagram>> chunks = new ArrayList<>();
        
        for (int i = 0; i < datagrams.size(); i += chunkSize) {
            int end = Math.min(i + chunkSize, datagrams.size());
            chunks.add(datagrams.subList(i, end));
        }
        
        // Procesar chunks en paralelo
        Map<String, SpeedStatistics> resultadoFinal = new ConcurrentHashMap<>();
        chunks.parallelStream().forEach(chunk -> {
            Map<String, SpeedStatistics> parcial = calcularVelocidadesRealesSecuencial(chunk, nodos);
            resultadoFinal.putAll(parcial);
        });
        
        // Combinar estadísticas de arcos que aparecen en múltiples chunks
        return combinarEstadisticas(resultadoFinal);
    }
    
    /**
     * Calcula velocidades de forma secuencial (método base)
     */
    private static Map<String, SpeedStatistics> calcularVelocidadesRealesSecuencial(
            List<Datagram> datagrams, Map<String, GraphNode> nodos) {
        
        // Agrupar datagrams por bus y ruta, ordenados por timestamp
        Map<String, List<Datagram>> datagramsPorBus = new HashMap<>();
        for (Datagram dg : datagrams) {
            String key = dg.getBusId() + "-" + dg.getRouteId();
            datagramsPorBus.putIfAbsent(key, new ArrayList<>());
            datagramsPorBus.get(key).add(dg);
        }
        
        // Ordenar por timestamp
        for (List<Datagram> lista : datagramsPorBus.values()) {
            lista.sort(Comparator.comparingLong(Datagram::getTimestamp));
        }
        
        // Calcular tiempos entre paradas consecutivas
        Map<String, List<Double>> tiemposPorArco = new HashMap<>(); // key: "routeId-origen-destino"
        Map<String, Double> distanciasPorArco = new HashMap<>();
        
        for (List<Datagram> trayecto : datagramsPorBus.values()) {
            for (int i = 0; i < trayecto.size() - 1; i++) {
                Datagram origen = trayecto.get(i);
                Datagram destino = trayecto.get(i + 1);
                
                // Solo procesar si son paradas diferentes en la misma ruta
                if (origen.getRouteId().equals(destino.getRouteId()) && 
                    !origen.getStopId().equals(destino.getStopId())) {
                    
                    String key = generarClaveArco(origen.getRouteId(), 
                                                 origen.getStopId(), destino.getStopId());
                    
                    // Calcular tiempo en minutos
                    long tiempoMs = destino.getTimestamp() - origen.getTimestamp();
                    double tiempoMin = tiempoMs / (1000.0 * 60.0);
                    
                    // Filtros básicos para evitar cálculos absurdos
                    // Valores por defecto razonables para transporte urbano
                    double tiempoMinimo = Double.parseDouble(
                        System.getProperty("datagram.filter.tiempoMinimo", "0.1")); // 6 segundos mínimo
                    double tiempoMaximo = Double.parseDouble(
                        System.getProperty("datagram.filter.tiempoMaximo", "120.0")); // 2 horas máximo
                    
                    // Solo procesar si el tiempo está en un rango razonable
                    if (tiempoMin > tiempoMinimo && tiempoMin < tiempoMaximo) {
                        tiemposPorArco.putIfAbsent(key, new ArrayList<>());
                        tiemposPorArco.get(key).add(tiempoMin);
                        
                        // Calcular distancia si no está calculada
                        if (!distanciasPorArco.containsKey(key)) {
                            GraphNode nodoOrigen = nodos.get(origen.getStopId());
                            GraphNode nodoDestino = nodos.get(destino.getStopId());
                            
                            if (nodoOrigen != null && nodoDestino != null) {
                                double distancia = DistanceCalculator.calcularDistancia(
                                    nodoOrigen, nodoDestino);
                                distanciasPorArco.put(key, distancia);
                            } else {
                                // Si no se encuentran los nodos, usar coordenadas de los datagrams
                                if (origen.getLatitude() != 0 && origen.getLongitude() != 0 &&
                                    destino.getLatitude() != 0 && destino.getLongitude() != 0) {
                                    // Crear nodos temporales desde las coordenadas de los datagrams
                                    GraphNode tempOrigen = new GraphNode(
                                        origen.getStopId(), "", "", 
                                        origen.getLongitude(), origen.getLatitude());
                                    GraphNode tempDestino = new GraphNode(
                                        destino.getStopId(), "", "", 
                                        destino.getLongitude(), destino.getLatitude());
                                    double distancia = DistanceCalculator.calcularDistancia(
                                        tempOrigen, tempDestino);
                                    distanciasPorArco.put(key, distancia);
                                } else {
                                    // Si no hay coordenadas, poner distancia 0 (se calculará velocidad 0)
                                    distanciasPorArco.put(key, 0.0);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Calcular estadísticas promedio
        // SIN FILTROS: Procesar todos los arcos sin restricciones
        Map<String, SpeedStatistics> estadisticas = new HashMap<>();
        
        for (Map.Entry<String, List<Double>> entry : tiemposPorArco.entrySet()) {
            String key = entry.getKey();
            List<Double> tiempos = entry.getValue();
            
            // Calcular tiempo promedio
            double tiempoPromedio = tiempos.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
            
            // Obtener distancia
            double distancia = distanciasPorArco.getOrDefault(key, 0.0);
            
            // Filtros básicos de distancia
            double distanciaMinima = Double.parseDouble(
                System.getProperty("datagram.filter.distanciaMinima", "0.01")); // 10 metros mínimo
            
            // Calcular velocidad promedio
            double velocidadPromedio = 0.0;
            if (tiempoPromedio > 0 && distancia >= distanciaMinima) {
                double tiempoEnHoras = tiempoPromedio / 60.0;
                velocidadPromedio = distancia / tiempoEnHoras;
                
                // Filtros básicos de velocidad para evitar valores absurdos
                double velocidadMinima = Double.parseDouble(
                    System.getProperty("datagram.filter.velocidadMinima", "1.0")); // 1 km/h mínimo
                double velocidadMaxima = Double.parseDouble(
                    System.getProperty("datagram.filter.velocidadMaxima", "120.0")); // 120 km/h máximo (transporte urbano)
                
                // Si la velocidad está fuera del rango razonable, descartar
                if (velocidadPromedio < velocidadMinima || velocidadPromedio > velocidadMaxima) {
                    velocidadPromedio = 0.0; // Marcar como inválido
                }
            }
            
            // Parsear clave para obtener componentes
            String[] partes = key.split("-");
            if (partes.length >= 3) {
                String routeId = partes[0];
                String origenStopId = partes[1];
                String destinoStopId = partes[2];
                
                // Solo agregar si tiene velocidad válida (filtros básicos aplicados)
                if (velocidadPromedio > 0) {
                    estadisticas.put(key, new SpeedStatistics(
                        routeId, origenStopId, destinoStopId,
                        distancia, tiempoPromedio, velocidadPromedio, tiempos.size()
                    ));
                }
            }
        }
        
        return estadisticas;
    }
    
    /**
     * Combina estadísticas de arcos que aparecen en múltiples chunks
     */
    private static Map<String, SpeedStatistics> combinarEstadisticas(
            Map<String, SpeedStatistics> estadisticas) {
        
        // Agrupar por clave de arco y combinar muestras
        Map<String, List<SpeedStatistics>> agrupadas = new HashMap<>();
        for (SpeedStatistics stats : estadisticas.values()) {
            String key = generarClaveArco(stats.getRouteId(), 
                                         stats.getOrigenStopId(), 
                                         stats.getDestinoStopId());
            agrupadas.putIfAbsent(key, new ArrayList<>());
            agrupadas.get(key).add(stats);
        }
        
        // Combinar estadísticas
        Map<String, SpeedStatistics> resultado = new HashMap<>();
        for (Map.Entry<String, List<SpeedStatistics>> entry : agrupadas.entrySet()) {
            List<SpeedStatistics> lista = entry.getValue();
            if (lista.size() == 1) {
                resultado.put(entry.getKey(), lista.get(0));
            } else {
                // Combinar múltiples estadísticas
                SpeedStatistics primera = lista.get(0);
                int totalMuestras = lista.stream().mapToInt(SpeedStatistics::getNumMuestras).sum();
                double tiempoPromedioPonderado = lista.stream()
                    .mapToDouble(s -> s.getTiempoPromedio() * s.getNumMuestras())
                    .sum() / totalMuestras;
                double distancia = primera.getDistancia(); // Misma distancia
                double velocidadPromedio = distancia > 0 && tiempoPromedioPonderado > 0 ?
                    distancia / (tiempoPromedioPonderado / 60.0) : 0.0;
                
                resultado.put(entry.getKey(), new SpeedStatistics(
                    primera.getRouteId(), primera.getOrigenStopId(), primera.getDestinoStopId(),
                    distancia, tiempoPromedioPonderado, velocidadPromedio, totalMuestras
                ));
            }
        }
        
        return resultado;
    }
    
    /**
     * Genera una clave única para un arco
     */
    private static String generarClaveArco(String routeId, String origenId, String destinoId) {
        return routeId + "-" + origenId + "-" + destinoId;
    }
    
    /**
     * Representa estadísticas de velocidad por ruta completa
     */
    public static class RouteStatistics {
        private String routeId;
        private double distanciaTotal; // km
        private double tiempoTotal; // minutos
        private double velocidadPromedio; // km/h (promedio ponderado por distancia)
        private int numArcos; // Número de arcos en la ruta
        private int numMuestrasTotal; // Total de muestras de todos los arcos
        
        public RouteStatistics(String routeId, double distanciaTotal, double tiempoTotal, 
                              double velocidadPromedio, int numArcos, int numMuestrasTotal) {
            this.routeId = routeId;
            this.distanciaTotal = distanciaTotal;
            this.tiempoTotal = tiempoTotal;
            this.velocidadPromedio = velocidadPromedio;
            this.numArcos = numArcos;
            this.numMuestrasTotal = numMuestrasTotal;
        }
        
        // Getters
        public String getRouteId() { return routeId; }
        public double getDistanciaTotal() { return distanciaTotal; }
        public double getTiempoTotal() { return tiempoTotal; }
        public double getVelocidadPromedio() { return velocidadPromedio; }
        public int getNumArcos() { return numArcos; }
        public int getNumMuestrasTotal() { return numMuestrasTotal; }
    }
    
    /**
     * Calcula velocidades promedio por ruta a partir de estadísticas de arcos
     * Usa promedio ponderado por distancia para obtener la velocidad real de la ruta
     * 
     * @param estadisticasPorArco Mapa de estadísticas por arco (clave: "routeId-origen-destino")
     * @return Mapa de estadísticas por ruta (clave: routeId)
     */
    public static Map<String, RouteStatistics> calcularVelocidadesPorRuta(
            Map<String, SpeedStatistics> estadisticasPorArco) {
        
        // Agrupar estadísticas por ruta
        Map<String, List<SpeedStatistics>> estadisticasPorRuta = new HashMap<>();
        
        for (SpeedStatistics stats : estadisticasPorArco.values()) {
            String routeId = stats.getRouteId();
            estadisticasPorRuta.putIfAbsent(routeId, new ArrayList<>());
            estadisticasPorRuta.get(routeId).add(stats);
        }
        
        // Calcular estadísticas por ruta
        Map<String, RouteStatistics> rutas = new HashMap<>();
        
        for (Map.Entry<String, List<SpeedStatistics>> entry : estadisticasPorRuta.entrySet()) {
            String routeId = entry.getKey();
            List<SpeedStatistics> arcos = entry.getValue();
            
            // Calcular distancia total y tiempo total
            double distanciaTotal = arcos.stream()
                .mapToDouble(SpeedStatistics::getDistancia)
                .sum();
            
            // Tiempo total es la suma de tiempos promedio de cada arco
            // (asumiendo que los arcos son consecutivos)
            double tiempoTotal = arcos.stream()
                .mapToDouble(SpeedStatistics::getTiempoPromedio)
                .sum();
            
            // Calcular velocidad promedio ponderada por distancia
            // velocidad = suma(distancia_i * velocidad_i) / distancia_total
            double velocidadPromedioPonderada = 0.0;
            
            // Filtrar arcos con velocidad válida (> 0) y distancia válida (> 0)
            List<SpeedStatistics> arcosValidos = arcos.stream()
                .filter(s -> s.getVelocidadPromedio() > 0 && s.getDistancia() > 0)
                .collect(java.util.stream.Collectors.toList());
            
            if (arcosValidos.isEmpty()) {
                // Si no hay arcos válidos, intentar calcular desde distancia y tiempo total
                if (distanciaTotal > 0 && tiempoTotal > 0) {
                    double tiempoEnHoras = tiempoTotal / 60.0;
                    velocidadPromedioPonderada = distanciaTotal / tiempoEnHoras;
                } else {
                    velocidadPromedioPonderada = 0.0;
                }
            } else if (distanciaTotal > 0) {
                // Calcular promedio ponderado solo con arcos válidos
                double sumaPonderada = arcosValidos.stream()
                    .mapToDouble(s -> s.getDistancia() * s.getVelocidadPromedio())
                    .sum();
                double distanciaTotalValida = arcosValidos.stream()
                    .mapToDouble(SpeedStatistics::getDistancia)
                    .sum();
                if (distanciaTotalValida > 0) {
                    velocidadPromedioPonderada = sumaPonderada / distanciaTotalValida;
                } else {
                    // Si no hay distancia válida, usar promedio simple
                    velocidadPromedioPonderada = arcosValidos.stream()
                        .mapToDouble(SpeedStatistics::getVelocidadPromedio)
                        .average()
                        .orElse(0.0);
                }
            } else {
                // Si no hay distancia total, usar promedio simple de velocidades válidas
                velocidadPromedioPonderada = arcosValidos.stream()
                    .mapToDouble(SpeedStatistics::getVelocidadPromedio)
                    .average()
                    .orElse(0.0);
            }
            
            // Total de muestras
            int numMuestrasTotal = arcos.stream()
                .mapToInt(SpeedStatistics::getNumMuestras)
                .sum();
            
            rutas.put(routeId, new RouteStatistics(
                routeId, distanciaTotal, tiempoTotal, 
                velocidadPromedioPonderada, arcos.size(), numMuestrasTotal
            ));
        }
        
        return rutas;
    }
    
    /**
     * Calcula velocidades para un lote específico de datagrams (para procesamiento distribuido)
     */
    public static Map<String, SpeedStatistics> calcularVelocidadesParaLote(
            List<Datagram> batch, Map<String, GraphNode> nodos) {
        return calcularVelocidadesRealesSecuencial(batch, nodos);
    }
    
    /**
     * Detecta si la primera línea del CSV contiene encabezados o datos
     * Busca palabras clave típicas de encabezados (bus, route, stop, date, etc.)
     */
    private static boolean detectarSiTieneEncabezados(String[] primeraLinea) {
        if (primeraLinea == null || primeraLinea.length == 0) {
            return false;
        }
        
        // Palabras clave que indican que es un encabezado
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
        
        // Si más de la mitad de los campos contienen palabras clave, probablemente son encabezados
        // También verificamos si algún campo parece ser un número (si todos son números, probablemente son datos)
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
        
        // Si todos son números, definitivamente son datos, no encabezados
        if (todosSonNumeros) {
            return false;
        }
        
        // Si hay al menos 2 coincidencias con palabras clave, probablemente son encabezados
        return coincidencias >= 2;
    }
    
    /**
     * Crea un mapa de índices cuando no hay encabezados, usando un orden predefinido
     * Orden por defecto basado en estructura real del CSV:
     * 0: eventType, 1: registerdate, 2: stopId, 3: odometer, 4: latitude, 
     * 5: longitude, 6: taskId, 7: lineId, 8: tripId, 9: unknown1, 
     * 10: datagramDate, 11: busId
     * 
     * Mapeo:
     * - busId: columna 11
     * - routeId/lineId: columna 7
     * - stopId: columna 2
     * - latitude: columna 4
     * - longitude: columna 5
     * - timestamp/datagramDate: columna 10
     * - sequence/tripId: columna 8
     * 
     * Puedes configurar el orden usando propiedades del sistema:
     * - datagram.csv.column.busId=11
     * - datagram.csv.column.routeId=7
     * - datagram.csv.column.stopId=2
     * - datagram.csv.column.latitude=4
     * - datagram.csv.column.longitude=5
     * - datagram.csv.column.timestamp=10
     */
    private static Map<String, Integer> crearIndicesSinEncabezados(int numColumnas) {
        Map<String, Integer> indices = new HashMap<>();
        
        // Orden por defecto basado en estructura real: 
        // eventType, registerdate, stopId, odometer, latitude, longitude, 
        // taskId, lineId, tripId, unknown1, datagramDate, busId
        // Puede ser sobrescrito por propiedades del sistema
        int busIdIdx = Integer.parseInt(System.getProperty("datagram.csv.column.busId", "11"));
        int routeIdIdx = Integer.parseInt(System.getProperty("datagram.csv.column.routeId", "7"));
        int stopIdIdx = Integer.parseInt(System.getProperty("datagram.csv.column.stopId", "2"));
        int latitudeIdx = Integer.parseInt(System.getProperty("datagram.csv.column.latitude", "4"));
        int longitudeIdx = Integer.parseInt(System.getProperty("datagram.csv.column.longitude", "5"));
        int timestampIdx = Integer.parseInt(System.getProperty("datagram.csv.column.timestamp", "10"));
        int sequenceIdx = Integer.parseInt(System.getProperty("datagram.csv.column.sequence", "8"));
        
        // Validar que los índices estén dentro del rango
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
        
        System.out.println("Orden de columnas detectado (sin encabezados):");
        System.out.println("  Estructura esperada: eventType, registerdate, stopId, odometer, latitude, longitude, taskId, lineId, tripId, unknown1, datagramDate, busId");
        System.out.println("  busId: columna " + busIdIdx);
        System.out.println("  routeId/lineId: columna " + routeIdIdx);
        System.out.println("  stopId: columna " + stopIdIdx);
        System.out.println("  latitude: columna " + latitudeIdx);
        System.out.println("  longitude: columna " + longitudeIdx);
        System.out.println("  timestamp/datagramDate: columna " + timestampIdx);
        if (sequenceIdx >= 0) {
            System.out.println("  sequence/tripId: columna " + sequenceIdx);
        }
        System.out.println("  (Total de columnas: " + numColumnas + ")");
        System.out.println();
        System.out.println("NOTA: Si el orden es incorrecto, puedes configurarlo con propiedades del sistema:");
        System.out.println("  -Ddatagram.csv.column.busId=11");
        System.out.println("  -Ddatagram.csv.column.routeId=7");
        System.out.println("  -Ddatagram.csv.column.stopId=2");
        System.out.println("  -Ddatagram.csv.column.latitude=4");
        System.out.println("  -Ddatagram.csv.column.longitude=5");
        System.out.println("  -Ddatagram.csv.column.timestamp=10");
        System.out.println("  -Ddatagram.csv.column.sequence=8");
        System.out.println();
        
        return indices;
    }
    
    /**
     * Mapea encabezados del CSV a índices, buscando variaciones de nombres
     * Soporta estructura real: eventType, registerdate, stopId, odometer, latitude, 
     * longitude, taskId, lineId, tripId, unknown1, datagramDate, busId
     */
    private static Map<String, Integer> mapearEncabezados(String[] encabezados) {
        Map<String, Integer> indices = new HashMap<>();
        
        for (int i = 0; i < encabezados.length; i++) {
            String encabezado = encabezados[i].replace("\"", "").trim().toLowerCase();
            
            // Mapeo específico para estructura real del CSV
            if (encabezado.equals("busid") || encabezado.equals("bus_id") || 
                encabezado.matches(".*bus.*id.*")) {
                indices.put("bus_id", i);
            }
            if (encabezado.equals("lineid") || encabezado.equals("line_id") || 
                encabezado.matches(".*line.*id.*|.*route.*id.*")) {
                indices.put("route_id", i); // lineId se mapea a route_id internamente
            }
            if (encabezado.equals("stopid") || encabezado.equals("stop_id") || 
                encabezado.matches(".*stop.*id.*")) {
                indices.put("stop_id", i);
            }
            if (encabezado.equals("latitude") || encabezado.equals("lat")) {
                indices.put("latitude", i);
            }
            if (encabezado.equals("longitude") || encabezado.equals("lon") || 
                encabezado.equals("lng")) {
                indices.put("longitude", i);
            }
            if (encabezado.equals("datagramdate") || encabezado.equals("datagram_date") ||
                encabezado.matches(".*datagram.*date.*|.*timestamp.*|.*time.*")) {
                indices.put("timestamp", i); // datagramDate se mapea a timestamp
            }
            if (encabezado.equals("tripid") || encabezado.equals("trip_id") ||
                encabezado.matches(".*trip.*id.*|.*sequence.*|.*order.*")) {
                indices.put("sequence", i); // tripId se usa como secuencia
            }
        }
        
        return indices;
    }
    
    /**
     * Parsea un archivo CSV considerando comillas
     */
    private static String[] parsearCSV(String linea) {
        List<String> campos = new ArrayList<>();
        boolean dentroComillas = false;
        StringBuilder campo = new StringBuilder();
        
        for (char c : linea.toCharArray()) {
            if (c == '"') {
                dentroComillas = !dentroComillas;
            } else if (c == ',' && !dentroComillas) {
                campos.add(campo.toString().trim());
                campo = new StringBuilder();
            } else {
                campo.append(c);
            }
        }
        campos.add(campo.toString().trim());
        
        return campos.toArray(new String[0]);
    }
    
    /**
     * Obtiene un campo del array usando el mapa de índices
     */
    private static String obtenerCampo(String[] campos, Map<String, Integer> indices, String nombre) {
        Integer indice = indices.get(nombre);
        if (indice != null && indice < campos.length) {
            return campos[indice].replace("\"", "").trim();
        }
        return null;
    }
    
    /**
     * Parsea un double con valor por defecto
     */
    private static double parsearDouble(String[] campos, Map<String, Integer> indices, 
                                       String nombre, double valorDefecto) {
        String valor = obtenerCampo(campos, indices, nombre);
        if (valor != null && !valor.isEmpty()) {
            try {
                return Double.parseDouble(valor);
            } catch (NumberFormatException e) {
                return valorDefecto;
            }
        }
        return valorDefecto;
    }
    
    /**
     * Parsea un int con valor por defecto
     */
    private static int parsearInt(String[] campos, Map<String, Integer> indices, 
                                 String nombre, int valorDefecto) {
        String valor = obtenerCampo(campos, indices, nombre);
        if (valor != null && !valor.isEmpty()) {
            try {
                return Integer.parseInt(valor);
            } catch (NumberFormatException e) {
                return valorDefecto;
            }
        }
        return valorDefecto;
    }
    
    /**
     * Parsea un datagram desde un array de campos
     * Adaptado para estructura real: busId, lineId, stopId, latitude, longitude, datagramDate
     */
    private static Datagram parsearDatagram(String[] campos, Map<String, Integer> indices) {
        String busId = obtenerCampo(campos, indices, "bus_id");
        String routeId = obtenerCampo(campos, indices, "route_id"); // lineId en el CSV
        String stopId = obtenerCampo(campos, indices, "stop_id");
        
        // Las coordenadas están en microgrados (ej: 34761183 = 34.761183)
        double latitude = parsearDouble(campos, indices, "latitude", 0.0);
        double longitude = parsearDouble(campos, indices, "longitude", 0.0);
        
        // Convertir de microgrados a grados decimales
        if (Math.abs(latitude) > 90) {
            latitude = latitude / 1000000.0;
        }
        if (Math.abs(longitude) > 180) {
            longitude = longitude / 1000000.0;
        }
        
        // Timestamp desde datagramDate (formato: "2019-05-27 20:14:43")
        long timestamp = parsearTimestamp(campos, indices);
        
        // Usar tripId como secuencia
        int sequence = parsearInt(campos, indices, "sequence", 0);
        
        if (busId != null && routeId != null && stopId != null && timestamp > 0) {
            return new Datagram(busId, routeId, stopId, 
                             latitude, longitude, timestamp, sequence);
        }
        return null;
    }
    
    /**
     * Parsea timestamp desde datagramDate
     * Formato esperado: "2019-05-27 20:14:43" (YYYY-MM-DD HH:MM:SS)
     */
    private static long parsearTimestamp(String[] campos, Map<String, Integer> indices) {
        String valor = obtenerCampo(campos, indices, "timestamp");
        if (valor == null || valor.isEmpty()) {
            return 0;
        }
        
        try {
            // Intentar como milisegundos (timestamp Unix)
            return Long.parseLong(valor);
        } catch (NumberFormatException e) {
            try {
                // Intentar como segundos y convertir a milisegundos
                return (long)(Double.parseDouble(valor) * 1000);
            } catch (NumberFormatException e2) {
                // Intentar parsear como fecha "2019-05-27 20:14:43"
                try {
                    return parsearFechaHora(valor);
                } catch (Exception e3) {
                    // Si no se puede parsear, retornar 0
                    return 0;
                }
            }
        }
    }
    
    /**
     * Parsea fecha/hora en formato "2019-05-27 20:14:43" a timestamp Unix
     */
    private static long parsearFechaHora(String fechaHora) {
        try {
            // Formato: "2019-05-27 20:14:43"
            String[] partes = fechaHora.trim().split(" ");
            if (partes.length != 2) {
                return 0;
            }
            
            String fecha = partes[0]; // "2019-05-27"
            String hora = partes[1];  // "20:14:43"
            
            String[] fechaPartes = fecha.split("-");
            String[] horaPartes = hora.split(":");
            
            if (fechaPartes.length != 3 || horaPartes.length != 3) {
                return 0;
            }
            
            int año = Integer.parseInt(fechaPartes[0]);
            int mes = Integer.parseInt(fechaPartes[1]) - 1; // Mes es 0-based en Calendar
            int dia = Integer.parseInt(fechaPartes[2]);
            int horas = Integer.parseInt(horaPartes[0]);
            int minutos = Integer.parseInt(horaPartes[1]);
            int segundos = Integer.parseInt(horaPartes[2]);
            
            // Crear Calendar y convertir a timestamp
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.set(java.util.Calendar.YEAR, año);
            cal.set(java.util.Calendar.MONTH, mes);
            cal.set(java.util.Calendar.DAY_OF_MONTH, dia);
            cal.set(java.util.Calendar.HOUR_OF_DAY, horas);
            cal.set(java.util.Calendar.MINUTE, minutos);
            cal.set(java.util.Calendar.SECOND, segundos);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            
            return cal.getTimeInMillis();
            
        } catch (Exception e) {
            return 0;
        }
    }
}

