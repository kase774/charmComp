@file:Suppress("SpellCheckingInspection")
@file:OptIn(ExperimentalStdlibApi::class)

package dev.kason.charm

import io.github.oshai.kotlinlogging.KotlinLogging

val logger = KotlinLogging.logger {}

enum class Code {
    eq,
    ne,
    cs,
    hs,
    cc,
    lo,
    mi,
    pl,
    vs,
    vc,
    hi,
    ls,
    ge,
    lt,
    gt,
    le,
    al
}

interface CharmBuilder {
    // m format
    /** Load with offset */
    fun ldur(dest: Register, location: Register, offset: Int = 0)

    /** Store with offset */
    fun stur(data: Register, pointer: Register, offset: Int = 0)

    // i1
    /** move immediate to res */
    fun movk(res: Register, immediate: UShort)

    /** move immediate to res, but since immediate is u16, shift it by [shift] */
    fun movkShift(res: Register, immediate: UShort, shift: Int)

    /** move to register, but 0 out */
    fun movz(res: Register, immediate: UShort)

    // i2
    fun adrp(res: Register, lbl: String)
    // rr
    /** dest = a + b */
    fun adds(dest: Register, a: Register, b: Register)

    /** dest = a - b */
    fun subs(dest: Register, a: Register, b: Register)
    fun mvn(dest: Register, a: Register, b: Register)
    fun orr(dest: Register, a: Register, b: Register)
    fun eor(dest: Register, a: Register, b: Register)
    fun ands(dest: Register, a: Register, b: Register)
    fun tst(dest: Register, a: Register, b: Register)
    fun cmp(a: Register, b: Register)
    fun cmn(a: Register, b: Register)

    // ri (immediate is acc only 12 bytes, check for that)
    fun add(dest: Register, a: Register, immediate: UShort)
    fun sub(dest: Register, a: Register, immediate: UShort)
    fun lsl(dest: Register, a: Register, immediate: UShort)
    fun lsr(dest: Register, a: Register, immediate: UShort)
    fun ubfm(dest: Register, a: Register, immediate: UShort)
    fun asr(dest: Register, a: Register, immediate: UShort)
    fun cmp(a: Register, immediate: UShort)
    fun cmn(a: Register, immediate: UShort)

    fun label(str: String)
    fun b(str: String)
    fun b(code: Code, str: String)
    fun bl(str: String)


    fun ret()

    fun nop()
    fun hlt()


}

/** Use the import `import dev.kason.charm.Register.*` to save your time!
 * Represents the registers available*/
@Suppress("EnumEntryName")
enum class Register {
    // contain only x0-x19, see https://edstem.org/us/courses/71821/discussion/6346381
    x0, x1, x2, x3, x4, x5,
    x6, x7, x8, x9, x10, x11, x12,
    x13, x14, x15, x16, x17, x18, x19;
}

class StrCharmBuilder : CharmBuilder {
    private val builder = StringBuilder()
    private fun m(op: String, dest: Register, location: Register, offset: Int = 0) {
        builder.appendLine("$op $dest, [$location, $offset] ")
    }

    private fun i1(op: String, res: Register, immediate: UShort) {
        builder.appendLine("$op $res, #0x${immediate.toHexString()}")
    }

    private fun i2(op: String, res: Register, lbl: String) {
        builder.appendLine("$op $res, $lbl")
    }

    private fun ri(op: String, dest: Register, a: Register, immediate: UShort) {
        if (immediate > (1 shl 12).toUInt()) {
            logger.warn { "RI-typed op immediate too big!" }
            logger.warn {
                "For the function call `$op($dest, $a, $immediate)`, the immediate (${immediate.toHexString()})," +
                        "\n is too big and will not properly fit when compiled into a 32-bit AArch64 instruction."
            }
        }
        builder.appendLine("$op $dest, $a, #0x${immediate.toHexString()}")
    }

    private fun rr(op: String, dest: Register, a: Register, b: Register) {
        builder.appendLine("$op $dest, $a, $b")
    }

    override fun ldur(dest: Register, location: Register, offset: Int) = m("ldur", dest, location, offset)
    override fun stur(dest: Register, location: Register, offset: Int) = m("stur", dest, location, offset)

    override fun movk(res: Register, immediate: UShort) = i1("mokv", res, immediate)
    override fun movkShift(res: Register, immediate: UShort, shift: Int) {
        builder.appendLine("movk $res, #0x${immediate.toHexString()}, lsl $shift")
    }

    override fun movz(res: Register, immediate: UShort) = i1("movz", res, immediate)

    override fun adrp(res: Register, lbl: String) = i2("adrp", res, lbl)

    override fun adds(dest: Register, a: Register, b: Register) = rr("adds", dest, a, b)
    override fun subs(dest: Register, a: Register, b: Register) = rr("subs", dest, a, b)
    override fun mvn(dest: Register, a: Register, b: Register) = rr("mvn", dest, a, b)
    override fun orr(dest: Register, a: Register, b: Register) = rr("orr", dest, a, b)
    override fun eor(dest: Register, a: Register, b: Register) = rr("eor", dest, a, b)
    override fun ands(dest: Register, a: Register, b: Register) = rr("ands", dest, a, b)
    override fun tst(dest: Register, a: Register, b: Register) = rr("tst", dest, a, b)

    fun cmpCmn(n: String, res: Register, immediate: UShort) {
        builder.appendLine("$n $res, #0x${immediate.toHexString()}")
    }

    fun cmpCmn(n: String, res: Register, res2: Register) {
        builder.appendLine("$n $res, $res2")
    }

    override fun cmp(a: Register, b: Register) = cmpCmn("cmp", a, b)
    override fun cmp(a: Register, immediate: UShort) = cmpCmn("cmp", a, immediate)
    override fun cmn(a: Register, b: Register) = cmpCmn("cmn", a, b)
    override fun cmn(a: Register, immediate: UShort) = cmpCmn("cmn", a, immediate)
    override fun add(dest: Register, a: Register, immediate: UShort) = ri("add", dest, a, immediate)
    override fun sub(dest: Register, a: Register, immediate: UShort) = ri("sub", dest, a, immediate)
    override fun lsl(dest: Register, a: Register, immediate: UShort) = ri("lsl", dest, a, immediate)
    override fun lsr(dest: Register, a: Register, immediate: UShort) = ri("lsr", dest, a, immediate)
    override fun ubfm(dest: Register, a: Register, immediate: UShort) = ri("ubfm", dest, a, immediate)
    override fun asr(dest: Register, a: Register, immediate: UShort) = ri("asr", dest, a, immediate)
    override fun label(str: String) {
        builder.appendLine("$str:")
    }

    override fun b(str: String) {
        builder.appendLine("b $str")
    }

    override fun b(code: Code, str: String) {
        builder.appendLine("b.$code $str")
    }

    override fun bl(str: String) {
        builder.appendLine("bl $str")
    }

    override fun ret() {
        builder.appendLine("ret")
    }

    override fun nop() {
        builder.appendLine("nop")
    }

    override fun hlt() {
        builder.appendLine("hlt")
    }

    override fun toString(): String = builder.toString()
}

/** Helper function to run charm code: simply do
 * ```kt
 * outputBuilder {
 *      // kt charm code here
 * }
 * ``` */
fun outputBuilder(block: CharmBuilder.() -> Unit) {
    println(StrCharmBuilder().apply(block).toString())
}

fun CharmBuilder.moveConstTo(dest: Register, bits: ULong) {
    // bit mask sections of the long
    repeat(4) {
        movkShift(dest, (bits shr (it * 16)).toUShort(), it * 16)
    }
}