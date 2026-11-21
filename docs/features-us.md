# TicketYa — Features y User Stories

## F01 — Gestión de eventos

### US-01 — Crear evento en borrador

**Como** organizador **quiero** crear un evento con sus datos básicos **para** prepararlo antes de
publicarlo.

**Criterios de aceptación**
1. **Dado** que envío nombre, descripción, tipo (recital, teatro, deporte, conferencia u otro), fecha
   y hora futuras y un lugar existente, **cuando** creo el evento, **entonces** queda en estado
   **borrador**, con mi usuario como organizador, y recibo 201 con su ID.
2. **Dado** que falta el nombre, el tipo, la fecha o el lugar, o la fecha no es futura, **cuando**
   intento crearlo, **entonces** recibo 400 y no se guarda nada.
3. **Dado** un evento en borrador, **cuando** un cliente lo busca o pide su detalle, **entonces** no
   aparece en las búsquedas y el detalle devuelve 404.

### US-02 — Modificar evento

**Como** organizador **quiero** modificar los datos de mi evento **para** corregir o actualizar la
información.

**Criterios de aceptación**
1. **Dado** un evento propio en borrador, **cuando** lo edito, **entonces** puedo cambiar cualquier dato.
2. **Dado** un evento propio publicado, **cuando** lo edito, **entonces** solo puedo cambiar
   descripción, participantes, fecha y hora.
3. **Dado** un evento publicado con entradas vendidas, **cuando** cambio la fecha o la hora,
   **entonces** se registra como *cambio relevante* y se emite el evento interno que consume la
   notificación de US-39.
4. **Dado** un evento con entradas vendidas, **cuando** intento cambiar el lugar, **entonces** recibo
   422 (simplificación deliberada para no reubicar asientos ya emitidos).
5. **Dado** un evento cancelado o finalizado, **cuando** intento editarlo, **entonces** recibo 409.
6. **Dado** un evento de otro organizador, **cuando** intento editarlo, **entonces** recibo 403.

### US-03 — Gestionar participantes del evento

**Como** organizador **quiero** asociar artistas, equipos o disertantes a mi evento **para** que los
clientes puedan encontrarlo por ellos.

**Criterios de aceptación**
1. **Regla:** un participante tiene ID, nombre y tipo (artista, equipo, disertante). Un evento puede
   tener cero o más participantes y un participante puede estar en muchos eventos.
2. **Dado** un participante que ya existe (mismo nombre, sin distinguir mayúsculas), **cuando** lo
   asocio a un evento, **entonces** se reutiliza el existente y no se crea un duplicado.
3. **Dado** un participante nuevo, **cuando** lo asocio, **entonces** se crea y queda asociado al evento.
4. **Regla:** los participantes son criterio de búsqueda (US-12) y la base de los artistas favoritos
   (US-35).

### US-04 — Publicar evento

**Como** organizador **quiero** publicar mi evento **para** que quede disponible para búsqueda y compra.

**Criterios de aceptación**
1. **Dado** un evento propio en borrador con lugar, fecha futura y todos sus sectores habilitados con
   precio, **cuando** lo publico, **entonces** pasa a **publicado**, aparece en las búsquedas, se
   puede comprar y se emite el evento interno *evento publicado* (usado por US-40).
2. **Dado** un evento sin lugar, sin fecha futura, sin sectores habilitados o con algún sector sin
   precio, **cuando** intento publicarlo, **entonces** recibo 422 con la lista de faltantes.
3. **Dado** un evento que no está en borrador, **cuando** intento publicarlo, **entonces** recibo 409.
4. **Dado** un evento de otro organizador, **cuando** intento publicarlo, **entonces** recibo 403.

### US-05 — Marcar evento de alta demanda
*Actualizada en la Entrega 2.*

**Como** organizador **quiero** marcar un evento como de alta demanda **para** que su venta se ordene
mediante fila virtual.

**Criterios de aceptación**
1. **Regla:** *alta demanda* es un atributo booleano del evento, editable mientras está en borrador.
2. **Dado** un evento de alta demanda, **cuando** se publica, **entonces** se abre su fila virtual
   con tandas de 10 clientes y una ventana de compra de 10 minutos (US-30 a US-32).
3. **Dado** un evento que no es de alta demanda, **cuando** se publica, **entonces** no se crea fila y
   la compra es directa.

### US-06 — Suspender, reanudar o cancelar un evento

**Como** administrador **quiero** suspender, reanudar o cancelar cualquier evento **para** responder a
incidentes operativos.

**Criterios de aceptación**
1. **Dado** un evento publicado, **cuando** un administrador lo suspende, **entonces** pasa a
   **suspendido**: no admite nuevas reservas ni compras, no aparece en búsquedas y las entradas ya
   emitidas siguen siendo válidas. Si tiene fila virtual, la fila queda pausada.
2. **Dado** un evento suspendido, **cuando** un administrador lo reanuda, **entonces** vuelve a
   **publicado** (y su fila, si tiene, se reabre).
3. **Dado** un evento no finalizado, **cuando** un administrador lo cancela, **entonces** pasa a
   **cancelado** de forma irreversible, se liberan las reservas vigentes, se cierra su fila y se
   dispara el reintegro masivo de US-29.
4. **Dado** un evento finalizado o una transición no permitida, **cuando** se intenta la acción,
   **entonces** se recibe 409.
5. **Dado** un usuario que no es administrador, **cuando** intenta cualquiera de estas acciones,
   **entonces** recibe 403.
6. **Regla:** cada acción registra usuario, motivo y fecha.

### US-07 — Actualizar estados automáticamente

**Como** sistema **quiero** actualizar el estado de los eventos según las ventas y la fecha **para**
mantener coherente la oferta pública.

