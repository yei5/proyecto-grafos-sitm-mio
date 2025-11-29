package com.sitm.mio.grafos;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Clase para visualizar grafos sobre mapas reales usando JXMapViewer2
 */
public class VisualizadorMapa {
    
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
     * Muestra el grafo en un mapa interactivo en tiempo real usando JXMapViewer2
     * @param grafo Grafo a visualizar
     * @param titulo Título de la ventana
     */
    public static void mostrarMapaInteractivo(Grafo grafo, String titulo) {
        try {
            // Usar reflexión para cargar JXMapViewer2 dinámicamente
            Class<?> mapViewerClass = Class.forName("org.jxmapviewer.JXMapViewer");
            Object mapViewer = mapViewerClass.getDeclaredConstructor().newInstance();
            
            // Configurar tile factory
            Class<?> osmTileFactoryInfoClass = Class.forName("org.jxmapviewer.OSMTileFactoryInfo");
            Object tileFactoryInfo = osmTileFactoryInfoClass.getDeclaredConstructor().newInstance();
            
            Class<?> defaultTileFactoryClass = Class.forName("org.jxmapviewer.viewer.DefaultTileFactory");
            Constructor<?> tileFactoryConstructor = defaultTileFactoryClass.getConstructor(
                Class.forName("org.jxmapviewer.viewer.TileFactoryInfo"));
            Object tileFactory = tileFactoryConstructor.newInstance(tileFactoryInfo);
            
            Method setTileFactory = mapViewerClass.getMethod("setTileFactory", 
                Class.forName("org.jxmapviewer.viewer.TileFactory"));
            setTileFactory.invoke(mapViewer, tileFactory);
            
            // Calcular centro
            Object centro = calcularCentroGrafo(grafo);
            if (centro != null) {
                Method setAddressLocation = mapViewerClass.getMethod("setAddressLocation", 
                    Class.forName("org.jxmapviewer.viewer.GeoPosition"));
                setAddressLocation.invoke(mapViewer, centro);
                Method setZoom = mapViewerClass.getMethod("setZoom", int.class);
                setZoom.invoke(mapViewer, 12);
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
            
            // Crear painters usando reflexión - crear implementaciones dinámicas
            Class<?> painterClass = Class.forName("org.jxmapviewer.painter.Painter");
            List<Object> painters = new ArrayList<>();
            
            // Crear painters dinámicamente usando Proxy
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
            
            // Crear ventana
            JFrame frame = new JFrame(titulo != null ? titulo : "Grafo SITM-MIO - Cali, Colombia");
            frame.setLayout(new BorderLayout());
            frame.add((Component) mapViewer, BorderLayout.CENTER);
            
            JPanel infoPanel = crearPanelInformacion(grafo);
            frame.add(infoPanel, BorderLayout.SOUTH);
            
            frame.setSize(1400, 900);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            
        } catch (ClassNotFoundException e) {
            System.out.println("JXMapViewer2 no encontrado. El mapa interactivo no está disponible.");
            System.out.println("Para habilitarlo, ejecuta: mvn clean compile");
        } catch (Exception e) {
            System.err.println("Error al mostrar mapa interactivo: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Calcula el centro geográfico del grafo basado en las coordenadas de los nodos
     */
    private static Object calcularCentroGrafo(Grafo grafo) {
        try {
            double sumLat = 0.0;
            double sumLon = 0.0;
            int count = 0;
            
            for (Nodo nodo : grafo.obtenerNodos()) {
                double lat = nodo.getLatitud();
                double lon = nodo.getLongitud();
                
                // Filtrar coordenadas válidas para Cali
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
     * Crea un panel con información del grafo
     */
    private static JPanel crearPanelInformacion(Grafo grafo) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Información del Grafo"));
        
        String info = String.format("Nodos: %d | Arcos: %d | Rutas: %d", 
                                    grafo.getNumeroNodos(), 
                                    grafo.getNumeroArcos(),
                                    grafo.obtenerArcosPorRuta().size());
        
        JLabel label = new JLabel(info);
        panel.add(label);
        
        return panel;
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
                    g.setStroke(new BasicStroke(2.0f));
                    
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
                        
                        g.setColor(new Color(41, 128, 185));
                        g.fillOval(x - 6, y - 6, 12, 12);
                        
                        g.setColor(new Color(21, 67, 96));
                        g.setStroke(new BasicStroke(2.0f));
                        g.drawOval(x - 6, y - 6, 12, 12);
                        
                        String nombre = nodo.getNombre();
                        if (nombre != null && !nombre.trim().isEmpty() && nombre.length() <= 8) {
                            g.setColor(Color.WHITE);
                            g.setFont(new Font("Arial", Font.BOLD, 8));
                            FontMetrics fm = g.getFontMetrics();
                            int textoAncho = fm.stringWidth(nombre);
                            g.drawString(nombre, x - textoAncho / 2, y + 3);
                        }
                    }
                }
            }
            return null;
        }
    }
}

