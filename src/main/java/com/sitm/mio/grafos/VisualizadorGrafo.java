package com.sitm.mio.grafos;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Clase para visualizar y exportar grafos como imágenes PNG
 */
public class VisualizadorGrafo {
    
    private static final int ANCHO_DEFAULT = 1600;
    private static final int ALTO_DEFAULT = 1200;
    private static final int MARGEN = 50;
    private static final int RADIO_NODO = 12;
    private static final int TAMAÑO_FLECHA = 10;
    
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
        exportar(grafo, rutaArchivo, null);
    }
    
    /**
     * Exporta el grafo como una imagen PNG con título personalizado
     * @param grafo Grafo a exportar
     * @param rutaArchivo Ruta donde se guardará la imagen
     * @param titulo Título personalizado (null para usar título por defecto)
     */
    public static void exportar(Grafo grafo, String rutaArchivo, String titulo) {
        exportar(grafo, rutaArchivo, titulo, ANCHO_DEFAULT, ALTO_DEFAULT);
    }
    
    /**
     * Exporta el grafo como una imagen PNG con título y tamaño personalizados
     * Intenta usar JXMapViewer2 para renderizar el mapa real como fondo
     * @param grafo Grafo a exportar
     * @param rutaArchivo Ruta donde se guardará la imagen
     * @param titulo Título personalizado (null para usar título por defecto)
     * @param ancho Ancho de la imagen en píxeles
     * @param alto Alto de la imagen en píxeles
     */
    public static void exportar(Grafo grafo, String rutaArchivo, String titulo, int ancho, int alto) {
        try {
            // Por ahora, usar siempre el método tradicional
            // El renderizado con mapa real requiere configuración adicional
            BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = imagen.createGraphics();
            configurarRenderizado(g2d);
            
            // Fondo blanco
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, ancho, alto);
            
            // Calcular área de dibujo
            int areaAncho = ancho - (2 * MARGEN);
            int areaAlto = alto - (2 * MARGEN);
            
            // Calcular posiciones de los nodos basadas en GPS
            Map<String, Point> posicionesNodos = calcularPosicionesGPS(
                grafo, areaAncho, areaAlto, MARGEN
            );
            
            // Determinar título
            String tituloFinal = titulo;
            if (tituloFinal == null) {
                Map<String, List<Arco>> arcosPorRuta = grafo.obtenerArcosPorRuta();
                if (arcosPorRuta.size() == 1) {
                    tituloFinal = "Ruta: " + arcosPorRuta.keySet().iterator().next();
                } else {
                    tituloFinal = "Grafo SITM-MIO - Cali, Colombia";
                }
            }
            
            // Dibujar componentes
            dibujarMapaFondo(g2d, grafo, areaAncho, areaAlto, MARGEN);
            dibujarArcos(g2d, grafo, posicionesNodos);
            dibujarNodos(g2d, grafo, posicionesNodos);
            dibujarTitulo(g2d, ancho, tituloFinal);
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
     * Renderiza el grafo sobre un mapa real usando JXMapViewer2 (sin mostrar ventana)
     * @param grafo Grafo a renderizar
     * @param ancho Ancho de la imagen
     * @param alto Alto de la imagen
     * @return BufferedImage con el mapa renderizado, o null si no está disponible
     */
    private static BufferedImage renderizarConMapaReal(Grafo grafo, int ancho, int alto) {
        try {
            // Usar reflexión para cargar JXMapViewer2
            Class<?> mapViewerClass = Class.forName("org.jxmapviewer.JXMapViewer");
            Object mapViewer = mapViewerClass.getDeclaredConstructor().newInstance();
            
            // Configurar tamaño
            if (mapViewer instanceof java.awt.Component) {
                ((java.awt.Component) mapViewer).setSize(ancho, alto);
                ((java.awt.Component) mapViewer).setBounds(0, 0, ancho, alto);
            } else {
                Method setSize = mapViewerClass.getMethod("setSize", int.class, int.class);
                setSize.invoke(mapViewer, ancho, alto);
            }
            
            // Configurar tile factory para OpenStreetMap
            Class<?> osmTileFactoryInfoClass = Class.forName("org.jxmapviewer.OSMTileFactoryInfo");
            Object tileFactoryInfo = osmTileFactoryInfoClass.getDeclaredConstructor().newInstance();
            
            Class<?> defaultTileFactoryClass = Class.forName("org.jxmapviewer.viewer.DefaultTileFactory");
            Constructor<?> tileFactoryConstructor = defaultTileFactoryClass.getConstructor(
                Class.forName("org.jxmapviewer.viewer.TileFactoryInfo"));
            Object tileFactory = tileFactoryConstructor.newInstance(tileFactoryInfo);
            
            Method setTileFactory = mapViewerClass.getMethod("setTileFactory", 
                Class.forName("org.jxmapviewer.viewer.TileFactory"));
            setTileFactory.invoke(mapViewer, tileFactory);
            
            // Calcular centro del grafo
            Object centro = calcularCentroGrafoParaMapa(grafo);
            if (centro != null) {
                Method setAddressLocation = mapViewerClass.getMethod("setAddressLocation", 
                    Class.forName("org.jxmapviewer.viewer.GeoPosition"));
                setAddressLocation.invoke(mapViewer, centro);
                
                int zoom = calcularZoomApropiado(grafo);
                Method setZoom = mapViewerClass.getMethod("setZoom", int.class);
                setZoom.invoke(mapViewer, zoom);
            } else {
                // Centro por defecto: Cali
                Class<?> geoPositionClass = Class.forName("org.jxmapviewer.viewer.GeoPosition");
                Constructor<?> geoPosConstructor = geoPositionClass.getConstructor(double.class, double.class);
                Object caliPos = geoPosConstructor.newInstance(3.4516, -76.5320);
                Method setAddressLocation = mapViewerClass.getMethod("setAddressLocation", geoPositionClass);
                setAddressLocation.invoke(mapViewer, caliPos);
                Method setZoom = mapViewerClass.getMethod("setZoom", int.class);
                setZoom.invoke(mapViewer, 12);
            }
            
            // Crear painters para rutas y nodos
            Class<?> painterClass = Class.forName("org.jxmapviewer.painter.Painter");
            List<Object> painters = new ArrayList<>();
            
            java.lang.reflect.InvocationHandler routeHandler = new RoutePainterHandler(grafo, COLORES_RUTAS);
            Object routePainter = java.lang.reflect.Proxy.newProxyInstance(
                painterClass.getClassLoader(),
                new Class[]{painterClass},
                routeHandler);
            painters.add(routePainter);
            
            java.lang.reflect.InvocationHandler nodeHandler = new NodePainterHandler(grafo);
            Object nodePainter = java.lang.reflect.Proxy.newProxyInstance(
                painterClass.getClassLoader(),
                new Class[]{painterClass},
                nodeHandler);
            painters.add(nodePainter);
            
            // Crear CompoundPainter
            Class<?> compoundPainterClass = Class.forName("org.jxmapviewer.painter.CompoundPainter");
            Constructor<?> compoundConstructor = compoundPainterClass.getConstructor(List.class);
            Object compoundPainter = compoundConstructor.newInstance(painters);
            
            Method setOverlayPainter = mapViewerClass.getMethod("setOverlayPainter", painterClass);
            setOverlayPainter.invoke(mapViewer, compoundPainter);
            
            // Renderizar el mapa a una imagen (sin mostrar ventana)
            BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = imagen.createGraphics();
            configurarRenderizado(g2d);
            
            // Pintar el mapa usando el componente
            if (mapViewer instanceof java.awt.Component) {
                java.awt.Component mapComponent = (java.awt.Component) mapViewer;
                // Forzar actualización del mapa
                try {
                    Method updateUI = mapComponent.getClass().getMethod("updateUI");
                    updateUI.invoke(mapComponent);
                } catch (Exception e) {
                    // Ignorar si no existe
                }
                // Pintar el componente en la imagen
                mapComponent.paint(g2d);
            } else {
                // Método alternativo usando paintComponent
                try {
                    Method paintComponent = mapViewerClass.getMethod("paintComponent", Graphics2D.class);
                    paintComponent.invoke(mapViewer, g2d);
                } catch (Exception e) {
                    g2d.dispose();
                    return null;
                }
            }
            
            g2d.dispose();
            return imagen;
            
        } catch (ClassNotFoundException e) {
            // JXMapViewer2 no está disponible
            return null;
        } catch (Exception e) {
            System.err.println("Error al renderizar con mapa real: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Verifica si una imagen es completamente blanca
     */
    private static boolean esImagenBlanca(BufferedImage imagen) {
        if (imagen == null) return true;
        int width = imagen.getWidth();
        int height = imagen.getHeight();
        int whitePixel = Color.WHITE.getRGB();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (imagen.getRGB(x, y) != whitePixel) {
                    return false;
                }
            }
        }
        return true;
    }
    
    
    /**
     * Calcula el centro geográfico del grafo para el mapa
     */
    private static Object calcularCentroGrafoParaMapa(Grafo grafo) {
        try {
            double sumLat = 0.0;
            double sumLon = 0.0;
            int count = 0;
            
            for (Nodo nodo : grafo.obtenerNodos()) {
                double lat = nodo.getLatitud();
                double lon = nodo.getLongitud();
                
                if (lat != 0.0 && lon != 0.0 && 
                    lon >= -80.0 && lon <= -75.0 && 
                    lat >= 3.0 && lat <= 4.0) {
                    sumLat += lat;
                    sumLon += lon;
                    count++;
                }
            }
            
            if (count > 0) {
                Class<?> geoPositionClass = Class.forName("org.jxmapviewer.viewer.GeoPosition");
                Constructor<?> constructor = geoPositionClass.getConstructor(double.class, double.class);
                return constructor.newInstance(sumLat / count, sumLon / count);
            }
        } catch (Exception e) {
            // Si falla, retornar null
        }
        
        return null;
    }
    
    /**
     * Calcula el zoom apropiado basado en el bounding box del grafo
     */
    private static int calcularZoomApropiado(Grafo grafo) {
        double minLon = Double.MAX_VALUE;
        double maxLon = Double.MIN_VALUE;
        double minLat = Double.MAX_VALUE;
        double maxLat = Double.MIN_VALUE;
        int count = 0;
        
        for (Nodo nodo : grafo.obtenerNodos()) {
            double lat = nodo.getLatitud();
            double lon = nodo.getLongitud();
            
            if (lat != 0.0 && lon != 0.0 && 
                lon >= -80.0 && lon <= -75.0 && 
                lat >= 3.0 && lat <= 4.0) {
                minLon = Math.min(minLon, lon);
                maxLon = Math.max(maxLon, lon);
                minLat = Math.min(minLat, lat);
                maxLat = Math.max(maxLat, lat);
                count++;
            }
        }
        
        if (count == 0) return 12; // Zoom por defecto
        
        // Calcular el rango
        double rangoLon = maxLon - minLon;
        double rangoLat = maxLat - minLat;
        double rangoMax = Math.max(rangoLon, rangoLat);
        
        // Ajustar zoom basado en el rango
        if (rangoMax > 0.1) return 11;
        if (rangoMax > 0.05) return 12;
        if (rangoMax > 0.02) return 13;
        if (rangoMax > 0.01) return 14;
        return 15;
    }
    
    /**
     * Handler para dibujar las rutas en el mapa usando reflexión
     */
    private static class RoutePainterHandler implements java.lang.reflect.InvocationHandler {
        private Grafo grafo;
        private Color[] colores;
        
        public RoutePainterHandler(Grafo grafo, Color[] colores) {
            this.grafo = grafo;
            this.colores = colores;
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().equals("paint")) {
                Graphics2D g = (Graphics2D) args[0];
                Object map = args[1];
                
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                Method getTileFactory = map.getClass().getMethod("getTileFactory");
                Object tileFactory = getTileFactory.invoke(map);
                Method getZoom = map.getClass().getMethod("getZoom");
                int zoom = (Integer) getZoom.invoke(map);
                
                Class<?> geoPositionClass = Class.forName("org.jxmapviewer.viewer.GeoPosition");
                Constructor<?> geoPosConstructor = geoPositionClass.getConstructor(double.class, double.class);
                Method geoToPixel = tileFactory.getClass().getMethod("geoToPixel", geoPositionClass, int.class);
                
                Map<String, List<Arco>> arcosPorRuta = grafo.obtenerArcosPorRuta();
                int indiceColor = 0;
                
                for (Map.Entry<String, List<Arco>> entrada : arcosPorRuta.entrySet()) {
                    List<Arco> arcos = entrada.getValue();
                    Color colorRuta = colores[indiceColor % colores.length];
                    g.setColor(colorRuta);
                    g.setStroke(new BasicStroke(3.0f)); // Líneas más gruesas para mejor visibilidad
                    
                    for (Arco arco : arcos) {
                        Nodo origen = arco.getOrigen();
                        Nodo destino = arco.getDestino();
                        
                        double latOrigen = origen.getLatitud();
                        double lonOrigen = origen.getLongitud();
                        double latDestino = destino.getLatitud();
                        double lonDestino = destino.getLongitud();
                        
                        if (latOrigen != 0.0 && lonOrigen != 0.0 && 
                            latDestino != 0.0 && lonDestino != 0.0 &&
                            lonOrigen >= -80.0 && lonOrigen <= -75.0 &&
                            lonDestino >= -80.0 && lonDestino <= -75.0 &&
                            latOrigen >= 3.0 && latOrigen <= 4.0 &&
                            latDestino >= 3.0 && latDestino <= 4.0) {
                            
                            Object posOrigen = geoPosConstructor.newInstance(latOrigen, lonOrigen);
                            Object posDestino = geoPosConstructor.newInstance(latDestino, lonDestino);
                            
                            Point2D pt1 = (Point2D) geoToPixel.invoke(tileFactory, posOrigen, zoom);
                            Point2D pt2 = (Point2D) geoToPixel.invoke(tileFactory, posDestino, zoom);
                            
                            g.drawLine((int) pt1.getX(), (int) pt1.getY(), 
                                      (int) pt2.getX(), (int) pt2.getY());
                        }
                    }
                    indiceColor++;
                }
            }
            return null;
        }
    }
    
    /**
     * Handler para dibujar los nodos en el mapa usando reflexión
     */
    private static class NodePainterHandler implements java.lang.reflect.InvocationHandler {
        private Grafo grafo;
        
        public NodePainterHandler(Grafo grafo) {
            this.grafo = grafo;
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().equals("paint")) {
                Graphics2D g = (Graphics2D) args[0];
                Object map = args[1];
                
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                Method getTileFactory = map.getClass().getMethod("getTileFactory");
                Object tileFactory = getTileFactory.invoke(map);
                Method getZoom = map.getClass().getMethod("getZoom");
                int zoom = (Integer) getZoom.invoke(map);
                
                Class<?> geoPositionClass = Class.forName("org.jxmapviewer.viewer.GeoPosition");
                Constructor<?> geoPosConstructor = geoPositionClass.getConstructor(double.class, double.class);
                Method geoToPixel = tileFactory.getClass().getMethod("geoToPixel", geoPositionClass, int.class);
                
                for (Nodo nodo : grafo.obtenerNodos()) {
                    double lat = nodo.getLatitud();
                    double lon = nodo.getLongitud();
                    
                    if (lat != 0.0 && lon != 0.0 && 
                        lon >= -80.0 && lon <= -75.0 && 
                        lat >= 3.0 && lat <= 4.0) {
                        
                        Object pos = geoPosConstructor.newInstance(lat, lon);
                        Point2D pt = (Point2D) geoToPixel.invoke(tileFactory, pos, zoom);
                        
                        int x = (int) pt.getX();
                        int y = (int) pt.getY();
                        
                        // Círculo más grande y visible
                        g.setColor(new Color(41, 128, 185));
                        g.fillOval(x - 8, y - 8, 16, 16);
                        
                        g.setColor(new Color(21, 67, 96));
                        g.setStroke(new BasicStroke(2.5f));
                        g.drawOval(x - 8, y - 8, 16, 16);
                        
                        // Nombre del nodo
                        String nombre = nodo.getNombre();
                        if (nombre != null && !nombre.trim().isEmpty()) {
                            String nombreMostrar = nombre.length() > 10 ? nombre.substring(0, 7) + "..." : nombre;
                            g.setColor(Color.WHITE);
                            g.setFont(new Font("Arial", Font.BOLD, 9));
                            FontMetrics fm = g.getFontMetrics();
                            int textoAncho = fm.stringWidth(nombreMostrar);
                            g.drawString(nombreMostrar, x - textoAncho / 2, y + 4);
                        }
                    }
                }
            }
            return null;
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
     * Calcula las posiciones de los nodos basadas en coordenadas GPS usando proyección Mercator
     */
    private static Map<String, Point> calcularPosicionesGPS(
            Grafo grafo, int areaAncho, int areaAlto, int margen) {
        
        Map<String, Point> posiciones = new HashMap<>();
        List<Nodo> listaNodos = new ArrayList<>(grafo.obtenerNodos());
        
        if (listaNodos.isEmpty()) return posiciones;
        
        // Calcular bounding box de las coordenadas GPS válidas
        double minLon = Double.MAX_VALUE;
        double maxLon = Double.MIN_VALUE;
        double minLat = Double.MAX_VALUE;
        double maxLat = Double.MIN_VALUE;
        
        boolean tieneCoordenadas = false;
        for (Nodo nodo : listaNodos) {
            double lon = nodo.getLongitud();
            double lat = nodo.getLatitud();
            // Filtrar coordenadas inválidas (0,0 o fuera de rango razonable para Cali)
            if (lon != 0.0 && lat != 0.0 && 
                lon >= -80.0 && lon <= -75.0 && // Rango razonable para Colombia
                lat >= 3.0 && lat <= 4.0) {     // Rango razonable para Cali
                tieneCoordenadas = true;
                minLon = Math.min(minLon, lon);
                maxLon = Math.max(maxLon, lon);
                minLat = Math.min(minLat, lat);
                maxLat = Math.max(maxLat, lat);
            }
        }
        
        // Si no hay coordenadas GPS válidas, usar layout circular como fallback
        if (!tieneCoordenadas || (maxLon - minLon) < 0.001 || (maxLat - minLat) < 0.001) {
            return calcularPosicionesCirculares(grafo, areaAncho, areaAlto, margen);
        }
        
        // Agregar padding al bounding box (5% en cada lado)
        double paddingLon = (maxLon - minLon) * 0.05;
        double paddingLat = (maxLat - minLat) * 0.05;
        minLon -= paddingLon;
        maxLon += paddingLon;
        minLat -= paddingLat;
        maxLat += paddingLat;
        
        // Calcular factores de escala
        double rangoLon = maxLon - minLon;
        double rangoLat = maxLat - minLat;
        
        // Convertir coordenadas GPS a píxeles usando proyección simple
        // Usar nombre como clave para posiciones (ya que estamos mergeando por nombre)
        Map<String, Point> posicionesPorNombre = new HashMap<>();
        
        for (Nodo nodo : listaNodos) {
            double lon = nodo.getLongitud();
            double lat = nodo.getLatitud();
            String nombre = nodo.getNombre();
            if (nombre == null || nombre.trim().isEmpty()) {
                nombre = nodo.getId();
            }
            
            // Si ya calculamos la posición para este nombre, reutilizarla
            if (posicionesPorNombre.containsKey(nombre)) {
                posiciones.put(nodo.getId(), posicionesPorNombre.get(nombre));
                continue;
            }
            
            // Si no tiene coordenadas válidas, usar posición central
            if (lon == 0.0 && lat == 0.0 || 
                lon < -80.0 || lon > -75.0 || lat < 3.0 || lat > 4.0) {
                int x = margen + areaAncho / 2;
                int y = margen + areaAlto / 2;
                Point punto = new Point(x, y);
                posiciones.put(nodo.getId(), punto);
                posicionesPorNombre.put(nombre, punto);
            } else {
                // Normalizar coordenadas (0.0 a 1.0) - proyección lineal simple
                double xNormalizado = (lon - minLon) / rangoLon;
                double yNormalizado = 1.0 - (lat - minLat) / rangoLat; // Invertir Y (latitud aumenta hacia arriba)
                
                // Convertir a píxeles
                int x = margen + (int)(xNormalizado * areaAncho);
                int y = margen + (int)(yNormalizado * areaAlto);
                
                Point punto = new Point(x, y);
                posiciones.put(nodo.getId(), punto);
                posicionesPorNombre.put(nombre, punto);
            }
        }
        
        return posiciones;
    }
    
    /**
     * Calcula las posiciones de los nodos en un layout circular (fallback)
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
     * Dibuja un mapa de fondo simple basado en las coordenadas GPS
     */
    private static void dibujarMapaFondo(Graphics2D g2d, Grafo grafo, 
                                        int areaAncho, int areaAlto, int margen) {
        List<Nodo> listaNodos = new ArrayList<>(grafo.obtenerNodos());
        
        if (listaNodos.isEmpty()) return;
        
        // Calcular bounding box
        double minLon = Double.MAX_VALUE;
        double maxLon = Double.MIN_VALUE;
        double minLat = Double.MAX_VALUE;
        double maxLat = Double.MIN_VALUE;
        
        boolean tieneCoordenadas = false;
        for (Nodo nodo : listaNodos) {
            if (nodo.getLongitud() != 0.0 || nodo.getLatitud() != 0.0) {
                tieneCoordenadas = true;
                minLon = Math.min(minLon, nodo.getLongitud());
                maxLon = Math.max(maxLon, nodo.getLongitud());
                minLat = Math.min(minLat, nodo.getLatitud());
                maxLat = Math.max(maxLat, nodo.getLatitud());
            }
        }
        
        if (!tieneCoordenadas) return;
        
        // Agregar padding
        double paddingLon = (maxLon - minLon) * 0.1;
        double paddingLat = (maxLat - minLat) * 0.1;
        minLon -= paddingLon;
        maxLon += paddingLon;
        minLat -= paddingLat;
        maxLat += paddingLat;
        
        // Dibujar fondo del mapa (color tierra/verde claro para representar terreno)
        g2d.setColor(new Color(245, 250, 245)); // Verde muy claro
        g2d.fillRect(margen, margen, areaAncho, areaAlto);
        
        // Dibujar borde del mapa
        g2d.setColor(new Color(180, 180, 180));
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawRect(margen, margen, areaAncho, areaAlto);
        
        // Dibujar grid de referencia más sutil
        g2d.setColor(new Color(230, 230, 230));
        g2d.setStroke(new BasicStroke(0.5f));
        int numLineas = 8;
        for (int i = 1; i < numLineas; i++) {
            int x = margen + (areaAncho * i / numLineas);
            int y = margen + (areaAlto * i / numLineas);
            g2d.drawLine(x, margen, x, margen + areaAlto);
            g2d.drawLine(margen, y, margen + areaAncho, y);
        }
        
        // Dibujar etiqueta del mapa con mejor formato
        Font fuenteMapa = new Font("Arial", Font.PLAIN, 9);
        g2d.setFont(fuenteMapa);
        g2d.setColor(new Color(80, 80, 80));
        String etiqueta = String.format("Cali, Colombia - Lat: %.4f a %.4f, Lon: %.4f a %.4f", 
                                        minLat, maxLat, minLon, maxLon);
        FontMetrics fm = g2d.getFontMetrics();
        int textoAncho = fm.stringWidth(etiqueta);
        g2d.drawString(etiqueta, margen + (areaAncho - textoAncho) / 2, margen + areaAlto + 15);
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
                
                // Mostrar nombre del nodo (más importante que el ID)
                String nombre = nodo.getNombre();
                if (nombre == null || nombre.trim().isEmpty()) {
                    nombre = nodo.getId();
                }
                
                // Truncar nombre si es muy largo para el círculo
                String nombreMostrar = nombre;
                if (nombreMostrar.length() > 10) {
                    nombreMostrar = nombreMostrar.substring(0, 7) + "...";
                }
                
                // Dibujar nombre en el círculo (más visible, fuente más pequeña para que quepa)
                Font fuenteNombreCirculo = new Font("Arial", Font.BOLD, 9);
                g2d.setFont(fuenteNombreCirculo);
                g2d.setColor(Color.WHITE);
                FontMetrics fm = g2d.getFontMetrics();
                int textoAncho = fm.stringWidth(nombreMostrar);
                int textoAlto = fm.getAscent();
                g2d.drawString(nombreMostrar, 
                             pos.x - textoAncho / 2, 
                             pos.y + textoAlto / 2 - 1);
                
                // Dibujar nombre completo debajo del nodo (si hay espacio)
                Font fuenteNombreCompleto = new Font("Arial", Font.PLAIN, 8);
                g2d.setFont(fuenteNombreCompleto);
                g2d.setColor(new Color(60, 60, 60));
                fm = g2d.getFontMetrics();
                
                String nombreCompleto = nombre;
                if (nombreCompleto.length() > 20) {
                    nombreCompleto = nombreCompleto.substring(0, 17) + "...";
                }
                
                textoAncho = fm.stringWidth(nombreCompleto);
                g2d.drawString(nombreCompleto, 
                             pos.x - textoAncho / 2, 
                             pos.y + RADIO_NODO + 10);
                
                g2d.setFont(fuenteNodo);
            }
        }
    }
    
    /**
     * Dibuja el título del grafo
     */
    private static void dibujarTitulo(Graphics2D g2d, int ancho, String titulo) {
        Font fuenteTitulo = new Font("Arial", Font.BOLD, 20);
        g2d.setFont(fuenteTitulo);
        g2d.setColor(new Color(44, 62, 80));
        
        if (titulo == null) {
            titulo = "Grafo SITM-MIO";
        }
        
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