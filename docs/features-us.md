# TicketYa — Features y User Stories

## F01 — Gestión de lugares y sectores

### US-01 — Registrar un lugar
**Como** administrador **quiero** registrar un lugar con su nombre, dirección, ciudad y ubicación
(latitud/longitud) **para** que los organizadores puedan crear eventos en él.

**Criterios de aceptación**
1. **Dado** que envío nombre, dirección, ciudad, latitud y longitud válidos, **cuando** registro el
   lugar, **entonces** se crea con un ID único y se responde 201 con los datos guardados.
2. **Dado** que falta el nombre, **cuando** intento registrar el lugar, **entonces** se rechaza con
   un error de validación (400) y no se guarda nada.
3. **Dado** que hay lugares registrados, **cuando** consulto el listado, **entonces** obtengo todos
   con su ID, nombre, dirección y ciudad.

### US-02 — Definir los sectores de un lugar
**Como** administrador **quiero** definir los sectores de un lugar, indicando si son numerados
(filas y asientos) o no numerados (zona con capacidad máxima) **para** poder vender entradas
según la distribución real del lugar.

**Criterios de aceptación**
1. **Dado** un sector numerado, **cuando** lo defino, **entonces** debo indicar sus asientos, cada
   uno con fila y número, y cada asiento queda con un ID único.
2. **Dado** un sector no numerado (p. ej. "Campo"), **cuando** lo defino, **entonces** debo indicar
   una capacidad máxima mayor a 0 y no se crean asientos individuales.
3. **Dado** un sector numerado sin asientos o una zona sin capacidad válida, **cuando** intento
   guardarlo, **entonces** se rechaza con un error de validación.
4. **Dado** un lugar con sectores, **cuando** lo consulto, **entonces** veo sus sectores con el tipo
   y la capacidad de cada uno.

---

## F02 — Gestión y publicación de eventos

### US-03 — Crear un evento en borrador
**Como** organizador **quiero** crear un evento con nombre, descripción, tipo, participantes, fecha y
hora, y lugar **para** empezar a prepararlo antes de ponerlo a la venta.

**Criterios de aceptación**
1. **Dado** que envío los datos obligatorios (nombre, tipo, fecha y hora, lugar existente),
   **cuando** creo el evento, **entonces** se guarda con un ID único, en estado **BORRADOR**, con
   mi usuario como organizador, y se responde 201.
2. **Dado** que indico un lugar que no existe, **cuando** creo el evento, **entonces** se rechaza
   (404) y no se crea nada.
3. **Dado** que la fecha del evento ya pasó, **cuando** creo el evento, **entonces** se rechaza con
   un error de validación.
4. **Dado** un evento en BORRADOR, **cuando** un cliente lista los eventos disponibles,
   **entonces** ese evento no aparece.

### US-04 — Configurar sectores y precios del evento
**Como** organizador **quiero** definir el precio de cada sector del lugar para mi evento **para**
vender cada sector a su valor.

**Criterios de aceptación**
1. **Dado** un evento en BORRADOR, **cuando** asigno un precio mayor a 0 a un sector del lugar del
   evento, **entonces** el precio queda registrado para ese evento y sector.
2. **Dado** un sector que no pertenece al lugar del evento, **cuando** intento asignarle precio,
   **entonces** se rechaza con un error de validación.
3. **Dado** que asigno precio dos veces al mismo sector, **cuando** guardo, **entonces** queda un
   solo precio (el último) y no un duplicado.
4. **Dado** un evento, **cuando** consulto sus sectores, **entonces** obtengo cada sector con su
   precio y su capacidad.

### US-05 — Publicar un evento
**Como** organizador **quiero** publicar mi evento **para** que los clientes puedan verlo y comprar
entradas.

**Criterios de aceptación**
1. **Dado** un evento en BORRADOR con al menos un sector con precio, **cuando** lo publico,
   **entonces** pasa a estado **PUBLICADO** y se responde 200.
2. **Dado** que el evento se publicó, **cuando** se consulta su disponibilidad, **entonces** el
   inventario tiene cargados todos sus asientos como LIBRES y el cupo completo de cada zona.
3. **Dado** un evento sin sectores con precio, **cuando** intento publicarlo, **entonces** se
   rechaza indicando qué falta.
4. **Dado** un evento CANCELADO o FINALIZADO, **cuando** intento publicarlo, **entonces** se
   rechaza (409).

