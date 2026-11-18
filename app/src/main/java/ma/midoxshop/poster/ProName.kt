package ma.midoxshop.poster

/**
 * The "pro" way to write a product name, the way brands write it: "dji mic mini single" →
 * "DJI Mic Mini Single", "canon 4000d + 18-55" → "Canon EOS 4000D + 18-55mm",
 * "sony fe 50mm 1.8" → "Sony FE 50mm f/1.8", "sandisk ultra 128go" → "SanDisk Ultra 128 Go".
 *
 * Rules written for camera gear, not an AI: it never invents a model, it only writes what you typed
 * the right way. Names it doesn't know stay as typed (null).
 */
object ProName {
    /** Brands as they write themselves. Anything here also marks the name as a brand product (title case). */
    private val BRANDS = mapOf(
        "sony" to "Sony", "canon" to "Canon", "nikon" to "Nikon", "fujifilm" to "Fujifilm", "fuji" to "Fujifilm",
        "panasonic" to "Panasonic", "lumix" to "Lumix", "olympus" to "Olympus", "leica" to "Leica", "pentax" to "Pentax",
        "ricoh" to "Ricoh", "hasselblad" to "Hasselblad", "blackmagic" to "Blackmagic", "kodak" to "Kodak",
        "sigma" to "Sigma", "tamron" to "Tamron", "tokina" to "Tokina", "samyang" to "Samyang", "viltrox" to "Viltrox",
        "yongnuo" to "Yongnuo", "laowa" to "Laowa", "meike" to "Meike", "7artisans" to "7Artisans", "ttartisan" to "TTArtisan",
        "dji" to "DJI", "gopro" to "GoPro", "insta360" to "Insta360", "zhiyun" to "Zhiyun", "feiyu" to "Feiyu", "moza" to "MOZA",
        "godox" to "Godox", "aputure" to "Aputure", "amaran" to "Amaran", "nanlite" to "Nanlite", "neewer" to "Neewer",
        "nicefoto" to "Nicefoto", "profoto" to "Profoto", "ulanzi" to "Ulanzi", "smallrig" to "SmallRig", "tilta" to "Tilta",
        "manfrotto" to "Manfrotto", "benro" to "Benro", "joby" to "JOBY", "gitzo" to "Gitzo", "vanguard" to "Vanguard",
        "rode" to "Rode", "røde" to "RØDE", "boya" to "BOYA", "hollyland" to "Hollyland", "saramonic" to "Saramonic",
        "sennheiser" to "Sennheiser", "shure" to "Shure", "zoom" to "Zoom", "tascam" to "TASCAM", "focusrite" to "Focusrite",
        "audio-technica" to "Audio-Technica", "deity" to "Deity", "maono" to "MAONO", "fifine" to "FIFINE",
        "sandisk" to "SanDisk", "lexar" to "Lexar", "kingston" to "Kingston", "samsung" to "Samsung", "prograde" to "ProGrade",
        "hoya" to "Hoya", "k&f" to "K&F", "lowepro" to "Lowepro", "peak" to "Peak", "atomos" to "Atomos", "feelworld" to "Feelworld",
        "elgato" to "Elgato", "jmary" to "Jmary", "weifeng" to "Weifeng", "puluz" to "PULUZ", "andoer" to "Andoer", "kf" to "K&F", "nisi" to "NiSi", "zeiss" to "Zeiss", "pgytech" to "PGYTECH", "spigen" to "Spigen", "apple" to "Apple", "iphone" to "iPhone", "ipad" to "iPad", "macbook" to "MacBook",
        "airpods" to "AirPods", "logitech" to "Logitech", "anker" to "Anker", "baseus" to "Baseus", "ugreen" to "UGREEN",
        "jbl" to "JBL", "xiaomi" to "Xiaomi", "huawei" to "Huawei", "lenovo" to "Lenovo", "asus" to "ASUS", "hp" to "HP",
        "dell" to "Dell", "msi" to "MSI", "lg" to "LG", "playstation" to "PlayStation", "ps5" to "PS5", "osmo" to "Osmo"
    )

