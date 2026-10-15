# API REST - Sistema de Gestión de Pólizas de Seguros

API REST de grado empresarial para la administración, renovación y cancelación de pólizas de seguros y riesgos asociados, con integración a sistema CORE heredado (Weblogic) y capa de seguridad por API Key.

---

## 1. Arquitectura y Decisiones Técnicas

El proyecto fue desarrollado bajo una **Arquitectura en Capas Limpia (Layered Architecture)** para garantizar separación de responsabilidades, alta cohesión y bajo acoplamiento:

```
com.bolivar.api_polizas
├── config          # Seguridad (ApiKeyAuthFilter) y carga de datos (DataInitializer)
├── controller      # Controladores REST (PolizaController, RiesgoController, CoreMockController)
├── service         # Lógica de negocio (PolizaService, RiesgoService, CoreMockService)
├── repository      # Interfaces Spring Data JPA para persistencia
├── model
│   ├── entity      # Entidades persistentes JPA (Poliza, Riesgo)
│   ├── enums       # Enumeraciones de dominio (TipoPoliza, EstadoPoliza, EstadoRiesgo)
│   └── dto         # Objetos de transferencia y validación (@Valid)
└── exception       # Manejo centralizado de excepciones (@RestControllerAdvice)
```

### Decisiones Técnicas Clave:
- **Java 17 & Spring Boot 3**: Aprovechamiento de características modernas, Java Time API (`java.time.LocalDate`) y Spring Framework.
- **Persistencia H2 en memoria**: Base de datos volátil y ultrarrápida (`jdbc:h2:mem:polizasdb`) configurada con consola web interactiva en `/h2-console`.
- **Prevención de Ciclos Infinitos en JSON**: Manejo de la relación bidireccional `@OneToMany` / `@ManyToOne` mediante las anotaciones `@JsonManagedReference` en la entidad `Poliza` y `@JsonBackReference` en `Riesgo`.
- **Filtro de Seguridad Flexible**: Implementación de `OncePerRequestFilter` (`ApiKeyAuthFilter`) que acepta indistintamente las cabeceras `x-api-key: 123456` o `api-key: 123456`, respondiendo con código HTTP `401 Unauthorized` si no coincide o está ausente, y omitiendo la ruta `/h2-console/**`.
- **Cálculo Robusto de Vigencia y Prima**: El canon y la prima total se calculan con base en los meses reales de vigencia (`ChronoUnit.MONTHS`). En renovaciones se conserva la duración del periodo original y se incrementa el canon y la prima aplicando el IPC (configurable desde `app.ipc` o enviado dinámicamente).
- **Manejo de Errores Estandarizado**: `@RestControllerAdvice` captura `BusinessRuleException` (400), `ResourceNotFoundException` (404) y errores de validación `@Valid` produciendo respuestas JSON consistentes con timestamp, status, error, mensaje y ruta.

---

## 2. Requisitos Previos e Instalación Local

- **JDK 17** o superior instalado y configurado en el `PATH`.
- Maven 3.8+ (o utilizar el wrapper incluido `./mvnw` / `mvnw.cmd`).

### Compilación y Ejecución

1. Clonar o ubicarse en la carpeta del proyecto:
   ```bash
   cd api-polizas
   ```

2. Compilar y verificar pruebas automáticas:
   ```bash
   ./mvnw clean test
   ```
   *(En Windows PowerShell: `.\mvnw.cmd clean test`)*

3. Iniciar la aplicación:
   ```bash
   ./mvnw spring-boot:run
   ```
   *(En Windows PowerShell: `.\mvnw.cmd spring-boot:run`)*

La aplicación iniciará en el puerto **8080**: `http://localhost:8080`.

---

## 3. Despliegue con Docker

El proyecto cuenta con un `Dockerfile` optimizado con **construcción multi-etapa (Multi-stage build)** que permite compilar el código dentro de un entorno aislado con Maven y empaquetar únicamente el artefacto ejecutable final sobre una imagen base ultra ligera de Java 17 Alpine (`eclipse-temurin:17-jre-alpine`).

### Requisitos de Contenedor
- **Docker Engine** o **Docker Desktop** instalado y en ejecución.