**Criterios de aceptación**
1. **Dado** un evento publicado, **cuando** se confirma la venta del último lugar, **entonces** pasa a
   **agotado** y no admite nuevas reservas.
2. **Dado** un evento agotado que no comenzó, **cuando** se aprueba una devolución que libera un
   lugar, **entonces** vuelve a **publicado**.
3. **Dado** un evento cuya hora de fin ya pasó, **cuando** el sistema lo revisa, **entonces** pasa a
   **finalizado** y deja de ser comprable, devolvible y visible en búsquedas.
4. **Dado** una transición no contemplada, **cuando** se intenta, **entonces** se rechaza con 409.
   El cliente ve el estado actual en el detalle (US-14).

---

## F02 — Lugares, sectores y precios

### US-08 — Registrar lugar

**Como** administrador (u organizador, para sus lugares) **quiero** registrar un lugar **para** poder
asignarlo a eventos.

**Criterios de aceptación**
1. **Dado** que envío nombre, dirección, ciudad, latitud (−90 a 90) y longitud (−180 a 180) válidos,
   **cuando** registro el lugar, **entonces** se crea con un ID único y recibo 201 con los datos guardados.
2. **Dado** que falta algún dato o las coordenadas están fuera de rango, **cuando** intento
   registrarlo, **entonces** recibo 400 y no se guarda nada.
3. **Dado** un ID de lugar que ya existe, **cuando** intento registrarlo, **entonces** recibo 409.
4. **Dado** un lugar registrado, **cuando** creo eventos, **entonces** puedo asignarlo a varios.

### US-09 — Configurar sector numerado

**Como** administrador u organizador **quiero** definir sectores con filas y asientos identificados
**para** vender asientos específicos.

**Criterios de aceptación**
1. **Dado** un sector numerado, **cuando** lo defino, **entonces** indico sus asientos con fila y
   número, únicos dentro del sector, y cada asiento recibe un ID.
2. **Dado** un sector numerado sin asientos o con asientos repetidos, **cuando** intento guardarlo,
   **entonces** recibo 400.
3. **Regla:** cada asiento pertenece a un único sector y un lugar puede tener varios sectores.
4. **Dado** un lugar con eventos publicados que tienen entradas vendidas, **cuando** intento modificar
   sus sectores o asientos, **entonces** recibo 422.

### US-10 — Configurar sector no numerado

**Como** administrador u organizador **quiero** definir una zona con capacidad máxima (por ejemplo
"Campo") **para** vender entradas sin asiento asignado.

**Criterios de aceptación**
1. **Dado** un sector no numerado con capacidad entera mayor a cero, **cuando** lo defino,
   **entonces** se guarda sin asientos individuales.
2. **Dado** una capacidad menor o igual a cero, **cuando** intento guardarlo, **entonces** recibo 400.
3. **Regla:** disponibilidad = capacidad − entradas vendidas − reservas vigentes, y nunca se venden
   más entradas que la capacidad (US-23).

### US-11 — Habilitar sectores y definir precios por evento

**Como** organizador **quiero** elegir qué sectores del lugar se ofrecen en mi evento y fijar el precio
de cada uno **para** definir mi oferta comercial.

**Criterios de aceptación**
1. **Dado** un evento en borrador, **cuando** habilito sectores y les pongo precio, **entonces** se
   guardan por evento y sector (el mismo lugar puede tener precios distintos en eventos distintos).
2. **Dado** un precio menor o igual a cero, **cuando** intento guardarlo, **entonces** recibo 400.
3. **Dado** un evento publicado, **cuando** cambio el precio de un sector sin entradas vendidas,
   **entonces** se acepta; si el sector ya tiene ventas, recibo 422. Las compras hechas conservan su importe.
4. **Regla:** no se puede publicar un evento con algún sector habilitado sin precio (US-04).

---

## F03 — Búsqueda y exploración

### US-12 — Buscar eventos por múltiples criterios

**Como** cliente **quiero** buscar eventos combinando filtros **para** encontrar rápido lo que me interesa.

**Criterios de aceptación**
1. **Dado** filtros opcionales de tipo, organizador, participante, ciudad y rango de fechas, **cuando**
   busco, **entonces** se combinan con Y (todos deben cumplirse).
2. **Dado** cualquier búsqueda, **cuando** obtengo resultados, **entonces** solo veo eventos
   publicados o agotados (identificados como tales); nunca borradores, suspendidos, cancelados ni finalizados.
3. **Dado** muchos resultados, **cuando** busco, **entonces** vienen paginados (máximo 50 por página)
   e incluyen el total.
4. **Dado** que no hay coincidencias, **cuando** busco, **entonces** recibo 200 con una lista vacía.
5. **Dado** un rango con fecha *desde* posterior a fecha *hasta*, **cuando** busco, **entonces** recibo 400.

### US-13 — Ordenar resultados

**Como** cliente **quiero** ordenar los resultados **para** priorizar lo que más me importa.

**Criterios de aceptación**
1. **Dado** una búsqueda, **cuando** pido ordenar por fecha, precio (mínimo del evento) o relevancia,
   ascendente o descendente, **entonces** el orden se aplica a todo el conjunto y no solo a la página devuelta.
2. **Dado** que no indico orden, **cuando** busco, **entonces** se usa fecha ascendente y, en empate, ID.
3. **Dado** un criterio de orden inválido, **cuando** busco, **entonces** recibo 400 con los criterios válidos.
4. **Regla:** la relevancia combina proximidad de fecha y volumen de ventas; la fórmula se documenta en el diseño.

### US-14 — Consultar detalle y disponibilidad

**Como** cliente **quiero** ver el detalle de un evento y su disponibilidad **para** decidir mi compra.

