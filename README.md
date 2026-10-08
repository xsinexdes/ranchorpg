# RanchoRPG

Plugin Paper/Spigot 1.21.1 (Java 21) que REEMPLAZA la ganadería y la agricultura vanilla por un sistema RPG:
necesidades, enfermedades, genética y mutaciones (estilo ARK), cultivos con requisitos, temperatura, estaciones,
eventos de estación, economía, nivel de granjero, habilidades, logros y misiones.

## Compilar
- GitHub Actions: sube el proyecto -> pestaña Actions -> "Build RanchoRPG" -> descarga el artifact `RanchoRPG.jar`.
- Local: `mvn clean package` (JDK 21) -> `target/RanchoRPG.jar`.

## Comandos y permisos
`/rancho` menú · `/rancho info|temp` · `/rancho libreta` · `/rancho termometro`
Admin (`rancho.admin`): `/rancho admin` (TODA la configuración por GUI), `reload`, `give <item> [n] [jugador]`, `spawn <especie> [n]`, `npc`.
`rancho.use` (por defecto todos) · `rancho.admin` (op).

## Cómo probar (resumen)
1. `/rancho admin` -> Items de Rancho: coge Libreta, Pienso, Regadera, Fertilizantes, Semillas...
2. Click derecho a un animal con la mano vacía (o la Libreta) = menú con todas sus stats. En caballos: agáchate (si no, montas).
3. Dale pienso/forraje (o su comida), agua cerca (agua o caldero con agua), refugio, cepillo y limpia el estiércol.
4. Cría: Suplemento de Cría sobre un adulto con pareja adulta y feliz cerca. Mutaciones: Suero, Estabilizador, Análisis Genético.
5. Cultivos: azada -> siembra (vanilla o semillas custom) -> riega con la Regadera (se rellena en agua), fertiliza, Tierra Mejorada.
   Libreta sobre un cultivo = qué necesita y cuánto falta. Invernadero/Maceta anulan clima y estación.
6. Vende con `/rancho` -> Mercado (o `/rancho npc`). Placeholders: %rancho_season%, %rancho_season_day%, %rancho_temperature%, %rancho_farmer_level%.
