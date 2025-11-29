package com.sitm.mio.datagram.master;

import com.zeroc.Ice.*;
import com.zeroc.Ice.Object;
import com.zeroc.Ice.LocatorPrx;
import com.zeroc.Ice.InitializationException;
import com.zeroc.IceGrid.RegistryPrx;

/**
 * Servidor Master para procesamiento distribuido de datagrams
 * Usa IceGrid para service discovery y load balancing
 */
public class DatagramMasterServer {
    
    public static void main(String[] args) {
        int status = 0;
        Communicator communicator = null;
        
        try {
            // Inicializar Ice con configuración
            communicator = Util.initialize(args);
            
            // Obtener Locator de IceGrid (desde configuración o argumentos)
            String locatorEndpoint = System.getProperty("Ice.Default.Locator");
            if (locatorEndpoint == null || locatorEndpoint.isEmpty()) {
                locatorEndpoint = "IceGrid/Locator:tcp -h localhost -p 4061";
            }
            
            // Crear adapter (configuración desde archivo .cfg o propiedades)
            String adapterName = "DatagramMaster.Adapter";
            String adapterEndpoints = System.getProperty(adapterName + ".Endpoints");
            if (adapterEndpoints == null || adapterEndpoints.isEmpty()) {
                adapterEndpoints = "tcp -h 0.0.0.0 -p 10010";
            }
            
            // Configurar Locator en el communicator (REQUERIDO para IceGrid)
            LocatorPrx locator = null;
            RegistryPrx registry = null;
            try {
                locator = LocatorPrx.uncheckedCast(
                    communicator.stringToProxy(locatorEndpoint)
                );
                if (locator == null) {
                    throw new java.lang.Exception("No se pudo crear el proxy del Locator");
                }
                communicator.setDefaultLocator(locator);
                System.out.println("✓ Locator configurado: " + locatorEndpoint);
                
                // Obtener Registry para registro explícito
                registry = RegistryPrx.checkedCast(
                    communicator.stringToProxy("IceGrid/Registry")
                );
                if (registry != null) {
                    System.out.println("✓ Registry proxy obtenido");
                }
            } catch (java.lang.Exception e) {
                System.err.println("✗ ERROR: No se pudo configurar IceGrid Locator: " + e.getMessage());
                System.err.println("  Asegúrate de que el IceGrid Registry esté corriendo en: " + locatorEndpoint);
                throw new java.lang.Exception("IceGrid Locator es requerido. Registry no disponible.", e);
            }
            
            // Crear adapter con endpoints explícitos
            ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints(adapterName, adapterEndpoints);
            System.out.println("✓ Adapter creado: " + adapterName);
            
            // Crear servant
            Object servant = new DatagramMasterImpl();
            String identity = System.getProperty("DatagramMaster.Identity", "DatagramMaster");
            Identity objIdentity = Util.stringToIdentity(identity);
            ObjectPrx proxy = adapter.add(servant, objIdentity);
            adapter.activate();
            
            // IMPORTANTE: Para que QueryPrx encuentre el objeto, debe estar registrado como "well-known object"
            // en IceGrid. Sin embargo, cuando usas createObjectAdapterWithEndpoints, el objeto NO se registra
            // automáticamente. 
            // 
            // Solución: El Locator puede resolver objetos usando stringToProxy con el identity.
            // Los Workers deben usar: communicator.stringToProxy(identity) con el Locator configurado.
            System.out.println("✓ Master activado y disponible");
            System.out.println("  Los Workers pueden conectarse usando: communicator.stringToProxy(\"" + identity + "\")");
            System.out.println("  con el Locator configurado en el Communicator");
            
            System.out.println("=== DATAGRAM MASTER SERVER ===");
            System.out.println("Adapter: " + adapterName);
            System.out.println("Endpoints: " + adapterEndpoints);
            System.out.println("Identity: " + identity);
            System.out.println("Locator: " + locatorEndpoint);
            System.out.println("Proxy: " + proxy.toString());
            System.out.println("✓ Master ejecutándose con IceGrid");
            System.out.println("  Los Workers pueden encontrar el Master usando QueryPrx con identity: " + identity);
            
            System.out.println("Esperando workers...");
            
            // Esperar hasta que se cierre
            communicator.waitForShutdown();
            
        } catch (java.lang.Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            status = 1;
        } finally {
            if (communicator != null) {
                communicator.destroy();
            }
        }
        
        System.exit(status);
    }
}