**Criterios de aceptación**
1. **Dado** un evento publicado o agotado, **cuando** pido su detalle, **entonces** veo datos del
   evento, estado, lugar con coordenadas, participantes, sectores y precios.
2. **Dado** un sector numerado, **cuando** consulto la disponibilidad, **entonces** veo el estado de
   cada asiento (libre, reservado, vendido); en un sector no numerado veo la cantidad disponible.
3. **Dado** un evento inexistente o en borrador, **cuando** pido su detalle, **entonces** recibo 404.
4. **Regla:** la disponibilidad se lee siempre de la fuente de verdad y nunca de caché (EN-08); los
   datos descriptivos sí son cacheables.

---

## F04 — Reserva temporal y compra

### US-15 — Iniciar compra eligiendo evento y sector

**Como** cliente **quiero** elegir un evento y un sector **para** comenzar mi compra.

**Criterios de aceptación**
1. **Dado** un evento publicado con disponibilidad en el sector, **cuando** inicio la compra,
   **entonces** veo el precio unitario del sector.
2. **Dado** un evento en otro estado o un sector sin disponibilidad, **cuando** intento iniciar la
   compra, **entonces** recibo 409 con el motivo.
3. **Dado** que no estoy identificado, **cuando** intento iniciar la compra, **entonces** recibo 401
   (US-44 en la Entrega 1, US-45 en la Entrega 2).

### US-16 — Seleccionar lugares

**Como** cliente **quiero** elegir asientos específicos o una cantidad de entradas **para** armar mi compra.

**Criterios de aceptación**
1. **Dado** un sector numerado, **cuando** elijo uno o más asientos libres, **entonces** se aceptan;
   si alguno está vendido, reservado por otro cliente o no existe, la respuesta indica cuáles.
2. **Dado** un sector no numerado, **cuando** elijo una cantidad, **entonces** debe ser mayor a cero
   y no superar la disponibilidad ni el límite por cliente (US-18).
3. **Dado** una selección válida, **cuando** la confirmo, **entonces** veo el precio total antes de reservar.
4. **Regla:** una compra involucra un único evento y un único sector.

### US-17 — Reservar temporalmente los lugares

**Como** cliente **quiero** que mis lugares queden reservados durante 10 minutos **para** pagar sin perderlos.

**Criterios de aceptación**
1. **Dado** lugares disponibles, **cuando** confirmo la selección, **entonces** se crea una reserva
   con cliente, evento, lugares, importe congelado y vencimiento (ahora + 10 minutos), y la respuesta
   incluye el ID de la reserva y su vencimiento.
2. **Dado** que un solo lugar de la selección no está disponible, **cuando** reservo, **entonces** no
   se reserva ninguno y recibo 409 con el detalle (todo o nada).
3. **Dado** una reserva vigente, **cuando** otro cliente intenta reservar o comprar esos lugares,
   **entonces** recibe 409.
4. **Dado** que ya tengo una reserva vigente para el evento, **cuando** intento crear otra,
   **entonces** se rechaza: máximo una reserva vigente por cliente y evento.
5. **Dado** un evento de alta demanda, **cuando** reservo, **entonces** debo presentar una
   autorización vigente de la fila virtual (US-32).

### US-18 — Limitar entradas por cliente

**Como** organizador **quiero** que cada cliente tenga un tope de entradas por evento **para** evitar
acaparamiento y reventa masiva.

**Criterios de aceptación**
1. **Regla:** el límite es por evento (6 por defecto) y configurable.
2. **Dado** un cliente, **cuando** reserva, **entonces** se suman sus entradas compradas no devueltas,
   sus reservas vigentes y la nueva selección.
3. **Dado** un cliente que con la nueva selección supera el límite, **cuando** intenta reservar,
   **entonces** recibe 422 antes de reservar y antes de procesar cualquier pago.

### US-19 — Pagar una reserva

**Como** cliente **quiero** pagar mi reserva a través de la pasarela de pagos **para** confirmar mi compra.

**Criterios de aceptación**
1. **Dado** una reserva vigente propia, **cuando** pago sin el encabezado `Idempotency-Key`,
   **entonces** recibo 400.
2. **Dado** un pago aprobado, **cuando** se procesa, **entonces** la compra queda confirmada y se
   emiten las entradas (US-22).
3. **Dado** un pago rechazado, **cuando** se procesa, **entonces** se liberan los lugares (US-21) y
   la compra queda rechazada con su motivo.
4. **Dado** una reserva vencida, **cuando** intento pagar, **entonces** no se cobra y recibo 409.
5. **Dado** que la aprobación llega con la reserva vencida y los lugares ya los tomó otro cliente,
   **cuando** se procesa, **entonces** se ejecuta el reintegro automático como compensación (EN-06).
6. **Regla:** en la Entrega 1 la pasarela es un mock con escenarios configurables (aprobado, rechazado, timeout,
   respuesta demorada); en la Entrega 2 se agrega la resiliencia de EN-07.

### US-20 — Registrar compra y consultar su resultado

**Como** cliente **quiero** consultar el estado y el detalle de mis compras **para** saber si se concretaron.

**Criterios de aceptación**
1. **Dado** una compra, **cuando** se registra, **entonces** guarda cliente, evento, entradas,
   importe, fecha, medio de pago (tipo y últimos cuatro dígitos, nunca datos completos) y referencia de pago.
2. **Regla:** estados de la compra: pendiente, aprobada, rechazada, expirada, reintegrada.
3. **Dado** mis compras, **cuando** las listo o consulto una, **entonces** veo su detalle.
4. **Dado** una compra de otro cliente, **cuando** la consulto, **entonces** recibo 404.

### US-21 — Liberar lugares cuando la reserva no se concreta

