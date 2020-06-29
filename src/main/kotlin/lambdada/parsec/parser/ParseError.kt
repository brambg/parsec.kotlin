package lambdada.parsec.parser

import lambdada.parsec.utils.Location

data class ParseError(val stack: List<ErrorLocation>)

data class ErrorLocation(val location: Location, val message: String)
