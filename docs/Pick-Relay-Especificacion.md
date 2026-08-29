# Pick Relay — Especificación funcional y técnica 1.1.1

> **Nombre:** Pick Relay
> **Summary:** **Automate the grind. Schedule your tools.**
> **Versión de esta especificación:** 1.1.1  
> **Estado:** baseline estable; desarrollo de la pasada 1.1.x finalizado y aceptado tras QA in-game el 2026-08-29.
> **Target:** Minecraft 1.21.1 · NeoForge 21.1.235+ · Client-side

---

## 1. Definición del producto

Pick Relay es un scheduler client-side de sesiones repetitivas de minería/trabajo. El jugador selecciona herramientas concretas, define su orden y configura individualmente cómo debe utilizarse cada una.

La identidad del producto es:

> **queue explícita + configuración por herramienta + relay automático + límites de uso + seguridad**.

Pick Relay no selecciona la herramienta «óptima» ni automatiza movimiento/pathfinding. Minecraft continúa controlando alcance, velocidad, encantamientos, drops y durabilidad.

---

## 2. Plataforma

- Minecraft 1.21.1.
- NeoForge 21.1.235 o superior dentro de la rama compatible 1.21.1.
- Java 21 para desarrollo/build.
- Exclusivamente client-side.
- El servidor no necesita instalar el mod.

---

## 3. Herramientas admitidas

La versión 1.0 admite herramientas reconocidas mediante los tags vanilla:

- Pickaxes.
- Axes.
- Shovels.
- Hoes.

Las herramientas modded correctamente incluidas en estos tags pueden participar del relay sin hardcodear IDs concretos.

---

## 4. Relay Queue

### 4.1 Capacidad

La queue admite un máximo de **36 entradas**, equivalente a los 27 slots del inventario principal + 9 de hotbar.

La GUI representa la queue como **4 filas × 9 columnas**.

### 4.2 La queue no almacena ItemStacks

La representación superior es un plan lógico. Los ítems reales permanecen en el inventario del jugador.

Reordenar, eliminar o limpiar una entrada nunca tira ni destruye el ItemStack físico.

### 4.3 Identidad de entrada

Cada herramienta seleccionada crea un `RelayEntry` con identidad local propia. El slot físico es solamente un locator mutable.

Pick Relay no modifica los ItemStacks con UUID/NBT/componentes ocultos.

La herramienta concreta se sigue mediante:

- identidad local de la entrada;
- fingerprint del ItemStack;
- reconciliación de movimientos dentro de los 36 slots;
- asignación uno-a-uno de coincidencias.

Si una herramienta seleccionada se mueve, Pick Relay vuelve a localizarla. Si desaparece del inventario, su entrada se marca `SKIPPED` cuando corresponde y el scheduler continúa.

---

## 5. Configuración por herramienta

La configuración pertenece al `RelayEntry` seleccionado, nunca a la queue completa ni a la posición numérica.

Cada entrada conserva su configuración al reordenarse.

### 5.1 Until Broken

Usa la herramienta hasta que se rompe.

Con `Preserve at 1` activo, funcionalmente significa «usar hasta el límite de seguridad».

### 5.2 Durability Budget

Consume una cantidad configurada de **durabilidad realmente perdida**.

Unbreaking no avanza el budget cuando evita daño. Si Mending repara posteriormente la herramienta, la durabilidad ya consumida continúa contabilizada.

### 5.3 Blocks Broken

Finaliza la entrada después de una cantidad configurada de bloques destruidos exitosamente por Pick Relay.

No cuenta:

- clicks;
- swings;
- golpes parciales;
- drops;
- ítems recogidos;
- destrucciones ajenas al proceso de minería de Pick Relay.

Por lo tanto funciona aunque los drops vayan directamente a hoppers u otros sistemas.

### 5.4 Preserve at 1

Protección independiente del Work Mode.

La seguridad tiene prioridad sobre el target productivo. Pick Relay realiza comprobaciones antes y después del proceso de rotura y aplica una guarda adicional en las últimas durabilidades para evitar un uso extra por actualizaciones tardías del stack.

Una herramienta que ya está en el umbral seguro se preserva/salta sin intentar romperla.

---

## 6. Modos de minería de sesión