**Como** sistema **quiero** liberar los lugares reservados que no se vendieron **para** que otros
clientes puedan comprarlos.

Disparadores: pago rechazado, vencimiento de la reserva, devolución aprobada (US-28) y cancelación
del evento (US-06).

**Criterios de aceptación**
1. **Dado** una reserva vigente, **cuando** se registra un pago rechazado, **entonces** los lugares
   quedan libres de inmediato.
2. **Dado** una reserva sin pago aprobado, **cuando** vence el TTL, **entonces** se libera
   automáticamente (máximo 5 segundos de demora) y toda consulta de disponibilidad ignora las reservas vencidas.
3. **Dado** una liberación ya ejecutada, **cuando** se ejecuta otra vez, **entonces** no genera
   inconsistencias ni afecta entradas vendidas (idempotente).
4. **Dado** lugares liberados, **cuando** consulto la disponibilidad (US-14), **entonces** aparecen libres.

---

## F05 — Entradas y no doble venta

### US-22 — Emitir entrada con QR único

**Como** cliente **quiero** recibir una entrada con QR único por cada lugar comprado **para** poder
ingresar al evento.

**Criterios de aceptación**
1. **Dado** un pago aprobado, **cuando** se confirma la compra, **entonces** se emite una entrada por
   cada lugar adquirido, asociada a su compra.
2. **Regla:** cada entrada tiene ID, QR único y no adivinable (por ejemplo, un token firmado),
   evento, cliente, asiento o sector, estado (válida, utilizada, devuelta, cancelada) y fecha de emisión.
3. **Dado** que la emisión falla, **cuando** se intenta confirmar la compra, **entonces** se compensa
   con reintegro y liberación de lugares (EN-06).

### US-23 — Evitar la doble venta

**Como** organizador **quiero** que un mismo lugar nunca se venda dos veces **para** garantizar la
integridad de mi evento.

**Criterios de aceptación**
1. **Dado** un asiento libre, **cuando** N solicitudes concurrentes intentan reservarlo (incluso en
   distintas instancias del servicio), **entonces** exactamente una tiene éxito y las demás reciben 409.
2. **Dado** un sector no numerado con un único lugar disponible, **cuando** N solicitudes
   concurrentes piden ese lugar, **entonces** solo una lo obtiene y nunca se supera la capacidad.
3. **Regla:** un asiento no puede figurar en dos entradas vigentes del mismo evento; se garantiza con
   una restricción de unicidad en la base de datos (EN-04).
4. **Regla de prueba:** test automatizado con al menos 50 hilos sobre el mismo asiento y sobre el
   último lugar de una zona (EN-13).

### US-24 — Procesar reintentos sin duplicados

**Como** cliente **quiero** reintentar un pago ante un problema de conexión **para** no pagar dos
veces ni recibir entradas duplicadas.

**Criterios de aceptación**
1. **Dado** un pago ya procesado, **cuando** llega un reintento con la misma `Idempotency-Key`,
   **entonces** se devuelve el resultado registrado (misma compra, mismo código) sin cobrar de nuevo
   ni emitir entradas adicionales.
2. **Dado** una clave ya usada, **cuando** llega con un cuerpo distinto, **entonces** recibo 422.
3. **Dado** dos reintentos simultáneos con la misma clave, **cuando** se procesan, **entonces** se
   ejecuta una sola vez: el segundo espera o recibe el resultado del primero.
4. **Regla:** la clave caduca a las 24 horas.

### US-25 — Ver mis entradas

**Como** cliente **quiero** ver mis entradas con su QR **para** presentarlas en el ingreso.

**Criterios de aceptación**
1. **Dado** mis entradas, **cuando** las listo, **entonces** veo evento, fecha, lugar, sector o
   asiento, estado y QR de cada una.
2. **Dado** una entrada devuelta, cancelada o de un evento finalizado, **cuando** la listo,
   **entonces** se muestra con su estado y sin QR utilizable.
3. **Dado** una entrada de otro cliente, **cuando** la consulto, **entonces** recibo 404.

---

## F06 — Devoluciones y cancelaciones

### US-26 — Solicitar devolución de una entrada

**Como** cliente **quiero** solicitar la devolución de una entrada **para** recuperar mi dinero si no
puedo asistir.

**Criterios de aceptación**
1. **Dado** una entrada propia válida de un evento que comienza en más de 72 horas, **cuando** pido
   la devolución, **entonces** la solicitud queda **pendiente** y la entrada sigue siendo válida
   hasta que se resuelva.
2. **Dado** un evento que comienza en menos de 72 horas, **cuando** pido la devolución, **entonces**
   recibo 422 indicando que el plazo venció.
3. **Dado** una entrada que ya tiene una solicitud pendiente, o que no es propia o no está válida,
   **cuando** pido otra, **entonces** se rechaza (409, 404 o 422 según el caso).
4. **Regla:** las entradas de eventos cancelados no requieren solicitud (US-29).

### US-27 — Resolver devolución

**Como** administrador **quiero** aprobar o rechazar solicitudes de devolución **para** controlar los reintegros.

**Criterios de aceptación**
1. **Dado** que soy administrador, **cuando** listo las solicitudes pendientes, **entonces** las veo
   con su entrada y cliente.
2. **Dado** una solicitud pendiente, **cuando** la apruebo o la rechazo, **entonces** se registra
   administrador, fecha, resultado y motivo (obligatorio al rechazar).
3. **Dado** una solicitud ya resuelta, **cuando** intento resolverla, **entonces** recibo 409.
4. **Dado** una entrada ya utilizada, **cuando** intento aprobar su devolución, **entonces** recibo 409.
5. **Dado** un usuario que no es administrador, **cuando** intenta resolver, **entonces** recibe 403.
6. **Dado** una solicitud propia, **cuando** la consulto como cliente, **entonces** veo su estado:
   pendiente, aprobada o rechazada.

