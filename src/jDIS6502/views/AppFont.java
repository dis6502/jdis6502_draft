package jDIS6502.views;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

/**
 * Loads the bundled "Atari800" TrueType font (resources/Atari800.ttf, supplied by the user) and
 * applies it across the entire UI. Not a translation of anything in the C++ source -- the
 * original app just used whatever Windows GDI font it was told to (Tahoma/Segoe UI/MS Gothic),
 * this is a new feature.
 *
 * Unlike the "Atari Vector" font tried earlier (since reverted), this one was measured and
 * confirmed genuinely fixed-width -- every character sampled (letters, digits, space, punctuation)
 * is exactly the same pixel width at a given point size -- so, unlike Atari Vector, it's safe to
 * use for the hex dump grid, the disassembly listing, the log, and the reference list too: their
 * column alignment (hex byte grid, ".byte $XX,$XX" lists, the "; $addr" comment column, etc.)
 * depends only on every character being the same width, not on that width matching any particular
 * other font.
 */
final class AppFont {

    private static final String RESOURCE_PATH = "/jDIS6502/resources/Atari800.ttf";
    private static final String FALLBACK_FAMILY = Font.MONOSPACED;

    private static Font baseFont; // 1pt; every real size/style is derived from this.
    private static boolean loadAttempted = false;

    private AppFont() {
    }

    /** The Atari800 font at the given style/size, or a monospaced fallback if it couldn't be loaded. */
    static Font of(int style, float size) {
        Font base = loadBaseFont();
        if (base != null) {
            return base.deriveFont(style, size);
        }
        return new Font(FALLBACK_FAMILY, style, Math.round(size));
    }

    /**
     * Replaces every Font-valued Swing UI default (Button.font, Label.font, Menu.font,
     * OptionPane.font, Table.font, TableHeader.font, TextArea.font, ...) with an Atari800
     * version at that same default's own style/size, so ordinary components created with no
     * explicit setFont call -- plain JLabels/JButtons/JCheckBoxes, JOptionPane and JFileChooser
     * dialogs, tooltips, menus -- pick it up automatically. Must be called before any of those
     * components are constructed (i.e. first thing in main(), before "new Dis6502Gui()").
     */
    static void installAsDefaultUIFont() {
        Font base = loadBaseFont();
        if (base == null) {
            return; // Couldn't load the font; leave the platform look-and-feel's defaults alone.
        }

        javax.swing.UIDefaults defaults = UIManager.getDefaults();
        Enumeration<Object> keys = defaults.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = defaults.get(key);
            if (value instanceof Font) {
                Font existing = (Font) value;
                defaults.put(key, new FontUIResource(base.deriveFont(existing.getStyle(), existing.getSize2D())));
            }
        }
    }

    private static synchronized Font loadBaseFont() {
        if (!loadAttempted) {
            loadAttempted = true;
            InputStream in = null;
            try {
                in = AppFont.class.getResourceAsStream(RESOURCE_PATH);
                if (in != null) {
                    Font loaded = Font.createFont(Font.TRUETYPE_FONT, in);
                    GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(loaded);
                    baseFont = loaded;
                }
            } catch (IOException e) {
                baseFont = null;
            } catch (java.awt.FontFormatException e) {
                baseFont = null;
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        // Ignore: nothing useful to do if closing the resource stream fails.
                    }
                }
            }
        }
        return baseFont;
    }
}
