package com.github.stanislaupalyn.intellijgitplugin

import com.intellij.openapi.components.service
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.github.stanislaupalyn.intellijgitplugin.services.RenameCurrentCommitService

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class MyPluginTest : BasePlatformTestCase() {

    fun testProjectService() {
        val projectService = project.service<RenameCurrentCommitService>()

        // TODO: Test with mock repositories
    }
}
