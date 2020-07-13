package lambdada.parsec.parser

import lambdada.parsec.io.Reader
import lambdada.parsec.parser.Response.Accept
import lambdada.parsec.parser.Response.Reject
import lambdada.parsec.parser.T11_ParseErrors.Token.*
import org.junit.Test
import kotlin.test.assertEquals
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

    sealed class Token {
        data class OpenTagToken(val name: String) : Token()
        data class CloseTagToken(val name: String) : Token()
        data class TextToken(val content: String) : Token()
    }

    @Test
    fun test_context_sensitive_parsing() {
        val anyTagName = scope("anyTagName", (charIn("abcdefghijklmnopqrstuvwxyz_").optrep))
                .mapToString()
        assertParsesWithResultValue(
                parser = anyTagName,
                input = "tag",
                expectedValue = "tag"
        )

        val anyOpenTag = scope("anyOpenTag", (char('[') thenRight anyTagName thenLeft char('>')))
                .map { OpenTagToken(it) }
        assertParsesWithResultValue(
                parser = anyOpenTag,
                input = "[tag>",
                expectedValue = OpenTagToken("tag")
        )

        val anyCloseTag = scope("anyCloseTag", char('<') thenRight anyTagName thenLeft char(']'))
                .map { CloseTagToken(it) }
        assertParsesWithResultValue(
                parser = anyCloseTag,
                input = "<tag]",
                expectedValue = CloseTagToken("tag")
        )

        val anyText = scope("anyText", not(charIn("[]<>")).optrep)
                .map { TextToken(it.joinToString("")) }
        assertParsesWithResultValue(
                parser = anyText,
                input = "Ave! Lorem ipsum dolor pecunia non olet.",
                expectedValue = TextToken("Ave! Lorem ipsum dolor pecunia non olet.")
        )

        val range = scope("anyRange", anyOpenTag then anyText then anyCloseTag)
                .map { it.flattenToList() }
        assertParsesWithResultValue(
                parser = range,
                input = "[a>Kermit is green<not_a]",
                expectedValue = listOf(OpenTagToken("a"), TextToken("Kermit is green"), CloseTagToken("not_a"))
        )

        val openThenClose = scope(
                "openThenClose",
                anyOpenTag.flatMap { openTagToken ->
                    closeTagParser(openTagToken.name)
                            .map { listOf(openTagToken, it) }
                }
        )
        assertParsesWithResultValue(
                parser = openThenClose,
                input = "[a><a]",
                expectedValue = listOf(OpenTagToken("a"), CloseTagToken("a"))
        )
        assertParsesWithResultValue(
                parser = openThenClose,
                input = "[b><b]",
                expectedValue = listOf(OpenTagToken("b"), CloseTagToken("b"))
        )
        assertParsingFailsWithStackTrace(
                parser = openThenClose,
                input = "[tag><not_tag]",
                expectedStackTrace = """
                    |Parse error: "unexpected token 'n'" at 7
                    |    in scope "CloseTag(tag)" starting at 5
                    |    in scope "openThenClose" starting at 0
                    |""".trimMargin()
        )

        val openTextClose = scope(
                "openTextClose",
                anyOpenTag.flatMap { openTagToken ->
                    (anyText then closeTagParser(openTagToken.name))
                            .map { listOf(openTagToken, it.first, it.second) }
                }
        )
        assertParsesWithResultValue(
                parser = openTextClose,
                input = "[a>text<a]",
                expectedValue = listOf(OpenTagToken("a"), TextToken("text"), CloseTagToken("a"))
        )
    }

    @Test
    fun test_correct_hierarchical_mixed_content() {
        assertParsesWithResultValue(
                parser = hierarchicalMixedContent(),
                input = "[a>[p>Alea [w>acta<w] est<p]<a]",
                expectedValue = listOf(
                        OpenTagToken("a"),
                        OpenTagToken("p"),
                        TextToken("Alea "),
                        OpenTagToken("w"),
                        TextToken("acta"),
                        CloseTagToken("w"),
                        TextToken(" est"),
                        CloseTagToken("p"),
                        CloseTagToken("a")
                )
        )
    }

    @Test
    fun test_incorrect_hierarchical_mixed_content() {
        assertParsingFailsWithStackTrace(
                parser = hierarchicalMixedContent(),
                input = "[a>[p>Pecunia [w>non<p] olet<w]<a]",
                expectedStackTrace = """
                    |Parse error: "unexpected token '['" at 15
                    |    in scope "CloseTag(p)" starting at 14
                    |""".trimMargin()
        )
    }

    private fun hierarchicalMixedContent(): Parser<Char, Any> = { reader ->
        (anyOpenTag.flatMap { openTagToken ->
            (hierarchicalMixedContent().rep then closeTagParser(openTagToken.name))
                    .map { listOf(openTagToken, it.first, it.second).flatten() }
        } or anyText).invoke(reader)
    }

    @Suppress("UNCHECKED_CAST")
    private fun List<Any>.flatten(): List<Token> {
        val list: MutableList<Token> = mutableListOf()
        for (any in this) {
            when (any) {
                is List<*> -> list += (any as List<Any>).flatten()
                is Token -> list += any
                else -> error("unexpected: $any")
            }
        }
        return list
    }

    private val anyTagName = scope("anyTagName", (charIn("abcdefghijklmnopqrstuvwxyz_").optrep))
            .mapToString()

    private val anyOpenTag = scope("anyOpenTag", (char('[') thenRight anyTagName thenLeft char('>')))
            .map { OpenTagToken(it) }

    private val anyText = scope("anyText", not(charIn("[]<>")).rep)
            .map { TextToken(it.joinToString("")) }

    private fun closeTagParser(tagname: String): Parser<Char, CloseTagToken> =
            { reader ->
                scope("CloseTag($tagname)",
                        char('<') then string(tagname) then char(']'))
                        .map { CloseTagToken(tagname) }
                        .invoke(reader)
            }

