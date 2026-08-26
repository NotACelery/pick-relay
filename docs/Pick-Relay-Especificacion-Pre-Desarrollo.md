# Pick Relay — Especificación funcional y técnica pre-desarrollo

> **Nombre:** Pick Relay  
> **Summary:** **Automate the grind. Schedule your tools.**  
> **Concepto:** gestor client-side de sesiones de minería/trabajo AFK basado en una queue explícita y ordenada de herramientas, con límites de uso por herramienta, rotación automática, protección de durabilidad, HUD, GUI de planificación y apagados de seguridad.
>
> **Estado de este documento:** especificación pre-desarrollo para revisión.  
> **Objetivo:** que un chat nuevo pueda comenzar la implementación sin depender del contexto completo de la conversación original.

---

# 1. Visión general

Pick Relay nace de una necesidad simple: automatizar tareas repetitivas de minería en granjas donde el jugador permanecería manualmente manteniendo presionado el botón de ataque y cambiando herramientas cuando se desgastan o rompen.

La idea evoluciona hacia algo más amplio que un simple auto-click o auto-tool-switcher.

Pick Relay debe permitir al usuario **programar una sesión de trabajo**:

- qué herramientas se usarán;
- en qué orden exacto;
- cuánto debe trabajar cada una;
- si puede romperse o debe preservarse;
- cuándo debe pasar a la siguiente;
- cuándo debe detenerse toda la sesión;
- y qué condiciones de seguridad deben cancelar inmediatamente el automining.

La unidad central no es “la mejor herramienta disponible”, sino una **entrada explícita en una queue configurada por el usuario**.

Ejemplo conceptual:

```text
#01 Stone Pickaxe
     Until broken

#02 Diamond Pickaxe
     Break 150 blocks
     Preserve at 1 durability

#03 Diamond Pickaxe — Silk Touch
     Break 120 blocks
     Preserve at 1 durability

#04 Netherite Pickaxe
     Consume 250 durability
     Preserve at 1 durability

END
→ Stop AFK Mining
```

La intención no es generar recursos gratuitamente ni acelerar las mecánicas vanilla.

Minecraft continúa:

- rompiendo los mismos bloques;
- utilizando las mismas herramientas;
- consumiendo la misma durabilidad;
- aplicando Efficiency, Haste, Unbreaking, Silk Touch, Fortune, etc.;
- y utilizando las reglas normales del servidor/mundo.

Pick Relay solamente automatiza la administración repetitiva de la sesión.

---

# 2. Identidad del mod

## 2.1 Nombre

**Pick Relay**

Se consideró `Tool Relay`, pero se descarta inicialmente porque:

- `Pick Relay` tiene mayor personalidad;
- comunica inmediatamente minería/Minecraft;
- evita mezclarse con muchos mods genéricos llamados Auto Tool, Tool Switcher, Tool Swap, etc.;
- la palabra “Relay” representa correctamente el concepto de que una herramienta entrega el trabajo a la siguiente.

Aunque el mod posteriormente admita hachas, palas y azadas, el nombre Pick Relay se mantiene.

## 2.2 Summary

> **Automate the grind. Schedule your tools.**

Debe conservarse como descripción corta principal salvo una razón fuerte para cambiarla.

Describe la diferencia central respecto a un auto-tool-switcher:

> Pick Relay no decide qué herramienta sería mejor.  
> El usuario **agenda/programa** exactamente qué herramientas harán el trabajo.

---

# 3. Plataforma y alcance inicial

## 3.1 Objetivo recomendado de primera versión

- Minecraft **1.21.1**
- **NeoForge**
- **Client-side**
- Diseñado inicialmente pensando en uso dentro de NeoBlock y otros modpacks con tareas repetitivas/granjas manuales.

La implementación debería mantenerse lo más genérica posible para funcionar también fuera de NeoBlock.

## 3.2 Client-side

La intención es que Pick Relay sea un mod exclusivamente client-side siempre que las APIs y operaciones necesarias lo permitan de forma robusta.

El servidor no debe necesitar instalar Pick Relay para que el jugador lo utilice.

La funcionalidad debe basarse en las mismas acciones que puede realizar legítimamente el cliente:

- mantener Attack;
- seleccionar slots de hotbar;
- realizar swaps/movimientos de inventario permitidos;
- observar el estado de la herramienta;
- observar confirmaciones de bloques destruidos;
- mostrar GUI/HUD local.

## 3.3 No objetivos iniciales

Pick Relay **NO** debería en su primera versión:

- encontrar automáticamente “la mejor herramienta”;
- seleccionar herramientas que el usuario no haya agregado a la queue;
- analizar drops recogidos;
- intentar producir exactamente X unidades de un ítem;
- reparar herramientas;
- aplicar Mending automáticamente;
- mover al jugador;
- apuntar automáticamente a bloques;
- buscar nuevos bloques que minar;
- incrementar alcance;
- aumentar velocidad de minería;
- romper múltiples bloques;
- hacer vein mining;
- generar recursos;
- realizar pathfinding;
- actuar como bot completo.

Pick Relay automatiza el trabajo frente al bloque/posición que el jugador haya preparado.

---

# 4. Diferenciación respecto a mods existentes

Durante la planificación se revisaron alternativas existentes en CurseForge y Modrinth.

Existen mods que cubren partes de la idea:

- automining/toggle del botón de ataque;
- cambio automático de herramientas;
- selección de herramientas desde la hotbar;
- reemplazo de herramientas desde inventario;
- protección contra rotura;
- selección automática de la herramienta más eficiente.

Sin embargo, no se encontró un equivalente que reúna el diseño completo de Pick Relay:

- queue manual y explícita;
- orden definido por el jugador;
- entradas individuales aunque sean herramientas del mismo tipo;
- herramientas seleccionadas desde inventario/hotbar;
- límites diferentes por entrada;
- límites por bloques destruidos;
- límites por durabilidad realmente consumida;
- “hasta romper”;
- preservación individual en 1 de durabilidad;
- reorder visual de la queue;
- GUI de planificación/inspección;
- HUD de progreso;
- apagados por desplazamiento o input manual;
- comportamiento pensado especialmente para granjas AFK delicadas.

La identidad competitiva de Pick Relay debe mantenerse centrada en ser un **scheduler de herramientas y sesiones**, no otro auto-tool-selector.

---

# 5. Concepto central: Relay Session

Una sesión activa representa un plan de trabajo inmutable que Pick Relay está ejecutando.

## 5.1 Estados generales

Se recomienda una máquina de estados equivalente a:

```text
IDLE
  ↓ abrir GUI / configurar
CONFIGURING
  ↓ Start AFK Mining
STARTING
  ↓ validación correcta
ACTIVE
  ↓ Stop / emergencia / error / queue completa
STOPPING
  ↓ cleanup
IDLE
```

La GUI puede abrirse durante `ACTIVE`, pero la queue debe pasar a modo **read-only**.

## 5.2 Principio de seguridad

Cuando haya duda entre:

- continuar minando;
- o detenerse;

Pick Relay debe preferir **detenerse**.

Especialmente en granjas de:

- cobblestone;
- stone;
- lava/agua;
- moss;
- madera movida por pistones;
- rooted dirt;
- generadores compactos;
- mecanismos donde romper el bloque incorrecto pueda dañar la construcción.

---

# 6. Herramientas admitidas

El concepto comenzó con picotas, pero debe generalizarse a herramientas de trabajo/minería.

Primera versión deseada:

- Pickaxes
- Axes
- Shovels
- Hoes

No debe depender exclusivamente de los ítems vanilla si existe una forma segura de reconocer herramientas modded compatibles.

La lógica debería intentar soportar herramientas modded siempre que se comporten dentro del sistema normal de herramientas/durabilidad de Minecraft.

## 6.1 La queue contiene herramientas concretas

La queue NO contiene simplemente:

```text
DIAMOND_PICKAXE
STONE_PICKAXE
```

Contiene referencias lógicas a **ItemStacks concretos seleccionados por el usuario**.

Dos Diamond Pickaxes idénticas visualmente pueden representar dos entradas diferentes:

