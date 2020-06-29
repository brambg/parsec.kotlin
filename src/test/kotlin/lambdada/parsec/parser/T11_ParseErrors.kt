package lambdada.parsec.parser

import lambdada.parsec.io.Reader
import lambdada.parsec.parser.Response.Accept
import lambdada.parsec.parser.Response.Reject
import org.junit.Test
import kotlin.test.fail

class T11_ParseErrors {

    @Test
    fun test1() {
        val parser = char('a').rep thenLeft eos()
        val result1 = run(parser, "aaaaaaaa")
        assert(result1.isSuccess())
        println(result1)
    }

    @Test
    fun test2() {
        val parser = char('a').rep thenLeft eos()
        val result2 = run(parser, "abba")
        assert(!result2.isSuccess())
        println(result2)
    }

    @Test
    fun test3() {
        val parser = char('a').rep thenLeft eos()
        val result3 = run(tag("many-a", parser), "abba")
        assert(!result3.isSuccess())
        assert(result3 is Reject)
        println(result3)
    }

    @Test
    fun test4() {
        val p4 = tag("just-many-a",
                (tag("many-a", char('a').rep) thenLeft eos()))
        val result4 = run(p4, "abba")
        assert(!result4.isSuccess())
        assert(result4 is Reject)
        println(result4)
    }

    @Test
    fun test5() {
        val p5 = (tag("many-a", char('a').rep) thenLeft tag("tagged-eos", eos()))
        val result5 = run(p5, "abba")
        assert(!result5.isSuccess())
        println(result5)
        assert(result5 is Reject)
    }

    @Test
    fun test_example_9_5_3() {
        val spaces = string(" ").rep
        val p1 = scope("magic spell") { reader: Reader<Char> ->
            (string("abra") then spaces then string("cadabra")).invoke(reader)
        }
        val p2 = scope("gibberish") { reader: Reader<Char> ->
            (string("abba") then spaces then string("babba")).invoke(reader)
        }
        val p = p1 or p2

        val result = run(p, "abra cAdabra")
        println(result)
        assert(result is Reject)
        val reject = result as Reject
        with(reject.parseError.stack[0]) {
            assert(location.position == 0)
            assert(message == "magic spell") // relevant scope
        }
        with(reject.parseError.stack[1]) {
            assert(location.position == 7)
        }
    }

    @Test
    fun test_context_sensitive_parsing() {
        val tagname = scope("tagname", (charIn('a'..'z').optrep))
        val opentag = scope("openTag", (char('[') then tagname then char('>')))
        val closetag = scope("closeTag", char('<') then tagname then char(']'))
        val text = scope("text", not(charIn("[]<>")).optrep)
        val range = scope("range", opentag then text then closetag)

        assertParses(tagname, "tag")
        assertParses(opentag, "[tag>")
        assertParses(closetag, "<tag]")
        assertParses(text, "Ave! Lorem ipsum dolor pecunia non olet.")
        assertParses(range, "[a>Kermit is green<a]")
    }

    private fun assertParses(parser: Parser<Char, Any>, input: String) {
        when (val result = run(parser, input)) {
            is Accept -> {
            }
            is Reject -> {
                fail(stackTrace(result))
            }
        }
    }

    private fun stackTrace(reject: Reject<Char, Any>): String {
        val rstack = reject.parseError.stack.asReversed()
        val error = rstack[0]
        val errorString = StringBuilder()
        errorString.append("""Parse error: "${error.message}" at ${error.location.position}""").append("\n")
        if (rstack.size > 1) {
            for (i in 1..rstack.lastIndex) {
                errorString.append("""    in scope "${rstack[i].message}" starting at ${rstack[i].location.position}""").append("\n")
            }
        }
        return errorString.toString()
    }
}