### US-28 — Reintegrar y liberar lugar por devolución aprobada

**Como** cliente **quiero** recibir el reintegro cuando se aprueba mi devolución **para** recuperar el
importe pagado.

**Criterios de aceptación**
1. **Dado** una devolución aprobada, **cuando** se procesa, **entonces** la entrada pasa a
   **devuelta** (su QR deja de ser válido), se libera el lugar (US-21) y se solicita a la pasarela
   el reintegro del importe pagado por esa entrada.
2. **Dado** un reintegro ya solicitado, **cuando** se vuelve a procesar la misma solicitud,
   **entonces** no se duplica (clave de idempotencia derivada de la solicitud).
3. **Dado** que la pasarela falla, **cuando** se pide el reintegro, **entonces** queda registrado
   como pendiente o rechazado, se reintenta y el administrador lo ve.
4. **Dado** un lugar liberado, **cuando** el evento sigue publicado y no comenzó, **entonces** vuelve
   a ofrecerse sin afectar otras entradas válidas.

### US-29 — Reintegrar entradas de un evento cancelado

**Como** cliente **quiero** recibir automáticamente el reintegro si el evento se cancela **para** no
perder mi dinero.

**Criterios de aceptación**
1. **Dado** un evento que pasa a cancelado, **cuando** se procesa la cancelación, **entonces** todas
   sus entradas válidas pasan a **canceladas** y se inicia un reintegro por cada compra afectada.
2. **Dado** que el proceso se interrumpe, **cuando** se reanuda, **entonces** continúa sin duplicar
   reintegros (idempotente por compra).
3. **Dado** cada comprador afectado, **cuando** se cancela el evento, **entonces** se le notifica: en la Entrega 1
   con un notificador simulado que deja registro en el log; en la Entrega 2 con el servicio real (US-39).
4. **Dado** que soy administrador, **cuando** consulto el avance, **entonces** veo los reintegros
   pendientes, completados y fallidos.

---

## F07 — Fila virtual

### US-30 — Ingresar a la fila virtual
*Actualizada en la Entrega 2.*

**Como** cliente **quiero** ingresar a una fila cuando abre la venta de un evento de alta demanda
**para** tener una oportunidad justa de compra.

**Criterios de aceptación**
1. **Dado** un evento de alta demanda con la fila abierta, **cuando** ingreso, **entonces** recibo un
   ticket de fila con ID, estado **en espera** y la hora de ingreso, y mi lugar se ordena por la
   marca de tiempo del servidor (no la del cliente).
2. **Dado** que ya estoy en la fila, **cuando** vuelvo a ingresar, **entonces** recibo el mismo
   ticket, conservo mi posición original, se indica que es un reingreso y no se duplica mi lugar.
3. **Dado** un evento sin fila o con la fila pausada, agotada o cancelada, **cuando** intento
   ingresar, **entonces** recibo 409 (`EVENTO_NO_HABILITADO`).
4. **Dado** un evento de alta demanda, **cuando** intento reservar sin una autorización vigente de la
   fila, **entonces** recibo 409 con el motivo (autorización inválida, vencida o no habilitada).
5. **Dado** un evento que no es de alta demanda, **cuando** reservo, **entonces** no se exige fila.
6. **Regla:** implementación simplificada sobre Redis (conjunto ordenado por hora de llegada y
   operaciones atómicas), justificada en el informe técnico.

### US-31 — Consultar mi posición en la fila
*Actualizada en la Entrega 2.*

**Como** cliente **quiero** conocer mi posición **para** saber cuánto falta para poder comprar.

**Criterios de aceptación**
1. **Dado** que estoy en espera, **cuando** consulto, **entonces** veo mi posición actual, cuántas
   personas tengo delante, mi estado y el intervalo sugerido para volver a consultar (10 segundos).
2. **Dado** que avanzó la fila, **cuando** vuelvo a consultar, **entonces** mi posición se actualiza.
3. **Dado** que fui habilitado, **cuando** consulto, **entonces** veo el estado **habilitado**, mi
   autorización de compra y hasta cuándo es válida.
4. **Dado** que mi ventana venció o ya usé la autorización, **cuando** consulto, **entonces** veo el
   estado **vencido** o **consumido**.
5. **Dado** que no estoy en la fila de ese evento, **cuando** consulto, **entonces** recibo 404.
6. **Regla:** cada cliente consulta solo su propia posición; se identifica por el token, no por un
   parámetro que pueda cambiar.

### US-32 — Habilitar la compra por orden y por tandas
*Actualizada en la Entrega 2.*

**Como** cliente **quiero** ser habilitado respetando el orden de llegada **para** comprar sin que el
sistema colapse.

**Criterios de aceptación**
1. **Dado** una fila abierta con clientes en espera, **cuando** se habilita una tanda, **entonces**
   pasan a **habilitados** los primeros N clientes en orden estricto de llegada (N configurable; 10
   por defecto) y cada uno recibe una autorización de compra única.
2. **Dado** un cliente habilitado, **cuando** reserva dentro de su ventana de 10 minutos,
   **entonces** la autorización se consume (es de un solo uso) y la reserva continúa (US-17).
3. **Dado** que la reserva falla después de consumir la autorización (por ejemplo, el asiento ya no
   está libre), **cuando** se rechaza, **entonces** la autorización se restaura para que el cliente
   pueda reintentar dentro de su ventana.
4. **Dado** un cliente habilitado que no reserva a tiempo, **cuando** vence su ventana, **entonces**
   pasa a **vencido** (el sistema revisa los vencimientos cada 30 segundos) y pierde el turno; puede
   volver a ingresar al final de la fila.
