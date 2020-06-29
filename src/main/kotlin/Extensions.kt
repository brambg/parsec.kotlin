package lambdada.parsec.parser

import arrow.core.lastOrNone
import arrow.optics.extensions.list.cons.cons
import lambdada.parsec.io.Reader
import lambdada.parsec.utils.Location

fun <A> run(p: Parser<Char, A>, input: String): Response<Char, A> =
        p.invoke(Reader.string(input))

fun <I, A> tag(message: String, p: Parser<I, A>): Parser<I, A> =
        { reader ->
            p.invoke(reader)
                    .mapError {
                        it.tag(message)
                    }
        }

private fun <I, A> Response<I, A>.mapError(f: (ParseError) -> ParseError): Response<I, A> =
        this.fold(
                { this },
                { Response.Reject(f((this as Response.Reject).parseError), this.consumed) }
        )

private fun ParseError.tag(msg: String): ParseError {
    val latest = this.stack.lastOrNone()
    val latestLocation = latest.map { it.location }
    return ParseError(latestLocation.map { ErrorLocation(it, msg) }.toList())
}

private fun ParseError.push(loc: Location, msg: String): ParseError =
        this.copy(stack = ErrorLocation(loc, msg) cons this.stack)

fun <I, A> scope(msg: String, pa: Parser<I, A>): Parser<I, A> =
        { reader ->
            pa.invoke(reader).mapError { pe -> pe.push(reader.location(), msg) }
        }

internal fun Location.toError(msg: String) =
        ParseError(listOf(ErrorLocation(this, msg)))

