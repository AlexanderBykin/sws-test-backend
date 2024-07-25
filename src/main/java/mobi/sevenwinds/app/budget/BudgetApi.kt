package mobi.sevenwinds.app.budget

import com.fasterxml.jackson.annotation.JsonInclude
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import com.papsign.ktor.openapigen.annotations.type.number.integer.max.Max
import com.papsign.ktor.openapigen.annotations.type.number.integer.min.Min
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import mobi.sevenwinds.app.author.AuthorResponse

fun NormalOpenAPIRoute.budget() {
    route("/budget") {
        route("/add").post<Unit, BudgetResponse, BudgetAddRequest>(info("Добавить запись")) { _, body ->
            respond(BudgetService.addRecord(body))
        }

        route("/year/{year}/stats") {
            get<BudgetYearParam, BudgetYearStatsResponse>(info("Получить статистику за год")) { param ->
                respond(BudgetService.getYearStats(param))
            }
        }
    }
}

data class BudgetAddRequest(
    @Min(1900) val year: Int,
    @Min(1) @Max(12) val month: Int,
    @Min(1) val amount: Int,
    val type: BudgetType,
    val authorId: Int? = null
)

data class BudgetResponse(
    val year: Int,
    val month: Int,
    val amount: Int,
    val type: BudgetType,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val author: AuthorResponse? = null
)

data class BudgetYearStatsRecord(
    val total: Int,
    val sum: Int,
    val type: BudgetType
)

data class BudgetYearParam(
    @PathParam("Год") val year: Int,
    @QueryParam("Лимит пагинации") val limit: Int,
    @QueryParam("Смещение пагинации") val offset: Int,
    @QueryParam("Имя автора") val authorName: String? = null
)

class BudgetYearStatsResponse(
    val total: Int,
    val totalByType: Map<String, Int>,
    val items: List<BudgetResponse>
)

enum class BudgetType {
    Приход, Расход
}