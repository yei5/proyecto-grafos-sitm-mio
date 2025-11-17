package com.sitm.mio.grafos;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Clase para visualizar y exportar grafos como imágenes PNG
 */
public class VisualizadorGrafo {
    
    private static final int ANCHO_DEFAULT = 1200;
    private static final int ALTO_DEFAULT = 1000;
    private static final int MARGEN = 80;
    private static final int RADIO_NODO = 18;
    private static final int TAMAÑO_FLECHA = 12;
    
    private static final Color[] COLORES_RUTAS = {
        new Color(52, 152, 219),   // Azul
        new Color(231, 76, 60),    // Rojo
        new Color(46, 204, 113),   // Verde
        new Color(155, 89, 182),   // Púrpura
        new Color(241, 196, 15),   // Amarillo
        new Color(230, 126, 34),   // Naranja
        new Color(26, 188, 156),   // Turquesa
        new Color(149, 165, 166)   // Gris
    };
    
    /**
     * Exporta el grafo como una imagen PNG con tamaño por defecto
     * @param grafo Grafo a exportar
     * @param rutaArchivo Ruta donde se guardará la imagen (ejemplo: "grafo.png")
     */
    public static void exportar(Grafo grafo, String rutaArchivo) {
        exportar(grafo, rutaArchivo, ANCHO_DEFAULT, ALTO_DEFAULT);
    }
    