```text
#04 Diamond Pickaxe A → 150 blocks
#05 Diamond Pickaxe B → 300 durability
```

No deben colapsarse ni tratarse como equivalentes.

---

# 7. Modelo conceptual de una entrada de queue

Nombre sugerido:

```text
RelayEntry
```

Información conceptual que debería representar:

```text
RelayEntry
├─ unique session identity
├─ queue index
├─ reference / locator for selected ItemStack
├─ original inventory slot
├─ current inventory slot
├─ item snapshot for GUI/session history
├─ initial durability at session start
├─ current durability
├─ work mode
├─ work target
├─ work progress
├─ preserveAtOne
├─ status
└─ metadata needed to safely identify/move the tool
```

No necesariamente todos esos campos deben persistirse literalmente; representan las responsabilidades necesarias.

## 7.1 Estado de entrada

Posibles estados conceptuales:

```text
PENDING
ACTIVE
COMPLETED
BROKEN
PRESERVED
SKIPPED
INVALID
```

Durante una sesión, las entradas ya utilizadas pueden mantenerse como snapshots visuales para mostrar progreso.

---

# 8. Modos de trabajo por herramienta

Cada entrada debe poder definir su propio comportamiento.

## 8.1 Until Broken

```text
Use until broken
```

La herramienta trabaja hasta desaparecer por pérdida total de durabilidad.

Uso ideal:

- Stone Pickaxes desechables;
- herramientas baratas;
- herramientas creadas específicamente para sacrificarlas.

## 8.2 Durability Budget

El usuario define cuánta **durabilidad real puede consumir Pick Relay** de esa herramienta.

Ejemplo:

```text
Durabilidad restante al empezar: 347
Budget: 180
```

Pick Relay deja de utilizarla una vez que Minecraft haya consumido realmente 180 puntos de durabilidad.

Resultado esperado aproximado:

```text
347 → 167 remaining
```

No se cuentan bloques ni golpes.

### Importante: Unbreaking

Unbreaking puede permitir romper más bloques sin consumir durabilidad.

Por lo tanto:

> 180 durability NO significa 180 bloques.

Pick Relay debe observar el desgaste real del ItemStack.

## 8.3 Block Budget

El usuario define cuántos **bloques deben ser destruidos exitosamente** usando esa herramienta.

Ejemplo:

```text
Normal Pickaxe
Break 150 blocks

Silk Touch Pickaxe
Break 120 blocks
```

Esto permite planificar producción en una granja:

- ~150 cobblestone con la primera;
- 120 stone con Silk Touch;
- después detenerse.

Pick Relay NO cuenta drops.

Sólo cuenta **bloques destruidos confirmados**.

Esto es deliberado porque los materiales pueden:

- caer en hoppers;
- ir directamente a cofres;
- ser transportados por corrientes;
- no entrar nunca al inventario del jugador;
- verse afectados por Fortune;
- tener múltiples drops.

## 8.4 Relación entre Work Limit y Safety

La protección de herramienta es independiente del modo de trabajo.

Ejemplo:

```text
Work limit:
Break 400 blocks

Safety:
Preserve at 1 durability
```

La entrada termina cuando ocurra primero:

- se cumplen los 400 bloques;
- o queda solamente 1 punto de durabilidad.

La prioridad es:

> **Safety > Production Target**

Pick Relay jamás debe romper una herramienta protegida simplemente para alcanzar la cuota configurada.

---

# 9. Preserve at 1 durability

Cada herramienta debe poder marcar:

```text
☑ Preserve at 1 durability
```

La herramienta debe dejar de utilizarse cuando llegue a un punto de durabilidad restante.

## 9.1 Semántica

Pick Relay debe detener el ataque/cambiar herramienta **antes de provocar el siguiente uso que pueda romperla**.

Flujo conceptual:

```text
Tool reaches 1 remaining durability
↓
release Attack
↓
complete/preserve current RelayEntry
↓
equip next tool
↓
resume Attack
```

## 9.2 Herramienta que ya está en 1

Si una herramienta seleccionada ya tiene:

```text
1 durability remaining
```

y posee `Preserve at 1`:

- puede permanecer en la queue;
- debe mostrarse con advertencia;
- cuando llegue su turno debe saltarse sin intentar utilizarla.

Mensaje posible:

```text
No usable durability
```

---

# 10. Contador de bloques destruidos

Esta parte es crítica.

## 10.1 Qué cuenta

Debe incrementarse únicamente cuando un bloque haya sido **destruido exitosamente**.

```text
0/150
1/150
2/150
...
150/150
```

## 10.2 Qué NO cuenta

No debe contar:

- click izquierdo;
- ticks manteniendo Attack;
- golpes parciales;
- comenzar a romper un bloque;
- cambiar de objetivo antes de destruirlo;
- recoger un drop;
- recibir un ítem;
- una animación de swing;
- tiempo de minería.

## 10.3 Objetivo

El contador debe seguir funcionando aunque:

- todos los drops caigan directamente en un hopper;
- el inventario esté lleno;
- Fortune genere cantidades variables;
- Silk Touch cambie el drop;
- los recursos sean transportados inmediatamente.

---

# 11. Queue

## 11.1 Capacidad

Máximo inicial:

```text
36 herramientas
```

Motivo:

- inventario normal del jugador = 27 slots;
- hotbar = 9 slots;
- total = 36.

La interfaz superior tendrá exactamente:

```text
4 filas × 9 columnas
```

No se utilizará un cofre doble vanilla completo de 54 slots porque las dos filas extra serían desperdiciadas.

## 11.2 La queue NO es un inventario

Los slots superiores son una representación/configuración.

No almacenan físicamente los ItemStacks.

Mover:

```text
Queue #8 → Queue #3
```

no mueve la herramienta en el inventario real.

Eliminar una entrada de la queue:

- no elimina el ítem;
- no lo tira al suelo;
- no cambia su slot físico necesariamente.

---

# 12. GUI principal

Diseño conceptual:

```text
┌─────────────────────────────────────────────┐
│                 PICK RELAY                  │
│                                             │
│             RELAY QUEUE (4 × 9)             │
│ [01][02][03][04][05][06][07][08][09]       │
│ [10][11][12][13][14][15][16][17][18]       │
│ [19][20][21][22][23][24][25][26][27]       │
│ [28][29][30][31][32][33][34][35][36]       │
│                                             │
│            SELECTED TOOL DETAILS            │
│              [Tool Icon]                    │
│                                             │
│              Work settings                  │
│             Slider / values                 │
│          Preserve at 1 checkbox             │
│                                             │
│              PLAYER INVENTORY               │
│ [ ][ ][ ][ ][ ][ ][ ][ ][ ]                 │
│ [ ][ ][ ][ ][ ][ ][ ][ ][ ]                 │
│ [ ][ ][ ][ ][ ][ ][ ][ ][ ]                 │
│ [ ][ ][ ][ ][ ][ ][ ][ ][ ]                 │
│                                             │
│           [ Start AFK Mining ]              │
└─────────────────────────────────────────────┘
```

La pantalla NO debe pausar el mundo.

---

# 13. Bind

Debe existir un keybind configurable.

Ejemplo deseado:

```text
Mouse Button 5
```

pero el jugador podrá cambiarlo desde Controles.

## 13.1 Si Pick Relay está inactivo

Pulsar el bind:

```text
→ abre Pick Relay GUI
```

## 13.2 Si Pick Relay está activo

Pulsar el bind:

```text
→ vuelve a abrir Pick Relay GUI
```

NO debe detener automáticamente la sesión.

Dentro de la GUI, el botón:

```text
Start AFK Mining
```

debe cambiar a:

```text
Stop AFK Mining
```

## 13.3 GUI durante sesión activa

La minería puede continuar mientras la GUI está abierta.

La pantalla debe permitir:

- inspeccionar la queue;
- hacer hover;
- ver tooltips;
- seleccionar visualmente herramientas para leer detalles;
- ver progreso;
- detener manualmente.

Pero debe bloquear modificaciones estructurales de la queue durante la sesión.

---

