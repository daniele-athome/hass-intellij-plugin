package it.casaricci.hass.plugin.completion

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase
import com.intellij.testFramework.replaceService
import it.casaricci.hass.plugin.MockHassRemoteRepository
import it.casaricci.hass.plugin.services.HassRemoteRepository
import org.junit.Test

class EntityCompletionTest : LightPlatformCodeInsightFixture4TestCase() {
    override fun getTestDataPath(): String = "src/test/resources/completion/entity"

    @Test
    fun testLocalCompletion() {
        myFixture.configureByFiles("configuration-local.yaml", "scripts.yaml")
        myFixture.complete(CompletionType.BASIC)
        val lookupElementStrings = myFixture.lookupElementStrings
        assertNotNull(lookupElementStrings)
        assertSameElements(
            lookupElementStrings!!,
            "script.script_test1",
            "script.do_some_action",
            "script.do_some_other_action",
        )
    }

    @Test
    fun testRemoteCompletion() {
        // mock entity states
        myFixture.project.replaceService(
            HassRemoteRepository::class.java,
            MockHassRemoteRepository(project, "$testDataPath/states.json", null),
            testRootDisposable,
        )

        myFixture.configureByFiles("configuration-remote.yaml", "scripts.yaml")
        myFixture.complete(CompletionType.BASIC)
        val lookupElementStrings = myFixture.lookupElementStrings
        assertNotNull(lookupElementStrings)
        assertSameElements(
            lookupElementStrings!!,
            "script.script_test2",
            "script.do_some_action",
            "script.do_some_other_action",
            "scene.house_entrance",
        )
    }
}
