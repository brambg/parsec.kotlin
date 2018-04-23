package lambdada.parsec.parser

import lambdada.parsec.io.Readers
import org.junit.Assert
import org.junit.Test

class T02_ElementParser {

    @Test
    fun shouldAnyParserReturnsAccept() {
        val parser = any

        Assert.assertEquals(parser(Readers.fromString("a")).fold({ it.value == 'a' && it.consumed }, { false }), true)
    }

    @Test
    fun shouldAnyParserReturnsReject() {
        val parser = any

        Assert.assertEquals(parser(Readers.fromString("")).fold({ false }, { true }), true)
    }

    @Test
    fun shouldEOSParserReturnsAccept() {
        val parser = eos

        Assert.assertEquals(parser(Readers.fromString("")).fold({ true }, { false }), true)
    }

    @Test
    fun shouldEOSParserReturnsReject() {
        val parser = eos

        Assert.assertEquals(parser(Readers.fromString("a")).fold({ false }, { true }), true)
    }

    @Test
    fun shouldChoiceParserReturnsReject() {
        val parser = ((any then any map { it.first }) or any) then eos

        Assert.assertEquals(parser(Readers.fromString("a")).fold({ false }, { true }), true)
    }

    @Test
    fun shouldChoiceWithBacktrackParserReturnsAccept() {
        val parser = (doTry(any then any map { it.first }) or any) then eos

        Assert.assertEquals(parser(Readers.fromString("a")).fold({ true }, { false }), true)
    }

    @Test
    fun shouldSatisfyParserReturnsAccept() {
        val parser = any
        Assert.assertEquals(parser(Readers.fromString("a")).fold({ it.value == 'a' && it.consumed }, { false }), true)
    }

    @Test
    fun shouldNotSatisfyOrAnyParserReturnsAccept() {
        val parser = doTry(any satisfy { it == 'a' }) or any

        Assert.assertEquals(parser(Readers.fromString("b")).fold({ it.value == 'b' && it.consumed }, { false }), true)
    }

    @Test
    fun shouldNotCharParserReturnsAccept() {
        val parser = not(char('a'))

        Assert.assertEquals(parser(Readers.fromString("b")).fold({ it.value == 'b' && it.consumed }, { false }), true)
    }

    @Test
    fun shouldNotCharParserReturnsReject() {
        val parser = not(char('a'))

        Assert.assertEquals(parser(Readers.fromString("a")).fold({ false }, { true }), true)
    }
}