# 14. Selección desde inventario/hotbar

La interfaz inferior representa el inventario real.

## 14.1 Click izquierdo corto

Sobre herramienta compatible:

```text
si NO está seleccionada:
→ agregar al final de queue

si YA está seleccionada:
→ eliminar de queue
```

## 14.2 Drag-select / “paint selection”

Debe imitar la sensación del drag de inventario vanilla.

El usuario mantiene click izquierdo y arrastra por distintas herramientas.

### La primera herramienta define la operación completa

Si el gesto comienza sobre una herramienta NO seleccionada:

```text
DRAG MODE = ADD
```

Todas las herramientas válidas atravesadas se agregan.

Si comienza sobre una herramienta YA seleccionada:

```text
DRAG MODE = REMOVE
```

Todas las herramientas seleccionadas atravesadas se eliminan.

El modo NO cambia durante el mismo gesto.

### Ejemplo ADD

```text
P1 → P2 → P3 → P5
```

produce:

```text
Queue:
1. P1
2. P2
3. P3
4. P5
```

El orden se determina por el orden en que el cursor entra por primera vez en cada slot.

### Ejemplo REMOVE

Si P2 y P3 están seleccionadas:

```text
start on P2
drag P2 → P3 → P6
```

Resultado:

- P2 se elimina;
- P3 se elimina;
- P6 NO se agrega, porque el gesto sigue siendo REMOVE.

## 14.3 Slots revisitados

Durante el mismo drag, cada slot debe procesarse como máximo una vez.

Ejemplo:

```text
P1 → P2 → P1 → P3
```

P1 no debe agregarse/eliminarse dos veces.

Implementación conceptual:

```text
visitedSlotsThisDrag
```

Se limpia al soltar click izquierdo.

## 14.4 Slots inválidos

Durante drag:

- slots vacíos se ignoran;
- ítems no compatibles se ignoran;
- no cambian el modo ADD/REMOVE.

---

# 15. Interacciones en la queue superior

La queue tiene una semántica diferente al inventario.

## 15.1 Click izquierdo corto

Selecciona la herramienta para mostrar su panel de detalles/configuración.

NO modifica su posición.

## 15.2 Click derecho

Elimina inmediatamente esa entrada de la queue.

Después:

- los índices se recalculan;
- la visualización se compacta.

Ejemplo:

```text
[1][2][3][4][5]
       right click #3
```

queda conceptualmente:

```text
[1][2][4][5]
```

y visualmente se renumera:

```text
[1][2][3][4]
```

## 15.3 Hold + drag

Mantener click izquierdo sobre una entrada y comenzar a desplazar el cursor entra en:

```text
QUEUE_DRAG
```

Debe diferenciarse de un click corto mediante un umbral pequeño de movimiento.

Durante el drag:

- el icono original puede atenuarse/dejar un hueco;
- un “ghost icon” sigue al cursor;
- se dibuja feedback del resultado esperado.

---

# 16. Reordenamiento: swap vs insert

## 16.1 Soltar sobre el centro de otro slot ocupado

Operación:

```text
SWAP
```

Ejemplo:

```text
[1][2][3][4][5]
```

mover #2 sobre el centro de #4:

```text
[1][4][3][2][5]
```

## 16.2 Soltar entre herramientas

Operación:

```text
INSERT
```

Ejemplo:

```text
[1][2][3][4][5]
```

mover #5 entre #1 y #2:

```text
[1][5][2][3][4]
```

## 16.3 Zonas dentro del slot

Para evitar exigir precisión de píxel, cada slot puede dividirse horizontalmente:

```text
┌────────────────────┐
│ INSERT | SWAP | INSERT │
└────────────────────┘
    25%     50%    25%
```

Conceptualmente:

- 25 % izquierdo → Insert Before
- 50 % central → Swap
- 25 % derecho → Insert After

Las proporciones exactas pueden ajustarse visualmente.

## 16.4 Feedback visual

Debe mostrarse claramente qué ocurrirá al soltar.

Ejemplos:

```text
[7] │ [8]
```

para INSERT.

O highlight central del slot para SWAP.

---

# 17. Soltar herramienta fuera de la zona de queue

Si durante `QUEUE_DRAG` la herramienta se suelta fuera de cualquiera de las 36 posiciones válidas:

```text
→ remove from queue
```

Esto NO tira ni mueve físicamente el ítem real.

Sólo elimina el `RelayEntry`.

---

# 18. Panel de detalles de herramienta

Al hacer click izquierdo corto sobre una entrada de queue debe aparecer un panel entre queue e inventario.

Debe mostrar como mínimo:

- icono de la herramienta;
- posición actual en queue;
- nombre;
- durabilidad actual;
- durabilidad máxima;
- porcentaje restante;
- configuración de trabajo;
- configuración de preservación.

El icono debe permitir hover.

---

# 19. Tooltip de herramienta

Debe reutilizarse el tooltip vanilla/modded completo siempre que sea posible.

Esto preserva automáticamente:

- custom name;
- enchantments;
- lore;
- componentes;
- atributos;
- tooltips aportados por otros mods.

Pick Relay debe agregar datos propios, no reemplazar el tooltip normal.

Ejemplo:

```text
Diamond Pickaxe
Efficiency V
Unbreaking III
Silk Touch

Pick Relay
Queue position: 7 / 24
Durability: 1384 / 1561
Remaining: 88.7%
Inventory location: Hotbar 1
```

Durante sesión activa:

```text
▶ Currently working
Blocks: 83 / 150
```

---

# 20. Selector de modo de trabajo

En el panel central debería existir algo equivalente a:

```text
Usage mode:
( ) Until broken
( ) Durability
( ) Blocks broken
```

La presentación visual final puede variar.

---

# 21. Slider de durabilidad

Para `Durability Budget`, mostrar:

```text
1 ───────────●──────────── remainingDurability
          Use: N
```

A la izquierda:

```text
1
```

A la derecha:

```text
durabilidad restante actual
```

El tooltip del slider o indicador flotante debe mostrar el valor exacto seleccionado.

Ejemplo:

```text
Use 180 durability
```

## 21.1 Preserve at 1 + slider

Si se activa:

```text
☑ Preserve at 1 durability
```

puede resultar más claro deshabilitar visualmente el slider cuando corresponda o reflejar claramente que el safety limit tendrá prioridad.

Debe evitarse que la UI parezca tener dos órdenes contradictorias.

---

# 22. Límite por bloques

Para `Blocks Broken`, el límite no está naturalmente acotado por la durabilidad restante.

Por eso se recomienda:

- campo numérico editable;
- opcionalmente slider para rangos pequeños;
- botones de incremento/decremento si aportan comodidad.

Ejemplo:

```text
Break: [ 120 ] blocks
```

El diseño exacto queda abierto, pero debe permitir cantidades razonablemente grandes.

---

# 23. Start AFK Mining

Al pulsar `Start AFK Mining`:

1. validar queue;
2. validar referencias a herramientas;
3. validar configuración de cada entrada;
4. crear snapshot de sesión;
5. registrar posición de anclaje;
6. preparar primera herramienta;
7. resolver hotbar;
8. cerrar o mantener GUI según diseño final;
9. comenzar sesión;
10. mantener Attack.

La queue de la sesión debe tratarse como inmutable.

---

# 24. Posición de anclaje y apagado por movimiento

Al comenzar:

```text
anchorPosition = player position
```

Mientras la sesión está activa, cualquier desplazamiento real debe cancelar el automining.

Debe incluir desplazamiento provocado por:

- WASD;
- salto;
- agua;
- mobs;
- aldeanos;
- wandering traders;
- jugadores;
- pistones;
- knockback;
- cualquier otra fuente.

Motivo:

una granja de cobblestone puede romperse si el jugador es desplazado y comienza a minar un bloque estructural diferente.

## 24.1 Tolerancia

La implementación deberá definir una tolerancia mínima razonable para evitar falsos positivos por diferencias numéricas minúsculas, pero conceptualmente el jugador debe permanecer en la misma posición.

No debe permitirse caminar “un poco”.

---

# 25. Rotación de cámara y modo de minería

