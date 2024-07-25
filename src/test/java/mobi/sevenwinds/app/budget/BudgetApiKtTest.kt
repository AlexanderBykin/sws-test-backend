package mobi.sevenwinds.app.budget

import io.restassured.RestAssured
import mobi.sevenwinds.app.author.AuthorAddRequest
import mobi.sevenwinds.app.author.AuthorResponse
import mobi.sevenwinds.app.author.AuthorTable
import mobi.sevenwinds.common.ServerTest
import mobi.sevenwinds.common.jsonBody
import mobi.sevenwinds.common.toResponse
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BudgetApiKtTest : ServerTest() {

    @BeforeEach
    internal fun setUp() {
        transaction {
            BudgetTable.deleteAll()
            AuthorTable.deleteAll()
        }
    }

    @Test
    fun testBudgetPagination() {
        addBudgetRecord(BudgetAddRequest(2020, 5, 10, BudgetType.Приход))
        addBudgetRecord(BudgetAddRequest(2020, 5, 5, BudgetType.Приход))
        addBudgetRecord(BudgetAddRequest(2020, 5, 20, BudgetType.Приход))
        addBudgetRecord(BudgetAddRequest(2020, 5, 30, BudgetType.Приход))
        addBudgetRecord(BudgetAddRequest(2020, 5, 40, BudgetType.Приход))
        addBudgetRecord(BudgetAddRequest(2030, 1, 1, BudgetType.Расход))

        RestAssured.given()
            .queryParam("limit", 3)
            .queryParam("offset", 1)
            .get("/budget/year/2020/stats")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println("${response.total} / ${response.items} / ${response.totalByType}")

                Assert.assertEquals(5, response.total)
                Assert.assertEquals(3, response.items.size)
                Assert.assertEquals(105, response.totalByType[BudgetType.Приход.name])
            }
    }

    @Test
    fun testStatsSortOrder() {
        addBudgetRecord(BudgetAddRequest(2020, 5, 100, BudgetType.Приход))
        addBudgetRecord(BudgetAddRequest(2020, 1, 5, BudgetType.Приход))
        addBudgetRecord(BudgetAddRequest(2020, 5, 50, BudgetType.Приход))
        addBudgetRecord(BudgetAddRequest(2020, 1, 30, BudgetType.Приход))
        addBudgetRecord(BudgetAddRequest(2020, 5, 400, BudgetType.Приход))

        // expected sort order - month ascending, amount descending

        RestAssured.given()
            .get("/budget/year/2020/stats?limit=100&offset=0")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println(response.items)

                Assert.assertEquals(30, response.items[0].amount)
                Assert.assertEquals(5, response.items[1].amount)
                Assert.assertEquals(400, response.items[2].amount)
                Assert.assertEquals(100, response.items[3].amount)
                Assert.assertEquals(50, response.items[4].amount)
            }
    }

    @Test
    fun testAddBudgetWithExistAuthor() {
        val author1 = addAuthorRecord(AuthorAddRequest(fio = "qwe"))
        val author2 = addAuthorRecord(AuthorAddRequest(fio = "asd"))
        val author3 = addAuthorRecord(AuthorAddRequest(fio = "zxc"))
        val budget1 = addBudgetRecord(BudgetAddRequest(2024, 7, 100, BudgetType.Приход, authorId = author1.id))
        val budget2 = addBudgetRecord(BudgetAddRequest(2024, 7, 200, BudgetType.Приход, authorId = author2.id))
        val budget3 = addBudgetRecord(BudgetAddRequest(2024, 7, 300, BudgetType.Приход, authorId = author3.id))
        RestAssured.given()
            .get("/budget/year/2024/stats?limit=100&offset=0")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println(response.items)

                Assert.assertEquals(3, response.total)
                Assert.assertEquals(3, response.items.size)

                Assert.assertEquals(budget3.amount, response.items[0].amount)
                Assert.assertEquals(budget3.author?.id, author3.id)
                Assert.assertEquals(budget3.author?.id, response.items[0].author?.id)

                Assert.assertEquals(budget2.amount, response.items[1].amount)
                Assert.assertEquals(budget2.author?.id, author2.id)
                Assert.assertEquals(budget2.author?.id, response.items[1].author?.id)

                Assert.assertEquals(budget1.amount, response.items[2].amount)
                Assert.assertEquals(budget1.author?.id, author1.id)
                Assert.assertEquals(budget1.author?.id, response.items[2].author?.id)
            }
    }

    @Test
    fun testAddBudgetWithoutExistAuthor() {
        val budget1 = addBudgetRecord(BudgetAddRequest(2024, 7, 500, BudgetType.Приход, authorId = 101), authorExists = false)
        val budget2 = addBudgetRecord(BudgetAddRequest(2024, 7, 600, BudgetType.Приход, authorId = 102), authorExists = false)
        val budget3 = addBudgetRecord(BudgetAddRequest(2024, 7, 700, BudgetType.Приход, authorId = 103), authorExists = false)
        RestAssured.given()
            .get("/budget/year/2024/stats?limit=100&offset=0")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println(response.items)

                Assert.assertEquals(3, response.total)
                Assert.assertEquals(3, response.items.size)

                Assert.assertEquals(budget3.amount, response.items[0].amount)
                Assert.assertEquals(null, budget3.author?.id)
                Assert.assertEquals(null, response.items[0].author?.id)

                Assert.assertEquals(budget2.amount, response.items[1].amount)
                Assert.assertEquals(null, budget2.author?.id)
                Assert.assertEquals(null, response.items[1].author?.id)

                Assert.assertEquals(budget1.amount, response.items[2].amount)
                Assert.assertEquals(null, budget1.author?.id)
                Assert.assertEquals(null, response.items[2].author?.id)
            }
    }

    @Test
    fun testInvalidMonthValues() {
        RestAssured.given()
            .jsonBody(BudgetAddRequest(2020, -5, 5, BudgetType.Приход))
            .post("/budget/add")
            .then().statusCode(400)

        RestAssured.given()
            .jsonBody(BudgetAddRequest(2020, 15, 5, BudgetType.Приход))
            .post("/budget/add")
            .then().statusCode(400)
    }

    @Test
    fun testAddAuthorInvalidCharacterLengthAtName() {
        RestAssured.given()
            .jsonBody(
                AuthorAddRequest(
                    fio = ""
                )
            )
            .post("/author/add")
            .then().statusCode(400)
    }

    private fun addBudgetRecord(record: BudgetAddRequest, authorExists: Boolean = true): BudgetResponse {
        return RestAssured.given()
            .jsonBody(record)
            .post("/budget/add")
            .toResponse<BudgetResponse>().let { response ->
                Assert.assertEquals(record.year, response.year)
                Assert.assertEquals(record.month, response.month)
                Assert.assertEquals(record.type, response.type)
                Assert.assertEquals(record.amount, response.amount)
                if (authorExists) Assert.assertEquals(record.authorId, response.author?.id)
                response
            }
    }

    private fun addAuthorRecord(record: AuthorAddRequest): AuthorResponse {
        return RestAssured.given()
            .jsonBody(record)
            .post("/author/add")
            .toResponse<AuthorResponse>().let { response ->
                Assert.assertEquals(record.fio, response.fio)
                response
            }
    }
}