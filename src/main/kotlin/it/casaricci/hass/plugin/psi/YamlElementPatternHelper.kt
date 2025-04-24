package it.casaricci.hass.plugin.psi

import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import org.jetbrains.yaml.YAMLLanguage
import org.jetbrains.yaml.YAMLTokenTypes
import org.jetbrains.yaml.psi.YAMLDocument
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequence
import org.jetbrains.yaml.psi.YAMLSequenceItem

object YamlElementPatternHelper {
    fun getScalarKeyWithRootParentKey(rootKeyName: String): PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_KEY)
            .withParent(
                PlatformPatterns.psiElement(YAMLKeyValue::class.java)
                    .withParent(
                        PlatformPatterns.psiElement(YAMLMapping::class.java)
                            .withParent(
                                PlatformPatterns.psiElement(YAMLKeyValue::class.java)
                                    .withName(rootKeyName)
                                    .withParent(
                                        PlatformPatterns.psiElement(YAMLMapping::class.java)
                                            .withParent(
                                                PlatformPatterns.psiElement(
                                                    YAMLDocument::class.java
                                                )
                                            )
                                    )
                            )
                    )
            )
            .withLanguage(YAMLLanguage.INSTANCE)

    fun getSingleLineScalarKey(vararg keyName: String?): ElementPattern<PsiElement> {
        // key: | and key: "quote" is valid here
        // getKeyPattern
        return PlatformPatterns.or(
            PlatformPatterns.psiElement(YAMLTokenTypes.TEXT)
                .withParent(
                    PlatformPatterns.psiElement(YAMLScalar::class.java)
                        .withParent(
                            PlatformPatterns.psiElement(YAMLKeyValue::class.java)
                                .withName(PlatformPatterns.string().oneOf(*keyName))
                        )
                )
                .withLanguage(YAMLLanguage.INSTANCE),
            PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_DSTRING)
                .withParent(
                    PlatformPatterns.psiElement(YAMLScalar::class.java)
                        .withParent(
                            PlatformPatterns.psiElement(YAMLKeyValue::class.java)
                                .withName(PlatformPatterns.string().oneOf(*keyName))
                        )
                )
                .withLanguage(YAMLLanguage.INSTANCE),
            PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_STRING)
                .withParent(
                    PlatformPatterns.psiElement(YAMLScalar::class.java)
                        .withParent(
                            PlatformPatterns.psiElement(YAMLKeyValue::class.java)
                                .withName(PlatformPatterns.string().oneOf(*keyName))
                        )
                )
                .withLanguage(YAMLLanguage.INSTANCE),
        )
    }

    fun getScalarValueWithTagPrefix(prefix: String): ElementPattern<PsiElement> =
        PlatformPatterns.or(
            PlatformPatterns.psiElement(YAMLTokenTypes.TEXT)
                .afterLeaf(prefix)
                .withLanguage(YAMLLanguage.INSTANCE),
            PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_DSTRING)
                .afterLeaf(prefix)
                .withLanguage(YAMLLanguage.INSTANCE),
            PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_STRING)
                .afterLeaf(prefix)
                .withLanguage(YAMLLanguage.INSTANCE),
        )

    fun getSingleLineScalarParentKey(vararg keyName: String?): ElementPattern<PsiElement> {
        // key: | and key: "quote" is valid here
        // getKeyPattern
        return PlatformPatterns.or(
            PlatformPatterns.psiElement(YAMLTokenTypes.TEXT)
                .withParent(
                    PlatformPatterns.psiElement(YAMLScalar::class.java)
                        .withParent(
                            PlatformPatterns.psiElement(YAMLSequenceItem::class.java)
                                .withParent(
                                    PlatformPatterns.psiElement(YAMLSequence::class.java)
                                        .withParent(
                                            PlatformPatterns.psiElement(YAMLKeyValue::class.java)
                                                .withName(PlatformPatterns.string().oneOf(*keyName))
                                        )
                                )
                        )
                )
                .withLanguage(YAMLLanguage.INSTANCE),
            PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_DSTRING)
                .withParent(
                    PlatformPatterns.psiElement(YAMLScalar::class.java)
                        .withParent(
                            PlatformPatterns.psiElement(YAMLSequenceItem::class.java)
                                .withParent(
                                    PlatformPatterns.psiElement(YAMLSequence::class.java)
                                        .withParent(
                                            PlatformPatterns.psiElement(YAMLKeyValue::class.java)
                                                .withName(PlatformPatterns.string().oneOf(*keyName))
                                        )
                                )
                        )
                )
                .withLanguage(YAMLLanguage.INSTANCE),
            PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_STRING)
                .withParent(
                    PlatformPatterns.psiElement(YAMLScalar::class.java)
                        .withParent(
                            PlatformPatterns.psiElement(YAMLSequenceItem::class.java)
                                .withParent(
                                    PlatformPatterns.psiElement(YAMLSequence::class.java)
                                        .withParent(
                                            PlatformPatterns.psiElement(YAMLKeyValue::class.java)
                                                .withName(PlatformPatterns.string().oneOf(*keyName))
                                        )
                                )
                        )
                )
                .withLanguage(YAMLLanguage.INSTANCE),
        )
    }
}
