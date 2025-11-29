package com.sitm.mio.grafos;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) {

        System.out.println("=== SITM-MIO - GENERACIÓN DE GRAFOS ===\n");

        String rutaDirectorioCSV = args.length > 0 ? args[0] : "datos";

        try {
            System.out.println("Construyendo grafo general y grafos por ruta...");
            System.out.println("Directorio: " + new File(rutaDirectorioCSV).getAbsolutePath());
            System.out.println("Archivos requeridos: stops.csv, lines.csv, linestops.csv\n");

            // Crear directorio de imágenes
            File dirImagenes = new File(rutaDirectorioCSV, "imagenes");
            if (!dirImagenes.exists()) {
                dirImagenes.mkdirs();
            }

            // =====================================================
            // 1. GRAFO GENERAL
            // =====================================================

            Grafo grafoGeneral = ConstructorGrafo.construirGrafoDesdeCSV(rutaDirectorioCSV);

            System.out.println("=== GRAFO GENERAL ===");
            System.out.println("Nodos: " + grafoGeneral.getNumeroNodos());
            System.out.println("Arcos: " + grafoGeneral.getNumeroArcos());
            System.out.println("--------------------------------------------");

            // Imagen del grafo general
            String pngGeneral = new File(rutaDirectorioCSV, "grafo_exportado.png").getAbsolutePath();
            VisualizadorGrafo.exportar(grafoGeneral, pngGeneral, "Grafo General SITM-MIO - Cali, Colombia");
            System.out.println("Imagen generada: " + pngGeneral);
            
            // El mapa real se renderiza automáticamente en la imagen PNG si JXMapViewer2 está disponible

            // TXT del grafo general
            String txtGeneral = new File(rutaDirectorioCSV, "grafo_exportado.txt").getAbsolutePath();
            exportarTxtGrafo(grafoGeneral, "Grafo General", txtGeneral);
            System.out.println("TXT generado: " + txtGeneral);

            System.out.println("\n=== FIN GRAFO GENERAL ===\n");


            // =====================================================
            // 2. GRAFOS POR RUTA Y SENTIDO
            // =====================================================

            Map<String, Grafo> grafosPorRuta =
                    ConstructorGrafo.construirGrafosPorRutaYSentido(rutaDirectorioCSV);

            System.out.println("\n--- Se generaron " + grafosPorRuta.size() + " grafos de rutas ---\n");

            for (String nombre : grafosPorRuta.keySet()) {

                Grafo gRuta = grafosPorRuta.get(nombre);

                System.out.println("============================================");
                System.out.println("Grafo ruta: " + nombre);
                System.out.println("Nodos: " + gRuta.getNumeroNodos());
                System.out.println("Arcos: " + gRuta.getNumeroArcos());
                System.out.println("--------------------------------------------");

                // Imagen PNG por ruta
                String rutaPng = new File(dirImagenes, nombre + ".png").getAbsolutePath();
                VisualizadorGrafo.exportar(gRuta, rutaPng, nombre);
                System.out.println("Imagen generada: " + rutaPng);
                
                // El mapa real se renderiza automáticamente en la imagen PNG si JXMapViewer2 está disponible

                // TXT por ruta
                String rutaTxt = new File(dirImagenes, nombre + ".txt").getAbsolutePath();
                exportarTxtGrafo(gRuta, nombre, rutaTxt);
                System.out.println("TXT generado: " + rutaTxt);

                System.out.println("============================================\n");
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.out.println("\nUso: java com.sitm.mio.grafos.Main [ruta_csv]");
        }
    }


    // =====================================================
    // MÉTODO AUXILIAR PARA EXPORTAR ARCHIVOS TXT
    // =====================================================
    private static void exportarTxtGrafo(Grafo grafo, String nombre, String rutaTxt) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(rutaTxt))) {

            pw.println("== " + nombre + " ==");
            pw.println("Nodos: " + grafo.getNumeroNodos());
            pw.println("Arcos: " + grafo.getNumeroArcos());
            pw.println("------------------------------------");

            pw.println("\n=== Lista de Nodos (Paradas) ===");
            pw.println("Formato: ID - Nombre - Coordenadas GPS (Lat, Lon)");
            pw.println("------------------------------------");
            for (Nodo n : grafo.obtenerNodos()) {
                String coords = "";
                if (n.getLatitud() != 0.0 || n.getLongitud() != 0.0) {
                    coords = String.format(" - GPS: (%.6f, %.6f)", n.getLatitud(), n.getLongitud());
                }
                pw.println(n.getId() + " - " + n.getNombre() + coords);
            }

            pw.println("\n=== Lista de Arcos (Rutas) ===");
            pw.println("Formato: Origen (ID: Nombre) -> Destino (ID: Nombre) | Ruta: [ID y Nombre]");
            pw.println("------------------------------------");
            
            // Agrupar arcos por ruta para mejor organización
            Map<String, List<Arco>> arcosPorRuta = grafo.obtenerArcosPorRuta();
            
            for (Map.Entry<String, List<Arco>> entrada : arcosPorRuta.entrySet()) {
                String ruta = entrada.getKey();
                List<Arco> arcos = entrada.getValue();
                
                pw.println("\n--- Ruta: " + ruta + " (" + arcos.size() + " arcos) ---");
                
                int secuencia = 1;
                for (Arco a : arcos) {
                    Nodo origen = a.getOrigen();
                    Nodo destino = a.getDestino();
                    
                    pw.println(String.format(
                        "%d. Origen: %s (ID: %s) -> Destino: %s (ID: %s) | Ruta: %s",
                        secuencia,
                        origen.getNombre(),
                        origen.getId(),
                        destino.getNombre(),
                        destino.getId(),
                        ruta
                    ));
                    secuencia++;
                }
            }

        } catch (Exception e) {
            System.err.println("Error al generar archivo TXT (" + nombre + "): " + e.getMessage());
        }
    }
}
