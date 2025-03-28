package dev.kason.charm

import dev.kason.charm.Register.*
import dev.kason.charm.Code.*

fun hammingDistance() = outputBuilder {
    eor(x0, x1, x0)
    val values = listOf<ULong>(
        0x5555555555555555u, 0x3333333333333333u, 0x0f0f0f0f0f0f0f0fu,
        0x00ff00ff00ff00ffu, 0x0000ffff0000ffffu, 0x00000000ffffffffu
    )
    for ((index, value) in values.withIndex()) {
        moveConstTo(x3, value)
        ands(x1, x0, x3)
        lsr(x2, x0, (1 shl index).toUShort())
        ands(x2, x2, x3)
        adds(x0, x1, x2)
    }
}

fun transpose() = outputBuilder {
    repeat(3) {
        outerPS()
    }
}

val data = listOf(
    arrayOf<ULong>(0x00000000ffff0000u, 0xffff00000000ffffu, 16u),
    arrayOf<ULong>(0x0000ff000000ff00u, 0xff0000ffff0000ffu, 8u),
    arrayOf<ULong>(0x00f000f000f000f0u, 0xf00ff00ff00ff00fu, 4u),
    arrayOf<ULong>(0x0c0c0c0c0c0c0c0cu, 0xc3c3c3c3c3c3c3c3u, 2u),
    arrayOf<ULong>(0x2222222222222222u, 0x9999999999999999u, 1u),
)

fun CharmBuilder.outerPS() {
    for ((m1, m2, rawS) in data) {
        val s = rawS.toUShort()
        moveConstTo(x4, m1)
        moveConstTo(x5, m2)
        ands(x3, x0, x4)
        lsl(x3, x3, s)
        lsr(x2, x0, s)
        ands(x2, x2, x4)
        ands(x1, x0, x5)
        adds(x0, x3, x2)
        adds(x0, x0, x1)
    }
}

fun changeCase() = outputBuilder {
    // x2 is moving pointer starting at x0
    movz(x2, 0u)
    adds(x2, x0, x2)

    // x11 = first 56 bits
    // x10 = last 8 bits
    moveConstTo(x11, ((1 shl 8) - 1).toULong().inv())
    moveConstTo(x10, ((1 shl 8) - 1).toULong())
    label("loop")
    // x3 = *c
    ldur(x3, x2)
    ands(x3, x10, x3)

    cmp(x3, 0u)
    b(eq, "finish")

    cmp(x1, 0u)
    b(ne, "try_upper")
    cmp(x3, 'A'.code.toUShort())
    b(lt, "try_upper")
    cmp(x3, 'Z'.code.toUShort())
    b(gt, "try_upper")
    add(x3, x3, 32u)

    ldur(x5, x2)
    // load the other bytes
    ands(x5, x5, x11)
    orr(x3, x3, x5)
    stur(x3, x2)

    label("try_upper")
    cmp(x1, 0u)
    b(eq, "loop_end")
    cmp(x3, 'a'.code.toUShort())
    b(lt, "loop_end")
    cmp(x3, 'z'.code.toUShort())
    b(gt, "loop_end")
    sub(x3, x3, 32u)

    ldur(x5, x2)
    ands(x5, x5, x11)
    orr(x3, x3, x5)
    stur(x3, x2)

    label("loop_end")
    add(x2, x2, 1u)
    b("loop")
    label("finish")
    ret()
}

fun treeDepth() = outputBuilder {

}

fun main() = changeCase()