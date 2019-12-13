package io.vertx.starter.database;

import io.vertx.core.json.JsonObject;
import io.vertx.starter.database.enums.SqlQuery;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class SqlLoader {
  private static final String CONFIG_WIKIDB_SQL_QUERIES_RESOURCE_FILE = "wikidb.sqlqueries.resource.file";

  public static Map<SqlQuery,String> loadSqlQueries(JsonObject config) throws IOException {
    Map<SqlQuery, String> sqlQueries = null;
    String queriesFile = config.getString(CONFIG_WIKIDB_SQL_QUERIES_RESOURCE_FILE);
    InputStream queriesInputStream;
    if (queriesFile != null) {
      queriesInputStream = new FileInputStream(queriesFile);
    } else {
      queriesInputStream = SqlLoader.class.getResourceAsStream("/db-queries.properties");
    }

    Properties queriesProps = new Properties();
    queriesProps.load(queriesInputStream);
    queriesInputStream.close();

    sqlQueries = new HashMap<>();
    sqlQueries.put(SqlQuery.CREATE_PAGES_TABLE, queriesProps.getProperty("create-pages-table"));
    sqlQueries.put(SqlQuery.ALL_PAGES, queriesProps.getProperty("all-pages"));
    sqlQueries.put(SqlQuery.GET_PAGE, queriesProps.getProperty("get-page"));
    sqlQueries.put(SqlQuery.CREATE_PAGE, queriesProps.getProperty("create-page"));
    sqlQueries.put(SqlQuery.SAVE_PAGE, queriesProps.getProperty("save-page"));
    sqlQueries.put(SqlQuery.DELETE_PAGE, queriesProps.getProperty("delete-page"));

    return sqlQueries;
  }
}
