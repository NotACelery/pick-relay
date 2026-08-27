# Pick Relay 1.0.0 — Final regression checklist

Esta lista no redefine el producto: sirve para comprobar rápidamente el **JAR final exacto** antes de subirlo.

## Build / loading

- [ ] Build final con Java 21.
- [ ] JAR generado como `pickrelay-1.21.1-1.0.0.jar`.
- [ ] Carga en Minecraft 1.21.1 con NeoForge 21.1.235.
- [ ] Aparece correctamente en la lista de mods.
- [ ] No requiere instalación server-side.

## GUI

- [ ] Keybind abre Pick Relay.
- [ ] Queue 4×9 visible sin overlaps.
- [ ] Selected Tool, inventory, session mode y acciones quedan legibles.
- [ ] Close limpia la queue si no existe sesión activa.
- [ ] Close durante ACTIVE mantiene la sesión.
- [ ] Clear limpia sólo la planificación.

## Queue / tracking

- [ ] Click add/remove.
- [ ] Drag ADD/REMOVE.
- [ ] Swap/insert/drop outside.
- [ ] Orden lógico respetado.
- [ ] Mover una tool de slot mantiene su entrada.
- [ ] Sacar/tirar una tool pendiente produce SKIPPED y continúa.
- [ ] Queue completa de 36 tools ejecuta correctamente.

## Work modes

- [ ] Until Broken.
- [ ] Durability Budget.
- [ ] Blocks Broken.
- [ ] Configuración distinta por tool permanece independiente.
- [ ] Preserve at 1 funciona de forma consecutiva sobre varias tools.
- [ ] Tool ya en 1 + Preserve se salta/preserva.

## Mining

- [ ] Single Block no rompe el bloque de respaldo.
- [ ] Line Mining sigue la cámara.
- [ ] Aire temporal no cancela.
- [ ] Hopper no afecta Blocks Broken.
- [ ] Unbreaking sólo consume budget cuando hay daño real.
- [ ] Mending no resta consumo ya registrado.

## Relay

- [ ] Tool ya en hotbar se selecciona.
- [ ] Tool de inventario usa primer hueco libre.
- [ ] Hotbar llena usa relay slot.
- [ ] Herramienta preservada/completada no se pierde ni duplica.
- [ ] Romper una tool avanza a la siguiente.

## Safety / lifecycle

- [ ] WASD detiene por cambio de posición.
- [ ] Salto/caída detiene por cambio de posición.
- [ ] Empujón/knockback detiene.
- [ ] Muerte detiene/limpia.
- [ ] Disconnect detiene/limpia.
- [ ] Cambio de dimensión detiene/limpia.
- [ ] Chat/inventario/ESC no cancelan por sí solos.
- [ ] Stop desde GUI detiene y limpia.
- [ ] Queue agotada detiene y limpia.

## Herramientas

- [ ] Pickaxe.
- [ ] Axe.
- [ ] Shovel.
- [ ] Hoe.
- [ ] Al menos una herramienta modded correctamente tageada, si está disponible en la instancia de prueba.