Mover la cámara **NO** debe cancelar Pick Relay.

Antes de iniciar una sesión, la GUI debe presentar dos modos mutuamente excluyentes:

```text
[ Single Block ]   [ Line Mining ]
```

## 25.1 Single Block

Pensado para generadores compactos/iniciales donde romper el bloque de atrás podría destruir la granja.

Al pulsar Start:

```text
singleBlockTarget = bloque válido actualmente bajo el crosshair
```

Mientras la sesión permanezca activa:

- Pick Relay sólo puede minar esa coordenada concreta;
- si el bloque desaparece temporalmente, la sesión sigue ACTIVE y espera;
- si el jugador gira la cámara y apunta a otro bloque, **no se cancela**, pero tampoco mina ese otro bloque;
- al volver a apuntar a la coordenada capturada —incluido un nuevo bloque generado en ella— el automining continúa;
- el bloque de respaldo detrás del generador nunca debe convertirse automáticamente en nuevo objetivo.

El modo no permite minar una coordenada fuera del crosshair/alcance normal: fija **qué bloque puede trabajar**, no concede alcance ni aim artificial.

## 25.2 Line Mining

Pensado para granjas avanzadas, líneas de bloques y situaciones donde el jugador necesita barrer varios objetivos desde la misma posición.

Debe replicar el comportamiento validado de Auto Mining de THE Pick:

- cada ciclo vuelve a hacer raycast desde la cámara usando el alcance normal;
- mina el bloque válido actualmente bajo el crosshair;
- mover la cámara redirige inmediatamente el automining al nuevo bloque apuntado;
- aire temporal, entidades o ausencia de bloque válido no cancelan la sesión;
- cuando vuelve a existir/apuntarse un bloque válido, el automining reanuda.

## 25.3 Seguridad común

En ambos modos:

- la cámara puede rotarse libremente sin cancelar;
- el ancla de seguridad continúa siendo exclusivamente la posición/dimensión del jugador;
- cualquier desplazamiento real, voluntario o involuntario, detiene la sesión.

El modo seleccionado queda read-only durante `ACTIVE`.

---

# 26. Click manual como emergencia

Mientras la sesión está activa:

- click izquierdo físico del jugador;
- click derecho físico del jugador;

deben producir apagado de emergencia.

## 26.1 Distinción crítica

Pick Relay mantiene artificialmente Attack.

Ese estado NO debe confundirse con un click izquierdo físico del usuario.

El detector debe distinguir:

```text
synthetic/controlled attack state
```

de:

```text
physical mouse input
```

De lo contrario el mod se cancelaría a sí mismo inmediatamente.

## 26.2 GUI exception

Clicks dentro de `PickRelayScreen` NO deben considerarse emergencia.

Debe distinguirse:

```text
Gameplay mouse input
```

de:

```text
PickRelayScreen input
```

---

# 27. Otras condiciones de apagado

Debe detenerse limpiamente ante:

- muerte del jugador;
- desconexión;
- salida del mundo;
- cambio de dimensión;
- referencia de herramienta inválida;
- inconsistencia grave de inventario;
- fallo que haga inseguro continuar;
- queue agotada;
- botón `Stop AFK Mining`;
- desplazamiento;
- click físico gameplay izquierdo;
- click físico gameplay derecho.

---

# 28. Manual Stop

Durante sesión:

```text
[ Stop AFK Mining ]
```

Debe:

1. liberar Attack;
2. detener controlador;
3. cerrar/finalizar entrada activa;
4. limpiar la sesión;
5. limpiar queue/session state;
6. retirar HUD;
7. volver a IDLE.

Se acordó que la queue se limpia al terminar la sesión manualmente.

---

# 29. Queue agotada

Cuando no quedan entradas:

```text
release Attack
↓
stop session
↓
clear session/queue
↓
remove HUD
↓
IDLE
```

Mensaje discreto opcional:

```text
Pick Relay: Queue exhausted
```

---

# 30. Gestión hotbar/inventario

Esta es una responsabilidad central de Pick Relay.

## 30.1 Si la siguiente herramienta ya está en hotbar

Seleccionar su slot.

## 30.2 Si está en inventario y existe un slot libre en hotbar

Moverla al **primer slot libre de hotbar de izquierda a derecha**.

Después seleccionarla.

## 30.3 Caso inicial: hotbar llena y ninguna herramienta seleccionada está allí

Ejemplo:

```text
Hotbar:
[Food][Blocks][Sword][Torch][...]
```

La primera herramienta de queue está en inventario.

Como no existe slot libre:

```text
swap first queued tool ↔ hotbar slot 1
```

Internamente:

```text
hotbar index 0
```

El ítem que estaba originalmente en slot 1 pasa al slot de inventario ocupado por la herramienta.

La herramienta queda equipada.

## 30.4 Herramientas siguientes

Cuando la herramienta del slot 1 se rompe:

- el slot queda vacío;
- la siguiente herramienta de inventario entra allí.

Mientras exista un hueco, siempre utilizar:

```text
first empty hotbar slot from left to right
```

## 30.5 Herramienta preservada/no rota

Si una herramienta termina por:

- block budget;
- durability budget;
- preserve at 1;

sigue existiendo físicamente.

La estrategia de movimiento debe evitar perderla o sobrescribirla.

### Política de relevo con hotbar llena

El slot actualmente utilizado puede actuar como **relay slot**.

Si:

- la herramienta actual termina sin romperse;
- la siguiente herramienta está en el inventario principal;
- y no existe ningún slot vacío en hotbar;

Pick Relay debe hacer un swap directo entre:

```text
current active hotbar tool ↔ next queued inventory tool
```

Resultado:

- la herramienta terminada/preservada queda guardada en el slot de inventario que ocupaba la siguiente;
- la siguiente herramienta llega exactamente al slot activo de hotbar;
- no hace falta disponer de ningún hueco libre adicional;
- no se pierde ni sobrescribe ningún ItemStack.

Ejemplo:

```text
Hotbar slot 1: Diamond Pickaxe A (terminó en 1 durability)
Inventory slot 14: Diamond Pickaxe B (siguiente en queue)

SWAP

Hotbar slot 1: Diamond Pickaxe B
Inventory slot 14: Diamond Pickaxe A
```

Esto permite ejecutar incluso una queue de 36 herramientas no desechables con el inventario/hotbar completamente ocupados.

Si la siguiente herramienta ya está en hotbar, simplemente se selecciona su slot y no es necesario hacer ese swap todavía.

---

# 31. Identidad y seguimiento de ItemStacks

No basta comparar:

```text
item == DIAMOND_PICKAXE
```

porque pueden existir múltiples herramientas idénticas.

Pick Relay debe poder seguir la herramienta específica seleccionada aunque:

- se mueva de inventario a hotbar;
- se haga swap;
- cambie su durabilidad;
- tenga nombre custom;
- tenga encantamientos idénticos a otra.

Debe evitar confundir dos ItemStacks equivalentes.

Este es uno de los puntos técnicos de mayor riesgo y debe diseñarse antes de implementar la rotación completa.

---

# 32. HUD

Mientras Pick Relay está activo debe aparecer un HUD compacto debajo del crosshair.

Debe mostrarse como un **mensaje de evento estándar** en la posición habitual bajo el crosshair.

Ejemplo base:

```text
⛏ Pick Relay · Tool 7/24
```

## 32.1 Con Block Budget

```text
⛏ Pick Relay · Tool 7/24 · Blocks 83/150
```

## 32.2 Con Durability Budget

```text
⛏ Pick Relay · Tool 7/24 · Durability 81/120
```

Debe quedar claro si el valor representa consumido/objetivo.

## 32.3 Until Broken

```text
⛏ Pick Relay · Tool 7/24 · Until broken
```

## 32.4 Preserve

Puede existir un indicador discreto:

```text
🛡 Min. 1
```

o equivalente.

La HUD no debe convertirse en una hoja de cálculo.

Debe mostrar sólo:

- mod activo;
- herramienta Z/X;
- progreso relevante;
- protección si aporta valor.

---