### US-06 — Suspender un evento
**Como** administrador **quiero** suspender cualquier evento publicado **para** frenar la venta
ante un problema sin cancelarlo definitivamente.

**Criterios de aceptación**
1. **Dado** un evento PUBLICADO, **cuando** lo suspendo, **entonces** pasa a SUSPENDIDO y el
   inventario deja de aceptar nuevas reservas para ese evento (409).
2. **Dado** un evento SUSPENDIDO, **cuando** lo vuelvo a publicar, **entonces** se reanuda la venta
   con la disponibilidad que tenía.
3. **Dado** un evento suspendido, **cuando** se consulta, **entonces** las entradas ya vendidas
   siguen siendo válidas.

### US-07 — Ver el detalle de un evento
**Como** cliente **quiero** ver el detalle de un evento con sus sectores, precios y lugares
disponibles **para** decidir qué entrada comprar.

**Criterios de aceptación**
1. **Dado** un evento PUBLICADO, **cuando** consulto su detalle, **entonces** veo nombre,
   descripción, tipo, participantes, fecha y hora, lugar, estado, y cada sector con su precio.
2. **Dado** un sector numerado, **cuando** consulto la disponibilidad, **entonces** veo cada
   asiento (fila y número) con su estado: LIBRE, RETENIDO o VENDIDO.
3. **Dado** un sector no numerado, **cuando** consulto la disponibilidad, **entonces** veo la
   capacidad y cuántos lugares quedan disponibles.
4. **Dado** que una reserva venció, **cuando** consulto la disponibilidad, **entonces** sus asientos
   se muestran LIBRES aunque el proceso de limpieza todavía no haya corrido.
5. **Dado** un ID de evento inexistente, **cuando** lo consulto, **entonces** se responde 404.

---

## F03 — Búsqueda de eventos

### US-08 — Buscar eventos por varios criterios
**Como** cliente **quiero** buscar eventos combinando tipo, organizador, participante, ciudad y
rango de fechas **para** encontrar rápido lo que me interesa.

**Criterios de aceptación**
1. **Dado** eventos publicados de distintos tipos, **cuando** filtro por tipo "recital",
   **entonces** solo obtengo recitales.
2. **Dado** que combino varios filtros (p. ej. ciudad "Tandil" y fechas entre el 01/12 y el 31/12),
   **cuando** busco, **entonces** obtengo solo los eventos que cumplen **todos** los filtros.
3. **Dado** que busco por participante, **cuando** indico parte del nombre sin importar
   mayúsculas, **entonces** obtengo los eventos donde participa.
4. **Dado** que no envío ningún filtro, **cuando** busco, **entonces** obtengo todos los eventos
   publicados.
5. **Dado** cualquier búsqueda, **cuando** obtengo resultados, **entonces** nunca aparecen eventos
   en BORRADOR, CANCELADOS ni FINALIZADOS.
6. **Dado** que ningún evento cumple los filtros, **cuando** busco, **entonces** obtengo una lista
   vacía (200), no un error.

### US-09 — Ordenar los resultados de búsqueda
**Como** cliente **quiero** ordenar los resultados por fecha, precio o relevancia **para** comparar
opciones fácilmente.

**Criterios de aceptación**
1. **Dado** resultados de búsqueda, **cuando** ordeno por fecha, **entonces** aparecen del más
   próximo al más lejano.
2. **Dado** resultados de búsqueda, **cuando** ordeno por precio, **entonces** se ordenan por el
   precio más bajo de cada evento, de menor a mayor.
3. **Dado** que busco con un texto, **cuando** ordeno por relevancia, **entonces** aparecen primero
   los eventos cuyo nombre o participantes coinciden con el texto.
4. **Dado** que indico un criterio de orden inválido, **cuando** busco, **entonces** se responde
   400 indicando los criterios válidos.

---

## F04 — Compra con reserva temporal y emisión de QR

### US-10 — Elegir asientos numerados y reservarlos
**Como** cliente **quiero** elegir asientos específicos de un sector numerado y que se reserven
mientras pago **para** no perderlos durante el pago.

**Criterios de aceptación**
1. **Dado** un evento PUBLICADO con asientos libres, **cuando** elijo uno o más asientos del mismo
   sector, **entonces** quedan RETENIDOS a mi nombre por **10 minutos** y recibo la reserva con su
   hora de vencimiento.
2. **Dado** que alguno de los asientos elegidos ya está retenido o vendido, **cuando** intento
   reservar, **entonces** se rechaza toda la reserva (409) y no queda retenido ninguno.
