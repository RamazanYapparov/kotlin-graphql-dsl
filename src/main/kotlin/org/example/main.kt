package org.example

import kotlin.reflect.KProperty

fun main() {
    val m = mutation {
        deleteSlackChannel(id = "some id") {
            id
            name
            slackMap {
                key
            }
        }
    }
    println(prettify("mutation$m"))
    val q = query {
        slackClientId
        slackAuthApi
    }
    println(prettify("query$q"))
}

data class Entry<T>(val name: String, val arguments: Map<String, Any>, val selector: T) {
    override fun toString(): String {
        val args = arguments.map { (k, v) -> "$k: $v" }.joinToString(",")
        return """$name${if (arguments.isNotEmpty()) {
            "($args)"
        } else {
            ""
        }}${when (selector.toString()) {
            "kotlin.String" -> NEW_LINE
            "kotlin.Int" -> NEW_LINE
            else -> selector.toString()
        }}"""
    }
}

class SlackChannelInfo : BaseRequest() {
    val id: String by GraphQlField
    val name: String by GraphQlField

    fun slackMap(block: SlackMap.() -> Unit) {
        val res = SlackMap()
        block(res)
        addTo(Entry("slackMap", emptyMap(), res))
    }
}

class SlackMap : BaseRequest() {
    val key: String by GraphQlField
    val value: String by GraphQlField
}

abstract class BaseRequest {
    var data: List<Entry<out Any>> = listOf()

    object GraphQlField {
        operator fun getValue(thisRef: BaseRequest, property: KProperty<*>): String {
            thisRef.data += Entry(property.name, emptyMap(), property.returnType)
            return property.name
        }

    }

    protected fun <T : Any> addTo(d: Entry<T>) {
        data = data + d
    }

    override fun toString(): String {
        return "{${data.joinToString("")}}"
    }


}

class Query : BaseRequest() {
    val slackClientId: String by GraphQlField
    val slackAuthApi: String by GraphQlField
}

class Mutation : BaseRequest() {
    fun deleteSlackChannel(id: String, block: SlackChannelInfo.() -> Unit) {
        val res = SlackChannelInfo()
        block(res)
        addTo(Entry("deleteSlackChannel", mapOf("id" to "\"$id\""), res))
    }
}

fun query(block: Query.() -> Unit): String {
    val q = Query()
    block(q)
    return q.toString()
}

fun mutation(block: Mutation.() -> Unit): String {
    val m = Mutation()
    block(m)
    return m.toString()
}

const val NEW_LINE = "\n"

private fun prettify(str: String): String {
    var indentation = 0
    val result = StringBuilder()
    for (i in str.indices) {
        val currentChar = str[i]
        if (i == str.length - 1) {
            result.append(currentChar)
            break
        }
        val nextChar = str[i + 1]
        var postfix = ""
        when {
            currentChar == '{' || currentChar == '(' -> {
                indentation += 2
                postfix = NEW_LINE + " ".repeat(indentation)
            }
            nextChar == '{' || nextChar == '(' -> {
                postfix = " "
            }
            nextChar == '}' || nextChar == ')' -> {
                indentation -= 2
                postfix = (if (currentChar != '\n') NEW_LINE else "") + " ".repeat(indentation)
            }
            currentChar == '\n' -> {
                postfix = " ".repeat(indentation)
            }
            currentChar == ',' -> {
                postfix = NEW_LINE + " ".repeat(indentation)
            }
        }
        result.append("$currentChar$postfix")
    }
    return result.toString()
}