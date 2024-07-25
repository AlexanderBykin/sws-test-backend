package mobi.sevenwinds.app.author

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable

object AuthorTable : IntIdTable("author") {
    val fio = varchar("fio", 255)
    val dateCreate = datetime("date_create")
}

class AuthorEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AuthorEntity>(AuthorTable)

    var fio by AuthorTable.fio
    var dateCreate by AuthorTable.dateCreate

    fun toResponse(): AuthorResponse {
        return AuthorResponse(
            id.value,
            fio,
            dateCreate.toString("yyyy-MM-dd HH:mm:ssZ")
        )
    }
}