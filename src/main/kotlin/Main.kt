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

suspend fun main() {

    embeddedServer(Netty, port = 8080) {
        install(Authentication) {
            basic("auth-basic") {
                realm = "Access to the '/' path"
                validate { credentials ->
                    UserIdPrincipal(credentials.name)
                }
            }
        }

        routing {
            authenticate("auth-basic") {
                get("{...}") {
                    val user = call.principal<UserIdPrincipal>()?.name
                    val method = call.request.httpMethod.value
                    val path = call.request.path()
                    val token = call.request.queryParameters["token"]

                    val isAuthorized = checkPolicy(user, method, path, token)

                    if (isAuthorized)
                        call.respondText("You are authorized! :)")
                    else
                        call.respond(HttpStatusCode.Unauthorized, "Nope!")
                }
            }
        }
    }.start(wait = true)
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
    val token: String?,
)

// OPA response JSON (stripped)
@Serializable
data class Result(
    val allow: Boolean
)
@Serializable
data class OpaResponse(
    val result: Result
)

suspend fun checkPolicy(user: String?, method: String, path: String, token: String?): Boolean {
    val client = HttpClient(CIO) {
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    // "/httpapi/auth_example" comes from the example-policy.rego file
    // https://www.openpolicyagent.org/docs/latest/policy-language/
    val opaUrl = "http://localhost:8181/v1/data/httpapi/auth_example"

    val opaData = OpaRequestBody(Input(user, method, path.removePrefix("/").split("/"), token))

    val response: OpaResponse = client.post(opaUrl) {
        contentType(ContentType.Application.Json)
        setBody(opaData)
    }.body()

    return response.result.allow
}