package net.multipul.kmm.shared

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.TransacterImpl
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.internal.copyOnWriteList
import kotlin.Any
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.collections.MutableList
import kotlin.jvm.JvmField
import kotlin.reflect.KClass
import net.multipul.kmm.Database
import net.multipul.kmm.Server
import net.multipul.kmm.ServerQueries

internal val KClass<Database>.schema: SqlDriver.Schema
  get() = DatabaseImpl.Schema

internal fun KClass<Database>.newInstance(driver: SqlDriver): Database = DatabaseImpl(driver)

private class DatabaseImpl(
  driver: SqlDriver
) : TransacterImpl(driver), Database {
  override val serverQueries: ServerQueriesImpl = ServerQueriesImpl(this, driver)

  object Schema : SqlDriver.Schema {
    override val version: Int
      get() = 1

    override fun create(driver: SqlDriver) {
      driver.execute(null, """
          |CREATE TABLE server (
          |    ID INTEGER PRIMARY KEY AUTOINCREMENT,
          |    title TEXT,
          |    url TEXT,
          |    token TEXT
          |)
          """.trimMargin(), 0)
    }

    override fun migrate(
      driver: SqlDriver,
      oldVersion: Int,
      newVersion: Int
    ) {
    }
  }
}

private class ServerQueriesImpl(
  private val database: DatabaseImpl,
  private val driver: SqlDriver
) : TransacterImpl(driver), ServerQueries {
  internal val selectAll: MutableList<Query<*>> = copyOnWriteList()

  internal val selectByID: MutableList<Query<*>> = copyOnWriteList()

  override fun <T : Any> selectAll(mapper: (
    ID: Long,
    title: String?,
    url: String?,
    token: String?
  ) -> T): Query<T> = Query(1713300054, selectAll, driver, "Server.sq", "selectAll",
      "SELECT * FROM server") { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1),
      cursor.getString(2),
      cursor.getString(3)
    )
  }

  override fun selectAll(): Query<Server> = selectAll { ID, title, url, token ->
    net.multipul.kmm.Server(
      ID,
      title,
      url,
      token
    )
  }

  override fun <T : Any> selectByID(ID: Long, mapper: (
    ID: Long,
    title: String?,
    url: String?,
    token: String?
  ) -> T): Query<T> = SelectByIDQuery(ID) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1),
      cursor.getString(2),
      cursor.getString(3)
    )
  }

  override fun selectByID(ID: Long): Query<Server> = selectByID(ID) { ID, title, url, token ->
    net.multipul.kmm.Server(
      ID,
      title,
      url,
      token
    )
  }

  override fun insert(
    title: String?,
    url: String?,
    token: String?
  ) {
    driver.execute(1395811592, """
    |INSERT INTO server (
    |    title,
    |    url,
    |    token
    |)
    |VALUES (?,?,?)
    """.trimMargin(), 3) {
      bindString(1, title)
      bindString(2, url)
      bindString(3, token)
    }
    notifyQueries(1395811592, {database.serverQueries.selectAll +
        database.serverQueries.selectByID})
  }

  override fun update(
    title: String?,
    url: String?,
    token: String?,
    ID: Long
  ) {
    driver.execute(1740757784, """
    |UPDATE server SET
    |    title = ?,
    |    url = ?,
    |    token = ?
    |WHERE ID = ?
    """.trimMargin(), 4) {
      bindString(1, title)
      bindString(2, url)
      bindString(3, token)
      bindLong(4, ID)
    }
    notifyQueries(1740757784, {database.serverQueries.selectAll +
        database.serverQueries.selectByID})
  }

  override fun deleteByID(ID: Long) {
    driver.execute(698313420, """DELETE FROM server WHERE ID = ?""", 1) {
      bindLong(1, ID)
    }
    notifyQueries(698313420, {database.serverQueries.selectAll + database.serverQueries.selectByID})
  }

  private inner class SelectByIDQuery<out T : Any>(
    @JvmField
    val ID: Long,
    mapper: (SqlCursor) -> T
  ) : Query<T>(selectByID, mapper) {
    override fun execute(): SqlCursor = driver.executeQuery(1572735389,
        """SELECT * FROM server WHERE ID = ?""", 1) {
      bindLong(1, ID)
    }

    override fun toString(): String = "Server.sq:selectByID"
  }
}