    /**
     * Exporta el grafo como una imagen PNG con tamaño personalizado
     * @param grafo Grafo a exportar
     * @param rutaArchivo Ruta donde se guardará la imagen
     * @param ancho Ancho de la imagen en píxeles
     * @param alto Alto de la imagen en píxeles
     */
    public static void exportar(Grafo grafo, String rutaArchivo, int ancho, int alto) {
        try {
            // Crear imagen
            BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = imagen.createGraphics();
            
            // Configurar calidad de renderizado
            configurarRenderizado(g2d);
            
            // Fondo blanco
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, ancho, alto);
            
            // Calcular área de dibujo
            int areaAncho = ancho - (2 * MARGEN);
            int areaAlto = alto - (2 * MARGEN);
            
            // Calcular posiciones de los nodos
            Map<String, Point> posicionesNodos = calcularPosicionesCirculares(
                grafo, areaAncho, areaAlto, MARGEN
            );
            
            // Dibujar componentes
            dibujarArcos(g2d, grafo, posicionesNodos);
            dibujarNodos(g2d, grafo, posicionesNodos);
            dibujarTitulo(g2d, ancho);
            dibujarEstadisticas(g2d, grafo, ancho, alto);
            dibujarLeyenda(g2d, grafo, ancho, alto);
            
            // Guardar imagen
            g2d.dispose();
            ImageIO.write(imagen, "PNG", new File(rutaArchivo));
            
            System.out.println("Imagen PNG exportada correctamente en: " + rutaArchivo);
            
        } catch (IOException e) {
            System.err.println("Error al exportar imagen PNG: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Configura la calidad de renderizado
     */
    private static void configurarRenderizado(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                            RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, 
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, 
                            RenderingHints.VALUE_RENDER_QUALITY);
    }
    
    /**
     * Calcula las posiciones de los nodos en un layout circular
     */
    private static Map<String, Point> calcularPosicionesCirculares(
            Grafo grafo, int areaAncho, int areaAlto, int margen) {
        
        Map<String, Point> posiciones = new HashMap<>();
        List<Nodo> listaNodos = new ArrayList<>(grafo.obtenerNodos());
        
        int n = listaNodos.size();
        if (n == 0) return posiciones;
        
        // Radio del círculo
        int radio = Math.min(areaAncho, areaAlto) / 2 - 30;
        
        // Centro del área de dibujo
        int centroX = margen + areaAncho / 2;
        int centroY = margen + areaAlto / 2;
        
        // Distribuir nodos uniformemente en círculo
        for (int i = 0; i < n; i++) {
            double angulo = 2 * Math.PI * i / n - Math.PI / 2;
            int x = centroX + (int)(radio * Math.cos(angulo));
            int y = centroY + (int)(radio * Math.sin(angulo));
            posiciones.put(listaNodos.get(i).getId(), new Point(x, y));
        }
        
        return posiciones;
    }
    
    /**
     * Dibuja los arcos del grafo
     */
    private static void dibujarArcos(Graphics2D g2d, Grafo grafo, 
                                     Map<String, Point> posiciones) {
        g2d.setStroke(new BasicStroke(2.0f));
        
        Map<String, List<Arco>> arcosPorRuta = grafo.obtenerArcosPorRuta();
        
        int indiceColor = 0;
        for (Map.Entry<String, List<Arco>> entrada : arcosPorRuta.entrySet()) {
            Color colorRuta = COLORES_RUTAS[indiceColor % COLORES_RUTAS.length];
            g2d.setColor(colorRuta);
            
            for (Arco arco : entrada.getValue()) {
                Point p1 = posiciones.get(arco.getOrigen().getId());
                Point p2 = posiciones.get(arco.getDestino().getId());
                
                if (p1 != null && p2 != null) {
                    g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
                    dibujarFlecha(g2d, p1, p2, colorRuta);
                }
            }
            indiceColor++;
        }
    }
    
    /**
     * Dibuja una flecha en el extremo de un arco
     */
    private static void dibujarFlecha(Graphics2D g2d, Point origen, 
                                      Point destino, Color color) {
        double angulo = Math.atan2(destino.y - origen.y, destino.x - origen.x);
        
        int ajuste = 20;
        int x2 = destino.x - (int)(ajuste * Math.cos(angulo));
        int y2 = destino.y - (int)(ajuste * Math.sin(angulo));
        
        int x1 = x2 - (int)(TAMAÑO_FLECHA * Math.cos(angulo - Math.PI / 6));
        int y1 = y2 - (int)(TAMAÑO_FLECHA * Math.sin(angulo - Math.PI / 6));
        
        int x3 = x2 - (int)(TAMAÑO_FLECHA * Math.cos(angulo + Math.PI / 6));
        int y3 = y2 - (int)(TAMAÑO_FLECHA * Math.sin(angulo + Math.PI / 6));
        
        g2d.setColor(color);
        g2d.fillPolygon(new int[]{x2, x1, x3}, new int[]{y2, y1, y3}, 3);
    }
    
    /**
     * Dibuja los nodos del grafo
     */
    private static void dibujarNodos(Graphics2D g2d, Grafo grafo, 
                                     Map<String, Point> posiciones) {
        Font fuenteNodo = new Font("Arial", Font.BOLD, 11);
        g2d.setFont(fuenteNodo);
        
        for (Nodo nodo : grafo.obtenerNodos()) {
            Point pos = posiciones.get(nodo.getId());
            if (pos != null) {
                // Círculo del nodo
                g2d.setColor(new Color(41, 128, 185));
                g2d.fillOval(pos.x - RADIO_NODO, pos.y - RADIO_NODO, 
                           RADIO_NODO * 2, RADIO_NODO * 2);
                
                // Borde del nodo
                g2d.setColor(new Color(21, 67, 96));
                g2d.setStroke(new BasicStroke(2.5f));
                g2d.drawOval(pos.x - RADIO_NODO, pos.y - RADIO_NODO, 
                           RADIO_NODO * 2, RADIO_NODO * 2);
                
                // ID del nodo
                g2d.setColor(Color.WHITE);
                FontMetrics fm = g2d.getFontMetrics();
                int textoAncho = fm.stringWidth(nodo.getId());
                int textoAlto = fm.getAscent();
                g2d.drawString(nodo.getId(), 
                             pos.x - textoAncho / 2, 
                             pos.y + textoAlto / 2 - 2);
                
                // Nombre del nodo
                Font fuenteNombre = new Font("Arial", Font.PLAIN, 10);
                g2d.setFont(fuenteNombre);
                g2d.setColor(Color.BLACK);
                fm = g2d.getFontMetrics();
                
                String nombre = nodo.getNombre();
                if (nombre.length() > 20) {
                    nombre = nombre.substring(0, 17) + "...";
                }
                
                textoAncho = fm.stringWidth(nombre);
                g2d.drawString(nombre, 
                             pos.x - textoAncho / 2, 
                             pos.y + RADIO_NODO + 15);
                
                g2d.setFont(fuenteNodo);
            }
        }
    }
    
    /**
     * Dibuja el título del grafo
     */
    private static void dibujarTitulo(Graphics2D g2d, int ancho) {
        Font fuenteTitulo = new Font("Arial", Font.BOLD, 20);
        g2d.setFont(fuenteTitulo);
        g2d.setColor(new Color(44, 62, 80));
        
        String titulo = "Grafo SITM-MIO";
        FontMetrics fm = g2d.getFontMetrics();
        int textoAncho = fm.stringWidth(titulo);
        g2d.drawString(titulo, (ancho - textoAncho) / 2, 35);
    }
    
    /**
     * Dibuja las estadísticas del grafo
     */
    private static void dibujarEstadisticas(Graphics2D g2d, Grafo grafo, 
                                           int ancho, int alto) {
        Font fuenteStats = new Font("Arial", Font.PLAIN, 12);
        g2d.setFont(fuenteStats);
        g2d.setColor(new Color(52, 73, 94));
        
        String stats = String.format(
            "Nodos: %d | Arcos: %d | Velocidad promedio: %.2f km/h", 
            grafo.getNumeroNodos(), 
            grafo.getNumeroArcos(), 
            grafo.calcularVelocidadPromedioGeneral()
        );
        
        FontMetrics fm = g2d.getFontMetrics();
        int textoAncho = fm.stringWidth(stats);
        g2d.drawString(stats, (ancho - textoAncho) / 2, alto - 25);
    }
    
    /**
     * Dibuja la leyenda de colores por ruta
     */
    private static void dibujarLeyenda(Graphics2D g2d, Grafo grafo, 
                                      int ancho, int alto) {
        Map<String, List<Arco>> arcosPorRuta = grafo.obtenerArcosPorRuta();
        
        if (arcosPorRuta.isEmpty()) return;
        
        Font fuenteLeyenda = new Font("Arial", Font.PLAIN, 10);
        g2d.setFont(fuenteLeyenda);
        
        int x = 15;
        int y = alto - 100;
        int espaciado = 18;
        int indiceColor = 0;
        
        g2d.setColor(new Color(44, 62, 80));
        g2d.drawString("Rutas:", x, y);
        y += espaciado;
        
        for (String ruta : arcosPorRuta.keySet()) {
            Color color = COLORES_RUTAS[indiceColor % COLORES_RUTAS.length];
            
            // Dibujar cuadrado de color
            g2d.setColor(color);
            g2d.fillRect(x, y - 8, 12, 12);
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1.0f));
            g2d.drawRect(x, y - 8, 12, 12);
            
            // Dibujar nombre de ruta
            g2d.drawString(ruta, x + 18, y + 3);
            
            y += espaciado;
            indiceColor++;
            
            // Limitar cantidad de rutas en leyenda
            if (indiceColor >= 10) {
                g2d.drawString("...", x + 18, y + 3);
                break;
            }
        }
    }
}