5. **Dado** un evento que se suspende, se cancela o se agota, **cuando** cambia su estado,
   **entonces** la fila pasa a pausada, cancelada o agotada, no admite ingresos ni habilita tandas y
   se informa a los clientes en espera. Al reanudar el evento, la fila se reabre.
6. **Regla:** el avance de la fila no bloquea el flujo de compra.

---

## F08 — Control de acceso

### US-33 — Validar una entrada al escanear su QR
*Actualizada en la Entrega 2.*

**Como** personal de acceso **quiero** escanear el QR de una entrada **para** verificar su legitimidad.

**Criterios de aceptación**
1. **Dado** un QR existente, en estado emitida y del evento en el que estoy controlando, **cuando** lo
   escaneo, **entonces** recibo 200 con **autorizado**, el sector o asiento y la hora de ingreso, sin
   datos personales del cliente.
2. **Dado** un QR inexistente, **cuando** lo escaneo, **entonces** recibo 404 con el motivo `INEXISTENTE`.
3. **Dado** un QR de otro evento, **cuando** lo escaneo, **entonces** recibo 409 con el motivo
   `EVENTO_EQUIVOCADO`.
4. **Dado** un QR de una entrada devuelta o cancelada, **cuando** lo escaneo, **entonces** recibo 409
   con el estado de la entrada como motivo.
5. **Regla:** la operación recibe el código QR y el evento; solo la pueden usar el personal de acceso y
   el administrador (US-49).

### US-34 — Marcar entrada como utilizada y rechazar el segundo uso
*Actualizada en la Entrega 2.*

**Como** personal de acceso **quiero** que cada QR ingrese una sola vez **para** evitar accesos duplicados.

**Criterios de aceptación**
1. **Dado** una entrada emitida, **cuando** se autoriza su ingreso, **entonces** pasa a **usada** y se
   guarda la fecha y hora del ingreso.
2. **Dado** una entrada ya usada, **cuando** se escanea de nuevo, **entonces** recibo 409 con el motivo
   `YA_USADA` y la hora del primer ingreso, y el primer registro no se modifica.
3. **Dado** dos lectores que escanean el mismo QR al mismo tiempo, **cuando** se procesan,
   **entonces** solo uno es autorizado: el cambio a usada es una actualización condicional atómica
   (solo se aplica si la entrada sigue emitida).
4. **Dado** cualquier validación, autorizada o rechazada, **cuando** termina, **entonces** queda
   registrada en el log con su identificador de correlación.

---

## F09 — Preferencias y recomendaciones

### US-35 — Registrar preferencias
*Actualizada en la Entrega 2.*

**Como** cliente **quiero** registrar mis gustos **para** recibir una oferta acorde a mis intereses.

**Criterios de aceptación**
1. **Dado** que guardo tipos de evento, artistas favoritos y ciudades de interés, **cuando** las
   consulto, **entonces** las veo tal como las guardé.
2. **Dado** que ya tengo preferencias, **cuando** las vuelvo a guardar, **entonces** se actualizan
   (un único documento por cliente, sin duplicados).
3. **Dado** preferencias de otro cliente, **cuando** intento verlas o modificarlas, **entonces** se
   rechaza: solo son visibles y modificables por su dueño.
4. **Dado** un artista favorito que no existe como participante, **cuando** lo guardo, **entonces**
   recibo 404.
5. **Regla:** se almacenan en MongoDB (EN-02), porque son documentos de estructura flexible que se
   consultan por cliente.

### US-36 — Recibir recomendaciones según gustos e historial

**Como** cliente **quiero** que me sugieran eventos según mis preferencias y compras anteriores
**para** descubrir propuestas de mi interés.

**Criterios de aceptación**
1. **Dado** un cliente con preferencias o compras, **cuando** pide recomendaciones, **entonces**
   recibe eventos que coinciden con sus tipos, artistas o ciudades de interés y con su historial.
2. **Dado** cualquier recomendación, **cuando** se arma la lista, **entonces** solo incluye eventos
   publicados con disponibilidad; nunca borradores, suspendidos, agotados, cancelados, finalizados ni
   eventos para los que el cliente ya compró.
3. **Dado** un cliente sin historial ni preferencias, **cuando** pide recomendaciones, **entonces**
   recibe los eventos más vendidos de su ciudad.
4. **Regla:** se admite una versión simplificada del algoritmo, justificada en el diseño.

### US-37 — Sugerir eventos relacionados
*Actualizada en la Entrega 2.*

**Como** cliente **quiero** ver "quienes compraron este evento también compraron…" **para** encontrar
eventos afines.

**Criterios de aceptación**
1. **Dado** un evento con sugerencias calculadas, **cuando** las consulto, **entonces** recibo otros
   eventos cuyos compradores coinciden, ordenados por cantidad de coincidencias, y la fecha de la
   última actualización.
2. **Dado** un evento sin sugerencias calculadas, **cuando** las consulto, **entonces** recibo 404.
3. **Regla:** solo se exponen datos agregados, nunca datos personales de otros clientes, y se exige
   un mínimo configurable de compradores coincidentes.
4. **Regla:** las sugerencias se guardan precalculadas por evento y se actualizan a partir de las
   compras confirmadas (versión simplificada; se acepta consistencia eventual).
5. **Regla:** solo se sugieren eventos publicados con disponibilidad.

---

## F10 — Notificaciones

### US-38 — Notificar compra confirmada
*Actualizada en la Entrega 2.*

**Como** cliente **quiero** recibir una notificación al confirmarse mi compra **para** tener el
comprobante de mis entradas.

