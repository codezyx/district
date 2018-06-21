package com.nosoft.district;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLClient;

public class JdbcUtil {

    private static JdbcUtil instance;
    private static SQLClient sqlClient;
    private static JsonObject config;

    private JdbcUtil() {
    }

    public static JdbcUtil create(Vertx vertx, JsonObject config) {
        if (instance == null) {
            JdbcUtil.config = config;
            synchronized (JdbcUtil.class) {
                if (instance == null) {
                    instance = new JdbcUtil();
                    instance.init(vertx);
                }
            }
        }
        return instance;
    }

    private void init(Vertx vertx) {
        sqlClient = JDBCClient.createShared(vertx, config);
    }

    public SQLClient getJDBCClient() {
        return sqlClient;
    }
}
