package org.gamekins

import hudson.Extension
import hudson.model.AbstractItem
import hudson.model.RootAction
import hudson.model.User
import hudson.util.FormValidation
import jenkins.model.Jenkins
import kotlinx.coroutines.*
import net.sf.json.JSONArray
import net.sf.json.JSONObject
import net.sf.json.JsonConfig
import net.sf.json.util.CycleDetectionStrategy
import net.sf.json.util.PropertyFilter
import org.gamekins.challenge.Challenge
import org.gamekins.util.ActionUtil
import org.gamekins.util.Constants
import org.kohsuke.stapler.QueryParameter
import org.kohsuke.stapler.WebMethod
import org.kohsuke.stapler.json.JsonBody
import org.kohsuke.stapler.json.JsonHttpResponse
import org.kohsuke.stapler.verb.GET
import org.kohsuke.stapler.verb.POST
import java.io.Serializable
import java.util.concurrent.CopyOnWriteArrayList

@Extension
class CustomAPI : RootAction {

    private val jsonConfig = JsonConfig()

    init {
        jsonConfig.isAllowNonStringKeys = true
        jsonConfig.cycleDetectionStrategy = CycleDetectionStrategy.NOPROP
        jsonConfig.excludes = arrayOf("File")
        jsonConfig.jsonPropertyFilter = PropertyFilter { _, name, _ -> name == "changedByUsers" }
    }

    override fun getIconFileName(): String? {
        return null
    }

    override fun getDisplayName(): String? {
        return null
    }

    override fun getUrlName(): String {
        return "gamekins"
    }

