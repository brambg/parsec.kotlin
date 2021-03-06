package lambdada.parsec.parser

import lambdada.parsec.extension.stringsToString

//
// Characters parser
//

fun string(s: String): Parser<Char, String> =
        s.fold(returns<Char, Unit>(Unit)) { a, c -> a thenLeft char(c) } map { s }

fun delimitedString(): Parser<Char, String> = char('"') thenRight
        (`try`(string("\\\"")) or (not(char('"')).map(Char::toString))).optrep thenLeft
        char('"') map { it.stringsToString() }