**Criterios de aceptación**
1. **Dado** una compra completada, **cuando** se confirma, **entonces** se me envía un email con el
   número de compra, el evento, la fecha, el lugar y cada entrada (sector, asiento y código QR).
2. **Dado** que el envío del email falla, **cuando** se intenta, **entonces** la compra sigue confirmada
   (nunca se revierte) y la notificación queda **fallida**, con hasta 3 reintentos automáticos con
   espera creciente (30 s, 60 s, …).
3. **Dado** una notificación fallida que agotó sus reintentos, **cuando** un administrador pide
   reintentarla, **entonces** vuelve a pendiente y se reintenta; si no estaba fallida, se rechaza.
4. **Dado** el mismo aviso de compra recibido dos veces, **cuando** se procesa, **entonces** se ignora
   el duplicado y se envía un solo email.
5. **Regla:** el envío es asíncrono (mensajería con RabbitMQ) y no demora la respuesta de la compra. El
   email se simula y queda registrado en el log.

### US-39 — Notificar cambios y cancelaciones de eventos
*Actualizada en la Entrega 2.*

**Como** cliente **quiero** ser avisado si cambia o se cancela un evento al que voy a asistir **para**
tomar decisiones a tiempo.

**Criterios de aceptación**
1. **Dado** un evento modificado (US-02), **cuando** se registra el cambio, **entonces** se notifica a
   cada comprador con el nombre del evento y los cambios realizados.
2. **Dado** un evento cancelado (US-06), **cuando** se procesa la cancelación, **entonces** se notifica
   a cada comprador con el nombre del evento y el motivo de la cancelación.
3. **Dado** un comprador con varias entradas, **cuando** se notifica, **entonces** recibe un solo
   mensaje (por comprador, no por entrada).
4. **Dado** que el aviso del evento llega o se procesa más de una vez, **cuando** se reprocesa,
   **entonces** no se generan mensajes duplicados.

### US-40 — Notificar evento de artista favorito

**Como** cliente **quiero** enterarme cuando un artista favorito publica un evento en mi ciudad
**para** no perder la oportunidad de comprar.

**Criterios de aceptación**
1. **Dado** que tengo un artista favorito y una ciudad de interés configurados (US-35), **cuando** se
   publica (US-04) un evento con ese artista en un lugar de esa ciudad, **entonces** se me notifica.
2. **Dado** un cliente que no tiene configurados ese artista y esa ciudad, **cuando** se publica el
   evento, **entonces** no se le notifica.
3. **Dado** un evento no publicado, **cuando** cambia de datos, **entonces** no se notifica.
4. **Dado** un evento ya notificado, **cuando** el proceso se repite, **entonces** se envía una sola
   notificación por cliente y evento.

---

## F11 — Reportes para organizadores

### US-41 — Consultar ventas y devoluciones por sector
*Actualizada en la Entrega 2.*

**Como** organizador **quiero** ver las entradas vendidas y devueltas por sector **para** seguir la
evolución de mi evento.

**Criterios de aceptación**
1. **Dado** que soy organizador, **cuando** listo los reportes, **entonces** veo solo los de mis eventos.
2. **Dado** un evento propio, **cuando** pido su reporte, **entonces** veo por sector y en total las
   entradas vendidas, devueltas y válidas.
3. **Dado** un evento de otro organizador, **cuando** pido su reporte, **entonces** recibo 403.
4. **Dado** que soy administrador, **cuando** pido reportes, **entonces** veo los de cualquier evento y
   puedo filtrar por organizador.
5. **Regla:** el reporte se alimenta de forma asíncrona con las ventas y reintegros confirmados, así
   que puede tener una demora de segundos (consistencia eventual). Se guarda en caché 30 segundos y la
   caché se invalida con cada venta o reintegro nuevo.

### US-42 — Consultar porcentaje de ocupación
*Actualizada en la Entrega 2.*

**Como** organizador **quiero** conocer la ocupación de mi evento **para** decidir acciones comerciales.

**Criterios de aceptación**
1. **Dado** un evento propio, **cuando** pido su reporte, **entonces** veo la capacidad y el porcentaje
   de ocupación de cada sector y del evento completo.
2. **Regla:** ocupación = entradas válidas (vendidas − devueltas) ÷ capacidad × 100. La capacidad total
   es la suma de las capacidades de los sectores.
3. **Ejemplo:** un sector de 200 lugares con 150 vendidas y 10 devueltas tiene 70 % de ocupación.
4. **Dado** un evento ajeno, **cuando** pido el reporte, **entonces** recibo 403 (como en US-41).

### US-43 — Consultar recaudación
*Actualizada en la Entrega 2.*

**Como** organizador **quiero** ver la recaudación de mi evento **para** evaluar su rendimiento financiero.

**Criterios de aceptación**
1. **Dado** un evento propio, **cuando** pido su reporte, **entonces** veo por sector y en total el
   importe bruto, el importe reintegrado, el importe pendiente de reintegro y el importe neto.
2. **Regla:** neto = bruto − reintegrado. Los reintegros pendientes se muestran aparte y no se
   descuentan hasta que se aplican.
3. **Regla:** los importes se guardan en centavos para evitar errores de redondeo.
4. **Dado** un evento ajeno, **cuando** pido el reporte, **entonces** recibo 403; solo el organizador
   propietario o un administrador pueden consultarlo.

---

## F12 — Identidad y permisos
encabezados; en la Entrega 2 se reemplaza por JWT sin cambiar las reglas de negocio.

### US-44 — Identificarse mediante encabezados
*Actualizada en la Entrega 2.*

**Como** usuario de cualquier rol **quiero** identificarme con encabezados **para** operar en la
Entrega 1 sin autenticación.