3. **Dado** que elijo asientos repetidos o de sectores distintos, **cuando** intento reservar,
   **entonces** se rechaza con un error de validación.
4. **Dado** un evento que no está PUBLICADO, **cuando** intento reservar, **entonces** se rechaza
   (409).

### US-11 — Comprar en un sector no numerado
**Como** cliente **quiero** indicar cuántas entradas quiero en un sector no numerado **para**
comprar sin elegir asientos.

**Criterios de aceptación**
1. **Dado** una zona con cupo suficiente, **cuando** pido N entradas, **entonces** se retienen N
   lugares del cupo por 10 minutos.
2. **Dado** que el cupo restante es menor a N, **cuando** pido N entradas, **entonces** se rechaza
   (409) y el cupo no cambia.
3. **Dado** un pedido que trae asientos y cantidad a la vez, o ninguno de los dos, **cuando** lo
   envío, **entonces** se rechaza con un error de validación.

### US-12 — Pagar y recibir entradas con QR
**Como** cliente **quiero** pagar mi reserva y recibir mis entradas con un código QR único **para**
poder ingresar al evento.

**Criterios de aceptación**
1. **Dado** una reserva vigente y un pago **aprobado**, **cuando** se completa la compra,
   **entonces** los lugares pasan a VENDIDO, se emite una entrada por lugar y la compra queda
   COMPLETADA (201).
2. **Dado** que se emitieron las entradas, **cuando** las consulto, **entonces** cada una tiene un
   código QR único, el evento, el sector, el asiento (si es numerado) y estado EMITIDA.
3. **Dado** cualquier par de entradas del sistema, **cuando** se comparan sus QR, **entonces**
   nunca son iguales (restricción de unicidad en la base).
4. **Dado** que la compra se completó, **cuando** la consulto, **entonces** veo cada paso realizado
   (reserva, pago, confirmación, emisión) con su fecha y hora.

### US-13 — Liberar los lugares si el pago es rechazado
**Como** cliente **quiero** que, si mi pago es rechazado, los lugares se liberen y no se me emitan
entradas **para** no quedar con una compra a medias.

**Criterios de aceptación**
1. **Dado** una reserva vigente, **cuando** la pasarela **rechaza** el pago, **entonces** la reserva
   se libera, los lugares vuelven a estar LIBRES y la compra queda PAGO_RECHAZADO (402).
2. **Dado** un pago rechazado, **cuando** consulto mis entradas, **entonces** no hay ninguna entrada
   emitida para esa compra.
3. **Dado** un pago rechazado, **cuando** otro cliente consulta la disponibilidad, **entonces** esos
   lugares están disponibles de inmediato.

### US-14 — Liberar los lugares si la reserva vence
**Como** cliente **quiero** que, si no termino de pagar dentro del plazo, los lugares se liberen
**para** que otros puedan comprarlos.

**Criterios de aceptación**
1. **Dado** una reserva ACTIVA, **cuando** pasan 10 minutos sin confirmarse, **entonces** pasa a
   VENCIDA y sus lugares vuelven a estar LIBRES.
2. **Dado** una reserva VENCIDA, **cuando** se intenta confirmarla, **entonces** se rechaza (409).
3. **Dado** que el pago se aprobó pero la reserva ya había vencido, **cuando** la confirmación
   falla, **entonces** se solicita el **reintegro** del pago, no se emiten entradas y la compra
   queda RESERVA_VENCIDA (409).

### US-15 — Limitar las entradas por cliente
**Como** organizador **quiero** limitar la cantidad de entradas que un cliente compra para un mismo
evento **para** evitar la reventa y repartir mejor las entradas.

**Criterios de aceptación**
1. **Dado** un evento con un máximo de N entradas por cliente, **cuando** un cliente intenta
   reservar más de N en total, **entonces** se rechaza indicando el límite y cuántas ya tiene.
2. **Dado** que el conteo incluye entradas vendidas y reservas activas del cliente, **cuando** un
   cliente con 3 de 4 posibles pide 2 más, **entonces** se rechaza.
3. **Dado** que una reserva del cliente venció o se liberó, **cuando** vuelve a reservar,
   **entonces** esos lugares ya no cuentan para su límite.

### US-16 — Registrar la compra y consultar mis entradas
**Como** cliente **quiero** que quede registrado el detalle de mi compra y el medio de pago, y poder
ver mis entradas **para** tener constancia y acceso a ellas.

