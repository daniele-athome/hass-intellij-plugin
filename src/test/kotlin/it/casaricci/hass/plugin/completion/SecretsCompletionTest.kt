package it.casaricci.hass.plugin.completion

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase
import org.junit.Test

class SecretsCompletionTest : LightPlatformCodeInsightFixture4TestCase() {

    override fun getTestDataPath(): String = "src/test/resources/completion/secrets"

    @Test
    fun testCompletion() {
        myFixture.configureByFiles("configuration.yaml", "secrets.yaml")
        myFixture.complete(CompletionType.BASIC)
        val lookupElementStrings = myFixture.lookupElementStrings
        assertNotNull(lookupElementStrings)
        assertSameElements(lookupElementStrings!!, "latitude_home", "longitude_home")
    }

}
