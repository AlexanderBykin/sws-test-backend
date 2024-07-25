package mobi.sevenwinds.app.budget

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mobi.sevenwinds.app.author.AuthorEntity
import mobi.sevenwinds.app.author.AuthorTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object BudgetService {
    suspend fun addRecord(body: BudgetAddRequest): BudgetResponse = withContext(Dispatchers.IO) {
        transaction {
            val author = body.authorId?.let { AuthorEntity.findById(it) }
            val entity = BudgetEntity.new {
                this.year = body.year
                this.month = body.month
                this.amount = body.amount
                this.type = body.type
                this.author = author
            }

            return@transaction entity.toResponse()
        }
    }

    suspend fun getYearStats(param: BudgetYearParam): BudgetYearStatsResponse = withContext(Dispatchers.IO) {
        transaction {
            val totalQuery = BudgetTable
                .join(AuthorTable, JoinType.LEFT, BudgetTable.authorId, AuthorTable.id)
                .slice(
                    BudgetTable.id.count(),
                    BudgetTable.amount.sum(),
                    BudgetTable.type,
                )
                .select {
                    BudgetTable.year eq param.year
                }
                .andWhere {
                    if (!param.authorName.isNullOrEmpty())
                        AuthorTable.fio.lowerCase().like("%${param.authorName.toLowerCase()}%")
                    else
                        Op.TRUE
                }
                .groupBy(BudgetTable.type)

            val query = BudgetTable
                .join(AuthorTable, JoinType.LEFT, BudgetTable.authorId, AuthorTable.id)
                .select { BudgetTable.year eq param.year }
                .andWhere {
                    if (!param.authorName.isNullOrEmpty())
                        AuthorTable.fio.lowerCase().like("%${param.authorName.toLowerCase()}%")
                    else
                        Op.TRUE
                }
                .orderBy(BudgetTable.month to SortOrder.ASC, BudgetTable.amount to SortOrder.DESC)
                .limit(param.limit, param.offset)

            val totalData = totalQuery.map {
                BudgetYearStatsRecord(
                    total = it[BudgetTable.id.count()],
                    sum = it[BudgetTable.amount.sum()] ?: 0,
                    type = it[BudgetTable.type]
                )
            }
            val data = BudgetEntity.wrapRows(query).map { it.toResponse() }

            val totalSumByType = totalData.associate { it.type.name to it.sum }

            return@transaction BudgetYearStatsResponse(
                total = totalData.sumOf { it.total },
                totalByType = totalSumByType,
                items = data
            )
        }
    }
}