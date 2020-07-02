package lambdada.parsec.parser

import arrow.core.lastOrNone
import arrow.optics.extensions.list.cons.cons
import lambdada.parsec.io.Reader
import lambdada.parsec.utils.Location

fun <A> run(p: Parser<Char, A>, input: String): Response<Char, A> =
        p.invoke(Reader.string(input))

fun <I, A> scope(msg: String, pa: Parser<I, A>): Parser<I, A> =
        { reader ->
            pa.invoke(reader).mapError { pe -> pe.push(reader.location(), msg) }
        }

fun <I, A> tag(message: String, p: Parser<I, A>): Parser<I, A> =
        { reader ->
            p.invoke(reader).mapError { it.tag(message) }
        }

fun <I, A> Response.Reject<I, A>.stackTrace(): String {
    val errorStack = this.parseError.stack.asReversed()
    val error = errorStack[0]
    val errorString = StringBuilder()
    errorString.append("""Parse error: "${error.message}" at ${error.location.position}""").append("\n")
    if (errorStack.size > 1) {
        for (i in 1..errorStack.lastIndex) {
            errorString.append("""    in scope "${errorStack[i].message}" starting at ${errorStack[i].location.position}""").append("\n")
        }
    }
    return errorString.toString()
}

private fun <I, A> Response<I, A>.mapError(f: (ParseError) -> ParseError): Response<I, A> =
        this.fold(
                { this },
                { Response.Reject(f((this as Response.Reject).parseError), this.consumed) }
        )

private fun ParseError.push(loc: Location, msg: String): ParseError =
        this.copy(stack = ErrorLocation(loc, msg) cons this.stack)

private fun ParseError.tag(msg: String): ParseError {
    val latest = this.stack.lastOrNone()
    val latestLocation = latest.map { it.location }
    return ParseError(latestLocation.map { ErrorLocation(it, msg) }.toList())
}

internal fun Location.toError(msg: String) =
        ParseError(listOf(ErrorLocation(this, msg)))

