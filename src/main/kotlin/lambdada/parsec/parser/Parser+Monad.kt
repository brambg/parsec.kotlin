package lambdada.parsec.parser

import lambdada.parsec.extension.pipe
import lambdada.parsec.parser.Response.Accept
import lambdada.parsec.parser.Response.Reject

//
// Parser providing pseudo-Monadic ADT
//

infix fun <A, B> Parser<A>.map(f: (A) -> B): Parser<B> =
        Parser { input ->
            val a = this.parse(input)
            when (a) {
                is Reject -> Reject<B>(a.location, a.consumed)
                is Accept -> Accept(f(a.value), a.input, a.consumed)
            }
        }

fun <A> join(p: Parser<Parser<A>>): Parser<A> =
        Parser { input ->
            val a = p.parse(input)
            when (a) {
                is Reject -> Reject<A>(a.location, a.consumed)
                is Accept -> {
                    val b = a.value.parse(a.input)
                    when (b) {
                        is Reject -> Reject<A>(b.location, a.consumed || b.consumed)
                        is Accept -> Accept(b.value, b.input, a.consumed || b.consumed)
                    }
                }
            }
        }

infix fun <A, B> Parser<A>.flatMap(f: (A) -> Parser<B>): Parser<B> =
        join(this map f)

//
// Filtering
//

infix fun <A> Parser<A>.satisfy(p: (A) -> Boolean): Parser<A> =
        this flatMap {
            if (p(it)) {
                returns(it)
            } else {
                fails()
            }
        }

//
// Applicative
//

infix fun <A, B> Parser<A>.applicative(f: Parser<(A) -> B>): Parser<B> =
// this flatMap { v -> f map { it(v) } }
        f flatMap { f -> this map f }

//
// Kliesli
//

infix fun <A, B, C> ((A) -> Parser<B>).then(p2: (B) -> Parser<C>): (A) -> Parser<C> =
        this pipe { b -> b flatMap p2 }
