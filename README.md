**Guía de Ejecución**

Requisitos Previos
- Java SDK: 17 o superior.
- Maven: Para la resolución de dependencias (pom.xml).
- Docker / Docker Compose: Para levantar los contenedores de las bases de datos relacionales y Redis.
- Postman: Para importar la colección de pruebas de la API.

- Paso 1: Levantar las Bases de Datos y Redis
  Desde la terminal en la raíz del proyecto asegurate de levantar los servicios de persistencia: **docker-compose up -d**

- Paso 2: Ejecución de los Microservicios
  En IntelliJ IDEA, abrís cada módulo y ejecutás el método main de su clase de configuración principal (haciendo clic en el botón de Play verde al lado de la clase):
  Catálogo: Ejecutar CatalogoServiceApplication
  Entradas: Ejecutar EntradaServiceApplication
  Compra / Reserva: Ejecutar CompraServiceApplication (o ReservaServiceApplication)
  Pago (Mock): Ejecutar PagoServiceApplication
  API Gateway: (Debe ser el último en levantarse) Ejecutar ApiGatewayApplication
  Importante: Levantar todos los microservicios de negocio primero y dejar el API Gateway para el final.
  
- Paso 3: Realizar Consultas y Pruebas
  Una vez que el API Gateway esté corriendo (generalmente en el puerto 8080), abrir Postman, importar la colección de requests y empezar a interactuar con el sistema apuntando siempre al Gateway.


