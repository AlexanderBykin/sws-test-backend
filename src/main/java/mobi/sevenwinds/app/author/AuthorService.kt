package mobi.sevenwinds.app.author

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.Instant

object AuthorService {
    suspend fun addRecord(body: AuthorAddRequest): AuthorResponse = withContext(Dispatchers.IO) {
        transaction {
            val entity = AuthorEntity.new {
                fio = body.fio
                dateCreate = Instant.now().toDateTime()
            }
            return@transaction entity.toResponse()
        }
    }
}