La elección del modo es global para la sesión, mientras que los límites de uso son individuales por herramienta.

### 6.1 Single Block

Al iniciar se captura la coordenada exacta del bloque bajo el crosshair.

- Sólo se mina esa coordenada.
- Mirar a otro lugar pausa el ataque sin detener la sesión.
- Si el bloque desaparece temporalmente, la sesión espera.
- Al volver a apuntar a la coordenada y existir nuevamente un bloque, continúa.

Objetivo principal: granjas compactas donde romper el bloque de respaldo sería peligroso.

### 6.2 Line Mining

Cada ciclo vuelve a resolver el bloque válido bajo el crosshair usando el alcance normal del jugador.

- Girar la cámara redirige el automining inmediatamente.
- Aire temporal/no objetivo válido pausa el ataque sin detener la sesión.
- No existe target fijo.

Objetivo principal: líneas de generación y granjas avanzadas.

---

## 7. GUI

La pantalla principal no pausa deliberadamente el mundo. Sus bloques funcionales son:

1. Relay Queue 4×9.
2. Session panel de telemetría y efectos.
3. Player Inventory.
4. Herramienta seleccionada y detalles.
5. Controles individuales de Work Mode/target/Preserve.
6. Session Mode: Single Block / Line Mining.
7. Start/Stop, Clear y Close.

En altura compacta, Queue y Player Inventory se muestran lado a lado; cuando existe ancho suficiente, el Session panel se ubica entre ambos y **Selected Tool** permanece debajo del bloque superior. En altura normal, el Session panel se ubica a la derecha de Queue. Si el ancho no alcanza, el panel se oculta antes de sacrificar la legibilidad del layout. Toda la geometría interactiva (hover, click y drag) debe usar las mismas funciones de layout que el render para que redimensionar la ventana no desplace las hitboxes respecto de los slots visibles.

### 7.1 Interacciones de inventario

- Click izquierdo sobre herramienta válida: toggle add/remove.
- Drag iniciado en herramienta no seleccionada: gesto ADD.
- Drag iniciado en herramienta seleccionada: gesto REMOVE.
- Un slot se procesa una sola vez por gesto.
- El orden de ADD sigue el primer ingreso del cursor a cada slot.

### 7.2 Interacciones de queue

- Click izquierdo: seleccionar/inspeccionar la entrada.
- Click derecho: eliminar entrada.
- Hold + drag: reorder.
- Zona central del destino: swap.
- Bordes/entre slots: insert before/after.
- Drop fuera de la queue: remove del `RelayEntry`.
- Entrada ACTIVE: borde exterior blanco.
- Entrada seleccionada para inspección: borde interior dorado, independiente del estado ACTIVE.
- Destino de swap/inserción durante drag: indicador cian.

### 7.3 ACTIVE

Durante una sesión:

- la estructura/configuración queda read-only;
- siguen disponibles hover, tooltips, selección visual e inspección;
- puede cerrarse/reabrirse la GUI sin detener la sesión;
- el mismo bind configurable de Pick Relay puede cerrar la GUI;
- la tecla vanilla de inventario también cierra la GUI;
- al abrir o mantener abierta la GUI, Pick Relay suprime inputs normales de gameplay en segundo plano: uso de ítems, ataque manual y teclas de movimiento;
- cualquier uso de ítem ya iniciado se libera inmediatamente y un minado manual en curso se aborta;
- si la sesión está ACTIVE, el automining controlado por Pick Relay es la única acción de gameplay que continúa intencionalmente detrás de la GUI;
- Stop AFK Mining finaliza manualmente el relay.

### 7.4 Session panel (1.1.0)

El panel de sesión expone información que normalmente quedaría oculta mientras el usuario mantiene Pick Relay abierto durante automining:

- tiempo transcurrido desde Start, medido en ticks de sesión;
- bloques destruidos exitosamente por llamadas cuya procedencia pertenece a Pick Relay;
- BPS teórico de la herramienta ACTIVE contra el bloque actualmente bajo el crosshair;
- posición de la herramienta ACTIVE dentro de la queue (`X/Y`);
- efectos activos del jugador, incluyendo nivel y duración restante;
- prioridad visual para Haste, Conduit Power y Mining Fatigue;
- modificadores positivos de `BLOCK_BREAK_SPEED`, `MINING_EFFICIENCY` y `SUBMERGED_MINING_SPEED`.