    /** Words brands write in capitals, or a set way. */
    private val WORDS = mapOf(
        "stm" to "STM", "usm" to "USM", "is" to "IS", "oss" to "OSS", "gm" to "GM", "g" to "G", "vr" to "VR", "ois" to "OIS",
        "dg" to "DG", "dn" to "DN", "hsm" to "HSM", "os" to "OS", "di" to "Di", "rxd" to "RXD", "vxd" to "VXD", "wr" to "WR",
        "ef" to "EF", "ef-s" to "EF-S", "efs" to "EF-S", "ef-m" to "EF-M", "rf" to "RF", "rf-s" to "RF-S", "rfs" to "RF-S",
        "fe" to "FE", "xf" to "XF", "xc" to "XC", "af" to "AF", "mf" to "MF", "af-s" to "AF-S", "af-p" to "AF-P",
        "nikkor" to "Nikkor", "art" to "Art", "contemporary" to "Contemporary", "sport" to "Sport", "macro" to "Macro",
        "ii" to "II", "iii" to "III", "iv" to "IV", "v" to "V", "vi" to "VI", "mk2" to "Mark II", "mkii" to "Mark II",
        "mk3" to "Mark III", "mkiii" to "Mark III", "mk4" to "Mark IV", "mkiv" to "Mark IV", "mark" to "Mark",
        "pro" to "Pro", "max" to "Max", "mini" to "Mini", "plus" to "Plus", "ultra" to "Ultra", "extreme" to "Extreme",
        "usb-c" to "USB-C", "usb" to "USB", "hdmi" to "HDMI", "led" to "LED", "rgb" to "RGB", "ttl" to "TTL", "hss" to "HSS",
        "nd" to "ND", "uv" to "UV", "cpl" to "CPL", "sd" to "SD", "sdxc" to "SDXC", "microsd" to "microSD", "cfexpress" to "CFexpress",
        "4k" to "4K", "8k" to "8K", "tx" to "TX", "rx" to "RX", "rs" to "RS", "rc" to "RC", "gen" to "Gen", "xlr" to "XLR", "kit" to "Kit", "combo" to "Combo",
        "fly" to "Fly", "more" to "More", "creator" to "Creator", "vlog" to "Vlog", "go" to "Go", "mo/s" to "Mo/s", "mb/s" to "Mo/s"
    )

    /** Small French and English words that stay small inside a name. */
    private val SMALL = setOf("de", "du", "des", "la", "le", "les", "pour", "avec", "et", "en", "sans", "a", "à", "x", "for", "with", "and", "of", "+", "&")

    // A camera body typed bare: "4000d", "r50", "a6700", "zv-e10"…
    private val CANON_BODY = Regex("^(\\d{1,4}d|r\\d{1,2}c?|m\\d{2}|r)$", RegexOption.IGNORE_CASE)
    private val SONY_BODY = Regex("^(a\\d{1,4}[a-z]?|zv-?e\\d{1,2}|zv-?\\d|fx\\d{1,2})$", RegexOption.IGNORE_CASE)
    private val NIKON_BODY = Regex("^(z\\d{1,2}|zf|zfc|d\\d{2,4})$", RegexOption.IGNORE_CASE)
    private val FUJI_BODY = Regex("^x-?(t|s|e|h|m|pro|a)\\d{1,3}$", RegexOption.IGNORE_CASE)

    /** Brands that make lenses: there "18-55" means 18-55mm. */
    private val LENS_BRANDS = setOf("canon", "nikon", "sony", "fujifilm", "fuji", "sigma", "tamron", "tokina", "samyang", "viltrox",
        "yongnuo", "laowa", "meike", "7artisans", "ttartisan", "panasonic", "lumix", "olympus", "leica", "pentax")

