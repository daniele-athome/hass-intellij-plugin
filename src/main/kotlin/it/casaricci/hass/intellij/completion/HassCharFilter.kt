package it.casaricci.hass.intellij.completion

import com.intellij.codeInsight.lookup.CharFilter
import com.intellij.codeInsight.lookup.Lookup
import org.jetbrains.yaml.YAMLLanguage
import org.jetbrains.yaml.psi.YAMLScalar

class HassCharFilter : CharFilter() {

    override fun acceptChar(c: Char, prefixLength: Int, lookup: Lookup?): Result? {
        if (lookup?.psiFile?.language == YAMLLanguage.INSTANCE && lookup?.psiElement?.nextSibling is YAMLScalar) {
            // TODO make this decision for other characters in a YAML string literal
            // "." is for entity ID: "sensor.name"
            // ":" is for icon names: "mdi:home"
            if (c == '.' || c == ':') {
                return Result.ADD_TO_PREFIX
            }
        }
        return null
    }

}
