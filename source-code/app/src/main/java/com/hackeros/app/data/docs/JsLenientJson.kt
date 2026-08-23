package com.hackeros.app.data.docs

/**
 * Converts a small but meaningfully common subset of JS object-literal syntax into strict JSON
 * text that `org.json.JSONObject`/`JSONArray` can parse - without embedding a JS engine.
 *
 * This exists because the HackerOS website's documentation data file
 * (`translations/hackeros-documentation.js`) is NOT uniform: the `pl` and `en` blocks are
 * written as plain JS object literals (unquoted keys, single-quoted strings, e.g.
 * `content:{introduction:{h2:'1. Wprowadzenie', ...}}`), while every other language block is
 * already strict, double-quoted JSON. [convert] handles both transparently in one pass:
 *  - Bareword object keys (`key:`) are wrapped in double quotes.
 *  - Single-quoted strings are re-emitted as double-quoted JSON strings, with internal escaping
 *    fixed up (`\'` -> `'`, unescaped/escaped `"` -> `\"`).
 *  - Already-double-quoted strings and already-quoted keys pass through unchanged (aside from
 *    the same escape normalization), so strict-JSON input is unaffected.
 *  - `//` line comments and `/* ... *‍/` block comments outside of strings are stripped (the
 *    source file ends with a `//` comment).
 *
 * This is a single-pass, string-aware tokenizer - not a full JS parser - so it does not support
 * arbitrary JS expressions, only object/array/string/number/boolean/null literals, which is
 * exactly what these data files contain.
 */
object JsLenientJson {

    fun convert(input: String): String {
        val sb = StringBuilder(input.length + 64)
        val n = input.length
        var i = 0

        // Strips a trailing comma (JS allows trailing commas before `}`/`]`; strict JSON does
        // not), so `{a:1,}` becomes `{"a":1}` instead of failing to parse.
        fun trimTrailingComma(builder: StringBuilder) {
            var end = builder.length
            while (end > 0 && builder[end - 1].isWhitespace()) end--
            if (end > 0 && builder[end - 1] == ',') {
                builder.setLength(end - 1)
            }
        }

        // Stack of open containers: true = object, false = array.
        val containerIsObject = ArrayDeque<Boolean>()
        // For each open object, whether we're currently expecting a KEY (true) or a VALUE (false).
        // Parallel stack to containerIsObject entries that are objects; we keep it simple by
        // using one combined stack of "expect key" flags indexed the same as containerIsObject.
        val expectKey = ArrayDeque<Boolean>()

        fun currentIsObject(): Boolean = containerIsObject.isNotEmpty() && containerIsObject.last()
        fun currentExpectsKey(): Boolean = expectKey.isNotEmpty() && expectKey.last()
        fun setExpectKey(value: Boolean) {
            if (expectKey.isNotEmpty()) {
                expectKey.removeLast()
                expectKey.addLast(value)
            }
        }

        fun skipWhitespaceAndComments() {
            while (i < n) {
                val c = input[i]
                if (c.isWhitespace()) { i++; continue }
                if (c == '/' && i + 1 < n && input[i + 1] == '/') {
                    while (i < n && input[i] != '\n') i++
                    continue
                }
                if (c == '/' && i + 1 < n && input[i + 1] == '*') {
                    i += 2
                    while (i + 1 < n && !(input[i] == '*' && input[i + 1] == '/')) i++
                    i = (i + 2).coerceAtMost(n)
                    continue
                }
                break
            }
        }

        while (i < n) {
            skipWhitespaceAndComments()
            if (i >= n) break
            val c = input[i]

            when {
                c == '{' -> {
                    sb.append('{'); i++
                    containerIsObject.addLast(true)
                    expectKey.addLast(true)
                }
                c == '}' -> {
                    trimTrailingComma(sb)
                    sb.append('}'); i++
                    if (containerIsObject.isNotEmpty()) containerIsObject.removeLast()
                    if (expectKey.isNotEmpty()) expectKey.removeLast()
                }
                c == '[' -> {
                    sb.append('['); i++
                    containerIsObject.addLast(false)
                    expectKey.addLast(false)
                }
                c == ']' -> {
                    trimTrailingComma(sb)
                    sb.append(']'); i++
                    if (containerIsObject.isNotEmpty()) containerIsObject.removeLast()
                    if (expectKey.isNotEmpty()) expectKey.removeLast()
                }
                c == ',' -> {
                    sb.append(','); i++
                    if (currentIsObject()) setExpectKey(true)
                }
                c == ':' -> {
                    sb.append(':'); i++
                    setExpectKey(false)
                }
                c == '\'' || c == '"' -> {
                    val quote = c
                    val wasKey = currentIsObject() && currentExpectsKey()
                    i++
                    val str = StringBuilder()
                    while (i < n && input[i] != quote) {
                        val ch = input[i]
                        if (ch == '\\' && i + 1 < n) {
                            val next = input[i + 1]
                            when {
                                quote == '\'' && next == '\'' -> { str.append('\''); i += 2 }
                                next == '"' -> { str.append("\\\""); i += 2 }
                                next == '\\' -> { str.append("\\\\"); i += 2 }
                                next == 'n' -> { str.append("\\n"); i += 2 }
                                next == 't' -> { str.append("\\t"); i += 2 }
                                next == 'r' -> { str.append("\\r"); i += 2 }
                                next == 'u' -> {
                                    str.append("\\u"); i += 2
                                    var k = 0
                                    while (k < 4 && i < n) { str.append(input[i]); i++; k++ }
                                }
                                else -> { str.append(next); i += 2 }
                            }
                        } else {
                            if (ch == '"') str.append("\\\"") else str.append(ch)
                            i++
                        }
                    }
                    if (i < n) i++ // consume closing quote
                    sb.append('"').append(str).append('"')
                    if (wasKey) setExpectKey(false)
                }
                c.isLetter() || c == '_' || c == '$' -> {
                    val start = i
                    while (i < n && (input[i].isLetterOrDigit() || input[i] == '_' || input[i] == '$')) i++
                    val word = input.substring(start, i)
                    val isKeyPos = currentIsObject() && currentExpectsKey()
                    if (isKeyPos) {
                        sb.append('"').append(word).append('"')
                        setExpectKey(false)
                    } else {
                        // true / false / null literal (or an unexpected bareword - passed through
                        // as-is; JSONObject will fail loudly on genuinely malformed input rather
                        // than silently produce wrong data).
                        sb.append(word)
                    }
                }
                else -> {
                    sb.append(c); i++
                }
            }
        }

        return sb.toString()
    }
}