**Criterios de aceptación**
1. **Dado** una compra, **cuando** se registra, **entonces** guarda cliente, evento, lugares,
   importe total, medio de pago, estado y fecha.
2. **Dado** un pago, **cuando** se registra, **entonces** guarda la compra a la que pertenece, el
   monto, la moneda, el estado (APROBADO, RECHAZADO, INCIERTO, REINTEGRADO) y la referencia de la
   pasarela.
3. **Dado** que soy un cliente con compras, **cuando** consulto mis entradas, **entonces** veo todas
   las mías con su QR y estado, y ninguna de otros clientes.

---

## F05 — No doble venta e idempotencia

### US-17 — Un asiento nunca se vende dos veces
**Como** cliente **quiero** que, si muchos compramos el mismo asiento a la vez, solo uno lo
consiga **para** no llegar al evento y encontrar mi lugar ocupado.

**Criterios de aceptación**
1. **Dado** un asiento LIBRE, **cuando** muchos clientes intentan reservarlo **al mismo tiempo**,
   **entonces** exactamente una reserva resulta exitosa y las demás reciben 409.
2. **Dado** que hay varias instancias del servicio de inventario ejecutándose, **cuando** ocurre lo
   anterior, **entonces** el resultado es el mismo (el control se hace en la base de datos, con
   una actualización condicional del estado, no en memoria).
3. **Dado** el test automatizado de no doble venta, **cuando** se ejecuta, **entonces** verifica que
   de N compras simultáneas del mismo asiento solo una queda COMPLETADA.

### US-18 — Reintentar un pago sin cobrar dos veces
**Como** cliente **quiero** poder reintentar la compra si se me corta la conexión, sin que se me
cobre ni se me emitan entradas dos veces.

**Criterios de aceptación**
1. **Dado** que toda compra se envía con una clave de idempotencia (`Idempotency-Key`), **cuando**
   repito la misma solicitud con la misma clave, **entonces** recibo la misma compra y no se crea
   otra.
2. **Dado** un cobro ya procesado, **cuando** se repite con la misma clave, **entonces** pagos
   devuelve el mismo pago y la pasarela no cobra de nuevo.
3. **Dado** que reutilizo una clave con datos distintos (otro monto u otro evento), **cuando**
   envío la solicitud, **entonces** se rechaza indicando que la clave ya se usó para otra
   operación.
4. **Dado** que dos reintentos con la misma clave llegan a la vez, **cuando** se procesan,
   **entonces** se registra un solo pago (restricción de unicidad sobre la clave).
5. **Dado** una compra sin clave de idempotencia, **cuando** se envía, **entonces** se rechaza (400).

### US-19 — El último lugar de una zona se vende una sola vez
**Como** cliente **quiero** que, si quedan pocos lugares en una zona, nunca se vendan más de los
que hay.

**Criterios de aceptación**
1. **Dado** una zona con 1 lugar disponible, **cuando** varios clientes piden 1 entrada a la vez,
   **entonces** solo uno lo obtiene y los demás reciben 409.
2. **Dado** cualquier secuencia de reservas concurrentes, **cuando** termina, **entonces** vendidos
   más retenidos nunca supera la capacidad de la zona.

---

## F06 — Devoluciones y cancelación de eventos

### US-20 — Solicitar la devolución de una entrada
**Como** cliente **quiero** pedir la devolución de una entrada **para** recuperar mi dinero si no
puedo ir.

**Criterios de aceptación**
1. **Dado** una entrada EMITIDA mía y faltan **más de 72 horas** para el evento, **cuando** pido la
   devolución, **entonces** se registra una solicitud PENDIENTE de aprobación.
2. **Dado** que faltan 72 horas o menos para el evento, **cuando** pido la devolución, **entonces**
   se rechaza indicando el plazo.
3. **Dado** una entrada usada, cancelada o de otro cliente, **cuando** pido la devolución,
   **entonces** se rechaza.
4. **Dado** que ya hay una solicitud pendiente para esa entrada, **cuando** la pido de nuevo,
   **entonces** no se crea una segunda solicitud.

### US-21 — Aprobar o rechazar una devolución
**Como** administrador **quiero** aprobar o rechazar las solicitudes de devolución **para**
controlar los reintegros.

**Criterios de aceptación**
1. **Dado** una solicitud PENDIENTE, **cuando** la **apruebo**, **entonces** la entrada pasa a
   CANCELADA, el asiento (o el lugar de la zona) vuelve a estar disponible y se solicita el
   reintegro a la pasarela.
