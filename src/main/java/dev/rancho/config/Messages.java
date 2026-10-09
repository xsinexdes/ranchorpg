package dev.rancho.config;

import dev.rancho.item.CustomItems;

/** Define todos los mensajes por defecto (se guardan en lang.yml y se editan desde el menú de administración). */
public final class Messages {

    private Messages() {}

    private static void d(String key, String value) {
        Lang.define(key, value);
    }

    public static void init() {
        d("prefix", "&6[Rancho] &7");
        d("no-permission", "&cNo tienes permiso para hacer eso.");
        d("only-players", "&cEse comando solo puede usarlo un jugador.");
        d("reloaded", "&aConfiguración y mensajes recargados.");
        d("unknown-subcommand", "&cSubcomando desconocido. Usa &e/rancho&c.");
        d("word.yes", "&aSí");
        d("word.no", "&cNo");

        d("season.spring", "&aPrimavera");
        d("season.summer", "&eVerano");
        d("season.autumn", "&6Otoño");
        d("season.winter", "&bInvierno");
        d("season.change.title", "{season}");
        d("season.change.subtitle", "&7Ha cambiado la estación");
        d("season.change.chat", "&7Ha llegado: {season}&7.");
        d("season.bossbar", "{season} &7- Día &f{day}&7/&f{length}");
        d("season.bossbar.event", " &8| &6{event}");

        d("event.harvest", "&6Fiesta de la Cosecha");
        d("event.frost", "&bHelada");
        d("event.heatwave", "&cOla de calor");
        d("event.drought", "&eSequía");
        d("event.subtitle", "&7Un evento de estación ha comenzado");
        d("event.start", "&6¡Evento! {event} &7durará &f{days} &7días.");
        d("event.end", "&7El evento {event} &7ha terminado.");

        d("info.header", "&6&l=== RanchoRPG ===");
        d("info.season", "&7Estación: {season} &7(día &f{day}&7/&f{length}&7)");
        d("info.temperature", "&7Temperatura aquí: {color}{temp}°C");
        d("info.farmer", "&7Granjero nivel &a{level} &7(XP &f{xp}&7) · Monedas &6{coins}");
        d("climate.cold", "&b¡Hace mucho frío! Abrígate o busca calor.");
        d("climate.hot", "&c¡Hace demasiado calor! Busca sombra y agua.");

        d("menu.main.title", "&6Rancho");
        d("menu.admin.title", "&4Administración de Rancho");
        d("menu.seasons.title", "&6Ajustes de estaciones");
        d("menu.temperature.title", "&6Ajustes de temperatura");
        d("menu.messages.title", "&6Mensajes (pág. {page})");
        d("menu.back", "&cVolver");
        d("menu.close", "&cCerrar");
        d("menu.next", "&aSiguiente página");
        d("menu.prev", "&aPágina anterior");

        d("prompt.ask", "&eEscribe el nuevo texto en el chat para &f{key}&e (o &ccancelar&e).");
        d("prompt.value", "&eEscribe el nuevo valor para &f{key}&e (o &ccancelar&e).");
        d("prompt.newid", "&eEscribe el id nuevo (letras, números y _) o &ccancelar&e.");
        d("prompt.invalid-id", "&cId inválido o ya existente.");
        d("prompt.cancelled", "&7Edición cancelada.");
        d("prompt.saved", "&aMensaje guardado.");

        d("product.name", "{color}{name} Nivel {q}");
        d("product.lore.tier", "&7Calidad: {color}{tier}");
        d("product.lore.sat", "&7Saturación extra: &a+{sat}");
        d("product.lore.buff", "&7Efecto: &b{effect} {secs}s");
        d("product.buff", "&7Calidad {level}&7: obtienes &b{effect}&7.");

        d("animal.fed", "&a{name} ha comido.");
        d("animal.full", "&e{name} no tiene más hambre ni sed por ahora.");
        d("animal.wont-eat", "&c{name} no quiere eso.");
        d("animal.drink", "&b{name} ha bebido.");
        d("animal.brushed", "&a{name} está más limpio.");
        d("animal.no-effect", "&7Eso no tendría ningún efecto ahora.");
        d("animal.sick", "&c{name} ha enfermado de &2{disease}&c.");
        d("animal.not-sick", "&e{name} no está enfermo.");
        d("animal.wrong-cure", "&cEsa medicina no cura {disease}.");
        d("animal.cured", "&a{name} se ha curado.");
        d("animal.vaccinated", "&a{name} está inmunizado.");
        d("animal.grew", "&a{name} ahora es &f{stage}&a.");
        d("animal.level", "&a{name} ha subido al nivel &e{level}&a.");
        d("animal.old-death", "&7{name} ha muerto de vejez.");
        d("animal.born", "&a{name} ha tenido &f{count} &acría(s).");
        d("animal.breed.ok", "&d{mother} y {father} formarán una nueva generación.");
        d("animal.breed.fail", "&c{name}: {reason}.");
        d("animal.breed.nomate", "&c{name} necesita una pareja adulta y feliz cerca.");
        d("animal.breed.season", "&c{name} no se reproduce en esta estación.");
        d("animal.milk.ok", "&aHas ordeñado a {name}.");
        d("animal.milk.wait", "&e{name} aún no tiene leche ({pct}%).");
        d("animal.shear.ok", "&aHas esquilado a {name}.");
        d("animal.shear.wait", "&e{name} aún no tiene lana ({pct}%).");
        d("animal.mutation.new", "&d{name} ha mutado: {mutation}&d.");
        d("animal.mutation.max", "&c{name} ya no puede tener más mutaciones.");
        d("animal.stabilized.removed", "&a{name} perdió la mutación {mutation}&a.");
        d("animal.stabilized.protected", "&a{name} dará a luz sin mutaciones negativas.");
        d("animal.analysis.done", "&9Análisis genético de {name} completado.");

        d("crop.need-improved", "&c{crop} necesita tierra mejorada.");
        d("crop.need-fertilizer", "&c{crop} necesita tierra con fertilizante para plantarse.");
        d("crop.soil-full", "&eLa tierra ya está muy fértil.");
        d("crop.fertilized", "&aFertilidad de la tierra: &f{fert}%");
        d("crop.already-improved", "&eEsa tierra ya está mejorada.");
        d("crop.improved", "&aTierra mejorada.");
        d("crop.can-empty", "&cLa regadera está vacía. Rellénala en agua.");
        d("crop.can-filled", "&aRegadera rellena.");
        d("crop.controller-placed", "&aColocado. Anula clima y estación en &f{range} &abloques a la redonda.");
        d("crop.soil-info", "&7Agua &f{water}% &8| &7Fertilidad &f{fert}% &8| &7Mejorada {improved}");
        d("item.can.uses", "&7Usos: &f{uses}&7/&f{max}");

        d("farmer.levelup", "&a¡Subes al nivel de granjero &e{level}&a! (+1 punto de habilidad)");
        d("farmer.skill.max", "&eEsa habilidad ya está al máximo.");
        d("farmer.skill.nopoints", "&cNo tienes puntos de habilidad.");
        d("farmer.skill.up", "&a{skill} sube al nivel &e{level}&a.");
        d("quest.complete", "&6¡Misión completada! &f{quest} &7(+{coins} monedas)");
        d("achievement.unlocked", "&6¡Logro desbloqueado! &f{name} &7(+{coins} monedas)");


        d("command.give-usage", "&cUso: /rancho give <item> [cantidad] [jugador]");
        d("command.no-player", "&cJugador no encontrado.");
        d("command.unknown-item", "&cItem desconocido.");
        d("command.given", "&aEntregado &f{amount}x {item} &aa {player}.");
        d("command.unknown-species", "&cEspecie desconocida. Usa /rancho spawn <especie> [cantidad].");
        d("command.spawned", "&aGenerados {amount} x {species}.");

        d("animal.locked", "&c{owner} es el dueño de este animal.");
        d("food.fresh", "&7Frescura: {bar} &f{pct}% &8(se pudre en {time})");
        d("food.lore.points", "&7Hambre extra: &a+{points}");
        d("food.lore.effect", "&7Efecto: &b{effect} &8({secs}s)");
        d("food.rotten.name", "&2Comida Podrida");
        d("food.rotten.lore", "&7Se ha echado a perder.|&8Mejor no comerla...");
        d("food.eat-rotten", "&c¡Estaba podrida! Te sientes fatal.");
        d("food.cooked", "&aHas obtenido &f{amount}x {food} &7(Nivel {q})&a.");
        d("food.started", "&6Cocinando &f{food}&6... listo en &f{secs} &6segundos.");
        d("food.ready", "&a¡Tu {food} &aestá listo en la cocina!");
        d("food.wait", "&eTodavía se está cocinando (&f{time}&e).");
        d("food.busy", "&cLa cocina está ocupada con otro plato.");
        d("food.missing", "&cTe faltan ingredientes para esa receta.");
        d("food.kitchen-placed", "&aCocina colocada. Click derecho para cocinar.");
        d("trough.feeder-placed", "&aComedero colocado. Échale pienso o forraje.");
        d("trough.waterer-placed", "&aAbrevadero colocado: agua infinita para los animales cercanos.");
        d("trough.waterer-info", "&bAbrevadero: agua infinita para los animales cercanos.");
        d("trough.feeder-info", "&eComedero: &f{servings}&7/&f{max} &eraciones");
        d("trough.full", "&cEl comedero está lleno.");

        d("model.prompt", "&eEscribe el número de CustomModelData para &f{key}&e (o &ccancelar&e).");
        d("model.set", "&aModelo de &f{key} &aestablecido en &f{value}&a.");
        d("model.reset", "&7Modelo de &f{key} &7quitado.");
        d("model.invalid", "&cEscribe un número entero válido.");
        d("model.usage", "&cUso: /rancho model <hand|list|clave> [número]  (claves: item:id, food:id, seed:id, crop:id, meat:id, leather:id, produce:id)");
        d("model.unknown", "&cClave desconocida. Usa /rancho model list o el tabulador.");
        d("model.no-item", "&cEse item no es de RanchoRPG; sostén una comida, item, semilla o producto del plugin.");

        d("food.cask-placed", "&aBarrica colocada. Click derecho para fermentar y curar.");
        d("food.mill-placed", "&aMolino colocado. Click derecho para moler y prensar.");
        d("food.wrong-station", "&cEsa receta no se prepara en esta estación.");
        d("food.aged", "&5El reposo en la barrica mejoró el resultado: &f+{bonus} &5niveles de calidad.");
        d("crop.no-pest", "&eEste cultivo no tiene plagas.");
        d("crop.pest-cured", "&aHas eliminado la plaga del cultivo.");
        d("machine.sprinkler.placed", "&aAspersor colocado. Rellénalo con cubos de agua.");
        d("machine.scarecrow.placed", "&aEspantapájaros colocado: protege a los cultivos cercanos de las plagas.");
        d("machine.hive.placed", "&aColmena colocada. Cuantas más flores cerca, mejor miel.");
        d("machine.fridge.placed", "&aNevera colocada. Click derecho para guardar comida.");
        d("machine.aquarium.placed", "&aAcuario colocado. Click derecho para guardar peces vivos.");
        d("machine.sprinkler.full", "&cEl aspersor ya tiene el depósito lleno.");
        d("machine.sprinkler.info", "&3Aspersor: &f{charges}&7/&f{max} &3cargas de agua");
        d("machine.scarecrow.info", "&6Espantapájaros: protege a los cultivos en &f{radius} &6bloques.");
        d("machine.hive.info", "&eColmena: &f{pct}% &elista · flores cerca: &f{flowers}");
        d("machine.hive.notready", "&eAún no hay miel suficiente ({pct}%).");
        d("machine.hive.honey", "&aHas recogido &f{food} &7(Nivel {q})&a.");
        d("machine.fridge.title", "&bNevera de Rancho");
        d("machine.fridge.reject", "&cSolo se puede guardar comida y productos del rancho.");
        d("machine.aquarium.title", "&9Acuario de Rancho");
        d("machine.aquarium.reject", "&cSolo se pueden guardar peces.");
        d("fish.caught", "&b¡Has pescado &f{fish} &7(Nivel {q})&b!");
        d("fair.start", "&6¡Feria de ganado abierta durante &f{minutes} &6minutos! Inscribe a tu mejor animal adulto desde su menú.");
        d("fair.cancelled", "&7La feria de ganado ha sido cancelada.");
        d("fair.inactive", "&cNo hay ninguna feria abierta ahora mismo.");
        d("fair.not-adult", "&c{name} debe ser adulto para competir.");
        d("fair.not-owner", "&c{name} no es tuyo: aliméntalo o cuídalo primero para ser su dueño.");
        d("fair.entered", "&a{name} ({species}) inscrito en la feria con una puntuación de &f{score}&a.");
        d("fair.result", "&6[Feria] &f{species} &7- puesto &e{place}&7: &f{player} &7con &f{animal} &7({score} pts)");
        d("fair.empty", "&7La feria terminó sin participantes.");
        d("fair.status-on", "&6Feria abierta: quedan &f{time}&6.");
        d("fair.status-off", "&7No hay ninguna feria abierta.");

        CustomItems.defineLang();
    }
}
