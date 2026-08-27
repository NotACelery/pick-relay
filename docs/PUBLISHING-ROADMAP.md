# Pick Relay 1.0.0 — Hoja de ruta de publicación

Objetivo: preparar una publicación consistente en **Modrinth** y **CurseForge** sin volver a tocar el motor funcional salvo que el JAR final revele una regresión.

## Fase 1 — Cerrar identidad visual

Pendiente principal antes de publicar:

- [x] Crear icono final hecho a mano/no generado por IA.
- [x] Exportar versión cuadrada de alta calidad para las páginas del proyecto (`src/main/resources/pick-relay-icon.png`).
- [x] Incluir el mismo icono dentro del JAR/lista de mods mediante `logoFile="pick-relay-icon.png"` y `logoBlur=false`.
- [ ] Verificar legibilidad del icono a tamaños pequeños.

El icono oficial 1.0.0 es `src/main/resources/pick-relay-icon.png`. Usar este mismo archivo como imagen del proyecto en Modrinth y CurseForge para mantener una identidad visual consistente.

## Fase 2 — Congelar el artefacto 1.0.0

- [ ] Ejecutar `build.bat` o `build.sh` con Java 21.
- [ ] Confirmar `pickrelay-1.21.1-1.0.0.jar`.
- [ ] Ejecutar `docs/TESTING-1.0.0.md` sobre **ese mismo JAR**.
- [ ] No modificar código después de esa validación sin volver a generar/probar el JAR.
- [ ] Crear tag Git `v1.0.0` una vez congelado.

## Fase 3 — Material visual

Preparar capturas reales ingame, sin mockups engañosos:

1. GUI con una queue variada y varias configuraciones por herramienta.
2. Line Mining activo mostrando HUD.
3. Single Block en una granja de cobblestone/stone.
4. Ejemplo de Preserve at 1 o Blocks Broken si puede mostrarse claramente.

Opcionalmente preparar una imagen comparativa sencilla:

```text
Plan queue → Start → tools relay automatically → Stop safely
```

## Fase 4 — Texto público

### Identidad corta

**Pick Relay**  
**Automate the grind. Schedule your tools.**

### Puntos que deben aparecer arriba de la descripción

- Client-side.
- Minecraft 1.21.1 / NeoForge.
- Queue manual de hasta 36 herramientas.
- Until Broken / Durability / Blocks Broken por herramienta.
- Preserve at 1.
- Single Block / Line Mining.
- Reubicación automática de tools dentro del inventario.
- No pathfinding, no recursos gratis, no velocidad/alcance modificados.

### Mensaje conceptual recomendado

La descripción debe dejar claro que Pick Relay **no elige la mejor herramienta**. El jugador programa exactamente cuáles se usarán y en qué orden.

## Fase 5 — Metadata de ambas plataformas

Mantener los dos proyectos alineados:

- Nombre: `Pick Relay`.
- Versión: `1.0.0`.
- Game version: Minecraft `1.21.1`.
- Mod loader: NeoForge.
- Environment: client-side.
- Source: repositorio GitHub `NotACelery/pick-relay`.
- License: usar la misma política declarada por el proyecto (`All Rights Reserved`) salvo que se decida cambiarla antes de publicar.
- Changelog: reutilizar la sección `1.0.0` de `CHANGELOG.md`.

Verificar manualmente los campos exactos de cada plataforma al momento de crear la página, ya que sus formularios pueden cambiar.

## Fase 6 — Orden recomendado de publicación

1. Congelar/taggear GitHub.
2. Crear la primera página en una plataforma.
3. Revisar desde fuera que icono, descripción, imágenes, dependencias y versión se vean bien.
4. Replicar la metadata en la segunda plataforma.
5. Comparar ambas páginas antes de anunciarlas.
6. No subir JARs diferentes con el mismo `1.0.0`.

## Fase 7 — Primera semana post-release

No planificar features inmediatamente. Prioridad:

1. Crash/loading reports.
2. Tool loss/duplication reports.
3. Preserve failures.
4. Relay desyncs.
5. Compatibilidad con tools modded.
6. Problemas visuales menores.

Si aparece un bug pequeño y concreto, publicar `1.0.1`. Si implica cambio funcional mayor, evaluarlo separadamente en una versión posterior.

## Fase 8 — Futuro posterior a estabilidad

Sólo después de estabilizar 1.0.x:

- integración opcional con THE Pick / Eruruu's Patch;
- evaluar otros loaders/versiones;
- QoL nuevos únicamente si mantienen la identidad de scheduler explícito.

Evitar convertir Pick Relay en un bot/pathfinder o auto-best-tool selector.