2. **Dado** una solicitud PENDIENTE, **cuando** la **rechazo**, **entonces** la entrada sigue
   EMITIDA y no se reintegra nada.
3. **Dado** una solicitud ya resuelta, **cuando** intento aprobarla de nuevo, **entonces** no se
   genera un segundo reintegro.
4. **Dado** un reintegro aprobado por el total, **cuando** consulto el pago, **entonces** está
   REINTEGRADO.

### US-22 — Cancelar un evento
**Como** administrador **quiero** cancelar un evento **para** que todas sus entradas vendidas pasen
a reintegro.

**Criterios de aceptación**
1. **Dado** un evento con entradas vendidas, **cuando** lo cancelo, **entonces** el evento pasa a
   CANCELADO, todas sus entradas EMITIDAS pasan a CANCELADA y se registra un reintegro pendiente
   por cada compra.
2. **Dado** un evento cancelado, **cuando** un cliente intenta reservar, **entonces** se rechaza
   (409) y se liberan las reservas que estaban activas.
3. **Dado** un evento ya cancelado, **cuando** lo cancelo otra vez, **entonces** no se duplican
   reintegros ni cancelaciones (operación idempotente).
4. **Dado** un evento cancelado, **cuando** consulto el resumen de reintegros del evento,
   **entonces** veo cuántos están pendientes, completados y fallidos.

### US-23 — Reintentar reintegros fallidos
**Como** administrador **quiero** que los reintegros que fallaron por problemas de la pasarela se
reintenten **para** que ningún cliente se quede sin su dinero.

**Criterios de aceptación**
1. **Dado** un reintegro PENDIENTE o INCIERTO, **cuando** corre el proceso de reintento
   automático, **entonces** se vuelve a pedir a la pasarela con la misma clave de idempotencia.
2. **Dado** un reintegro fallido, **cuando** lo reintento manualmente, **entonces** se actualiza su
   estado y se incrementa el contador de intentos.
3. **Dado** que la pasarela ya había procesado ese reintegro, **cuando** se reintenta, **entonces**
   no se devuelve el dinero dos veces.

---

## F07 — Fila virtual para eventos de alta demanda

### US-24 — Entrar a la fila virtual
**Como** cliente **quiero** entrar a una fila virtual cuando abre la venta de un evento masivo y
ver mi posición **para** saber cuánto me falta.

**Criterios de aceptación**
1. Dado un evento de alta demanda con la venta abierta, cuando entro, recibo mi posición según el
   orden de llegada.
2. Dado que estoy en la fila, cuando consulto, veo mi posición actualizada.

### US-25 — Habilitar compras por tandas
**Como** sistema **quiero** habilitar a los clientes de la fila por tandas, en orden **para** que
la venta sea justa y el sistema no se sature.

**Criterios de aceptación**
1. Dado que llega mi turno, cuando me habilitan, puedo comprar durante un tiempo limitado.
2. Dado que no estoy habilitado, cuando intento reservar, se rechaza.

### US-26 — Marcar un evento como de alta demanda
**Como** administrador **quiero** marcar un evento como de alta demanda y definir el tamaño de las
tandas **para** activar la fila virtual.

**Criterios de aceptación**
1. Dado un evento marcado, cuando abre la venta, todo cliente pasa por la fila.
2. Dado un evento no marcado, la compra es directa.

## F08 — Control de acceso con QR

### US-27 — Validar una entrada en el ingreso
**Como** personal de acceso **quiero** escanear el QR de una entrada **para** dejar pasar solo a
quien tiene una entrada válida.

**Criterios de aceptación**
1. Dado un QR existente, EMITIDO y de este evento, cuando lo escaneo, se acepta y la entrada pasa a
   USADA.
2. Dado el mismo QR escaneado a la vez en dos puertas, solo una lectura se acepta.

### US-28 — Rechazar entradas inválidas
**Como** personal de acceso **quiero** que se rechacen los QR inválidos indicando el motivo.

**Criterios de aceptación**
1. Dado un QR ya usado, cancelado, inexistente o de otro evento, cuando lo escaneo, se rechaza con
   el motivo.

## F09 — Preferencias y recomendaciones

### US-29 — Registrar mis preferencias
**Como** cliente **quiero** guardar tipos de evento, artistas favoritos y ciudad **para** recibir
sugerencias a mi medida.

