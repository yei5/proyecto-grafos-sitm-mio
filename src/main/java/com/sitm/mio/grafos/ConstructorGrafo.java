package com.sitm.mio.grafos;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Clase que construye grafos separados por ruta y sentido a partir de archivos CSV
 */
public class ConstructorGrafo {

    /**
     * Construye un grafo general con todas las rutas
     * @param rutaDirectorio Ruta del directorio que contiene los archivos CSV
     * @return Grafo construido con todas las rutas
     */
    public static Grafo construirGrafoDesdeCSV(String rutaDirectorio) throws IOException {
        Grafo grafo = new Grafo();

        String rutaStops = rutaDirectorio + File.separator + "stops.csv";
        String rutaLines = rutaDirectorio + File.separator + "lines.csv";
        String rutaLineStops = rutaDirectorio + File.separator + "linestops.csv";

        verificarArchivos(rutaStops, rutaLines, rutaLineStops);

        Map<String, Nodo> paradas = cargarParadas(rutaStops);
        System.out.println("Paradas cargadas: " + paradas.size());

        Map<String, String> rutas = cargarRutas(rutaLines);
        System.out.println("Rutas cargadas: " + rutas.size());

        construirArcos(rutaLineStops, grafo, paradas, rutas);
        System.out.println("Arcos construidos: " + grafo.getNumeroArcos());

        return grafo;
    }

    /**
     * Construye un mapa de grafos separados por ruta y sentido
     * @param rutaDirectorio Ruta del directorio que contiene los archivos CSV
     * @return Mapa con clave "RUTA-SENTIDO" y valor el grafo correspondiente
     */
    public static Map<String, Grafo> construirGrafosPorRutaYSentido(String rutaDirectorio) throws IOException {
        Map<String, Grafo> grafosPorRuta = new HashMap<>();

        String rutaStops = rutaDirectorio + File.separator + "stops.csv";
        String rutaLines = rutaDirectorio + File.separator + "lines.csv";
        String rutaLineStops = rutaDirectorio + File.separator + "linestops.csv";

        verificarArchivos(rutaStops, rutaLines, rutaLineStops);

        Map<String, Nodo> paradas = cargarParadas(rutaStops);
        System.out.println("Paradas cargadas: " + paradas.size());

        Map<String, String> rutas = cargarRutas(rutaLines);
        System.out.println("Rutas cargadas: " + rutas.size());

        construirArcosSeparados(rutaLineStops, grafosPorRuta, paradas, rutas);
        
        System.out.println("\n=== GRAFOS CONSTRUIDOS POR RUTA Y SENTIDO ===");
        for (Map.Entry<String, Grafo> entrada : grafosPorRuta.entrySet()) {
            System.out.println(entrada.getKey() + ": " +
                             entrada.getValue().getNumeroNodos() + " nodos, " +
                             entrada.getValue().getNumeroArcos() + " arcos");
        }

        return grafosPorRuta;
    }

    /**
     * Construye grafos separados y los exporta como imágenes PNG
     * @param rutaDirectorio Ruta del directorio con los CSV
     * @param directorioSalida Directorio donde se guardarán las imágenes
     * @return Mapa de grafos construidos
     */
    public static Map<String, Grafo> construirYExportarGrafos(String rutaDirectorio, 
                                                               String directorioSalida) throws IOException {
        Map<String, Grafo> grafos = construirGrafosPorRutaYSentido(rutaDirectorio);
        
        // Crear directorio de salida si no existe
        File dirSalida = new File(directorioSalida);
        if (!dirSalida.exists()) {
            dirSalida.mkdirs();
        }
        
        System.out.println("\n=== EXPORTANDO IMÁGENES ===");
        int contador = 0;
        for (Map.Entry<String, Grafo> entrada : grafos.entrySet()) {
            String nombreRuta = entrada.getKey();
            Grafo grafo = entrada.getValue();
            
            String nombreArchivo = directorioSalida + File.separator + 
                                  "grafo_" + sanitizarNombre(nombreRuta) + ".png";
            
            VisualizadorGrafo.exportar(grafo, nombreArchivo);
            contador++;
            System.out.println("[" + contador + "/" + grafos.size() + "] " +
                             "Exportado: " + nombreArchivo);
        }
        
        System.out.println("\n✓ " + contador + " grafos exportados correctamente");
        return grafos;
    }