//    fun csCloseTagParser(context: Context): Parser<Char, CloseTagToken> =
//            { reader ->
//                val tagname = context.openTags.last()
//                scope("CloseTag($tagname)",
//                        char('<') then string(tagname) then char(']'))
//                        .map { CloseTagToken(tagname) }
//                        .invoke(reader)
//            }

//    class Context {
//        val openTags: MutableList<String> = mutableListOf()
//        val tokens: MutableList<Token> = mutableListOf()
//    }

//    @Test
//    fun parse_mixed_content_with_context_sensitive_reader() {
//
//        val tagml = anyOpenTag.flatMap { context ->
//            mixedContent(context).flatMap { c2 -> csCloseTagParser(c2) }
//        }
//
//    }

//    private fun mixedContent(context: Context): Parser<Char, Context> =
//            { reader ->
//                ((anyTextWithContext(context) or anyOpenTagWithContext(context) or closeTagWithContext(context))).invoke(reader)
//            }
//

    @Test
    fun test_flatten_pair_tree() {
        val tree = Pair(
                "eeny",
                Pair(
                        "meeny",
                        Pair(
                                Pair("moe", "larry"),
                                "john"
                        )
                )
        )
        assertEquals(
                listOf("eeny", "meeny", "moe", "larry", "john"),
                tree.flattenToList()
        )
    }

    private fun Parser<Char, List<Any>>.mapToString(): Parser<Char, String> =
            this.map { it.joinToString("") }

    private fun assertParsesWithResultValue(parser: Parser<Char, Any>, input: String, expectedValue: Any) {
        when (val result = run(parser, input)) {
            is Accept -> {
                println("value = ${result.value}")
                assertEquals(expectedValue, result.value)
            }
            is Reject -> {
                fail(result.stackTrace())
            }
        }
    }

    private fun assertParsingFailsWithStackTrace(parser: Parser<Char, Any>, input: String, expectedStackTrace: String) {
        when (val result = run(parser, input)) {
            is Accept -> {
                fail("expected parsing to fail, but got $result")
            }
            is Reject -> {
                assertEquals(expectedStackTrace, result.stackTrace())
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun Pair<Any, Any>.flattenToList(): List<Any> {
        val list = mutableListOf<Any>()
        if (this.first is Pair<*, *>) {
            list.addAll((this.first as Pair<Any, Any>).flattenToList())
        } else {
            list.add(this.first)
        }
        if (this.second is Pair<*, *>) {
            list.addAll((this.second as Pair<Any, Any>).flattenToList())
        } else {
            list.add(this.second)
        }
        return list
    }

}