Los bonus de atributos se leen desde el estado real del jugador. No existe una dependencia hardcodeada con Artifacts u otro mod: accesorios compatibles como Digging Claws pueden aparecer porque modifican atributos vanilla de velocidad de minado.

### 7.5 Estimador de velocidad de minado (1.1.0)

Mientras exista un bloque bajo el crosshair dentro del alcance normal del jugador, **Selected Tool** calcula una estimación para la entrada de queue inspeccionada sin equiparla realmente. La estimación considera:

- velocidad de minado que el `ItemStack` declara para ese `BlockState`;
- requisito de herramienta/tier correcto para el bloque;
- modificadores de atributos que tendría esa herramienta en `MAINHAND`, incluidos encantamientos como Efficiency;
- Haste o Conduit Power;
- Mining Fatigue;
- `BLOCK_BREAK_SPEED`, `MINING_EFFICIENCY` y `SUBMERGED_MINING_SPEED`;
- penalización por estar bajo el agua o sin tocar el suelo.

El resultado se cuantiza a ticks de Minecraft y se muestra como BPS aproximado y segundos por bloque. Durante ACTIVE, el Session panel usa el destroy-progress real de la herramienta actualmente equipada para acercarse todavía más al comportamiento efectivo.

El valor representa capacidad teórica de rotura del bloque. No intenta modelar respawn de generadores, latencia de red, TPS ni delays propios de otros mods fuera del cálculo normal de destroy progress.

---

## 8. Lifecycle de la configuración

La queue es temporal.

- Cerrar la GUI antes de Start limpia la planificación.
- Clear limpia manualmente la planificación mientras es editable.
- Stop manual limpia la sesión/queue.
- Queue agotada limpia la sesión/queue.
- Un stop de seguridad limpia la sesión/queue.
- Cerrar la GUI mientras ACTIVE no limpia ni detiene la sesión.

---

## 9. Relay de inventario/hotbar

El orden lógico de la queue siempre tiene prioridad sobre el orden físico.

### 9.1 Tool ya en hotbar

Se selecciona su slot y se sincroniza explícitamente el carried item con vanilla antes de continuar la minería.

### 9.2 Tool en inventario + hueco libre

Se mueve al primer slot libre de hotbar de izquierda a derecha.

### 9.3 Hotbar llena

El slot activo funciona como relay slot.

- La siguiente herramienta entra al slot activo.
- La herramienta preservada/completada ocupa el slot de inventario de origen de la siguiente.
- Si no existe un slot activo reutilizable inicial, el fallback es hotbar slot 1 (inventory index 0).

Los resultados de SWAP se verifican localmente, incluyendo cantidades exactas de stacks desplazados.

---

## 10. Tracking ante movimientos externos

Antes y durante la ejecución, Pick Relay reconcilia las entradas pendientes/activas contra el inventario actual.

- Tool movida: se actualiza su locator.
- Tool pendiente ausente: se conserva lógicamente y se marca `SKIPPED` al llegar su turno.
- Tool activa movida: se intenta relocalizar/re-equipar.
- Situación ambigua o insegura que no pueda resolverse de forma determinista: fail-safe.

Dos herramientas absolutamente idénticas a nivel de ItemStack no poseen una identidad física distinguible expuesta por Minecraft. En ese caso se asignan determinísticamente de forma uno-a-uno sin mutar los stacks.

---

## 11. Estados de entrada

Estados runtime utilizados:

- `PENDING`
- `ACTIVE`
- `COMPLETED`
- `BROKEN`
- `PRESERVED`
- `SKIPPED`

---

## 12. Session state

Máquina general:

```text
IDLE
  ↓ abrir/configurar
CONFIGURING
  ↓ Start
STARTING
  ↓
ACTIVE
  ↓ Stop / complete / safety
STOPPING
  ↓
IDLE
```

`PickRelayController` es la fuente de verdad del estado activo y centraliza el cleanup.

---

## 13. Safety

El ancla espacial es exclusivamente la posición/dimensión del jugador.

La sesión se detiene si:

