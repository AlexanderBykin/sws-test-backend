package mobi.sevenwinds.app.author

import com.papsign.ktor.openapigen.annotations.type.string.length.Length
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route

fun NormalOpenAPIRoute.author() {
    route("/author") {
        route("/add").post<Unit, AuthorResponse, AuthorAddRequest>(info("Добавить запись")) { _, body ->
            respond(AuthorService.addRecord(body))
        }
    }
}

data class AuthorAddRequest(
    @Length(min = 1, max = 255) val fio: String
)

data class AuthorResponse(
    val id: Int,
    val fio: String,
    val dateCreate: String
)