package it.casaricci.hass.plugin.completion

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase
import org.junit.Test

class EntityCompletionTest : LightPlatformCodeInsightFixture4TestCase() {

    override fun getTestDataPath(): String = "src/test/resources/completion/entity"

    @Test
    fun testLocalCompletion() {
        myFixture.configureByFiles("configuration.yaml", "scripts.yaml")
        myFixture.complete(CompletionType.BASIC)
        val lookupElementStrings = myFixture.lookupElementStrings
        assertNotNull(lookupElementStrings)
        assertSameElements(
            lookupElementStrings!!,
            "script.script_test1",
            "script.do_some_action",
            "script.do_some_other_action"
        )
    }

    @Test
    fun testRemoteCompletion() {
        // TODO we need somehow to inject states.json into systemDir
    }

}
