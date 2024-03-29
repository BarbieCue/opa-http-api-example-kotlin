import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

fun main() {
    embeddedServer(Netty, port = 5000, module = Application::opaExample).start(wait = true)
}

fun Application.opaExample() {
    install(Authentication) {
        basic("auth-basic") {
            validate { credentials ->
                UserIdPrincipal(credentials.name)
            }
        }
    }
    routing {
        authenticate("auth-basic") {
            get("finance/salary/{employee}") {
                val user = call.principal<UserIdPrincipal>()?.name
                val method = call.request.httpMethod.value
                val path = call.request.path()

                val isAuthorized = checkPolicy(user, method, path)

                if (isAuthorized) {
                    call.respondText("${call.parameters["employee"]}s salary is ${(0..5000000).random()}€")
                } else
                    call.respond(HttpStatusCode.Unauthorized, "Nope!")
            }
        }
    }
}

// Input JSON for OPA request
@Serializable
data class OpaRequestBody(
    val input: Input
)
@Serializable
data class Input(
    val user: String?,
    val method: String,
    val path: List<String>,
)

// OPA response JSON (stripped)
@Serializable
data class OpaResponse(
    val result: Boolean
)

private suspend fun checkPolicy(user: String?, method: String, path: String): Boolean {
    val client = HttpClient(CIO) {
        install(Logging) {
            level = LogLevel.ALL
        }
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                ignoreUnknownKeys = true
            })
        }
    }

    val opaData = OpaRequestBody(Input(user, method, path.removePrefix("/").split("/")))

    // Call the OPA Data API
    // Concrete: Retrieving the "allow" document from the example-policy by entering values
    // https://www.openpolicyagent.org/docs/latest/rest-api/#get-a-document-with-input
    val response: OpaResponse = client.post("http://localhost:8181/v1/data/httpapi/auth_example/allow") {
        contentType(ContentType.Application.Json)
        setBody(opaData)
    }.body()

    return response.result
}