### Paso 1: Construcción de la Imagen Docker
Ubícate en la raíz del proyecto (`api-polizas`) donde reside el archivo `Dockerfile` y ejecuta:

```bash
docker build -t api-polizas .
```

> **Nota DevOps**: Gracias al `.dockerignore`, el proceso de construcción no transferirá al daemon de Docker carpetas pesadas o temporales como `target/`, `.git/` o wrappers locales.

### Paso 2: Ejecución del Contenedor
Inicia un contenedor mapeando el puerto `8080` de tu máquina anfitriona al puerto `8080` del contenedor:

```bash
docker run -p 8080:8080 api-polizas
```

Para ejecutarlo en segundo plano (modo detached) con un nombre descriptivo:

```bash
docker run -d -p 8080:8080 --name api-polizas-container api-polizas
```

### Comandos de Utilidad Docker
- **Ver logs en tiempo real**:
  ```bash
  docker logs -f api-polizas-container
  ```
- **Detener el contenedor**:
  ```bash
  docker stop api-polizas-container
  ```
- **Eliminar el contenedor detenido**:
  ```bash
  docker rm api-polizas-container
  ```

---

## 4. Seguridad (API Key)

Todos los endpoints de la API (excepto la consola H2) exigen validación mediante cabecera HTTP:

| Header | Valor Requerido | Descripción |
| :--- | :--- | :--- |
| `x-api-key` | `123456` | Header principal de autenticación |
| `api-key` | `123456` | Header alternativo compatible |

Si la cabecera no se suministra o su valor es incorrecto, el sistema retorna:
```json
{
  "timestamp": "2026-10-15T16:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Header de seguridad invalido o ausente (x-api-key o api-key requerido)",
  "path": "/polizas"
}
```

---

## 5. Consola H2 Database

Para inspeccionar las tablas y registros en tiempo real:
- **URL**: `http://localhost:8080/h2-console`
- **JDBC URL**: `jdbc:h2:mem:polizasdb`
- **User Name**: `sa`
- **Password**: *(vacío)*

*(El `DataInitializer` precarga automáticamente 1 póliza individual con 1 riesgo y 1 póliza colectiva con 2 riesgos).*

---

## 6. Catálogo de Endpoints y Ejemplos con `curl`

> **Nota para usuarios de Windows PowerShell**:  
> En PowerShell, `curl` es un alias de `Invoke-WebRequest`. Para ejecutar los comandos `curl` estándar debes invocar directamente el binario **`curl.exe`** en lugar de solo `curl` (o ejecutarlos en Git Bash, CMD o WSL).

### 6.1. Listar Pólizas (con filtros opcionales)
Permite listar todas las pólizas, o filtrar por `tipo` (`INDIVIDUAL`, `COLECTIVA`) y/o `estado` (`ACTIVA`, `RENOVADA`, `CANCELADA`).

```bash
# Listar todas
curl.exe -X GET "http://localhost:8080/polizas" \
     -H "x-api-key: 123456"

# Filtrar por tipo y estado
curl.exe -X GET "http://localhost:8080/polizas?tipo=INDIVIDUAL&estado=ACTIVA" \
     -H "x-api-key: 123456"
```

---

### 6.2. Obtener Póliza por ID
```bash
curl.exe -X GET "http://localhost:8080/polizas/1" \
     -H "x-api-key: 123456"
```

---

### 6.3. Crear Póliza Nueva
Valida que si es `INDIVIDUAL`, como máximo contenga 1 riesgo asociado.

```bash
curl.exe -X POST "http://localhost:8080/polizas" \
     -H "x-api-key: 123456" \
     -H "Content-Type: application/json" \
     -d "{ \"tipo\": \"COLECTIVA\", \"fechaInicio\": \"2026-06-01\", \"fechaFin\": \"2027-06-01\", \"canonMensual\": 2500000.00, \"riesgos\": [ { \"identificadorRiesgo\": \"Calle 26 # 68-35 Local 101, Bogota\", \"descripcion\": \"Local comercial franquicia\" } ] }"
```

---