# 33. Queue durante sesión activa

Cuando `ACTIVE`:

- queue read-only;
- no drag;
- no reorder;
- no remove;
- no cambio de límites;
- no toggle de preserve.

Sí debe permitir:

- hover;
- tooltips;
- inspección;
- selección visual de entradas;
- ver progreso;
- Stop AFK Mining.

Motivo:

evitar modificar la estructura en el mismo tick en que una herramienta entrega el relevo a la siguiente.

---

# 34. Representación de progreso en GUI

Durante una sesión puede conservarse visualmente el historial de herramientas ya procesadas.

Ejemplo:

```text
[✓01][✓02][✓03][▶04][05][06][07]
```

- `✓` = completada;
- `▶` = activa;
- normal = pendiente.

Las herramientas ya rotas pueden representarse mediante snapshots visuales, aunque el ItemStack real ya no exista.

La sesión completa sirve como modelo visual hasta terminar.

Al finalizar:

```text
session snapshots → clear
```

---

# 35. Herramientas desechables

Este es uno de los casos de uso centrales.

Ejemplo:

inventario completo con Stone Pickaxes.

Flujo:

1. abrir Pick Relay;
2. hold left click;
3. arrastrar por las 36 picotas;
4. queue se llena en el orden atravesado;
5. todas configuradas `Until Broken`;
6. Start;
7. cada una trabaja hasta romperse;
8. Pick Relay trae automáticamente la siguiente;
9. al agotarse, Stop.

Debe ser posible configurar este caso sin realizar 36 clicks individuales.

---

# 36. Herramientas buenas / “mamadas”

Otro caso central:

```text
Diamond Pickaxe Efficiency V
Diamond Pickaxe Silk Touch
Netherite Pickaxe Fortune III
```

El usuario debe poder:

- gastar sólo parte de su durabilidad;
- preservar en 1;
- limitar por bloques;
- mezclarlas con herramientas desechables;
- definir orden exacto.

Ejemplo:

```text
#01 Stone Pickaxe
     Until Broken

#02 Diamond Pickaxe
     150 Blocks
     Preserve at 1

#03 Silk Touch Diamond Pickaxe
     120 Blocks
     Preserve at 1

#04 Stone Pickaxe
     Until Broken
```

---

# 37. Granjas objetivo

Pick Relay debe ser genérico para:

## 37.1 Cobblestone / Stone

Rotación:

- pickaxe normal;
- Silk Touch;
- pickaxes desechables.

## 37.2 Madera

Granjas donde:

- pistones desplazan logs;
- el jugador permanece atacando un punto;
- se usa axe.

## 37.3 Moss

Granjas donde puede utilizarse hoe para romper repetidamente moss/relacionados.

## 37.4 Rooted Dirt / Dirt

Uso de shovel en mecanismos repetitivos.

## 37.5 Otros modpacks

No deben hardcodearse bloques específicos de NeoBlock.

---

# 38. Interacción futura con THE Pick

Se planteó mover el **manejo de automining** de THE Pick hacia Pick Relay.

## 38.1 Separación deseada

Eruruu’s Patch:

- conserva THE Pick como ítem;
- conserva niveles/mejoras;
- conserva identidad/progresión;
- no debería duplicar el motor completo de AFK mining.

Pick Relay:

- controlador AFK;
- input;
- HUD;
- seguridad;
- queue;
- inventario/hotbar;
- session state.

## 38.2 Integración recomendada

Preferencia actual:

**integración opcional**, no dependencia obligatoria para todo Eruruu’s Patch.

Si Pick Relay está instalado:

- THE Pick puede delegar su automining a Pick Relay.

Si Pick Relay no está:

- Eruruu’s Patch debería seguir cargando normalmente.

La estrategia exacta de compatibilidad se definirá cuando Pick Relay esté probado.

## 38.3 Posible modo Single Tool

Pick Relay podría exponer internamente:

```text
Relay Mode
```

para queue normal.

Y:

```text
Single Tool Mode
```

para compatibilidad con THE Pick / comportamiento clásico.

No es imprescindible implementar ambos desde el primer commit.

---

# 39. Persistencia de configuración

Decisión todavía abierta.

Hay que definir qué sobrevive al cerrar GUI/juego.

Opciones:

## A. Queue temporal

- queue existe sólo mientras se configura;
- se limpia al terminar;
- al reabrir después hay que seleccionar de nuevo.

Ventaja: simple y segura.

## B. Configuración parcial persistente

Podrían persistirse preferencias generales:

- keybind se maneja por Minecraft;
- última posición HUD;
- último modo predeterminado;
- preserve at 1 por defecto.

No sería recomendable persistir referencias a ItemStacks específicos entre sesiones/mundos sin una razón fuerte.

**Recomendación inicial:** mantener la queue de herramientas como estado temporal.

---

# 40. Configuración por defecto al agregar herramienta

Punto a revisar.

Debe decidirse qué modo recibe automáticamente una nueva herramienta.

Opciones razonables:

- `Until Broken`;
- o recordar última opción utilizada;
- o `Preserve at 1` para herramientas valiosas.

**Recomendación actual para 1.0:**

```text
Default work mode: Until Broken
Preserve at 1: OFF
```

porque representa exactamente el comportamiento esperado de picotas desechables y no introduce acciones ocultas.

El usuario configura explícitamente herramientas caras.

---

# 41. Seguridad ante inventario modificado externamente

Durante sesión pueden ocurrir cambios inesperados:

- otro mod reorganiza inventario;
- herramienta desaparece;
- un servidor corrige inventario;
- ItemStack cambia;
- el usuario obtiene/pierde items;
- un contenedor modifica slots.

Pick Relay debe validar antes de usar/mover una herramienta.

Si no puede identificar de forma segura la siguiente entrada:

> preferir Stop antes que utilizar una herramienta equivocada.

---

# 42. GUI e input: prioridades

Debe existir una jerarquía clara para evitar conflictos:

```text
PickRelayScreen interactions
    > gameplay emergency click detection
```

Es decir:

- click en GUI = UI;
- click en mundo durante ACTIVE = emergency stop.

Drag de queue nunca debe filtrarse accidentalmente hacia Attack.

---

# 43. Stop y cleanup

Todos los caminos de salida deben terminar en una rutina central equivalente:

```text
stopSession(reason)
```

Responsabilidades:

- release Attack;
- cancelar estado activo;
- limpiar drag/input state;
- terminar entry actual;
- limpiar queue/session snapshots según política;
- quitar HUD;
- restaurar cualquier estado temporal propio;
- registrar/mostrar razón cuando corresponda.

Evitar múltiples implementaciones independientes de cleanup.

---

# 44. Posibles razones de stop

Enum conceptual:

```text
MANUAL
QUEUE_COMPLETE
PLAYER_MOVED
PHYSICAL_LEFT_CLICK
PHYSICAL_RIGHT_CLICK
PLAYER_DEATH
DISCONNECT
DIMENSION_CHANGE
TOOL_INVALID
INVENTORY_DESYNC
NO_VALID_NEXT_TOOL
INTERNAL_SAFETY
```

Esto ayuda tanto al código como a mensajes/debug.

---

# 45. Mensajes al jugador

Deben ser discretos.

Ejemplos:

```text
Pick Relay started
Pick Relay stopped
Pick Relay: Queue exhausted
Pick Relay stopped: Player moved
Pick Relay stopped: Tool unavailable
```

Evitar spam por cada cambio de herramienta.

El HUD ya muestra progreso normal.

---

# 46. Sonido

No se definió ningún sonido específico.

Recomendación:

- no agregar sonidos propios inicialmente;
- permitir que Minecraft mantenga sonidos normales de herramientas/bloques;
- quizás feedback vanilla-like al Start/Stop sólo si realmente mejora UX.

---

# 47. Compatibilidad con encantamientos

Pick Relay NO debe reinterpretar encantamientos.

Debe permitir que Minecraft maneje normalmente:

- Efficiency;
- Unbreaking;
- Silk Touch;
- Fortune;
- Mending;
- encantamientos modded.

Pick Relay sólo observa consecuencias relevantes:

