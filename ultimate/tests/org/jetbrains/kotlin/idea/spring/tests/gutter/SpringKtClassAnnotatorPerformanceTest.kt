/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.spring.tests.gutter

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.testFramework.PlatformTestUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.spring.tests.SpringKtLightHighlightingTestCase

class SpringKtClassAnnotatorPerformanceTest : SpringKtLightHighlightingTestCase() {

    private val beansCount = 50

    private val expectedGutters = beansCount * 2 + 4

    private fun createFileAndAnnotate(fileName: String, text: String, modificationText: (Long) -> String) {
        myFixture.addFileToProject(fileName, text)
        myFixture.configureByFile(fileName)
        configureFileSet(fileName)

        PlatformTestUtil.startPerformanceTest("do Highlighting and get gutters for $fileName", 3200) {
            myFixture.testHighlighting(fileName)
            val allGutters = myFixture.findAllGutters(fileName)
            TestCase.assertEquals(expectedGutters, allGutters.size)
        }.setup {
            val modCount = myFixture.psiManager.modificationTracker.outOfCodeBlockModificationCount
            WriteCommandAction.runWriteCommandAction(project) {
                val document = myFixture.editor.document
                document.insertString(myFixture.caretOffset, modificationText(modCount))
            }
            FileDocumentManager.getInstance().saveAllDocuments()
        }.attempts(5).assertTiming()

    }


    fun testAnnotateKt() {
        createFileAndAnnotate("Config.kt", """
                package pkg;

                @org.springframework.context.annotation.Configuration
                @org.springframework.context.annotation.ComponentScan
                open class Config {
                <caret>

               ${(0..beansCount).joinToString("\n") {
            """
                        @org.springframework.context.annotation.Bean
                        open fun localBean$it() = LocalBean$it()
                        """.trimIndent()
        }}
                }

                ${(0..beansCount).joinToString("\n") {
            "class LocalBean$it"
        }}

            """, { "fun foo$it(){}\n" })
    }

    fun testAnnotateJava() {
        createFileAndAnnotate("pkg/Config.java", """
        package pkg;

        @org.springframework.context.annotation.Configuration
        @org.springframework.context.annotation.ComponentScan
        public class Config {
        <caret>

       ${(0..beansCount).joinToString("\n") {
            """
                @org.springframework.context.annotation.Bean
                LocalBean$it localBean$it(){
                 return new LocalBean$it();
                }
                """.trimIndent()
        }}
        }

        ${(0..beansCount).joinToString("\n") {
            "class LocalBean$it {}"
        }}

    """, { "public void foo$it(){}\n" })
    }


}

class SpringKtClassAnnotatorPTest : SpringKtLightHighlightingTestCase() {
    fun testKtComponentScan() {
        val beansCount = 50

        myFixture.configureByText("Config.kt", """
        package pkg;

        @org.springframework.context.annotation.Configuration
        @org.springframework.context.annotation.ComponentScan
        open class Config {

       ${(0..beansCount).joinToString("\n") {
            """
                @org.springframework.context.annotation.Bean
                open fun localBean$it() = LocalBean$it()
                """.trimIndent()
        }}
        }

        ${(0..beansCount).joinToString("\n") {
            "class LocalBean$it"
        }}

    """)

        configureFileSet("Config.kt")


        myFixture.testHighlighting("Config.kt")

        val allGutters = myFixture.findAllGutters("Config.kt")
        println("count = " + allGutters.size)
        val guttersMapping = allGutters.map {
            (it as LineMarkerInfo.LineMarkerGutterIconRenderer<*>).lineMarkerInfo.let {
                it.element?.text.toString() to it.lineMarkerTooltip
            }
        }
        println("guttersMapping = " + guttersMapping)


    }

}