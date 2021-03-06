package dk.thrane.compiler.ast

import java.util.regex.Pattern

enum class Tokens {
    T_MULTIPLICATION("\\*"),
    T_DIVISION("/"),
    T_ADD("\\+"),
    T_SUBTRACTION("-"),
    T_LEFT_PARENTHESIS("\\("),
    T_RIGHT_PARENTHESIS("\\)"),
    T_ASSIGNMENT("="),
    T_COMPARE_EQUALS("=="),
    T_COMPARE_NOT_EQUALS("!="),
    T_COMPARE_LESS_THAN("<"),
    T_COMPARE_GREATER_THAN(">"),
    T_COMPARE_LESS_THAN_OR_EQUALS("<="),
    T_COMPARE_GREATER_THAN_OR_EQUALS(">="),
    T_LEFT_CURLY_BRACE("\\{"),
    T_RIGHT_CURLY_BRACE("\\}"),
    T_PERCENT_SIGN("%"),
    T_BANG("!"),
    T_OR("||"),
    T_VERTICAL_BAR("|"),
    T_COLON(":"),
    T_SEMI_COLON(";"),
    T_DOT("\\."),
    T_COMMA(","),
    T_LEFT_SQUARE_BRACE("\\["),
    T_RIGHT_SQUARE_BRACE("\\]"),
    T_AND("&&"),
    T_FUNCTION("func"),
    T_INT("int"),
    T_CHAR("char"),
    T_BOOL("bool"),
    T_ARRAY("array of"),
    T_RECORD("record of"),
    T_END("end"),
    T_VAR("var"),
    T_RETURN("return"),
    T_WRITE("write"),
    T_ALLOC("alloc"),
    T_IF("if"),
    T_THEN("then"),
    T_WHILE("while"),
    T_DO("do"),
    T_LENGTH("of length"),
    T_ELSE("else"),
    T_TRUE("true"),
    T_FALSE("false"),
    T_NULL("null"),
    T_TYPE("type"),
    T_NUM("0|([1-9][0-9]*)"),
    T_ID("[a-zA-Z_][a-zA-Z0-9_]*"),
    T_WHITESPACE("[ \\n\\t]+");

    var regex: Pattern? = null

    constructor() {}

    constructor(regex: String) {
        this.regex = Pattern.compile(regex)
    }

    companion object {
        fun optionallyConsume(type: Tokens, cursor: Cursor): Token? {
            T_WHITESPACE.consume(cursor)
            return type.consume(cursor)
        }

        fun consume(type: Tokens, cursor: Cursor): Token {
            return optionallyConsume(type, cursor) ?: throw IllegalStateException("Syntax error at " +
                    "'${cursor.remainingString}' expected token $type!")
        }

        fun peek(cursor: Cursor, count: Int): List<Token> {
            val start = cursor.idx
            val tokens = (1..count).map { nextToken(cursor) }
            cursor.idx = start
            return tokens
        }

        fun nextToken(cursor: Cursor, eat: Boolean = true): Token {
            T_WHITESPACE.consume(cursor)
            for (type in values()) {
                var token = type.consume(cursor, eat)
                if (token != null) {
                    return token
                }
            }
            throw IllegalStateException("Unexpected end of file at ${cursor.remainingString}")
        }

        private fun consumeRegex(cursor: Cursor, regex: Pattern): String? {
            val source = cursor.remainingString
            var found: Boolean = false
            var match: String? = null
            for (i in 1..source.length) {
                val potentialToken = source.substring(0, i)
                if (regex.matcher(potentialToken).matches()) {
                    found = true
                    match = potentialToken
                } else if (found) {
                    return match
                }
            }
            return match
        }

        private fun consumeRegexToken(type: Tokens, cursor: Cursor, pattern: Pattern, eat: Boolean = true): Token? {
            var token = consumeRegex(cursor, pattern)
            if (token != null) {
                if (eat) {
                    cursor.advance(token.length)
                }
                return Token(type, token)
            } else {
                return null
            }
        }
    }

    private fun consume(cursor: Cursor, eat: Boolean = true): Token? {
        if (regex != null) {
            return consumeRegexToken(this, cursor, regex as Pattern, eat)
        }
        return null
    }

}