**Criterios de aceptación**
1. Dado que guardo preferencias, cuando las consulto, las veo tal como las guardé (base NoSQL).

### US-30 — Recibir recomendaciones
**Como** cliente **quiero** ver eventos sugeridos según mi historial ("quienes compraron para este
evento también compraron…") **para** descubrir eventos nuevos.

**Criterios de aceptación**
1. Dado que compré para un evento, cuando pido recomendaciones, veo eventos publicados que
   compraron otros clientes de ese evento, sin incluir los que ya compré.

## F10 — Notificaciones

### US-31 — Confirmación de compra
**Como** cliente **quiero** recibir un email o push al confirmar la compra con mis entradas.

**Criterios de aceptación**
1. Dado una compra COMPLETADA, se envía la notificación de forma asíncrona; si el servicio de
   notificaciones falla, la compra igual se completa.

### US-32 — Aviso de cancelación o cambio de evento
**Como** cliente **quiero** ser avisado si el evento se cancela o cambia de fecha o lugar.

**Criterios de aceptación**
1. Dado un evento cancelado o modificado, cada comprador recibe una única notificación.

### US-33 — Aviso de artista favorito en mi ciudad
**Como** cliente **quiero** enterarme cuando un artista favorito publica un evento en mi ciudad.

**Criterios de aceptación**
1. Dado que se publica un evento con ese artista en mi ciudad, recibo la notificación.

## F11 — Reportes para organizadores

### US-34 — Ventas y devoluciones por sector
**Como** organizador **quiero** ver las entradas vendidas y devueltas por sector de mis eventos.

**Criterios de aceptación**
1. Dado un evento mío, cuando pido el reporte, veo vendidas y devueltas por sector.
2. Dado un evento de otro organizador, el reporte se rechaza.

### US-35 — Ocupación y recaudación
**Como** organizador **quiero** ver el porcentaje de ocupación y la recaudación de mis eventos.

**Criterios de aceptación**
1. Dado un evento, el reporte muestra ocupación (vendidas / capacidad) y recaudación neta de
   reintegros.

## F12 — Seguridad

### US-36 — Autenticarme
**Como** usuario **quiero** iniciar sesión y recibir un token JWT **para** usar el sistema con mi
rol.

**Criterios de aceptación**
1. Dado credenciales válidas, recibo un JWT con mi rol; dado credenciales inválidas, 401.

### US-37 — Acceso según el rol
**Como** administrador del sistema **quiero** que cada endpoint solo sea accesible por los roles
que corresponden.

**Criterios de aceptación**
1. Dado un cliente, cuando intenta publicar o cancelar un evento, recibe 403.
2. Dado una solicitud sin token a un endpoint protegido, recibe 401.

## F13 — Operación y calidad del sistema

### US-38 — Trazabilidad de las operaciones
**Como** administrador **quiero** que cada solicitud quede registrada con endpoint, usuario, código
de respuesta y tiempo, y que cada operación crítica tenga un identificador común entre servicios.

**Criterios de aceptación**
1. Dado una compra, puedo seguirla en el log centralizado de todos los servicios con un mismo
   `X-Correlation-Id`.

### US-39 — Resiliencia ante servicios externos
**Como** cliente **quiero** que el sistema responda aunque la pasarela o las notificaciones fallen.

**Criterios de aceptación**
1. Dado que la pasarela no responde, se aplican timeout, reintentos y fallback, y el pago queda
   INCIERTO para reconciliarse después, sin dejar lugares retenidos para siempre.

### US-40 — Cache del catálogo
**Como** cliente **quiero** que el catálogo y las búsquedas respondan rápido en momentos de mucha
demanda.

**Criterios de aceptación**
1. Dado un evento modificado, su cache se invalida; la disponibilidad de asientos nunca se cachea.

### US-41 — Despliegue con un solo comando
**Como** desarrollador **quiero** levantar todo el sistema con un único `docker-compose.yml`
**para** desplegarlo fácilmente.

**Criterios de aceptación**
1. Cada microservicio se puede construir y ejecutar por separado, con su propia base de datos.
2. Con `docker compose up` quedan arriba bases, gateway y microservicios, cada uno en su
   contenedor.

### US-42 — Soportar la apertura de venta masiva
**Como** organizador **quiero** que el sistema soporte miles de compras simultáneas.

**Criterios de aceptación**
1. Dado una prueba de carga del flujo de compra (JMeter), se reportan usuarios concurrentes, tiempos
   de respuesta y errores, y no hay ninguna doble venta.