    /**
     * Returns the list of completed achievements.
     */
    @GET
    @WebMethod(name = ["getCompletedAchievements"])
    fun getCompletedAchievements(@QueryParameter("job") job: String): JsonHttpResponse {

        val user: User = User.current()
            ?: throw FormValidation.error(Constants.Error.NO_USER_SIGNED_IN)
        val property = user.getProperty(GameUserProperty::class.java)
            ?: throw FormValidation.error(Constants.Error.RETRIEVING_PROPERTY)
        val myJsonObjects = property.getCompletedAchievements(job)

        val response = JSONArray()
        myJsonObjects.forEach { response.add(it) }
        val responseJson = JSONObject()
        responseJson["completedAchievements"] = myJsonObjects

        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * Returns the list of completed Challenges by [job].
     */
    @GET
    @WebMethod(name = ["getCompletedChallenges"])
    fun getCompletedChallenges(@QueryParameter("job") job: String): JsonHttpResponse {

        val user: User = User.current()
            ?: throw FormValidation.error(Constants.Error.NO_USER_SIGNED_IN)
        val property = user.getProperty(GameUserProperty::class.java)
            ?: throw FormValidation.error(Constants.Error.RETRIEVING_PROPERTY)
        val myJsonObjects = property.getCompletedChallenges(job)

        val responseJson = JSONObject()
        responseJson.accumulate("completedChallenges", myJsonObjects, jsonConfig)

        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * Returns the list of completed QuestTasks by [job].
     */
    @GET
    @WebMethod(name = ["getCompletedQuestTasks"])
    fun getCompletedQuestTasks(@QueryParameter("job") job: String): JsonHttpResponse {

        val user: User = User.current()
            ?: throw FormValidation.error(Constants.Error.NO_USER_SIGNED_IN)
        val property = user.getProperty(GameUserProperty::class.java)
            ?: throw FormValidation.error(Constants.Error.RETRIEVING_PROPERTY)
        val myJsonObjects = property.getCompletedQuestTasks(job)

        val responseJson = JSONObject()
        responseJson.accumulate("completedQuestTasks", myJsonObjects, jsonConfig)
        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * Returns the list of current Challenges by [job].
     */
    @GET
    @WebMethod(name = ["getCurrentChallenges"])
    fun getCurrentChallenges(@QueryParameter("job") job: String): JsonHttpResponse {

        val user: User = User.current()
            ?: throw FormValidation.error(Constants.Error.NO_USER_SIGNED_IN)
        val property = user.getProperty(GameUserProperty::class.java)
            ?: throw FormValidation.error(Constants.Error.RETRIEVING_PROPERTY)
        val myJsonObjects = property.getCurrentChallenges(job)

        val responseJson = JSONObject()
        responseJson.accumulate("currentChallenges", myJsonObjects, jsonConfig)

        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * Returns the list of current QuestTasks by [job].
     */
    @GET
    @WebMethod(name = ["getCurrentQuestTasks"])
    fun getCurrentQuestTasks(@QueryParameter("job") job: String): JsonHttpResponse {

        val user: User = User.current()
            ?: throw FormValidation.error(Constants.Error.NO_USER_SIGNED_IN)
        val property = user.getProperty(GameUserProperty::class.java)
            ?: throw FormValidation.error(Constants.Error.RETRIEVING_PROPERTY)

        val myJsonObjects = property.getCurrentQuestTasks(job)
        val response = JSONArray()
        myJsonObjects.forEach { response.add(it) }
        val responseJson = JSONObject()
        responseJson["currentQuestTasks"] = myJsonObjects
        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * Returns the list of rejected Challenges by [job].
     */
    @GET
    @WebMethod(name = ["getRejectedChallenges"])
    fun getRejectedChallenges(@QueryParameter("job") job: String): JsonHttpResponse {

        val user: User = User.current()
            ?: throw FormValidation.error(Constants.Error.NO_USER_SIGNED_IN)
        val property = user.getProperty(GameUserProperty::class.java)
            ?: throw FormValidation.error(Constants.Error.RETRIEVING_PROPERTY)

        val myJsonObjects = property.getRejectedChallenges(job)

        val responseJson = JSONObject()
        responseJson.accumulate("rejectedChallenges", myJsonObjects, jsonConfig)

        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * Returns the list of stored Challenges by [job].
     */
    @GET
    @WebMethod(name = ["getStoredChallenges"])
    fun getStoredChallenges(@QueryParameter("job") job: String): JsonHttpResponse {

        val user: User = User.current()
            ?: throw FormValidation.error(Constants.Error.NO_USER_SIGNED_IN)
        val property = user.getProperty(GameUserProperty::class.java)
            ?: throw FormValidation.error(Constants.Error.RETRIEVING_PROPERTY)

        val myJsonObjects = property.getStoredChallenges(job)

        val responseJson = JSONObject()
        responseJson.accumulate("storedChallenges", myJsonObjects, jsonConfig)

        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * Stores Challenges by [job].
     */
    @POST
    @WebMethod(name = ["storeChallenge"])
    fun storeChallenge(@JsonBody body: StoreChallenge): JsonHttpResponse {

        val job: AbstractItem = Jenkins.get().getItemByFullName(body.job) as AbstractItem

        val response = JSONObject()
        response["message"] = ActionUtil.doStoreChallenge(job, body.challengeName)
        return JsonHttpResponse(response, 200)
    }

    /**
     * Re-stores a Challenges by [job].
     */
    @POST
    @WebMethod(name = ["restoreChallenge"])
    fun restoreChallenge(@JsonBody body: StoreChallenge): JsonHttpResponse {

        val job: AbstractItem = Jenkins.get().getItemByFullName(body.job) as AbstractItem

        val response = JSONObject()
        response["message"] = ActionUtil.doRestoreChallenge(job, body.challengeName)
        return JsonHttpResponse(response, 200)
    }

    /**
     * Un-stores a Challenges by [job].
     */
    @POST
    @WebMethod(name = ["unshelveChallenge"])
    fun unshelveChallenge(@JsonBody body: StoreChallenge): JsonHttpResponse {

        val job: AbstractItem = Jenkins.get().getItemByFullName(body.job) as AbstractItem

        val response = JSONObject()
        response["message"] = ActionUtil.doUndoStoreChallenge(job, body.challengeName)
        return JsonHttpResponse(response, 200)
    }

    /**
     * Rejects a Challenges by [job].
     */
    @POST
    @WebMethod(name = ["rejectChallenge"])
    fun rejectChallenge(@JsonBody body: StoreChallenge): JsonHttpResponse {

        val job: AbstractItem = Jenkins.get().getItemByFullName(body.job) as AbstractItem

        val response = JSONObject()
        response["message"] = ActionUtil.doRejectChallenge(job, body.challengeName, body.reason)
        return JsonHttpResponse(response, 200)
    }

    /**
     * Returns the score of the user by [job].
     */
    @GET
    @WebMethod(name = ["getScore"])
    fun getScore(@QueryParameter("job") job: String): JsonHttpResponse {

        val user: User = User.current()
            ?: throw FormValidation.error(Constants.Error.NO_USER_SIGNED_IN)
        val property = user.getProperty(GameUserProperty::class.java)
            ?: throw FormValidation.error(Constants.Error.RETRIEVING_PROPERTY)

        val myJsonObjects = arrayListOf(property.getScore(job))
        val response = JSONArray()
        myJsonObjects.forEach { response.add(it) }
        val responseJson = JSONObject()
        responseJson["score"] = myJsonObjects
        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * Returns the name of the team the user is participating in the project [job].
     */
    @GET
    @WebMethod(name = ["getTeamName"])
    fun getTeamName(@QueryParameter("job") job: String): JsonHttpResponse {

        val user: User = User.current()
            ?: throw FormValidation.error(Constants.Error.NO_USER_SIGNED_IN)
        val property = user.getProperty(GameUserProperty::class.java)
            ?: throw FormValidation.error(Constants.Error.RETRIEVING_PROPERTY)

        val myJsonObjects = property.getTeamName(job)
        val response = JSONArray()
        myJsonObjects.forEach { response.add(it) }
        val responseJson = JSONObject()
        responseJson["teamName"] = myJsonObjects
        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * Returns the list of unsolved achievements.
     */
    @GET
    @WebMethod(name = ["getUnsolvedAchievements"])
    fun getUnsolvedAchievements(@QueryParameter("job") job: String): JsonHttpResponse {

        val user: User = User.current()
            ?: throw FormValidation.error(Constants.Error.NO_USER_SIGNED_IN)
        val property = user.getProperty(GameUserProperty::class.java)
            ?: throw FormValidation.error(Constants.Error.RETRIEVING_PROPERTY)

        val myJsonObjects = property.getUnsolvedAchievements(job)
        val response = JSONArray()
        myJsonObjects.forEach { response.add(it) }
        val responseJson = JSONObject()
        responseJson["unsolvedAchievements"] = myJsonObjects
        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * Returns the list of badge achievements.
     */
    @GET
    @WebMethod(name = ["getBadgeAchievements"])
    fun getBadgeAchievements(@QueryParameter("job") job: String): JsonHttpResponse {

        val user: User = User.current()
            ?: throw FormValidation.error(Constants.Error.NO_USER_SIGNED_IN)
        val property = user.getProperty(GameUserProperty::class.java)
            ?: throw FormValidation.error(Constants.Error.RETRIEVING_PROPERTY)

        val myJsonObjects = property.getBadgeAchievements(job)
        val response = JSONArray()
        myJsonObjects.forEach { response.add(it) }
        val responseJson = JSONObject()
        responseJson["badgeAchievements"] = myJsonObjects
        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * Returns the list of progress achievements.
     */
    @GET
    @WebMethod(name = ["getProgressAchievements"])
    fun getProgressAchievements(@QueryParameter("job") job: String): JsonHttpResponse {

        val user: User = User.current()
            ?: throw FormValidation.error(Constants.Error.NO_USER_SIGNED_IN)
        val property = user.getProperty(GameUserProperty::class.java)
            ?: throw FormValidation.error(Constants.Error.RETRIEVING_PROPERTY)

        val myJsonObjects = property.getProgressAchievements(job)
        val response = JSONArray()
        myJsonObjects.forEach { response.add(it) }
        val responseJson = JSONObject()
        responseJson["progressAchievements"] = myJsonObjects
        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * Returns the filename of the current avatar.
     */
    @GET
    @WebMethod(name = ["getAvatar"])
    fun getCurrentAvatar(): JsonHttpResponse {

        val user: User = User.current()
            ?: throw FormValidation.error(Constants.Error.NO_USER_SIGNED_IN)
        val property = user.getProperty(GameUserProperty::class.java)
            ?: throw FormValidation.error(Constants.Error.RETRIEVING_PROPERTY)

        val myJsonObjects = property.getCurrentAvatar()
        val response = JSONArray()
        myJsonObjects.forEach { response.add(it) }
        val responseJson = JSONObject()
        responseJson["currentAvatar"] = myJsonObjects

        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * Returns the parent/owner/user of the property.
     */
    @GET
    @WebMethod(name = ["getUsers"])
    fun getUsers(@QueryParameter("job") job: String): JsonHttpResponse {

        val realJob: AbstractItem = Jenkins.get().getItemByFullName(job) as AbstractItem
        val myJsonObjects = ActionUtil.getUserDetails(realJob)

        val response = JSONArray()
        myJsonObjects.forEach { response.add(it) }
        val responseJson = JSONObject()
        responseJson["users"] = myJsonObjects
        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * Returns the parent/owner/user of the property.
     */
    @GET
    @WebMethod(name = ["getTeams"])
    fun getTeams(@QueryParameter("job") job: String): JsonHttpResponse {

        val realJob: AbstractItem = Jenkins.get().getItemByFullName(job) as AbstractItem
        val myJsonObjects = ActionUtil.getTeamDetails(realJob)

        val response = JSONArray()
        myJsonObjects.forEach { response.add(it) }
        val responseJson = JSONObject()
        responseJson["teams"] = myJsonObjects
        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * StartSocket [job].
     */
    @POST
    @WebMethod(name = ["startSocket"])
    fun startSocket(): JsonHttpResponse {
        try {
            WebSocketServer.startServer()
        } catch (e: Exception) {
            println(e)
        }

        val response = JSONObject()
        return JsonHttpResponse(response, 200)
    }

    /**
     * Returns the limit of stored challenges of [job].
     */
    @GET
    @WebMethod(name = ["getStoredChallengesLimit"])
    fun getStoredChallengesLimit(@QueryParameter("job") job: String): JsonHttpResponse {

        val realJob: AbstractItem = Jenkins.get().getItemByFullName(job) as AbstractItem

        val responseJson = JSONObject()
        responseJson["limit"] = TaskAction(realJob).getStoredChallengesLimit()

        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * Returns whether challenges can be sent.
     */
    @GET
    @WebMethod(name = ["getCanSend"])
    fun getCanSend(@QueryParameter("job") job: String): JsonHttpResponse {

        val realJob: AbstractItem = Jenkins.get().getItemByFullName(job) as AbstractItem

        val responseJson = JSONObject()
        responseJson["canSend"] = TaskAction(realJob).getCanSend()

        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * Returns the details of all users participating in the current project that are eligible for getting sent
     * Challenges from you.
     */
    @GET
    @WebMethod(name = ["getUserDetailsForSending"])
    fun getUserDetailsForSending(@QueryParameter("job") job: String, @QueryParameter("username") username: String): JsonHttpResponse {
        val realJob: AbstractItem = Jenkins.get().getItemByFullName(job) as AbstractItem
        val details = CopyOnWriteArrayList(ActionUtil.getUserDetails(realJob))

        details.removeIf { ud -> User.getAll().first { it.id == username }.fullName == ud.userName }

        val response = JSONArray()
        details.forEach { response.add(it) }
        val responseJson = JSONObject()
        responseJson["users"] = details

        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * Send a [Challenge] to another user.
     */
    @POST
    @WebMethod(name = ["sendChallenge"])
    fun sendChallenge(@JsonBody body: SendChallenge)
    : JsonHttpResponse {

        val realJob: AbstractItem = Jenkins.get().getItemByFullName(body.job) as AbstractItem

        val response = JSONObject()
        response["message"] = ActionUtil.doSendChallenge(
            realJob, body.challengeName, body.sendTo, User.getAll().first { it.id == body.username })
        return JsonHttpResponse(response, 200)
    }

    class StoreChallenge: Serializable {

        lateinit var job: String;
        lateinit var challengeName: String;
        lateinit var reason: String;

        constructor()
        constructor(job: String,challengeName: String, reason: String ) {
            this.job = job
            this.challengeName = challengeName
            this.reason = reason
        }
    }

    class SendChallenge: Serializable {

        lateinit var job: String
        lateinit var challengeName: String
        lateinit var username: String
        lateinit var sendTo: String

        constructor()
        constructor(job: String,challengeName: String, username: String, sendTo: String) {
            this.job = job
            this.challengeName = challengeName
            this.username = username
            this.sendTo = sendTo
        }
    }

}