### 6.4. Listar Riesgos de una Póliza
Lista todos los riesgos asegurados bajo una póliza específica.

```bash
curl.exe -X GET "http://localhost:8080/polizas/1/riesgos" \
     -H "x-api-key: 123456"
```

---

### 6.5. Renovar Póliza (+IPC)
Aplica el incremento de IPC al `canonMensual` y a la `primaTotal`, recalcula la vigencia sumando el mismo periodo anterior, pasa el estado a `RENOVADA` y notifica al CORE Weblogic.
*(No permite renovar pólizas CANCELADAS).*

> **Convención Estricta de IPC (Porcentaje Nominal)**: Para eliminar cualquier ambigüedad, el IPC se especifica y procesa siempre como porcentaje nominal (`factor = ipc / 100`). De esta manera, `9.28` representa 9.28%, `10.0` representa 10.0% y `1.0` representa 1.0%.

```bash
# Usando IPC por defecto configurado (app.ipc=9.28 -> 9.28%)
curl.exe -X POST "http://localhost:8080/polizas/1/renovar" \
     -H "x-api-key: 123456"

# Especificando IPC personalizado en el cuerpo (ejemplo: 10.0%)
curl.exe -X POST "http://localhost:8080/polizas/1/renovar" \
     -H "x-api-key: 123456" \
     -H "Content-Type: application/json" \
     -d "{\"ipc\": 10.0}"
```

---

### 6.6. Cancelar Póliza
Cambia el estado de la póliza a `CANCELADA`, cancela automáticamente en cascada todos sus riesgos asociados (`CANCELADO`) y notifica al CORE Weblogic.

```bash
curl.exe -X POST "http://localhost:8080/polizas/2/cancelar" \
     -H "x-api-key: 123456"
```

---

### 6.7. Agregar Riesgo a Póliza (Validación Colectiva)
Solo permite agregar riesgos a pólizas de tipo `COLECTIVA`. Si la póliza es `INDIVIDUAL` o está `CANCELADA`, retorna error 400 Bad Request.

```bash
curl.exe -X POST "http://localhost:8080/polizas/2/riesgos" \
     -H "x-api-key: 123456" \
     -H "Content-Type: application/json" \
     -d "{ \"identificadorRiesgo\": \"Carrera 7 # 72-10 Oficina 504, Bogota\", \"descripcion\": \"Oficina anexa sala de juntas\" }"
```

---

### 6.8. Cancelar Riesgo Específico
Cambia el estado de un riesgo a `CANCELADO` y notifica al CORE Weblogic.

```bash
curl.exe -X POST "http://localhost:8080/riesgos/1/cancelar" \
     -H "x-api-key: 123456"
```

---

### 6.9. Endpoint Mock del CORE
Recibe eventos y registra en los logs de consola que la operación fue notificada al CORE legado en Weblogic.

```bash
curl.exe -X POST "http://localhost:8080/core-mock/evento" \
     -H "x-api-key: 123456" \
     -H "Content-Type: application/json" \
     -d "{ \"evento\": \"ACTUALIZACION\", \"polizaId\": 555 }"
```

---

## 7. Reglas de Negocio Implementadas

1. **Restricción de Renovación**: No es posible renovar una póliza que se encuentre en estado `CANCELADA` (retorna HTTP 400).
2. **Restricción de Riesgos por Tipo de Póliza**:
   - `INDIVIDUAL`: Admite únicamente un (1) riesgo asegurable.
   - `COLECTIVA`: Permite múltiples riesgos y adición dinámica mediante el endpoint correspondiente.
3. **Cancelación en Cascada**: Al cancelar una póliza, todos sus riesgos asociados cambian automáticamente a estado `CANCELADO`.
4. **Cálculo Dinámico de Prima**: `primaTotal = canonMensual * mesesDeVigencia`.
5. **Auditoría e Integración CORE**: Toda mutación de estado (`CREACION_POLIZA`, `RENOVACION`, `CANCELACION_POLIZA`, `AGREGAR_RIESGO`, `CANCELACION_RIESGO`) notifica automáticamente al `CoreMockService` emitiendo trazas estructuradas por consola.
