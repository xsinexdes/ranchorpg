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
        d("product.lore.value", "&7Valor base: &6{value} monedas");
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

        d("market.nothing", "&eNo tienes productos del rancho para vender.");
        d("market.sold", "&aHas vendido &f{units} &aproductos por &6{total} &amonedas.");
        d("market.no-money", "&cNecesitas {price} monedas.");
        d("market.bought", "&aHas comprado &f{amount} &apor &6{price} &amonedas.");

        d("command.give-usage", "&cUso: /rancho give <item> [cantidad] [jugador]");
        d("command.no-player", "&cJugador no encontrado.");
        d("command.unknown-item", "&cItem desconocido.");
        d("command.given", "&aEntregado &f{amount}x {item} &aa {player}.");
        d("command.unknown-species", "&cEspecie desconocida. Usa /rancho spawn <especie> [cantidad].");
        d("command.spawned", "&aGenerados {amount} x {species}.");
        d("command.npc", "&aMercader creado.");
        d("npc.name", "&6Mercader del Rancho");

        CustomItems.defineLang();
    }
}
