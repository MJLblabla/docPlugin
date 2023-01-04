package com.qncube.docbuider

import com.sun.xml.fastinfoset.util.StringArray
import java.util.function.Consumer

object DocLinkerUtils {

    fun checkLinker(name: String): String {
        return if (Doclet.links[name] == null) {
            var realName: String = ""
            Doclet.links.keys.forEach { s: String ->
                val ret: List<String> = s.split(".")
                val lastName = ret[ret.size - 1]
                if (lastName == name) {
                    realName = s
                    println(" $name  ->$realName")
                    return@forEach
                }
            }
            if (realName.isEmpty()) {
                name
            } else {
                "{{$realName}}"
            }
        } else {
            "{{$name}}"
        }
    }
}