    /** Units after a number, written the usual way: "90CM" → "90cm", "60w" → "60W", "2000mah" → "2000mAh". */
    private val UNIT = Regex("^(\\d+(?:[.,]\\d+)?)(cm|m|mm|kg|g|w|wh|mah|v|l|k)$", RegexOption.IGNORE_CASE)
    private fun unit(m: MatchResult): String {
        val u = when (val x = m.groupValues[2].lowercase()) { "w" -> "W"; "wh" -> "Wh"; "mah" -> "mAh"; "v" -> "V"; "k" -> "K"; else -> x }
        return m.groupValues[1] + u
    }

    /** The pro name, or null when it is already written that way (or the rules don't know it). */
    fun pro(name: String): String? {
        val before = name.trim().replace(Regex("\\s+"), " ")
        if (before.isEmpty()) return null
        // Units glued to numbers: "128go" → "128 Go", "1.8f" stays, "50 mm" → "50mm".
        var t = before
            .replace(Regex("(?i)(\\d+)\\s?(go|gb)\\b")) { m -> m.groupValues[1] + " Go" }
            .replace(Regex("(?i)(\\d+)\\s?(to|tb)\\b")) { m -> m.groupValues[1] + " To" }
            .replace(Regex("(?i)(\\d)\\s+mm\\b"), "$1mm")
            .replace(Regex("(?i)\\bmark\\s*(2|ii)\\b"), "Mark II")
            .replace(Regex("(?i)\\bmark\\s*(3|iii)\\b"), "Mark III")
            .replace(Regex("(?i)\\bmark\\s*(4|iv)\\b"), "Mark IV")
        // "canon 18-55" → "18-55mm" when the brand makes lenses.
        if (t.split(" ").any { it.lowercase() in LENS_BRANDS } || t.contains(Regex("(?i)\\d+mm")))
            t = t.replace(Regex("(?<![\\d.-])(\\d{1,3})-(\\d{2,3})(?![\\d.]|\\s?mm|mm)")) { m ->
                val a = m.groupValues[1].toInt(); val b = m.groupValues[2].toInt()
                if (a in 8..600 && b in 10..800 && a < b) m.value + "mm" else m.value
            }
        // "3th Gen" → "3rd Gen".
        t = Regex("(?<!\\d)([123])th\\b", RegexOption.IGNORE_CASE).replace(t) { m ->
            m.groupValues[1] + when (m.groupValues[1]) { "1" -> "st"; "2" -> "nd"; else -> "rd" }
        }
        // Apertures: "f1.8" / "F 2.8" / "f/4" → "f/1.8"; a bare "1.8" right after a focal length too.
        t = t.replace(Regex("(?i)(?<![\\p{L}\\d/])f\\s?/?\\s?(\\d{1,2}(\\.\\d)?(-\\d{1,2}(\\.\\d)?)?)(?![\\d.])")) { m -> "f/" + m.groupValues[1] }
        t = t.replace(Regex("(?i)(\\d+(-\\d+)?mm)\\s+(\\d{1,2}\\.\\d(-\\d{1,2}(\\.\\d)?)?)(?![\\d.]|\\s?(mm|go|to|m\\b|cm|kg|v\\b|w\\b|mah))")) { m ->
            m.groupValues[1] + " f/" + m.groupValues[3]
        }

        val words = t.split(" ").toMutableList()
        val brandIdx = words.indexOfFirst { BRANDS.containsKey(it.lowercase()) }
        val branded = brandIdx >= 0
        val brand = if (branded) BRANDS.getValue(words[brandIdx].lowercase()) else ""

        var inBrackets = false
        for (i in words.indices) {
            val w = words[i]
            val k = w.lowercase()
            val lead = w.takeWhile { it == '(' }
            val trail = w.takeLastWhile { it == ')' || it == ',' }
            val core = w.removePrefix(lead).removeSuffix(trail)
            val ck = core.lowercase()
            if (lead.isNotEmpty()) inBrackets = true
            val fixed = when {
                core.isEmpty() -> core
                BRANDS.containsKey(ck) -> BRANDS.getValue(ck)
                ck == "mm" || ck == "f/" -> ck
                Regex("^\\d+(-\\d+)?mm$").matches(ck) -> ck
                ck.startsWith("f/") -> ck
                UNIT.matches(core) -> UNIT.replace(core) { unit(it) }
                // "2i2", "4i4" (audio interfaces) stay small.
                Regex("^\\d+i\\d+$").matches(ck) -> ck
                brand == "Canon" && CANON_BODY.matches(ck) && i > 0 -> ck.uppercase()
                brand == "Sony" && SONY_BODY.matches(ck) -> sonyBody(ck)
                brand == "Nikon" && NIKON_BODY.matches(ck) -> if (ck == "zfc") "Z fc" else if (ck == "zf") "Z f" else ck.uppercase()
                brand == "Fujifilm" && FUJI_BODY.matches(ck) -> ck.uppercase().let { if (it[1] == '-') it else "X-" + it.substring(1) }
                WORDS.containsKey(ck) && (branded || ck in setOf("mk2", "mkii", "mk3", "mkiii", "mk4", "mkiv", "mo/s", "mb/s")) -> WORDS.getValue(ck)
                // Model codes: letters and digits mixed, "sl60ii" → "SL60II", "np-fw50" → "NP-FW50".
                core.any { it.isDigit() } && core.any { it.isLetter() } && !Regex("^\\d+(st|nd|rd|th|e|er|ème)$").matches(ck) ->
                    if (ck.matches(Regex("^v\\d+$"))) ck.uppercase() else core.uppercase()
                // A brand product: every word starts with a capital, except small ones.
                branded && ck in SMALL && i > 0 -> ck
                // Words in brackets, "(used)", "(single)": your own note, left as typed.
                branded && core.first().isLowerCase() && !inBrackets -> core.replaceFirstChar { it.uppercase() }
                else -> core
            }
            words[i] = lead + fixed + trail
            if (trail.contains(')')) inBrackets = false
        }

        // "Canon 4000D" is an "EOS"; "Sony a6700" an "Alpha"; unless already said.
        if (brand == "Canon" && brandIdx + 1 < words.size && CANON_BODY.matches(words[brandIdx + 1]) && words.none { it.equals("EOS", true) })
            words.add(brandIdx + 1, "EOS")
        if (brand == "Sony" && brandIdx + 1 < words.size && words[brandIdx + 1].matches(Regex("^a\\d.*")) && words.none { it.equals("Alpha", true) })
            words.add(brandIdx + 1, "Alpha")
        words.indexOfFirst { it.equals("eos", true) }.takeIf { it >= 0 }?.let { words[it] = "EOS" }

        val out = words.joinToString(" ").replaceFirstChar { it.uppercase() }
        return out.takeIf { it != before && it != name }
    }

