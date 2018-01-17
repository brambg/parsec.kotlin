package lambdada.parsec.parser

import lambdada.parsec.io.Reader
import org.junit.Assert
import org.junit.Test

class BasicParserTest {

    @Test
    fun shouldReturnsParserReturnsAccept() {
        val parser = returns('a')

        Assert.assertEquals(parser.parse(Reader.new("")).fold({ it.value == 'a' }, { false }), true)
    }

    @Test
    fun shouldFailsParserReturnsError() {
        val parser = fails<Char>()

        Assert.assertEquals(parser.parse(Reader.new("")).fold({ false }, { true }), true)
    }

    @Test
    fun shouldMappedReturnsParserReturnsAccept() {
        val parser = returns('a') map { it == 'a' }

        Assert.assertEquals(parser.parse(Reader.new("")).fold({ it.value }, { false }), true)
    }

    @Test
    fun shouldFlapMappedReturnsParserReturnsAccept() {
        val parser = returns('a') flatMap { s -> returns(s + "b") } map { it == "ab" }

        Assert.assertEquals(parser.parse(Reader.new("")).fold({ it.value }, { false }), true)
    }

    @Test
    fun shouldFlapMappedReturnsParserReturnsError() {
        val parser = returns('a') flatMap { fails<Char>() }

        Assert.assertEquals(parser.parse(Reader.new("")).fold({ false }, { true }), true)
    }

    @Test
    fun shouldLazyReturnsParserReturnsAccept() {
        val parser = lazy { returns('a') }

        Assert.assertEquals(parser.parse(Reader.new("")).fold({ it.value == 'a' }, { false }), true)
    }

    @Test
    fun shouldLazyFailsParserReturnsError() {
        val parser = lazy { fails<Char>() }

        Assert.assertEquals(parser.parse(Reader.new("")).fold({ false }, { true }), true)
    }

    //
    // Flow
    //

}