- el jugador cambia de posición más allá de la tolerancia mínima;
- cae por romperse el bloque bajo sus pies;
- recibe knockback/empuje;
- muere/respawnea;
- se desconecta/sale del mundo;
- cambia de dimensión;
- aparece un desync/estado de inventario que no puede resolverse con seguridad;
- la queue se agota;
- el usuario pulsa Stop en Pick Relay.

No son por sí solos motivos de Stop:

- rotar la cámara;
- abrir chat;
- abrir el inventario normal del jugador;
- abrir el menú de pausa;
- clicks físicos de gameplay.

Contenedores externos que cambien el `containerMenu` pueden invalidar temporalmente las operaciones de relay y se tratan de forma conservadora.

---

## 14. HUD

Mientras ACTIVE se muestra una línea compacta en la zona vanilla de mensajes de evento/jukebox sobre la hotbar.

Incluye según corresponda:

- `Tool Z/X`;
- Blocks `actual/target`;
- Durability `consumido/target`;
- Until Broken;
- indicador de Preserve;
- estado Waiting cuando no existe un bloque de trabajo válido.

---

## 15. Tooltips e inspección

Pick Relay conserva el tooltip vanilla/modded y agrega información propia:

- posición en queue;
- ubicación física actual;
- durabilidad restante/máxima/%;
- Work Mode;
- target;
- Preserve;
- estado runtime;
- progreso mientras ACTIVE.

Las entradas terminadas conservan snapshots visuales durante la sesión para mantener el historial legible.

---

## 16. Compatibilidad con encantamientos

Pick Relay no reimplementa encantamientos.

Minecraft continúa manejando Efficiency, Unbreaking, Silk Touch, Fortune, Mending y encantamientos modded. Pick Relay observa únicamente las consecuencias relevantes para la sesión.

---

## 17. No objetivos de la línea 1.x

Pick Relay no debe transformarse en un bot general. La línea 1.x no:

- busca la herramienta «mejor»;
- selecciona herramientas no agregadas por el usuario;
- cuenta drops para targets;
- repara herramientas;
- mueve al jugador;
- apunta automáticamente;
- busca nuevos bloques;
- aumenta alcance/velocidad;
- rompe múltiples bloques por acción;
- hace vein mining;
- hace pathfinding;
- genera recursos.

---

## 18. Arquitectura 1.x

Responsabilidades principales:

- `PickRelayController`: lifecycle, transiciones y coordinación.
- `RelayQueue` / `RelayEntry`: plan y estado por herramienta.
- `ToolTracker` / `ToolFingerprint`: identidad lógica y relocalización.
- `InventoryRelayManager`: equip/swap/hotbar sync.
- `MiningProgressTracker`: blocks/durability/completion.
- `SafetyMonitor`: posición, dimensión, vida/conexión.
- `PickRelayScreen`: planificación, inspección, gestos de queue/inventario, telemetría y geometría responsiva. La clase permanece monolítica durante 1.1.1 para no mezclar un refactor arquitectónico con el hotfix; `docs/DEVELOPMENT.md` define fronteras de extracción posteriores.
- `PickRelayHud`: feedback runtime.
- Mixin sobre `MultiPlayerGameMode.destroyBlock`: procedencia de destrucción y accounting.

---

## 19. Definición de completitud del motor 1.0

La versión 1.0 se considera completa cuando se valida ingame:

- queue de hasta 36 herramientas;
- add/remove/reorder;
- configuraciones independientes por entrada;
- tres Work Modes;
- Preserve at 1;
- Single Block y Line Mining;
- relay con hotbar llena;
- seguimiento de herramientas movidas;
- skip de herramientas ausentes;
- block budgets con hopper;
- Unbreaking/Mending;
- categorías pickaxe/axe/shovel/hoe;
- movement/death/disconnect/dimension safety;
- GUI/HUD/lifecycle.

Estos criterios fueron validados durante el desarrollo previo al cierre de 1.0.0.

---

## 20. Futuro

La integración opcional con **THE Pick / Eruruu's Patch** sigue siendo una mejora futura. Pick Relay funciona de manera independiente y no requiere Eruruu's Patch.

La integración futura debe reutilizar el controller ya validado, evitando duplicar dos motores distintos de automining.