    private fun sonyBody(k: String): String = when {
        k.startsWith("zv") -> "ZV-" + k.removePrefix("zv").removePrefix("-").uppercase()
        k.startsWith("fx") -> k.uppercase()
        else -> "a" + k.substring(1).uppercase()
    }

    /** Words that start a name but are not a brand ("Objectif Canon…", "Batterie Sony…"). */
    private val NOT_BRAND = setOf("objectif", "batterie", "chargeur", "charger", "battery", "trepied", "trépied", "tripied", "tripod",
        "location", "carte", "sac", "filtre", "cable", "câble", "micro", "lampe", "flash", "support", "adaptateur", "kit", "pack",
        "boitier", "boîtier", "appareil", "camera", "caméra", "ecran", "écran", "stabilisateur", "softbox", "reflecteur", "réflecteur",
        "fond", "pied", "perche", "bague", "housse", "lot", "new", "neuf", "used", "occasion")

    /** The brand for Facebook's Brand field: a known brand anywhere in the name, else a first word that looks like one. */
    fun brandOf(name: String): String {
        val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        words.firstOrNull { BRANDS.containsKey(it.lowercase()) }?.let { return BRANDS.getValue(it.lowercase()) }
        val first = words.firstOrNull() ?: return ""
        if (first.lowercase() in NOT_BRAND || first.any { it.isDigit() } || first.length < 2 || !first.all { it.isLetter() || it == '-' }) return ""
        return first.replaceFirstChar { it.uppercase() }
    }
}
