package toast.utilityMobs.client;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.Locale;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import toast.utilityMobs._UtilityMobs;

/**
    The mod's en_us.lang is present in the jar but Minecraft's resource pack layer does not serve it
    for this mod (textures from the same domain load fine, the lang file throws FileNotFound). To make
    entity/egg/creative-tab names show, load the lang straight off the classpath and inject it into the
    active Locale, re-applying on every resource reload (vanilla rebuilds the Locale on reload).
 */
@SideOnly(Side.CLIENT)
public class LangFix implements IResourceManagerReloadListener
{
    private static final String LANG_DIR = "/assets/utilitymobs/lang/";
    private static final String FALLBACK_LANG = "en_us";

    public static void install() {
        IResourceManager rm = Minecraft.getMinecraft().getResourceManager();
        if (rm instanceof IReloadableResourceManager) {
            ((IReloadableResourceManager) rm).registerReloadListener(new LangFix());
        }
        inject();
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        inject();
    }

    @SuppressWarnings("unchecked")
    private static void inject() {
        try {
            // Build the key set to inject: the active language first, then en_us to backfill any keys the
            // active language hasn't translated. putMissing means resource-pack-supplied keys (already in
            // the target Locale when this runs on reload) still win over both, and the active language wins
            // over the English fallback. Dropping an <code>.lang into the jar is all a new locale needs.
            Map<String, String> entries = new java.util.HashMap<String, String>();
            String active = activeLanguageCode();
            if (active != null && !active.equals(FALLBACK_LANG)) {
                entries.putAll(readLangFromClasspath(LANG_DIR + active + ".lang"));
            }
            putMissing(entries, readLangFromClasspath(LANG_DIR + FALLBACK_LANG + ".lang"));
            if (entries.isEmpty())
                return;

            // 1) Client Locale (net.minecraft.client.resources.I18n) - creative tab, GUI text.
            Locale locale = (Locale) field(I18n.class, "i18nLocale", "field_135054_a").get(null);
            if (locale != null) {
                Map<String, String> props = (Map<String, String>) field(Locale.class, "properties", "field_135032_a").get(locale);
                putMissing(props, entries);
            }

            // 2) Common LanguageMap (net.minecraft.util.text.translation.I18n) - spawn-egg names,
            //    entity nametags. This is a SEPARATE locale from the client one.
            Class<?> langMapCls = Class.forName("net.minecraft.util.text.translation.LanguageMap");
            Object langMap = field(langMapCls, "instance", "field_74817_a").get(null);
            if (langMap != null) {
                Map<String, String> list = (Map<String, String>) field(langMapCls, "languageList", "field_74816_c").get(langMap);
                putMissing(list, entries);
            }

            _UtilityMobs.console("LangFix injected ", Integer.toString(entries.size()), " translations.");
        }
        catch (Exception ex) {
            _UtilityMobs.console("[WARNING] LangFix failed to inject translations!");
            ex.printStackTrace();
        }
    }

    // Fills only keys the target map does not already have, so resource packs / localization mods that
    // provide their own utilitymobs.* strings win. Our reload listener runs after vanilla rebuilds the
    // Locale, so any pack-supplied keys are already present here and are left untouched.
    private static void putMissing(Map<String, String> target, Map<String, String> entries) {
        for (Map.Entry<String, String> e : entries.entrySet()) {
            if (!target.containsKey(e.getKey())) {
                target.put(e.getKey(), e.getValue());
            }
        }
    }

    /// The active language code (e.g. "ru_ru"), lowercased to match the jar's lang file names, or null.
    private static String activeLanguageCode() {
        try {
            return Minecraft.getMinecraft().getLanguageManager().getCurrentLanguage().getLanguageCode()
                    .toLowerCase(java.util.Locale.ROOT);
        }
        catch (Exception ex) {
            return null;
        }
    }

    private static Map<String, String> readLangFromClasspath(String path) throws Exception {
        Map<String, String> entries = new java.util.HashMap<String, String>();
        try (InputStream in = LangFix.class.getResourceAsStream(path)) {
            if (in == null) {
                // Missing locale file is normal (most languages aren't shipped) - only warn for the fallback.
                if (path.endsWith(FALLBACK_LANG + ".lang")) {
                    _UtilityMobs.console("[WARNING] LangFix: ", path, " not on classpath");
                }
                return entries;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#')
                    continue;
                int eq = line.indexOf('=');
                if (eq <= 0)
                    continue;
                entries.put(line.substring(0, eq), line.substring(eq + 1));
            }
        }
        return entries;
    }

    // Resolves a field by its MCP (dev) name, falling back to the SRG (obfuscated runtime) name.
    private static Field field(Class<?> cls, String mcpName, String srgName) throws NoSuchFieldException {
        try {
            Field f = cls.getDeclaredField(mcpName);
            f.setAccessible(true);
            return f;
        }
        catch (NoSuchFieldException ex) {
            Field f = cls.getDeclaredField(srgName);
            f.setAccessible(true);
            return f;
        }
    }
}
