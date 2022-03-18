// Declare dependencies
@file:DependsOn("com.github.hamidrezasahraei.fuel:fuel:2.3.1.1")
@file:DependsOn("com.github.hamidrezasahraei.fuel:fuel-json:2.3.1.1")
@file:DependsOn("com.github.hamidrezasahraei.fuel:fuel-gson:2.3.1.1")
@file:DependsOn("com.google.code.gson:gson:2.9.0")
@file:MavenRepository("mavenCentral","https://repo1.maven.org/maven2/" )
@file:MavenRepository("jitpack","https://jitpack.io" )

import java.net.*
import java.io.*
import com.github.kittinunf.fuel.*
import com.github.kittinunf.fuel.core.*
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.interceptors.cUrlLoggingRequestInterceptor
import com.github.kittinunf.fuel.gson.responseObject
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import kotlin.system.exitProcess

val reviewerIDs = mutableListOf<String>()
File("./merge-request-assign-queue.txt").forEachLine {
    reviewerIDs.add(it.split('-').first())
}

// Create project URL for api calls from pre defined variables
val ciCompleteProjectUrl = System.getenv("CI_PROJECT_URL")
val ciProjectId = System.getenv("CI_PROJECT_ID")
val sourceBranch = System.getenv("CI_COMMIT_REF_NAME")
val ciAccessToken = System.getenv("CI_ACCESS_TOKEN")
val userId = System.getenv("GITLAB_USER_ID")
val ciProjectHost = URL(ciCompleteProjectUrl).host
val ciProjectUrl = "https://$ciProjectHost/api/v4/projects/$ciProjectId"

securityCheck()
shouldCreateMergeRequest()
setBaseNetworkData()

// Get default Branch from git
val defaultBranchName = System.getenv("CI_DEFAULT_BRANCH")

// Get current Reviewer ID from git variables
var currentReviewerId = System.getenv("CURRENT_REVIEWER_USER_ID")

if (currentReviewerId.isEmpty()) {
    println("CURRENT_REVIEWER_USER_ID variable is not set.")
    exitProcess(1)
}

if(shouldCreateMergeRequest()) {
    createMergeRequest()
}

fun securityCheck() {
    if (ciAccessToken.isNullOrEmpty()) {
        println("GITLAB_PRIVATE_TOKEN not set")
        println("Please set the GitLab Private Token as GITLAB_PRIVATE_TOKEN")
        System.exit(1)
    }
}

fun shouldCreateMergeRequest(): Boolean {
    if (isMergeRequestExistsForBranch(sourceBranch)){
        println("A merge requests is exist for this branch.")
        exitProcess(0)
    }
    return true
}

// Set base url and access tokens
fun setBaseNetworkData() {
    FuelManager.instance.baseHeaders = mapOf("PRIVATE-TOKEN" to ciAccessToken)
    FuelManager.instance.basePath = ciProjectUrl
}

fun createMergeRequest() {
    val mergeRequestModel = MergeRequestModel(
            id = ciProjectId,
            source_branch = sourceBranch,
            target_branch = defaultBranchName,
            title = createMergeRequestTitle(),
            assignee_id = userId,
            reviewer_ids = getProperReviewers(),
            labels = getProperLabels()
    )
    val (request, response, result) = "/merge_requests"
            .httpPost()
            .jsonBody(Gson().toJson(mergeRequestModel))
            .responseString()

    when (result) {
        is Result.Success -> {
            setTheNextReviewer()
        }
        is Result.Failure -> {
            println(result.error)
        }
    }
}

fun isMergeRequestExistsForBranch(sourceBranch: String): Boolean {
    return getListOfOpenMergeRequests().any { it.source_branch == sourceBranch }
}

fun getListOfOpenMergeRequests(): List<MergeRequestModel> {
    return "/merge_requests?state=opened".httpGet().responseObject<List<MergeRequestModel>>().third.get()
}

fun getProperReviewers(): List<String> {
    if (userId == currentReviewerId) {
        currentReviewerId = reviewerIDs.nextOf(currentReviewerId)
    }
    return listOf(currentReviewerId)
}

fun getProperLabels(): List<String> {
    return listOf("Developer Review")
}

fun createMergeRequestTitle(): String {
    return "WIP: $sourceBranch"
}

fun setTheNextReviewer() {
    val nextReviewerId = findTheNextReviewerId()
    if (nextReviewerId.isEmpty()) {
        println("Can not set the next reviewer, Because it is not found.")
        exitProcess(1)
    } else {
        println("The next reviewer set to: $nextReviewerId")
        "/variables/CURRENT_REVIEWER_USER_ID"
                .httpPut(listOf("value" to nextReviewerId))
                .responseString()
    }
}

fun findTheNextReviewerId(): String {
    return reviewerIDs.nextOf(currentReviewerId)
}

data class MergeRequestModel(
        val id: String,
        val source_branch: String,
        val target_branch: String,
        val remove_source_branch: Boolean = true,
        val title: String,
        val assignee_id: String,
        val reviewer_ids: List<String>,
        val labels: List<String>
)

fun List<String>.nextOf(item: String): String {
    if (last() == item) {
        return first()
    }
    val indexOfNextItem = indexOf(item)+1
    return get(indexOfNextItem)
}