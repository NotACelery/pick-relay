> **HISTORICAL — SUPERSEDED BY alpha.6:** alpha.4 applied fixed-coordinate targeting to every session. That is still wrong as a global rule, but the behaviour now exists deliberately as the opt-in **Single Block** mode. **Line Mining** retains THE Pick-style crosshair following.

# Pick Relay — Handoff técnico 0.1.0-alpha.4

Este documento complementa la especificación funcional original. La especificación en
`docs/Pick-Relay-Especificacion-Pre-Desarrollo.md` sigue siendo la fuente funcional autoritativa.

## Estado actual

`0.1.0-alpha.4` contiene un pipeline end-to-end de sesión:

1. GUI 4x9 + inventario y selección/reorder de queue.
2. Configuración por `RelayEntry`: Until Broken, Durability Budget, Blocks Broken y Preserve at 1.
3. Validación previa al Start.
4. Captura de posición del jugador y del bloque de trabajo.
5. Equipado/relay entre inventario y hotbar mediante operaciones vanilla.
6. Sincronización explícita del selected hotbar slot con el servidor.
7. Ataque controlado mediante los métodos vanilla de `Minecraft`.
8. Conteo de bloques sólo para `destroyBlock` originados dentro de una invocación controlada por Pick Relay.
9. Conteo acumulado de desgaste observado, sin restar reparaciones posteriores.
10. HUD, progreso, Preserve, transición entre entradas y cleanup centralizado.

## Invariantes que no se deben romper

- La queue manda; Pick Relay nunca elige una herramienta no seleccionada.
- Una herramienta física no aparece dos veces en la queue.
- Ante identidad ambigua o desync, Stop; nunca buscar una herramienta “parecida”.
- No escribir UUID/NBT/componentes artificiales en herramientas para rastrearlas.
- `Preserve at 1` tiene prioridad sobre la cuota de trabajo.
- La sesión se detiene por desplazamiento real o click gameplay físico.
- Rotar la cámara NO detiene la sesión.
- El bloque inicial es el target de trabajo: mirar a otro lado pausa el ataque, volver al target lo reanuda.
- Un hueco temporal de un generador no autoriza atacar el bloque de atrás.
- Todos los caminos de salida pasan por `PickRelayController.stop(reason)`.

## Tracking de herramientas

Cada `RelayEntry` conserva:

- slot original;
- slot actual;
- snapshot configurado;
- último snapshot vivo conocido;
- estado runtime y progreso.

`ToolFingerprint` compara exactamente la herramienta pendiente. Durante runtime ignora sólo el damage para permitir desgaste normal. Los swaps que realiza Pick Relay actualizan los slots de todas las entradas desplazadas conocidas.

## Relay de inventario

- Tool ya en hotbar: seleccionar y sincronizar slot.
- Tool en inventario + hueco hotbar: primer hueco de izquierda a derecha.
- Hotbar llena al inicio: fallback al hotbar index 0 (slot 1 visible).
- Hotbar llena durante relay: reutilizar el slot activo de la entrada anterior.
- El swap usa `handleInventoryMouseClick(..., ClickType.SWAP)`.
- El estado local posterior se verifica antes de aceptar el movimiento lógico.

## Target lock añadido en alpha.4

Es un endurecimiento de seguridad derivado de la especificación, no una sustitución de ella.

Al iniciar se captura el `BlockPos` apuntado. Mientras ACTIVE:

- mismo `BlockPos` válido: atacar;
- otro bloque, entidad, aire o ningún target: soltar Attack y quedar esperando;
- volver a apuntar al `BlockPos` original: reanudar.

Esto conserva la regla “la cámara no cancela” y evita romper estructuras detrás del bloque regenerado.

## Estado del build

El código y recursos pasaron validaciones estáticas del snapshot, pero este entorno no pudo ejecutar el build Gradle completo porque no puede descargar la distribución/dependencias externas de Gradle.

La primera acción al probar localmente debe ser compilar con Java 21 y ejecutar `runClient`. Para esta alpha el helper fija Gradle 8.10.2 y verifica su SHA-256 antes de extraerlo.

## Orden recomendado de validación real

Usar `docs/TESTING-alpha.4.md`.

Prioridad máxima:

1. boot/mixins;
2. Start + target lock;
3. herramienta ya en hotbar;
4. inventory -> empty hotbar;
5. hotbar llena;
6. herramienta preservada + siguiente desde inventario;
7. Blocks Budget;
8. Durability Budget + Unbreaking;
9. movimiento/click emergency;
10. desync intencional.

No comenzar integración con THE Pick hasta que este pipeline haya sido validado dentro de Minecraft.
