package dev.rancho.gui;

/** Caracteres del resource pack para dibujar interfaces personalizadas (ver assets/rancho/font/gui.json). */
final class GuiGlyphs {

    /** El orden define el carácter de cada fondo; debe coincidir con el generador del resource pack. */
    static final String[] THEMES = {"main", "admin", "farm", "kitchen", "cask", "mill"};

    /** Avance del fondo en píxeles (ancho 176 + 1) y posición X del título dentro del banner. */
    private static final int ADVANCE = 177;
    private static final int TITLE_X = 14;

    private GuiGlyphs() {}

    static boolean supported(String theme, int rows) {
        return rows >= 3 && rows <= 6 && indexOf(theme) >= 0;
    }

    private static int indexOf(String theme) {
        for (int i = 0; i < THEMES.length; i++) {
            if (THEMES[i].equals(theme)) {
                return i;
            }
        }
        return -1;
    }

    static char image(String theme, int rows) {
        return (char) (0xE600 + indexOf(theme) * 4 + (rows - 3));
    }

    /** Cadena de espacios (positivos o negativos) que desplaza el cursor {@code amount} píxeles. */
    static String shift(int amount) {
        StringBuilder sb = new StringBuilder();
        int abs = Math.abs(amount);
        int base = amount < 0 ? 0xE000 : 0xE400;
        for (int k = 0; k < 9; k++) {
            if ((abs & (1 << k)) != 0) {
                sb.append((char) (base + (1 << k)));
            }
        }
        return sb.toString();
    }

    /** Prefijo del título: retrocede al borde, dibuja el fondo y coloca el cursor donde empieza el título. */
    static String prefix(String theme, int rows) {
        return shift(-8) + image(theme, rows) + shift(-(ADVANCE - TITLE_X));
    }
}