**Criterios de aceptación**
1. **Dado** un endpoint no público, **cuando** faltan `X-User-Id` o `X-User-Role`, **entonces** recibo
   401; un rol inválido devuelve 400.
2. **Regla:** las reglas de rol (por ejemplo, solo administradores) y de pertenencia (organizador
   propietario, cliente dueño) se aplican sobre esta identidad desde la Entrega 1, para que en la
   Entrega 2 solo cambie el mecanismo.
3. **Dado** la Entrega 2, **cuando** una request pasa por el gateway, **entonces** el gateway completa
   `X-User-Id` y `X-User-Role` a partir del JWT validado y descarta los que haya enviado el cliente.
4. **Regla:** el mecanismo de la Entrega 1 es temporal, se documenta como inseguro y no se expone
   fuera del entorno de desarrollo.

### US-45 — Registrarse e iniciar sesión con JWT
*Actualizada en la Entrega 2.*

**Como** usuario **quiero** registrarme e iniciar sesión **para** acceder al sistema de forma segura.

**Criterios de aceptación**
1. **Dado** nombre, email válido y contraseña de 8 a 72 caracteres, **cuando** me registro,
   **entonces** se crea mi cuenta siempre con rol **cliente** y recibo 201.
2. **Dado** datos inválidos, **cuando** me registro, **entonces** recibo 400; **dado** un email ya
   registrado, recibo 409.
3. **Dado** credenciales válidas, **cuando** inicio sesión, **entonces** recibo un JWT firmado (RS256)
   con mi identificador, email, rol y un vencimiento de 1 hora.
4. **Dado** credenciales inválidas, **cuando** inicio sesión, **entonces** recibo 401 sin indicar si
   falló el email o la contraseña.
5. **Dado** un token válido, **cuando** consulto mi cuenta, **entonces** veo mis datos sin la contraseña.
6. **Dado** cualquier request al sistema, **cuando** pasa por el gateway, **entonces** se validan
   firma, emisor y vencimiento del token con la clave pública que publica el servicio de identidad
   (JWKS), y la identidad se propaga a los servicios.
7. **Regla:** las contraseñas se guardan con hash (BCrypt) y nunca aparecen en logs ni respuestas.
   Al arrancar se crea un administrador inicial configurable por variables de entorno.

### US-46 — Permisos del cliente

**Como** cliente autenticado **quiero** acceder a mis funciones **para** comprar y gestionar mis entradas.

**Criterios de aceptación**
1. **Dado** un token de cliente, **cuando** busco eventos, compro, veo mis compras y entradas,
   ingreso a la fila virtual, gestiono mis preferencias o solicito devoluciones propias,
   **entonces** la operación se permite.
2. **Dado** un token de cliente, **cuando** intento crear, publicar, suspender o cancelar eventos,
   resolver devoluciones, consultar reportes o validar QR, **entonces** recibo 403.
3. **Dado** un token de cliente, **cuando** intento ver compras, entradas o preferencias de otro
   cliente, **entonces** se rechaza (403 o 404).

### US-47 — Permisos del organizador

**Como** organizador autenticado **quiero** gestionar mis eventos **para** publicarlos y seguir su desempeño.

**Criterios de aceptación**
1. **Dado** un token de organizador, **cuando** creo, configuro o publico mis propios eventos,
   registro lugares o consulto los reportes de mis eventos, **entonces** la operación se permite.
2. **Dado** un token de organizador, **cuando** intento suspender o cancelar eventos, resolver
   devoluciones o comprar, **entonces** recibo 403.
3. **Dado** un evento de otro organizador, **cuando** intento modificarlo, publicarlo o ver sus
   reportes, **entonces** recibo 403.

### US-48 — Permisos del administrador
*Actualizada en la Entrega 2.*

**Como** administrador autenticado **quiero** realizar las tareas operativas **para** mantener el sistema.

**Criterios de aceptación**
1. **Dado** un token de administrador, **cuando** suspendo, reanudo o cancelo cualquier evento,
   apruebo o rechazo devoluciones o consulto el avance de los reintegros, **entonces** la operación se permite.
2. **Dado** un token de administrador, **cuando** doy de alta un usuario organizador, administrador o
   personal de acceso, **entonces** se crea (201); **dado** que intento crear un cliente por esta vía,
   recibo 400 (los clientes se registran solos).
3. **Dado** un usuario que no es administrador, **cuando** intenta dar de alta usuarios, **entonces** recibe 403.
4. **Regla:** las acciones operativas del administrador quedan registradas en la auditoría
   centralizada (EN-09).

### US-49 — Permisos del personal de acceso

**Como** personal de acceso autenticado **quiero** validar entradas **para** controlar el ingreso.

**Criterios de aceptación**
1. **Dado** un token de personal de acceso, **cuando** valido un QR (US-33, US-34), **entonces** la
   operación se permite. El administrador también puede validar QR.
2. **Dado** un token de personal de acceso, **cuando** intento comprar, administrar eventos o
   resolver devoluciones, **entonces** recibo 403.

### Criterios transversales a US-46 a US-49

1. **Dado** una request a un endpoint protegido sin token, **cuando** llega al gateway, **entonces** recibo 401.
2. **Dado** un token inválido, con firma falsa, de otro emisor o vencido, **cuando** llega al
   gateway, **entonces** recibo 401.
3. **Dado** un token válido cuyo rol no tiene permiso, **cuando** llega al gateway, **entonces** recibo 403.
4. **Dado** un cliente que envía a mano `X-User-Role: ADMINISTRADOR`, **cuando** llega al gateway,
   **entonces** ese encabezado se ignora y vale el rol del token.
5. **Regla:** la matriz endpoint × rol se documenta en OpenAPI y se verifica con pruebas automáticas
   (EN-13).