- durabilidad consumida;
- bloque destruido.

---

# 48. Mending

Caso especial futuro.

Si una herramienta recupera durabilidad por Mending durante la sesión:

## Durability Budget

Debe medirse **durabilidad consumida por usos reales**, no simplemente:

```text
initialRemaining - currentRemaining
```

si Mending puede restaurarla y ocultar desgaste previo.

Este es un punto técnico importante.

Debe determinarse una estrategia robusta para contar “durability actually consumed” si el ItemStack puede repararse durante la misma sesión.

Alternativas:

- observar incrementos de damage causados por uso;
- llevar delta por eventos/ticks antes de que reparaciones posteriores lo reviertan.

**No dejar este detalle implícito durante desarrollo.**

---

# 49. Bloques destruidos por otras causas

Otro caso técnico importante.

Si mientras Pick Relay trabaja:

- un pistón mueve el bloque;
- una explosión lo destruye;
- otro jugador rompe el bloque;
- el servidor cambia el estado;

no debería contarse automáticamente como trabajo completado por nuestra herramienta.

Idealmente el contador debe asociar la destrucción confirmada con el proceso de minería del jugador.

Esto debe investigarse cuidadosamente en NeoForge 1.21.1.

---

# 50. Cambio de herramienta entre bloques

Cuando una entrada cumple su límite:

1. release Attack;
2. finalizar contador;
3. preservar/manejar herramienta actual;
4. equipar/mover siguiente;
5. esperar el estado mínimo necesario para que el cambio sea reconocido correctamente;
6. reactivar Attack.

No debe intentar cambiar de herramienta en medio de un estado incoherente de minería.

---

# 51. Capacidad 36 y herramientas duplicadas

Una herramienta física sólo puede aparecer una vez en queue.

Si el mismo slot/stack ya está seleccionado:

- click = remove;
- drag ADD debe ignorarlo;
- no debe crear duplicado.

Esto evita ejecutar dos veces la misma herramienta física por accidente.

---

# 52. Queue llena

Cuando hay 36 entradas:

- nuevas selecciones deben ignorarse;
- mostrar feedback sutil de queue completa;
- no eliminar nada automáticamente;
- drag ADD puede seguir recorriendo slots pero no agregar más.

---

# 53. Herramienta deja de ser válida antes de Start

Al pulsar Start debe ejecutarse validación completa.

Si una entrada ya no corresponde a la herramienta seleccionada:

- marcar problema;
- no comenzar silenciosamente con otra herramienta.

Opciones UX:

- cancelar Start y resaltar entradas inválidas;
- ofrecer limpieza de inválidas.

Recomendación: cancelar Start y mostrar cuáles necesitan corrección.

---

# 54. Herramienta deja de ser válida durante ACTIVE

Preferencia:

```text
Stop safely
```

antes que improvisar.

No sustituir automáticamente por “otra similar” que el usuario nunca seleccionó.

---

# 55. Reordering y configuración

La configuración pertenece a la **entrada**, no simplemente al número de slot.

Si:

```text
Tool A: 150 blocks
Tool B: Until broken
```

y se hace swap:

```text
A ↔ B
```

cada herramienta conserva SU configuración.

Sólo cambia el orden.

---

# 56. Remove + re-add

Punto a definir.

Si una herramienta configurada se elimina de queue y luego se vuelve a agregar:

**recomendación 1.0:** crear una entrada nueva con defaults.

No intentar conservar configuraciones de una entrada eliminada indefinidamente.

Esto simplifica comportamiento y evita historial oculto.

---

# 57. Visualización de números en queue

Cada icono debe mostrar su posición:

```text
01
02
03
...
36
```

El número puede dibujarse en una esquina con tamaño reducido.

Debe evitar tapar:

- durability bar;
- stack/icon details;
- markers de estado.

Se recomienda probar distintas esquinas antes de fijar diseño.

---

# 58. Tooltips avanzados

Los datos propios podrían incluir:

```text
Queue position: 12 / 28
Inventory slot: 22
Durability: 347 / 1561
Remaining: 22.2%
Mode: Blocks
Target: 120
Preserve at 1: Yes
```

Durante ACTIVE:

```text
Status: Currently working
Progress: 83 / 120 blocks
```

---

# 59. Apariencia de entrada activa

Debe ser distinguible inmediatamente.

Opciones:

- borde destacado;
- `▶`;
- overlay;
- pequeña animación.

Evitar efectos demasiado llamativos.

---

# 60. Entradas consumidas

Durante ACTIVE, las herramientas ya rotas/completadas pueden aparecer:

- atenuadas;
- con check;
- usando snapshot del icono.

Ejemplo:

```text
[✓][✓][▶][ ][ ]
```

Esto permite entender visualmente cuánto falta.

---

# 61. Apertura de GUI mientras se mina

Requisito:

la GUI no debe pausar singleplayer.

Mientras está abierta:

- automining puede continuar;
- HUD puede mantenerse o GUI reemplazar parte de la información;
- tool progress debe actualizarse en tiempo real.

Ejemplo: durability del tool activo puede disminuir en el tooltip/panel mientras se observa.

---

# 62. Cambio automático de herramienta desde inventario

Al rotar:

```text
current entry completed
↓
resolve next entry
↓
locate concrete tool
↓
if hotbar:
    select
else:
    find first empty hotbar slot
    move tool there
    select
↓
resume
```

Si no hay hueco:

- debe aplicarse la política segura de swap;
- especialmente el caso inicial definido con slot 1.

Para situaciones más complejas después de preservar una herramienta, la estrategia debe especificarse cuidadosamente antes del commit final del inventory manager.

---

# 63. Hotbar slot 1 como fallback

Regla explícitamente acordada:

Si:

- la hotbar está completamente llena;
- ninguna herramienta seleccionada está en hotbar;
- la primera herramienta está en inventario;

hacer:

```text
swap(queueFirst, hotbarSlot1)
```

y comenzar desde ahí.

Después, si la herramienta se rompe:

- slot 1 queda disponible;
- siguientes herramientas pueden reutilizarlo.

---

# 64. Interacción con herramientas ya presentes en hotbar

Si la queue incluye varias herramientas ya presentes allí, debe respetarse el orden de queue, NO el orden físico.

Ejemplo:

```text
Hotbar:
slot2 = Tool #3
slot4 = Tool #1
slot6 = Tool #2
```

Orden:

```text
#1 → #2 → #3
```

La queue manda.

---

# 65. Modos y orden mezclados

Debe permitirse libremente:

```text
#1 Until Broken
#2 120 Blocks
#3 80 Durability
#4 Until Broken + Preserve at 1
#5 400 Blocks + Preserve at 1
```

No existe una configuración global obligatoria para toda la queue.

---

# 66. “Preserve at 1” con Until Broken

Semánticamente:

```text
Until Broken + Preserve at 1
```

equivale a:

```text
Use as much as possible, but stop at 1
```

La UI debería mostrarlo claramente, aunque el nombre `Until Broken` pueda requerir un ajuste textual cuando Preserve está activo.

Posible label dinámico:

```text
Use until safety limit
```

Este punto es UX, no funcional.

---

# 67. Configuración de herramientas durante IDLE

Se puede:

- agregar;
- eliminar;
- reorder;
- inspect;
- cambiar work mode;
- cambiar target;
- cambiar preserve.

Durante ACTIVE:

- sólo inspect.

---

# 68. Drag del inventario: orden exacto

La queue debe reflejar el orden de **primera entrada del cursor a cada slot**.

No el orden físico del inventario.

Esto permite dibujar cualquier recorrido deseado.

Ejemplo:

```text
P5 → P1 → P9 → P2
```

queue:

```text
1 P5
2 P1
3 P9
4 P2
```

---

# 69. Drag remove: mantener operación

Si comienza removiendo:

- herramientas no seleccionadas se ignoran;
- no se agregan accidentalmente.

Si comienza agregando:

- herramientas ya seleccionadas se ignoran;
- no se eliminan accidentalmente.

Principio:

> Un gesto = una operación.

---

# 70. Reorder fuera de queue

