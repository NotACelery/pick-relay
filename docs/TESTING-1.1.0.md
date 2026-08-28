# Pick Relay 1.1.0 — Final regression checklist

Esta lista valida el **JAR final exacto** de 1.1.0. Primero cubre las funciones nuevas y después una pasada corta sobre el relay ya certificado en 1.0.1.

## Build / loading

- [ ] Build final con Java 21.
- [ ] JAR generado como `pickrelay-1.21.1-1.1.0.jar`.
- [ ] Carga en Minecraft 1.21.1 con NeoForge 21.1.235+.
- [ ] No requiere instalación server-side.

## Toggle del GUI

- [ ] El bind de Pick Relay abre la GUI desde gameplay.
- [ ] El mismo bind vuelve a cerrarla con su binding por defecto (Mouse Button 5).
- [ ] Rebindear Pick Relay a una tecla de teclado mantiene el comportamiento toggle.
- [ ] El bind no produce un cierre + reapertura inmediata por el mismo input.
- [ ] La tecla vanilla de inventario (`E` por defecto) cierra Pick Relay.
- [ ] Rebindear la tecla de inventario sigue cerrando Pick Relay con el nuevo binding.
- [ ] Cerrar con bind, Inventory, Close o ESC antes de Start limpia la queue.
- [ ] Cerrar con bind, Inventory, Close o ESC durante ACTIVE mantiene la sesión corriendo.

## Session panel

- [ ] En layout normal aparece a la derecha de Relay Queue.
- [ ] En layout responsivo ancho aparece entre Relay Queue y Player Inventory.
- [ ] En una ventana responsiva demasiado angosta se conserva el fallback Queue + Inventory sin overlaps.
- [ ] El timer parte en `00:00` al iniciar y avanza mientras la sesión está ACTIVE.
- [ ] Cerrar/reabrir la GUI durante ACTIVE conserva el timer.
- [ ] Blocks aumenta exactamente una vez por cada bloque destruido por Pick Relay.
- [ ] Bloques rotos manualmente fuera de Pick Relay no aumentan el contador de sesión.
- [ ] Tool muestra correctamente la posición activa `X/Y` al rotar por varias herramientas.
- [ ] Durante ACTIVE, apuntar a piedra muestra un BPS coherente con la herramienta que se está usando.
- [ ] Cambiar de bloque objetivo actualiza el BPS del Session panel.
- [ ] Al quedar sin bloque válido bajo el crosshair, el Session panel deja de presentar un BPS antiguo como si siguiera vigente.
- [ ] Haste I/II aparece con nivel y duración restante.
- [ ] Mining Fatigue aparece y se distingue visualmente.
- [ ] Conduit Power aparece como efecto relacionado con minería.
- [ ] Otros efectos activos también aparecen cuando hay espacio.
- [ ] Más efectos de los que caben producen el resumen `+N más` sin salir del panel.
- [ ] Un efecto de duración infinita no rompe el formato del panel.
- [ ] Al terminar/detener la sesión, los contadores finales permanecen legibles mientras la GUI actual siga abierta.

## Estimador BPS de Selected Tool

- [ ] Seleccionar distintas herramientas de la queue cambia el BPS sin necesidad de iniciar la sesión.
- [ ] Cambiar el bloque bajo el crosshair actualiza el nombre del bloque, BPS y segundos por bloque.
- [ ] Stone Pickaxe sobre Stone entrega aproximadamente `1.67 BPS / 0.60 s` sin buffs ni encantamientos.
- [ ] Efficiency aumenta el BPS esperado de la herramienta seleccionada.
- [ ] Haste I/II aumenta el BPS y Mining Fatigue lo reduce.
- [ ] Un bonus externo de `BLOCK_BREAK_SPEED` modifica el BPS de preview.
- [ ] Estar bajo el agua o en el aire aplica los penalizadores correspondientes.
- [ ] Una herramienta con tier incorrecto respeta el divisor de minado sin herramienta correcta.
- [ ] Un bloque irrompible no produce infinito/NaN en pantalla.
- [ ] Sin bloque dentro del alcance aparece el mensaje para apuntar a un bloque.
- [ ] El cálculo no mueve la herramienta real ni altera inventario/hotbar durante la preview.

## Bonus de velocidad de minado

- [ ] Un modificador positivo de `BLOCK_BREAK_SPEED` aparece como bonus.
- [ ] Si está disponible Artifacts, Digging Claws aparece sin requerir integración/dependencia hardcodeada.
- [ ] Modificadores positivos de `MINING_EFFICIENCY`/`SUBMERGED_MINING_SPEED` no rompen el panel.
- [ ] Modificadores negativos no se presentan como bonus positivo.

## Queue UX

- [ ] La entrada inspeccionada usa borde interior dorado.
- [ ] La herramienta ACTIVE conserva su borde blanco y puede verse simultáneamente con la selección dorada.
- [ ] Una entrada inválida conserva su indicación roja.
- [ ] El destino de SWAP usa cian y no se confunde con ACTIVE/selección.
- [ ] La línea INSERT BEFORE/AFTER usa cian y coincide con el destino real al soltar.
- [ ] Add/remove, drag ADD/REMOVE, swap, insert y drop-outside siguen funcionando.

## Regression corta del relay

- [ ] Until Broken.
- [ ] Durability Budget con Unbreaking.
- [ ] Blocks Broken con hopper.
- [ ] Preserve at 1.
- [ ] Single Block no rompe el backing block.
- [ ] Line Mining sigue la cámara.
- [ ] Hotbar llena rota herramientas sin perder/duplicar stacks.
- [ ] Herramienta pendiente movida de slot se relocaliza.
- [ ] Herramienta ausente se salta y continúa.
- [ ] Movimiento del jugador, muerte, disconnect y cambio de dimensión detienen de forma segura.
- [ ] Queue agotada termina y limpia la sesión.