    /**
     * Verifica que los archivos CSV existan
     */
    private static void verificarArchivos(String rutaStops, String rutaLines, 
                                         String rutaLineStops) throws IOException {
        File archivoStops = new File(rutaStops);
        File archivoLines = new File(rutaLines);
        File archivoLineStops = new File(rutaLineStops);

        if (!archivoStops.exists()) {
            throw new IOException("No se encontró stops.csv en: " + archivoStops.getAbsolutePath());
        }
        if (!archivoLines.exists()) {
            throw new IOException("No se encontró lines.csv en: " + archivoLines.getAbsolutePath());
        }
        if (!archivoLineStops.exists()) {
            throw new IOException("No se encontró linestops.csv en: " + archivoLineStops.getAbsolutePath());
        }
    }

    /**
     * Carga las paradas desde stops.csv
     */
    private static Map<String, Nodo> cargarParadas(String rutaArchivo) throws IOException {
        Map<String, Nodo> paradas = new HashMap<>();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(rutaArchivo), StandardCharsets.UTF_8))) {

            String linea = br.readLine(); 
            if (linea == null) throw new IOException("El archivo stops.csv está vacío");

            String[] encabezados = linea.replace("\"", "").split(",");

            int indiceId = buscarIndice(encabezados, new String[]{
                    "STOPID", "stopid", "stop_id", "id"
            });

            int indiceNombre = buscarIndice(encabezados, new String[]{
                    "SHORTNAME", "shortname", "LONGNAME", "longname", "name"
            });

            if (indiceId == -1 || indiceNombre == -1)
                throw new IOException("Encabezados inválidos en stops.csv");

            while ((linea = br.readLine()) != null) {
                if (linea.trim().isEmpty()) continue;

                String[] campos = parsearCSV(linea);

                if (campos.length > Math.max(indiceId, indiceNombre)) {
                    String id = campos[indiceId].replace("\"", "").trim();
                    String nombre = campos[indiceNombre].replace("\"", "").trim();

                    if (!id.isEmpty()) {
                        Nodo nodo = new Nodo(id, nombre, "Parada");
                        paradas.put(id, nodo);
                    }
                }
            }
        }

        return paradas;
    }

    /**
     * Carga rutas desde lines.csv
     */
    private static Map<String, String> cargarRutas(String rutaArchivo) throws IOException {
        Map<String, String> rutas = new HashMap<>();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(rutaArchivo), StandardCharsets.UTF_8))) {

            String linea = br.readLine();
            if (linea == null) throw new IOException("El archivo lines.csv está vacío");

            String[] encabezados = linea.replace("\"", "").split(",");

            int indiceId = buscarIndice(encabezados, new String[]{
                    "LINEID", "lineid", "line_id", "id"
            });

            int indiceNombre = buscarIndice(encabezados, new String[]{
                    "SHORTNAME", "shortname", "DESCRIPTION", "description", "name"
            });

            if (indiceId == -1)
                throw new IOException("Encabezado de LINEID no encontrado en lines.csv");

            while ((linea = br.readLine()) != null) {
                if (linea.trim().isEmpty()) continue;

                String[] campos = parsearCSV(linea);

                if (campos.length > indiceId) {
                    String id = campos[indiceId].replace("\"", "").trim();
                    String nombre = (indiceNombre != -1 && campos.length > indiceNombre)
                            ? campos[indiceNombre].replace("\"", "").trim()
                            : id;

                    rutas.put(id, nombre);
                }
            }
        }

        return rutas;
    }

    /**
     * Construye arcos para un grafo general (método original)
     */
    private static void construirArcos(String rutaArchivo, Grafo grafo,
                                       Map<String, Nodo> paradas, Map<String, String> rutas) throws IOException {

        Map<String, Map<String, List<ParadaOrdenada>>> rutasParadas = new HashMap<>();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(rutaArchivo), StandardCharsets.UTF_8))) {

            String linea = br.readLine();
            if (linea == null) throw new IOException("El archivo linestops.csv está vacío");

            String[] encabezados = linea.replace("\"", "").split(",");

            int indiceLineId = buscarIndice(encabezados, new String[]{
                    "LINEID", "lineid", "line_id"
            });
            int indiceStopId = buscarIndice(encabezados, new String[]{
                    "STOPID", "stopid", "stop_id"
            });
            int indiceOrientation = buscarIndice(encabezados, new String[]{
                    "ORIENTATION", "orientation"
            });
            int indiceSequence = buscarIndice(encabezados, new String[]{
                    "STOPSEQUENCE", "stopsequence", "sequence", "order"
            });

            if (indiceLineId == -1 || indiceStopId == -1 || indiceSequence == -1)
                throw new IOException("Encabezados inválidos en linestops.csv");

            while ((linea = br.readLine()) != null) {
                if (linea.trim().isEmpty()) continue;

                String[] campos = parsearCSV(linea);

                if (campos.length <= Math.max(indiceLineId, indiceStopId)) continue;

                String lineId = campos[indiceLineId].replace("\"", "").trim();
                String stopId = campos[indiceStopId].replace("\"", "").trim();

                String orientation = (indiceOrientation != -1 && campos.length > indiceOrientation)
                        ? campos[indiceOrientation].replace("\"", "").trim()
                        : "0";

                int sequence = parsearEntero(
                        campos[indiceSequence].replace("\"", "").trim(), 0
                );

                if (!lineId.isEmpty() && !stopId.isEmpty() && paradas.containsKey(stopId)) {
                    rutasParadas.putIfAbsent(lineId, new HashMap<>());
                    rutasParadas.get(lineId).putIfAbsent(orientation, new ArrayList<>());
                    rutasParadas.get(lineId).get(orientation)
                            .add(new ParadaOrdenada(stopId, sequence));
                }
            }
        }

        // Agregar nodos
        for (Nodo nodo : paradas.values()) grafo.agregarNodo(nodo);

        // Crear arcos
        for (var entradaRuta : rutasParadas.entrySet()) {
            String rutaId = entradaRuta.getKey();
            String nombreRuta = rutas.getOrDefault(rutaId, rutaId);

            for (var entradaOrientacion : entradaRuta.getValue().entrySet()) {
                String orientation = entradaOrientacion.getKey();
                List<ParadaOrdenada> paradasOrdenadas = entradaOrientacion.getValue();

                paradasOrdenadas.sort(Comparator.comparingInt(p -> p.sequence));

                for (int i = 0; i < paradasOrdenadas.size() - 1; i++) {
                    Nodo origen = paradas.get(paradasOrdenadas.get(i).stopId);
                    Nodo destino = paradas.get(paradasOrdenadas.get(i + 1).stopId);

                    if (origen != null && destino != null) {
                        String rutaCompleta = nombreRuta + "-" + orientation;
                        grafo.agregarArco(new Arco(origen, destino, 0.0, 0.0, rutaCompleta));
                    }
                }
            }
        }
    }

    /**
     * Construye grafos separados por ruta y sentido
     */
    private static void construirArcosSeparados(String rutaArchivo, 
                                               Map<String, Grafo> grafosPorRuta,
                                               Map<String, Nodo> paradas, 
                                               Map<String, String> rutas) throws IOException {

        Map<String, Map<String, List<ParadaOrdenada>>> rutasParadas = new HashMap<>();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(rutaArchivo), StandardCharsets.UTF_8))) {

            String linea = br.readLine();
            if (linea == null) throw new IOException("El archivo linestops.csv está vacío");

            String[] encabezados = linea.replace("\"", "").split(",");

            int indiceLineId = buscarIndice(encabezados, new String[]{
                    "LINEID", "lineid", "line_id"
            });
            int indiceStopId = buscarIndice(encabezados, new String[]{
                    "STOPID", "stopid", "stop_id"
            });
            int indiceOrientation = buscarIndice(encabezados, new String[]{
                    "ORIENTATION", "orientation"
            });
            int indiceSequence = buscarIndice(encabezados, new String[]{
                    "STOPSEQUENCE", "stopsequence", "sequence", "order"
            });

            if (indiceLineId == -1 || indiceStopId == -1 || indiceSequence == -1)
                throw new IOException("Encabezados inválidos en linestops.csv");

            while ((linea = br.readLine()) != null) {
                if (linea.trim().isEmpty()) continue;

                String[] campos = parsearCSV(linea);

                if (campos.length <= Math.max(indiceLineId, indiceStopId)) continue;

                String lineId = campos[indiceLineId].replace("\"", "").trim();
                String stopId = campos[indiceStopId].replace("\"", "").trim();

                String orientation = (indiceOrientation != -1 && campos.length > indiceOrientation)
                        ? campos[indiceOrientation].replace("\"", "").trim()
                        : "0";

                int sequence = parsearEntero(
                        campos[indiceSequence].replace("\"", "").trim(), 0
                );

                if (!lineId.isEmpty() && !stopId.isEmpty() && paradas.containsKey(stopId)) {
                    String claveRutaSentido = lineId + "-" + orientation;
                    
                    rutasParadas.putIfAbsent(claveRutaSentido, new HashMap<>());
                    rutasParadas.get(claveRutaSentido).putIfAbsent(orientation, new ArrayList<>());
                    rutasParadas.get(claveRutaSentido).get(orientation)
                            .add(new ParadaOrdenada(stopId, sequence));
                }
            }
        }

        // Crear un grafo por cada ruta-sentido
        for (var entradaRuta : rutasParadas.entrySet()) {
            String claveRutaSentido = entradaRuta.getKey();
            Grafo grafoIndividual = new Grafo();
            
            // Extraer lineId y orientation de la clave
            String[] partes = claveRutaSentido.split("-");
            String lineId = partes[0];
            String orientation = partes.length > 1 ? partes[partes.length - 1] : "0";
            
            String nombreRuta = rutas.getOrDefault(lineId, lineId);
            String sentidoTexto = orientation.equals("0") ? "Ida" : "Vuelta";
            String nombreCompleto = nombreRuta + " - " + sentidoTexto;

            for (var entradaOrientacion : entradaRuta.getValue().entrySet()) {
                List<ParadaOrdenada> paradasOrdenadas = entradaOrientacion.getValue();
                paradasOrdenadas.sort(Comparator.comparingInt(p -> p.sequence));

                // Agregar nodos al grafo individual
                for (ParadaOrdenada paradaOrd : paradasOrdenadas) {
                    Nodo nodo = paradas.get(paradaOrd.stopId);
                    if (nodo != null && grafoIndividual.obtenerNodo(nodo.getId()) == null) {
                        grafoIndividual.agregarNodo(nodo);
                    }
                }

                // Crear arcos
                for (int i = 0; i < paradasOrdenadas.size() - 1; i++) {
                    Nodo origen = paradas.get(paradasOrdenadas.get(i).stopId);
                    Nodo destino = paradas.get(paradasOrdenadas.get(i + 1).stopId);

                    if (origen != null && destino != null) {
                        grafoIndividual.agregarArco(
                            new Arco(origen, destino, 0.0, 0.0, nombreCompleto)
                        );
                    }
                }
            }

            grafosPorRuta.put(claveRutaSentido, grafoIndividual);
        }
    }

    /** Busca índice de columna */
    private static int buscarIndice(String[] encabezados, String[] nombresPosibles) {
        for (int i = 0; i < encabezados.length; i++) {
            String encabezado = encabezados[i].trim().toLowerCase();
            for (String nombre : nombresPosibles) {
                if (encabezado.equals(nombre.toLowerCase())) return i;
            }
        }
        return -1;
    }

    /** Parseo CSV con comillas */
    private static String[] parsearCSV(String linea) {
        List<String> campos = new ArrayList<>();
        boolean dentroComillas = false;
        StringBuilder campo = new StringBuilder();

        for (char c : linea.toCharArray()) {
            if (c == '"') dentroComillas = !dentroComillas;
            else if (c == ',' && !dentroComillas) {
                campos.add(campo.toString());
                campo = new StringBuilder();
            } else campo.append(c);
        }
        campos.add(campo.toString());

        return campos.toArray(new String[0]);
    }

    private static int parsearEntero(String v, int defecto) {
        try { return Integer.parseInt(v); }
        catch (Exception e) { return defecto; }
    }

    private static String sanitizarNombre(String nombre) {
        return nombre.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private static class ParadaOrdenada {
        String stopId;
        int sequence;

        ParadaOrdenada(String stopId, int sequence) {
            this.stopId = stopId;
            this.sequence = sequence;
        }
    }
}