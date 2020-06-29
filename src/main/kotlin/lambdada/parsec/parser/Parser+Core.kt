package lambdada.parsec.parser

import lambdada.parsec.parser.Response.Accept
import lambdada.parsec.parser.Response.Reject

//
// Basic parsers
//

fun <I, A> returns(v: A): Parser<I, A> = {
    Accept(v, it, false)
}

fun <I, A> fails(message: String = ""): Parser<I, A> = {
    Reject(it.location().toError(message), false)
}

//
// Element parser
//

fun <I> any(): Parser<I, I> =
        { reader ->
            reader.read()?.let { Accept(it.first, it.second, true) }
                    ?: Reject(reader.location().toError("any"), false)
        }

//
// Negation
//

fun <I> not(p: Parser<I, *>): Parser<I, I> =
        { reader ->
            p(reader).fold(
                    { Reject(reader.location().toError("not"), false) },
                    { any<I>()(reader) }
            )
        }

//
// Lazy parser
//

fun <I, A> lazy(f: () -> Parser<I, A>): Parser<I, A> =
        { f()(it) }

//
// Backtracking
//

fun <I, A> `try`(p: Parser<I, A>): Parser<I, A> =
        { reader ->
            p(reader).fold(
                    { it },
                    { Reject(it.parseError, false) }
            )
        }

//
// Lookahead / Breaks ll(1) limitation
//

fun <I, A> lookahead(p: Parser<I, A>): Parser<I, A> =
        { reader ->
            p(reader).fold(
                    { Accept(it.value, reader, false) },
                    { Reject(it.parseError, false) }
            )
        }