Soltar ghost item:

- sobre slot queue → swap/insert;
- entre slots → insert;
- fuera de zona queue → remove.

NO debe interactuar con inventario real aunque el cursor termine sobre un slot del inventario inferior.

---

# 71. Click derecho en inventario

No se definió comportamiento especial.

Recomendación:

dejar comportamiento neutro/no usarlo para selección, evitando conflictos con acciones vanilla.

La selección principal es left click/drag.

---

# 72. Shift/Ctrl y shortcuts futuros

Se discutió la posibilidad de shortcuts adicionales, pero no se consideran necesarios para 1.0.

Ejemplo futuro:

- remove tail;
- clear queue;
- select all compatible.

No agregarlos antes de validar UX básica.

---

# 73. Clear Queue

Se incorpora como QoL de configuración:

```text
Clear Queue
```

Disponible únicamente mientras la queue sea editable (`IDLE`/configuración).

- limpia sólo la representación/configuración de la queue;
- no elimina, tira ni mueve ItemStacks reales;
- queda deshabilitado durante `ACTIVE`;
- en layouts compactos puede ocultarse si no existe espacio seguro, manteniéndose disponibles los mecanismos normales de remove.

**Implementado desde `0.1.0-alpha.7`.**

---

# 74. Start con queue vacía

Botón Start:

- disabled;
- o al pulsar mostrar aviso.

No iniciar controlador vacío.

---

# 75. Herramientas no dañables

Pick Relay está pensado para herramientas con comportamiento de minería normal.

Si aparece una herramienta modded sin durability:

- `Blocks` podría ser válido;
- `Durability` y `Preserve at 1` no tendrían sentido.

La UI debería habilitar/deshabilitar opciones según capacidades reales.

Este soporte puede llegar después si complica 1.0.

---

# 76. Compatibilidad con herramientas especiales

No asumir que toda herramienta usa durabilidad vanilla simple.

Antes de soportar herramientas modded complejas:

- detectar capacidades;
- no aplicar lógica insegura.

Fail-safe > compatibilidad falsa.

---

# 77. Logging de desarrollo

Durante development será útil disponer de debug opcional para:

- session start/stop;
- entry transitions;
- inventory moves;
- block count increments;
- durability deltas;
- emergency reasons;
- tool resolution failures.

No debe spamear consola en builds finales si debug está desactivado.

---

# 78. Arquitectura sugerida

No es código obligatorio, pero una separación razonable sería:

```text
PickRelay
├─ client/
│  ├─ PickRelayController
│  ├─ RelaySession
│  ├─ RelayEntry
│  ├─ ToolLocator / ToolTracker
│  ├─ InventoryRelayManager
│  ├─ MiningProgressTracker
│  ├─ SafetyMonitor
│  ├─ input/
│  │  └─ RelayInputHandler
│  ├─ gui/
│  │  ├─ PickRelayScreen
│  │  ├─ RelayQueueWidget
│  │  ├─ ToolDetailsPanel
│  │  └─ widgets...
│  └─ hud/
│     └─ PickRelayHud
└─ compat/
   └─ eruruupatch / thepick (future)
```

La responsabilidad clave es evitar una clase gigante que mezcle:

- GUI;
- inventario;
- input;
- mining;
- safety;
- HUD.

---

# 79. PickRelayController

Responsabilidades sugeridas:

- estado global;
- start/stop;
- entry actual;
- transición entre herramientas;
- coordinación de managers;
- única fuente de verdad para ACTIVE.

No debería dibujar GUI directamente.

---

# 80. MiningProgressTracker

Responsabilidades:

- contar bloques destruidos;
- observar durabilidad consumida;
- determinar cuándo se cumple Work Limit.

Debe poder responder:

```text
isCurrentEntryComplete()
```

sin decidir cómo cambiar de herramienta.

---

# 81. SafetyMonitor

Responsabilidades:

- anchor position;
- desplazamiento;
- physical clicks;
- death;
- dimension;
- disconnect;
- condiciones inseguras.

Al detectar una:

```text
controller.stop(reason)
```

---

# 82. InventoryRelayManager

Responsabilidades:

- localizar herramienta concreta;
- seleccionar hotbar;
- encontrar hueco;
- mover inventory → hotbar;
- swap inicial;
- preservar herramientas;
- evitar sobrescritura/pérdida.

Es probablemente uno de los componentes que más pruebas requiere.

---

# 83. PickRelayScreen

Responsabilidades:

- mostrar estado;
- editar queue sólo si IDLE;
- dispatch de interacciones;
- no ejecutar lógica de minería;
- bloquear inputs gameplay cuando corresponda.

---

# 84. RelayQueueWidget

Responsabilidades:

- render 4×9;
- numbering;
- hover;
- drag ghost;
- swap;
- insert;
- remove;
- active/completed markers.

---

# 85. Criterios de aceptación mínimos para 1.0

Una primera versión se considera funcional cuando:

1. abre GUI mediante bind configurable;
2. permite agregar herramientas específicas;
3. permite seleccionar por drag;
4. permite quitar por drag;
5. permite ordenar queue;
6. permite right-click remove;
7. soporta 36 entradas;
8. permite modo Until Broken;
9. permite Block Budget;
10. permite Durability Budget;
11. permite Preserve at 1;
12. inicia automining;
13. mantiene Attack;
14. rota correctamente entre herramientas;
15. trae herramientas desde inventario;
16. maneja hotbar llena inicial;
17. respeta orden queue;
18. HUD muestra Z/X y progreso;
19. bloque destruido cuenta una vez;
20. drops no afectan contador;
21. mover al jugador detiene;
22. click físico gameplay detiene;
23. abrir GUI con bind durante ACTIVE no detiene;
24. Stop manual funciona;
25. queue active es read-only;
26. queue agotada detiene;
27. disconnect/death/dimension limpian estado;
28. jamás utiliza una herramienta no seleccionada;
29. jamás rompe una herramienta marcada Preserve at 1;
30. no duplica/pierde ItemStacks durante swaps.

---

# 86. Matriz de pruebas recomendada

## Caso A — 36 Stone Pickaxes

- hotbar + inventory llenos;
- drag-select todas;
- Until Broken;
- validar orden;
- validar sustitución;
- validar queue exhausted.

## Caso B — Hotbar llena, tools sólo en inventario

- ninguna herramienta seleccionada en hotbar;
- Start;
- validar swap con slot 1;
- validar sucesivas rotaciones.

## Caso C — Queue desordenada físicamente

- herramientas en slots 2, 7, 18, 34;
- queue personalizada 34 → 2 → 18 → 7;
- validar orden lógico.

## Caso D — Preserve at 1

- herramienta con poca durabilidad;
- validar que nunca se rompa.

## Caso E — Unbreaking

- Durability Budget 50;
- confirmar que cuenta desgaste real, no bloques.

## Caso F — Silk Touch + normal

- 150 blocks normal;
- 120 blocks Silk Touch;
- validar cambio exacto tras contador.

## Caso G — Hopper

- drops nunca entran al jugador;
- Block Budget debe funcionar igual.

## Caso H — Movement Safety

- empujón de villager/mob;
- confirmar stop.

## Caso I — Manual click

- left/right gameplay;
- stop inmediato.

## Caso J — GUI durante ACTIVE

- abrir con bind;
- mining sigue;
- inspección funciona;
- edición bloqueada;
- Stop manual funciona.

## Caso K — Queue drag reorder

- swap;
- insert before;
- insert after;
- drop outside;
- renumeración.

## Caso L — Tool desaparece

- provocar inconsistencia;
- confirmar stop fail-safe.

---

# 87. Roadmap sugerido de implementación

## Fase 1 — Skeleton

- mod metadata;
- keybind;
- controller IDLE/ACTIVE;
- HUD mínimo;
- toggle Attack básico sólo para validar pipeline.

## Fase 2 — GUI + Queue

- 4×9;
- inventory display;
- add/remove;
- drag add/remove;
- reorder;
- tool detail selection.

## Fase 3 — Single-type relay

- picotas;
- Until Broken;
- hotbar/inventory movement;
- 36 tools.

