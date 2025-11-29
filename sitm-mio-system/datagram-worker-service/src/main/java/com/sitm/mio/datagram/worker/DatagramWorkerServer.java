package com.sitm.mio.datagram.worker;

import DatagramProcessing.*;
import com.zeroc.Ice.*;
import com.zeroc.Ice.Object;
import com.zeroc.Ice.LocatorPrx;
import com.zeroc.Ice.InitializationException;
import com.zeroc.IceGrid.QueryPrx;

/**
 * Servidor Worker para procesar lotes de datagrams
 * Usa IceGrid para service discovery
 */
public class DatagramWorkerServer {
    
    public static void main(String[] args) {
        int status = 0;
        Communicator communicator = null;
        
        try {
            // Inicializar Ice con configuración
            communicator = Util.initialize(args);
            
            // Configurar Locator para IceGrid (REQUERIDO)
            String locatorEndpoint = System.getProperty("Ice.Default.Locator");
            if (locatorEndpoint == null || locatorEndpoint.isEmpty()) {
                locatorEndpoint = "IceGrid/Locator:tcp -h localhost -p 4061";
            }
            
            LocatorPrx locator = null;
            try {
                locator = LocatorPrx.uncheckedCast(
                    communicator.stringToProxy(locatorEndpoint)
                );
                if (locator == null) {
                    throw new java.lang.Exception("No se pudo crear el proxy del Locator");
                }
                communicator.setDefaultLocator(locator);
                System.out.println("✓ Locator configurado: " + locatorEndpoint);
            } catch (java.lang.Exception e) {
                System.err.println("✗ ERROR: No se pudo configurar IceGrid Locator: " + e.getMessage());
                System.err.println("  Asegúrate de que el IceGrid Registry esté corriendo en: " + locatorEndpoint);
                throw new java.lang.Exception("IceGrid Locator es requerido. Registry no disponible.", e);
            }
            
            // Obtener configuración del adapter
            String adapterName = "DatagramWorker.Adapter";
            String adapterEndpoints = System.getProperty(adapterName + ".Endpoints");
            if (adapterEndpoints == null || adapterEndpoints.isEmpty()) {
                // Puerto desde argumentos o default
                int port = 10020;
                if (args.length > 0) {
                    try {
                        port = Integer.parseInt(args[0]);
                    } catch (NumberFormatException e) {
                        System.err.println("Puerto inválido, usando default: 10020");
                    }
                }
                adapterEndpoints = "tcp -h 0.0.0.0 -p " + port;
            }
            
            // Crear adapter usando IceGrid (sin fallback)
            ObjectAdapter adapter = null;
            try {
                // Intentar crear adapter sin endpoints (requiere configuración en .cfg o icegrid.xml)
                // Si el adapter está configurado en el .cfg, IceGrid lo gestionará
                adapter = communicator.createObjectAdapter(adapterName);
                System.out.println("✓ Adapter creado para registro automático en IceGrid");
            } catch (InitializationException e) {
                // Si falla, usar endpoints explícitos pero aún con IceGrid
                System.out.println("⚠ Adapter no configurado en IceGrid, usando endpoints explícitos");
                System.out.println("  Nota: El objeto estará disponible pero puede no estar registrado explícitamente");
                adapter = communicator.createObjectAdapterWithEndpoints(adapterName, adapterEndpoints);
            }
            
            // Crear servant
            DatagramWorkerImpl servant = new DatagramWorkerImpl();
            String identity = System.getProperty("DatagramWorker.Identity", "DatagramWorker");
            ObjectPrx proxy = adapter.add(servant, Util.stringToIdentity(identity));
            adapter.activate();
            
            // Registrar con Master usando SOLO IceGrid QueryPrx (sin fallback)
            try {
                DatagramMasterPrx master = null;
                
                // Obtener QueryPrx de IceGrid para buscar objetos
                QueryPrx query = null;
                try {
                    query = QueryPrx.checkedCast(
                        communicator.stringToProxy("IceGrid/Query")
                    );
                    if (query == null) {
                        throw new java.lang.Exception("No se pudo obtener QueryPrx de IceGrid");
                    }
                    System.out.println("✓ QueryPrx obtenido de IceGrid");
                } catch (java.lang.Exception e) {
                    System.err.println("✗ ERROR: No se pudo obtener QueryPrx: " + e.getMessage());
                    throw new java.lang.Exception("IceGrid QueryPrx es requerido", e);
                }
                
                // Buscar Master usando stringToProxy con Locator (método más confiable)
                System.out.println("Buscando Master en IceGrid...");
                String masterIdentity = "DatagramMaster";
                ObjectPrx masterProxy = null;
                
                try {
                    // Método 1: Usar stringToProxy con el Locator configurado (más confiable)
                    // Esto funciona aunque el objeto no esté registrado explícitamente
                    System.out.println("Intentando con stringToProxy (Locator)...");
                    try {
                        masterProxy = communicator.stringToProxy(masterIdentity);
                        master = DatagramMasterPrx.checkedCast(masterProxy);
                        if (master != null) {
                            System.out.println("✓ Master encontrado vía stringToProxy");
                        }
                    } catch (java.lang.Exception e) {
                        System.out.println("⚠ stringToProxy falló: " + e.getMessage());
                    }
                    
                    // Método 2: Si stringToProxy falla, intentar con QueryPrx
                    if (master == null) {
                        System.out.println("Intentando con QueryPrx...");
                        try {
                            masterProxy = query.findObjectById(Util.stringToIdentity(masterIdentity));
                            if (masterProxy != null) {
                                master = DatagramMasterPrx.checkedCast(masterProxy);
                                if (master != null) {
                                    System.out.println("✓ Master encontrado vía QueryPrx");
                                }
                            }
                        } catch (java.lang.Exception e) {
                            System.out.println("⚠ QueryPrx falló: " + e.getMessage());
                        }
                    }
                    
                    // Método 3: Si ambos fallan, intentar con Locator.findObjectById
                    if (master == null) {
                        System.out.println("Intentando con Locator.findObjectById...");
                        try {
                            masterProxy = locator.findObjectById(Util.stringToIdentity(masterIdentity));
                            if (masterProxy != null) {
                                master = DatagramMasterPrx.checkedCast(masterProxy);
                                if (master != null) {
                                    System.out.println("✓ Master encontrado vía Locator.findObjectById");
                                }
                            }
                        } catch (java.lang.Exception e) {
                            System.out.println("⚠ Locator.findObjectById falló: " + e.getMessage());
                        }
                    }
                    
                    // Si no se encontró, esperar y reintentar
                    if (master == null) {
                        System.out.println("⚠ Master no encontrado, esperando...");
                        for (int i = 0; i < 10; i++) {
                            Thread.sleep(1000);
                            
                            // Intentar con stringToProxy primero
                            try {
                                masterProxy = communicator.stringToProxy(masterIdentity);
                                master = DatagramMasterPrx.checkedCast(masterProxy);
                                if (master != null) {
                                    System.out.println("✓ Master encontrado después de " + (i+1) + " segundos");
                                    break;
                                }
                            } catch (java.lang.Exception e) {
                                // Continuar
                            }
                            
                            System.out.println("  Intento " + (i+1) + "/10...");
                        }
                    }
                    
                    if (master == null) {
                        throw new java.lang.Exception("Master no encontrado después de 10 intentos. " +
                                                     "Verifica que el Master esté corriendo y el Locator configurado.");
                    }
                } catch (java.lang.Exception e) {
                    System.err.println("✗ ERROR: No se pudo encontrar Master en IceGrid: " + e.getMessage());
                    System.err.println("  Asegúrate de que:");
                    System.err.println("    1. El IceGrid Registry esté corriendo");
                    System.err.println("    2. El Master esté corriendo y registrado");
                    System.err.println("    3. Ambos usen el mismo Locator: " + locatorEndpoint);
                    throw new java.lang.Exception("No se pudo conectar al Master vía IceGrid", e);
                }
                
                // Registrar worker con Master
                DatagramWorkerPrx workerProxy = DatagramWorkerPrx.uncheckedCast(proxy);
                String workerId = master.registerWorker(workerProxy);
                
                System.out.println("=== DATAGRAM WORKER SERVER ===");
                System.out.println("Worker ID: " + workerId);
                System.out.println("Adapter: " + adapterName);
                System.out.println("Endpoints: " + adapterEndpoints);
                System.out.println("Identity: " + identity);
                System.out.println("✓ Registrado con Master exitosamente vía IceGrid");
                
            } catch (java.lang.Exception e) {
                System.err.println("✗ ERROR FATAL: No se pudo registrar con Master: " + e.getMessage());
                System.err.println("  El Worker no puede funcionar sin el Master.");
                throw new java.lang.Exception("Worker requiere Master disponible en IceGrid", e);
            }
            
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

