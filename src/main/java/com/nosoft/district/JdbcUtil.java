package com.nosoft.district;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.asyncsql.PostgreSQLClient;
import io.vertx.ext.sql.SQLClient;

public class JdbcUtil {

    private static JdbcUtil jdbcUtil;
    private static SQLClient jdbcClient;
    private static JsonObject config;

    public static JdbcUtil create(Vertx vertx, JsonObject config) {
        if (jdbcUtil == null) {
            JdbcUtil.config = config;
            synchronized (JdbcUtil.class) {
                if (jdbcUtil == null) {
                    jdbcUtil = new JdbcUtil();
                    jdbcUtil.init(vertx);
                }
            }
        }
        return jdbcUtil;
    }

    private void init(Vertx vertx) {
        jdbcClient = PostgreSQLClient.createShared(vertx, config);
    }

    public SQLClient getJDBCClient() {
        return jdbcClient;
    }
}