## Fase 4 — Safety

- anchor;
- physical inputs;
- death/disconnect/dimension;
- centralized stop.

## Fase 5 — Work Limits

- durability budget;
- Preserve at 1;
- block counter;
- progress state.

## Fase 6 — General Tools

- axes;
- shovels;
- hoes;
- modded tool compatibility where safe.

## Fase 7 — UX polish

- advanced tooltips;
- drag feedback;
- ghost icon;
- insert/swap zones;
- completed snapshots;
- messages.

## Fase 8 — THE Pick compatibility

- optional integration;
- remove/retire duplicated AFK controller only after validation.

---

# 88. Decisiones que conviene revisar antes de empezar a programar

Estas son las pocas áreas que aún merecen confirmación explícita.

## 88.1 Queue se limpia al Stop

Actualmente definido:

```text
Stop manual / emergency / complete
→ clear queue/session
```

Revisar si se desea que un emergency stop también destruya la queue preparada o si sería útil conservarla para relanzar.

La decisión conversada favorece limpiar completamente por seguridad.

## 88.2 Default mode al agregar

Propuesta:

```text
Until Broken
Preserve at 1 = false
```

Confirmar.

## 88.3 Block Budget UI

Definir si:

- sólo campo numérico;
- slider + campo;
- botones ±.

Funcionalmente ya está claro; falta UX final.

## 88.4 Herramienta preservada y siguiente tool con hotbar llena — RESUELTO EN DISEÑO

Se utilizará el **slot activo como relay slot** cuando no exista ningún hueco libre:

```text
current preserved tool ↔ next queued inventory tool
```

La herramienta terminada ocupa el antiguo slot de inventario de la siguiente y la siguiente entra directamente a la hotbar.

La implementación todavía debe validarse contra las operaciones de inventario reales de NeoForge/cliente-servidor, pero la semántica funcional queda definida.

## 88.5 Mending

Decidir si Durability Budget 1.0 soportará herramientas reparándose activamente.

Si no se puede medir con garantías, podría documentarse inicialmente:

> Durability Budget no garantiza conteo exacto si la herramienta recibe reparaciones durante la sesión.

Idealmente resolverlo bien.

## 88.6 Tool eligibility

Definir criterio NeoForge exacto para identificar:

- pickaxe;
- axe;
- shovel;
- hoe;
- modded tools.

No hardcodear sólo `Items.DIAMOND_PICKAXE`, etc.

## 88.7 Posición de HUD

Debe utilizar la posición estándar de los mensajes de evento bajo el crosshair.

---

# 89. Comentarios previos al desarrollo

## 89.1 Lo más valioso de Pick Relay no es el auto-click

La característica que debe protegerse durante todo el desarrollo es:

> **queue explícita + límites por entrada + seguridad.**

Si una decisión técnica empuja al mod a seleccionar herramientas automáticamente “porque es más fácil”, se estaría perdiendo la identidad principal.

## 89.2 La seguridad es parte del producto, no un extra

El stop por movimiento no es simplemente QoL.

Es una característica esencial para usar Pick Relay sin miedo en granjas donde:

- agua/lava;
- bloques estructurales;
- pistones;
- mobs;

pueden convertir una mínima desviación en un desastre.

## 89.3 La GUI debe sentirse como Minecraft

Las interacciones propuestas reutilizan hábitos existentes:

- click para seleccionar;
- drag para pintar selección;
- hover para tooltip;
- arrastrar stacks visualmente;
- click derecho para quitar;
- slots 9-wide.

Mientras más natural sea, menos documentación necesita el usuario.

## 89.4 No sobreautomatizar

Pick Relay NO debería convertirse rápidamente en:

- “encuentra materiales”;
- “mina automáticamente un área”;
- “busca bloques”;
- “camina”;
- “elige la herramienta más eficiente”.

Eso lo acercaría a bots/autominers existentes y diluiría su identidad.

## 89.5 El inventory manager será la parte más peligrosa

La GUI y HUD son relativamente directos.

Los puntos que merecen pruebas obsesivas son:

- tracking del ItemStack concreto;
- swaps;
- preservación;
- herramientas idénticas;
- hotbar llena;
- inventory corrections;
- tool disappearing;
- no perder/duplicar items.

Debe desarrollarse de manera conservadora.

---

# 90. Definición final del producto

**Pick Relay** es un mod client-side para Minecraft que permite al jugador construir una queue ordenada de herramientas y programar cómo debe utilizarse cada una durante una sesión AFK.

Cada herramienta puede:

- trabajar hasta romperse;
- consumir una cantidad específica de durabilidad;
- romper una cantidad específica de bloques;
- preservarse automáticamente en 1 de durabilidad.

Pick Relay:

- rota las herramientas en el orden indicado;
- mueve herramientas desde inventario a hotbar cuando sea necesario;
- muestra progreso;
- detiene la sesión cuando termina el plan;
- y cancela de forma segura si el jugador es desplazado o interviene manualmente.

Soporta el concepto tanto para:

- picotas desechables;
- herramientas encantadas;
- Silk Touch/Fortune;
- hachas;
- palas;
- azadas;
- y futuras herramientas compatibles.

> **Pick Relay**  
> **Automate the grind. Schedule your tools.**

---

# 91. Checklist de aprobación antes del primer commit

Revisar este documento y confirmar/corregir:

- [ ] Nombre `Pick Relay`.
- [ ] Summary definitivo.
- [ ] NeoForge 1.21.1 client-side como primer target.
- [ ] Queue máxima 36.
- [ ] Picotas + hachas + palas + azadas.
- [ ] Click inventory = toggle.
- [ ] Drag inventory = ADD/REMOVE fijado por primer slot.
- [ ] Click queue = detalles.
- [ ] Right-click queue = remove.
- [ ] Hold+drag queue = reorder.
- [ ] Centro del slot = swap.
- [ ] Borde/entre slots = insert.
- [ ] Drop fuera = remove from queue.
- [ ] Queue read-only durante ACTIVE.
- [ ] Bind abre GUI tanto IDLE como ACTIVE.
- [ ] Botón cambia Start ↔ Stop.
- [ ] GUI no pausa.
- [ ] Until Broken.
- [ ] Durability Budget.
- [ ] Blocks Broken Budget.
- [ ] Preserve at 1.
- [ ] Bloques cuentan al destruirse, no al recoger drops.
- [ ] First empty hotbar slot left→right.
- [ ] Hotbar llena inicial = swap con slot 1.
- [ ] Orden de queue manda sobre orden físico.
- [ ] Stop por movimiento.
- [ ] Stop por left/right click físico gameplay.
- [ ] Cámara NO detiene.
- [ ] GUI permite elegir Single Block o Line Mining antes de Start.
- [ ] Single Block fija la coordenada inicial y pausa al apuntar fuera de ella.
- [ ] Line Mining sigue el bloque actual bajo el crosshair como THE Pick.
- [ ] Death/disconnect/dimension detienen.
- [ ] Queue exhausted detiene.
- [ ] Mensaje de evento del modo activo en la posición estándar bajo el crosshair.
- [ ] HUD Z/X + progreso.
- [ ] Tooltips vanilla + datos avanzados.
- [ ] THE Pick integration opcional/futura.
- [ ] Queue temporal y limpiada al terminar.
- [ ] Fail-safe ante inconsistencias de inventario.
- [ ] Nunca utilizar herramientas no seleccionadas.
- [ ] Nunca romper herramienta con Preserve at 1.

---

# 92. Nota para el próximo chat

Al iniciar el desarrollo en un chat nuevo, entregar este archivo como fuente funcional principal.

Antes de escribir código:

1. revisar estas decisiones;
2. aplicar cualquier corrección del usuario;
3. crear estructura base del mod;
4. fijar versión objetivo exacta de NeoForge/Minecraft;
5. implementar por fases;
6. generar snapshots recuperables tras cada pasada importante según el flujo habitual del proyecto.

Este documento representa el diseño acordado antes del inicio del desarrollo y debe preferirse frente a reconstrucciones incompletas de la conversación original.
