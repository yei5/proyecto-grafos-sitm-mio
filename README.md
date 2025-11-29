# Proyecto de Análisis de Arcos del SITM-MIO  
**Integrantes:**  
- Samuel Domínguez  
- Yeison Rodríguez  
- Alejandro Osejo  

---

## Descripción del Proyecto

Este proyecto implementa un **analizador de grafos del sistema de transporte SITM-MIO**, procesando la estructura real del sistema a partir de tres bases de datos en formato CSV:

- `stops.csv` — Información de paradas  
- `lines.csv` — Información de rutas  
- `linestops.csv` — Secuencias de paradas por ruta y orientación  

El sistema construye un **grafo dirigido**, donde:

- Cada **parada** es un **nodo**  
- Cada **tramo entre dos paradas consecutivas** es un **arco**  
- Cada arco contiene:  
  ✔ ruta  
  ✔ orientación  
  ✔ secuencia  
  ✔ distancia y tiempo (en futuras versiones)  

Además, se generan **reportes automáticos** como:

- Estadísticas del grafo  
- Arcos ordenados por ruta  
- Velocidades promedio por tramo  
- Exportación completa del grafo en `grafo_exportado.txt`

---

## Gestión de Dependencias con Maven

Este proyecto ahora usa **Maven** para gestionar dependencias automáticamente.

### Requisitos

- **Java JDK 8 o superior**
- **Maven 3.6 o superior** (descargar de: https://maven.apache.org/download.cgi)

### Instalación de Maven

1. Descarga Maven desde: https://maven.apache.org/download.cgi
2. Extrae el archivo ZIP
3. Agrega Maven al PATH de tu sistema:
   - Windows: Agrega `C:\ruta\a\apache-maven-3.x.x\bin` al PATH
   - Verifica con: `mvn --version`

### Compilación y Ejecución con Maven

#### Opción 1: Usando comandos Maven directamente

```bash
# Compilar el proyecto
mvn clean compile

# Ejecutar el proyecto
mvn exec:java

# Crear un JAR ejecutable con todas las dependencias
mvn clean package
java -jar target/proyecto-grafos-sitm-mio-1.0.0-jar-with-dependencies.jar datos
```

#### Opción 2: Usando los scripts batch (Windows)

- `mvn-compilar.bat` - Compila el proyecto
- `mvn-ejecutar.bat` - Ejecuta el proyecto
- `mvn-empacar.bat` - Crea un JAR ejecutable

### Dependencias

Las dependencias se gestionan automáticamente con Maven (definidas en `pom.xml`):

- **JMapViewer 2.14** - Para visualización de mapas (se descarga automáticamente)

### Método Tradicional (sin Maven)

Si prefieres no usar Maven, puedes seguir usando:
- `compilar.bat` - Compilación manual
- `ejecutar.bat` - Ejecución manual
- Descargar manualmente `jmapviewer.jar` y colocarlo en `libs/`

---

##  Estructura del Proyecto

```
proyecto-grafos-sitm-mio/
│
├── datos/
│   ├── stops.csv
│   ├── lines.csv
│   ├── linestops.csv
|   └── grafo_exportado.txt
│
├── src/
    └── com/sitm/mio/grafos/
        ├── Main.java
        ├── Grafo.java
        ├── Arco.java
        ├── Nodo.java
        ├── ConstructorGrafo.java
        ├── CalculadoraVelocidad.java
        └── (otros)
```

---

##  Base de Datos (CSV)

### stops.csv — Paradas

Columnas utilizadas:
```
STOPID
SHORTNAME
LONGNAME
DECIMALLONGITUDE
DECIMALLATITUDE
```

###  lines.csv — Rutas

Columnas utilizadas:
```
LINEID
SHORTNAME
DESCRIPTION
```

###  linestops.csv — Secuencias de paradas

Columnas utilizadas:
```
LINEID
STOPID
STOPSEQUENCE
ORIENTATION
```

El sistema detecta automáticamente variaciones de nombres en mayúsculas y minúsculas.

---

##  Requisitos

- **Java JDK 8** o superior  
- **Maven 3.6** o superior (recomendado para gestión de dependencias)
- Editor recomendado: **VS Code** con extensiones de Java  
- Archivos CSV limpios y sin encabezados corruptos  

---

##  Cómo Ejecutarlo

###  Ubicar los archivos
Coloca los siguientes archivos dentro de una carpeta, por ejemplo:

```
proyecto-grafos-sitm-mio/
└── datos/
    ├── stops.csv
    ├── lines.csv
    └── linestops.csv
```

###  Compilar el proyecto

#### Con Maven (Recomendado):
```bash
mvn clean compile
# O usar el script: mvn-compilar.bat
```

#### Método tradicional:
```bash
cd proyecto-grafos-sitm-mio
javac src/com/sitm/mio/grafos/*.java
# O usar el script: compilar.bat
```

###  Ejecutar el Main

#### Con Maven (Recomendado):
```bash
mvn exec:java
# O usar el script: mvn-ejecutar.bat
```

#### Método tradicional:
```bash
java -cp bin com.sitm.mio.grafos.Main datos
# O usar el script: ejecutar.bat datos
```

O desde VS Code → botón **"Run Java"** ejecutando `Main.java`.

---

##  ¿Qué genera el sistema?

Cuando todo corre correctamente, se producirá:

### Estadísticas en consola  
- Número de nodos  
- Número de arcos  
- Rutas cargadas  

###  Arcos ordenados  
Por ruta y secuencia (ida y vuelta).

###  Reporte de velocidades  
Si los datos incluyen tiempo y distancia.

###  Archivo automático:
```
grafo_exportado.txt
```

Este archivo contiene:

- Lista completa de nodos  
- Lista completa de arcos  
- Orden y ruta de cada arco  

---

Exportación de Grafos

El sistema genera automáticamente dos tipos de grafos:

#### Grafo General del Sistema

El programa construye un grafo completo que incluye todas las rutas, todas las paradas y todos los arcos del SITM-MIO.

Archivos generados:
datos/imagenes/
│── grafo_general.png
└── grafo_general.txt

Contenido del archivo grafo_general.txt:

Lista total de nodos del sistema

Lista completa de arcos

Rutas, secuencias y orientaciones

Representación legible del grafo

Ejemplo:

== Grafo General ==
Nodos: 482
Arcos: 713

=== Lista de Arcos ===
ST001 -> ST002  (Ruta: A21)
ST002 -> ST003  (Ruta: A21)
...

#### Grafos por Ruta y Orientación

Además, el sistema genera un grafo separado para cada ruta y cada sentido (ida / regreso) basado en los datos reales del SITM-MIO.

**Cada grafo generado contiene:**

- Solo las paradas que pertenecen a esa ruta

- Secuencia real de parada según STOPSEQUENCE

- Orientación (0 = ida, 1 = regreso)

- Arcos conectados únicamente dentro de la ruta

**Archivos generados:**

Por ejemplo, para la ruta T31:

```
datos/imagenes/
│── T31_IDA.png
│── T31_IDA.txt
│── T31_REGRESO.png
└── T31_REGRESO.txt
```

Contenido típico del archivo T31_IDA.txt:
```
== T31 IDA ==
Nodos: 57
Arcos: 56

=== Lista de Arcos ===
ST311 -> ST289 (Ruta: T31)
ST289 -> ST205 (Ruta: T31)
```
...

##  Autores

Proyecto realizado por:

- **Samuel Domínguez**  
- **Yeison Rodríguez**  
- **Alejandro Osejo**

Carrera: **Ingeniería de Sistemas**

---

##  Licencia

Uso académico — libre para modificar y mejorar.
