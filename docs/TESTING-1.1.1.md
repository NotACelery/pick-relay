# Pick Relay 1.1.1 — Hotfix regression checklist

Esta pasada valida el hotfix que convierte la GUI de Pick Relay en un estado de entrada exclusivo: detrás de la pantalla sólo debe continuar el automining controlado por una sesión ACTIVE.

## Build / loading

- [ ] Build final con Java 21.
- [ ] JAR generado como `pickrelay-1.21.1-1.1.1.jar`.
- [ ] Carga en Minecraft 1.21.1 con NeoForge 21.1.235+.
- [ ] No aparecen errores nuevos al abrir/cerrar repetidamente la GUI.

## Uso de ítems

- [ ] Empezar a comer y abrir Pick Relay antes de completar la comida detiene el uso inmediatamente.
- [ ] Empezar a beber y abrir Pick Relay antes de completar el uso detiene la acción.
- [ ] Mantener un escudo/ítem de uso continuo y abrir Pick Relay termina el uso.
- [ ] Mantener presionado Use mientras la GUI está abierta no reinicia el uso en segundo plano.
- [ ] Cerrar la GUI devuelve el control normal de Use.

## Ataque y movimiento manual

- [ ] Minar manualmente un bloque sin sesión ACTIVE y abrir Pick Relay aborta ese progreso manual.
- [ ] Mantener Attack mientras la GUI está abierta no inicia minado manual ni ataques de fondo.
- [ ] Mantener W/A/S/D, Jump, Sneak o Sprint al abrir la GUI no deja esos key states pegados en segundo plano.
- [ ] Cerrar la GUI devuelve los controles normales.

## Automining permitido

- [ ] Iniciar una sesión, abrir Pick Relay y dejar la GUI visible mantiene el automining.
- [ ] El contador Blocks continúa aumentando con la GUI abierta.
- [ ] El timer de sesión continúa avanzando con la GUI abierta.
- [ ] Single Block conserva su coordenada y Line Mining conserva su comportamiento validado.
- [ ] Abrir la GUI durante ACTIVE no resetea el progreso de rotura controlado por Pick Relay.
- [ ] Si un uso de ítem estaba activo al reabrir la GUI durante ACTIVE, se libera y el relay puede retomar el minado.

## Layout responsivo

- [ ] Ventana normal: Queue visible, Session panel a la derecha y Player Inventory debajo sin solapamientos.
- [ ] Altura compacta con ancho suficiente: Queue y Player Inventory quedan lado a lado con Session panel entre ambos.
- [ ] En layout compacto, Selected Tool y sus controles permanecen visibles debajo del bloque superior.
- [ ] Ancho ultraestrecho: se oculta Session panel antes de comprimir o superponer Queue/Inventory.
- [ ] Redimensionar la ventana varias veces no desincroniza hover, clicks, paint-drag ni queue drag respecto de los slots dibujados.
- [ ] Los botones Single Block, Line Mining, Start/Stop, Clear y Close permanecen completos y clickeables en todos los layouts soportados.

## Regresión corta 1.1.0

- [ ] El bind de Pick Relay y la tecla Inventory siguen cerrando la GUI.
- [ ] Session panel, efectos y BPS siguen actualizándose.
- [ ] Selected Tool mantiene su BPS teórico y borde dorado.
- [ ] ACTIVE conserva borde blanco; drag target conserva cian.
- [ ] Cerrar la GUI durante ACTIVE no detiene la sesión.
- [ ] Cerrar la GUI antes de Start limpia la queue como antes.
