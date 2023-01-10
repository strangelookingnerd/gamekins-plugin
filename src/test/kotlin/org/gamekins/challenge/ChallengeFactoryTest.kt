/*
 * Copyright 2022 Gamekins contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gamekins.challenge

import hudson.FilePath
import hudson.model.Result
import hudson.model.TaskListener
import hudson.model.User
import hudson.tasks.Mailer.UserProperty
import org.gamekins.util.GitUtil
import org.gamekins.util.JacocoUtil
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beOfType
import io.mockk.*
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.revwalk.RevCommit
import org.gamekins.GamePublisherDescriptor
import org.gamekins.achievement.AchievementInitializer
import org.gamekins.event.EventHandler
import org.gamekins.file.SourceFileDetails
import org.gamekins.mutation.MutationDetails
import org.gamekins.mutation.MutationInfo
import org.gamekins.mutation.MutationResults
import org.gamekins.mutation.MutationUtils
import org.gamekins.util.JUnitUtil
import org.gamekins.util.Constants.Parameters
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.random.Random

class ChallengeFactoryTest : FeatureSpec({

    val user = mockkClass(User::class)
    val property = mockkClass(org.gamekins.GameUserProperty::class)
    val parameters = Parameters()
    val branch = "master"
    val listener = TaskListener.NULL
    val className = "Challenge"
    val path = mockkClass(FilePath::class)
    val shortFilePath = "src/main/java/io/jenkins/plugins/gamekins/challenge/$className.kt"
    val shortJacocoPath = "**/target/site/jacoco/"
    val shortJacocoCSVPath = "**/target/site/jacoco/csv"
    val mocoJSONPath = "**/target/site/moco/mutation/"
    val coverage = 0.0
    val testCount = 10
    lateinit var challenge : MutationTestChallenge
    lateinit var challenge1 : MutationTestChallenge
    lateinit var challenge2 : MutationTestChallenge
    lateinit var details : SourceFileDetails
    val newDetails = mockkClass(SourceFileDetails::class)

    val mutationDetails1 = MutationDetails(
        mapOf("className" to "io/jenkins/plugins/gamekins/challenge/Challenge", "methodName" to "foo1", "methodDescription" to "(Ljava/lang/Integer;)Ljava/lang/Integer;"),
        listOf(14),
        "AOR",
        "IADD-ISUB-51",
        "ABC.java", 51,
        "replace integer addition with integer subtraction",
        listOf("14"), mapOf("varName" to "numEntering"))
    val mutation1 = MutationInfo(mutationDetails1, "survived", -1547277781)

    val mutationDetails2 = MutationDetails(
        mapOf("className" to "io/jenkins/plugins/gamekins/challenge/Challenge", "methodName" to "foo", "methodDescription" to "(Ljava/lang/Integer;)Ljava/lang/Integer;"),
        listOf(22),
        "AOD",
        "IADD-S-56",
        "Feature.java", 56,
        "delete second operand after arithmetic operator integer addition",
        listOf("22"), mapOf())
    val mutation2 = MutationInfo(mutationDetails2, "killed", -1547277782)


    val mutationDetails3 = MutationDetails(
        mapOf("className" to "org/example/Feature", "methodName" to "foo", "methodDescription" to "(Ljava/lang/Integer;)Ljava/lang/Integer;"),
        listOf(30),
        "ROR",
        "IF_ICMPGT-IF_ICMPLT-50",
        "Hihi.java", 56,
        "replace less than or equal operator with greater than or equal operator",
        listOf("30"), mapOf())
    val mutation3 = MutationInfo(mutationDetails3, "survived", -1547277782)

    val entries = mapOf("io.jenkins.plugins.gamekins.challenge.Challenge" to setOf(mutation1, mutation2),
        "org.example.Feature" to setOf(mutation3))



    beforeTest {
        mockkStatic(AchievementInitializer::class)
        every { AchievementInitializer.initializeAchievements(any()) } returns listOf()
        GamePublisherDescriptor()
        every { path.remote } returns ""
        parameters.branch = branch
        parameters.projectName = "test-project"
        parameters.jacocoResultsPath = shortJacocoPath
        parameters.jacocoCSVPath = shortJacocoCSVPath
        parameters.mocoJSONPath = mocoJSONPath
        parameters.workspace = path
        mockkStatic(JacocoUtil::class)
        mockkStatic(JUnitUtil::class)
        mockkStatic(ChallengeFactory::class)
        val document = mockkClass(Document::class)
        mockkObject(MutationResults.Companion)
        every { user.getProperty(org.gamekins.GameUserProperty::class.java) } returns property
        every { user.fullName } returns "Philipp Straubinger"
        every { user.id } returns "id"
        val mailProperty = mockkClass(UserProperty::class)
        every { mailProperty.address } returns "philipp.straubinger@uni-passau.de"
        every { user.getProperty(UserProperty::class.java) } returns mailProperty
        every { property.getRejectedChallenges(any()) } returns CopyOnWriteArrayList()
        every { property.getStoredChallenges(any()) } returns CopyOnWriteArrayList()
        every { property.getGitNames() } returns CopyOnWriteArraySet(listOf("Philipp Straubinger"))
        every { JacocoUtil.calculateCurrentFilePath(any(), any()) } returns path
        every { JacocoUtil.getCoverageInPercentageFromJacoco(any(), any()) } returns coverage
        every { JacocoUtil.generateDocument(any()) } returns document
        every { JacocoUtil.calculateCoveredLines(any(), any()) } returns 0
        every { JUnitUtil.getTestCount(any(), any()) } returns testCount
        val commit = mockkClass(RevCommit::class)
        every { commit.name } returns "ef97erb"
        every { commit.authorIdent } returns PersonIdent("", "")
        every { path.act(ofType(GitUtil.HeadCommitCallable::class)) } returns commit
        every { path.act(ofType(JacocoUtil.FilesOfAllSubDirectoriesCallable::class)) } returns arrayListOf()
        every { path.remote } returns "/home/test/workspace"
        every { path.channel } returns null
        details = SourceFileDetails(parameters, shortFilePath, listener)
        challenge = MutationTestChallenge(mutation1, details, branch, "commitID", "snippet", "line")
        challenge1 = MutationTestChallenge(mutation2, details, branch, "commitID", "snippet", "")
        challenge2 = MutationTestChallenge(mutation3, details, branch, "commitID", "snippet", "line")
        mockkStatic(EventHandler::class)
        every { EventHandler.addEvent(any()) } returns Unit

        every { newDetails.coverage } returns 0.0
        every { newDetails.filesExists() } returns true
        every { newDetails.fileName } returns details.fileName
        every { newDetails.jacocoSourceFile } returns details.jacocoSourceFile
        every { newDetails.jacocoCSVFile } returns details.jacocoCSVFile
        every { newDetails.jacocoMethodFile } returns details.jacocoMethodFile
        every { newDetails.parameters } returns details.parameters
        every { newDetails.packageName } returns details.packageName
    }

    afterSpec {
        unmockkAll()
    }

    feature("generateBuildChallenge") {
        scenario("No build result")
        {
            ChallengeFactory.generateBuildChallenge(null, user, property, parameters) shouldBe false
        }

        scenario("Successful build")
        {
            ChallengeFactory.generateBuildChallenge(Result.SUCCESS, user, property, parameters) shouldBe false
        }

        mockkStatic(User::class)
        mockkStatic(GitUtil::class)
        every { User.getAll() } returns listOf()
        every { GitUtil.mapUser(any(), listOf()) } returns null
        scenario("No matching user")
        {
            ChallengeFactory.generateBuildChallenge(Result.FAILURE, user, property, parameters) shouldBe false
        }

        every { GitUtil.mapUser(any(), listOf()) } returns user
        every { property.getCurrentChallenges(any()) } returns CopyOnWriteArrayList(listOf(BuildChallenge(parameters)))
        scenario("Challenge already exists")
        {
            ChallengeFactory.generateBuildChallenge(Result.FAILURE, user, property, parameters) shouldBe false
        }

        every { property.getCurrentChallenges(any()) } returns CopyOnWriteArrayList()
        every { property.newChallenge(any(), any()) } returns Unit
        every { property.getUser() } returns user
        every { user.save() } returns Unit
        scenario("Successful Action")
        {
            ChallengeFactory.generateBuildChallenge(Result.FAILURE, user, property, parameters) shouldBe true
        }
    }

    feature("generateNewChallenges") {
        every { property.getCurrentChallenges(any()) } returns CopyOnWriteArrayList()
        scenario("Max challenges already reached")
        {
            ChallengeFactory.generateNewChallenges(user, property, parameters, arrayListOf(details), maxChallenges = 0) shouldBe 0
        }

        every { property.getUser() } returns user
        every { property.newChallenge(any(), any()) } returns Unit
        scenario("File is not changed by user")
        {
            ChallengeFactory.generateNewChallenges(user, property, parameters, arrayListOf(details)) shouldBe 0
        }

        val newDetails = mockkClass(SourceFileDetails::class)
        every { newDetails.changedByUsers } returns hashSetOf(GitUtil.GameUser(user))
        mockkStatic(ChallengeFactory::class)
        every { ChallengeFactory.generateChallenge(any(), any(), any(), any()) } returns mockkClass(TestChallenge::class)
        xscenario("Inconsistent results needs to be checked")//TODO complete rewrite of scenario, seems to only work due to mockkExceptions
        {
            ChallengeFactory.generateNewChallenges(user, property, parameters, arrayListOf(newDetails)) shouldBe 0
        }
        mockkStatic(ChallengeFactory::class)
    }

    feature("generateClassCoverageChallenge") {
        mockkObject(Random)
        GamePublisherDescriptor.challenges.clear()
        GamePublisherDescriptor.challenges[ClassCoverageChallenge::class.java] = 1
        every { JacocoUtil.calculateCoveredLines(any(), "nc") } returns 10
        ChallengeFactory.generateChallenge(user, parameters, listener, arrayListOf(newDetails)) should
                beOfType(ClassCoverageChallenge::class)
    }

    feature("generateDummyChallenge") {
        mockkObject(Random)
        every { Random.nextInt(any()) } returns 0
        GamePublisherDescriptor.challenges.clear()
        GamePublisherDescriptor.challenges[ClassCoverageChallenge::class.java] = 1
        ChallengeFactory.generateChallenge(user, parameters, listener, arrayListOf(details)) should
                beOfType(DummyChallenge::class)
    }

    feature("generateLineCoverageChallenge") {
        mockkObject(Random)
        GamePublisherDescriptor.challenges.clear()
        GamePublisherDescriptor.challenges[LineCoverageChallenge::class.java] = 1
        every { Random.nextInt(1) } returns 0
        every { JacocoUtil.calculateCoveredLines(any(), "pc") } returns 10
        val element = mockkClass(Element::class)
        val elements = Elements(listOf(element))
        every { JacocoUtil.getLines(any()) }  returns elements
        every { element.attr("id") } returns "L5"
        every { element.attr("class") } returns "nc"
        every { element.attr("title") } returns ""
        every { element.text() } returns "toString();"
        ChallengeFactory.generateChallenge(user, parameters, listener, arrayListOf(newDetails)) should
                beOfType(LineCoverageChallenge::class)
    }

    feature("generateMethodCoverageChallenge") {
        mockkObject(Random)
        GamePublisherDescriptor.challenges.clear()
        GamePublisherDescriptor.challenges[MethodCoverageChallenge::class.java] = 1
        every { Random.nextInt(1) } returns 0
        every { JacocoUtil.calculateCoveredLines(any(), "nc") } returns 10
        val method = JacocoUtil.CoverageMethod("toString", 10, 10, "")
        every { JacocoUtil.getMethodEntries(any()) } returns arrayListOf(method)
        ChallengeFactory.generateChallenge(user, parameters, listener, arrayListOf(newDetails)) should
                beOfType(MethodCoverageChallenge::class)
    }

    feature("generateTestChallenge") {
        mockkObject(Random)
        GamePublisherDescriptor.challenges.clear()
        GamePublisherDescriptor.challenges[TestChallenge::class.java] = 1
        ChallengeFactory.generateChallenge(user, parameters, listener, arrayListOf(details)) should
                beOfType(TestChallenge::class)
    }

    xfeature("generateMutationTestChallenge") {
        mockkObject(Random)
        mockkObject(MutationUtils)
        every { MutationUtils.getSurvivedMutationList(any(), any(), any(), any(), any()) } returns listOf()

        GamePublisherDescriptor.challenges.clear()
        GamePublisherDescriptor.challenges[MutationTestChallenge::class.java] = 1
        every { Random.nextInt(1) } returns 0
        every { MutationResults.retrievedMutationsFromJson(any(), any()) }  returns MutationResults(entries, "")
        every { property.getCurrentChallenges(any()) } returns CopyOnWriteArrayList(listOf())
        parameters.mocoJSONPath = "abc"
        scenario("Subject to change1")
        {
            ChallengeFactory.generateChallenge(user, parameters, listener, arrayListOf(details)) should
                    beOfType(DummyChallenge::class)
        }

        every { MutationUtils.getSurvivedMutationList(any(), any(), any(), any(), any()) } returns listOf(mutation1)
        every { MutationUtils.findMutationHasCodeSnippets(any(),any(),any(),any(),any()) } returns Pair(mutation1, mapOf("codeSnippet" to "abc", "mutatedSnippet" to "xyz"))
        scenario("Subject to change2")
        {
            ChallengeFactory.generateChallenge(user, parameters, listener, arrayListOf(details)) should
                    beOfType(MutationTestChallenge::class)
        }

        every { MutationUtils.findMutationHasCodeSnippets(any(),any(),any(),any(),any()) } returns Pair(null, mapOf("codeSnippet" to "abc", "mutatedSnippet" to "xyz"))
        scenario("Subject to change3")
        {
            ChallengeFactory.generateChallenge(user, parameters, listener, arrayListOf(details)) should
                    beOfType(MutationTestChallenge::class)
        }

        every { MutationResults.retrievedMutationsFromJson(any(), any()) }  returns null
        scenario("Subject to change4")
        {
            ChallengeFactory.generateChallenge(user, parameters, listener, arrayListOf(details)) should
                    beOfType(DummyChallenge::class)
        }

    }
})
