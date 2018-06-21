package com.nosoft.district;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class MainVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);
    private Map<String, List<JsonObject>> cache = new ConcurrentHashMap<>();
    private static final int FUTURE_CODE_OK = 1;
    private static final String CODE_CHINA = "100000";
    private static final String TABLE_NAME = "t_district";
    private static final String TABLE_NAME_LAST = "t_district_last";
    /**
     * 定时任务ID
     */
    private long timerId;
    /**
     * app.json配置信息
     */
    private JsonObject config;

    @Override
    public void start(Future<Void> future) {
        Future<JsonObject> initFuture = initConfig();
        initFuture.setHandler(result -> {
            if (result.succeeded()) {//配置读取成功才启动服务和定时任务
                LOGGER.info("Init configuration ok, configuration:" + result.result());
                createHttpServer();
                createUpdateJob();
            } else {
                LOGGER.error("Init configuration error", result.cause());
            }
        });
    }

    private Future<JsonObject> initConfig() {
        Future<JsonObject> future = Future.future();
        ConfigStoreOptions fileStore = new ConfigStoreOptions()
            .setType("file")
            .setConfig(new JsonObject().put("path", "app.json"));
        ConfigRetrieverOptions options = new ConfigRetrieverOptions().addStore(fileStore);
        ConfigRetriever retriever = ConfigRetriever.create(vertx, options);
        retriever.getConfig(ar -> {
            if (ar.failed()) {
                LOGGER.error("Retrieve configuration file error", ar.cause());
                future.fail(ar.cause());
            } else {
                config = ar.result();
                future.complete(ar.result());
            }
        });
        return future;
    }

    private void createUpdateJob() {
        long period = config.getLong("job.period", 1000L * 3600 * 24);
        LOGGER.info("Update job period:" + period + " milliseconds");
        timerId = vertx.setPeriodic(period, id -> updateJob());
    }

    private void createHttpServer() {
        Router router = Router.router(vertx);
        router.route("/").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "text/html").end("<h1>Hello </h1>");
        });
        router.get("/district/api/index").handler(this::getDistrict);
        router.get("/district/api/update").handler(this::update);
        int serverPort = config.getInteger("http.port", 9000);
        vertx.createHttpServer()
            .requestHandler(router::accept)
            .listen(serverPort,
                result -> {
                    if (result.succeeded()) {
                        LOGGER.info("Http server listening on " + serverPort);
                    } else {
                        LOGGER.error("Start http server error ", result.cause());
                    }
                }
            );
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        vertx.cancelTimer(timerId);
    }

    /**
     * 先从缓存里面查询有没有数据，如果没有再去数据库查询
     *
     * @param routingContext RoutingContext
     */
    private void getDistrict(RoutingContext routingContext) {
        String codeParam = routingContext.request().getParam("code");
        String code = codeParam == null ? CODE_CHINA : codeParam;
        List<JsonObject> list = cache.get(code);//先从缓存里面查询
        if (list != null && !list.isEmpty()) {
            LOGGER.info("/api/district cache hit, code:" + codeParam);
            outJson(routingContext, new JsonObject().put("result", list));
        } else {
            JdbcUtil jdbc = JdbcUtil.create(vertx, config);
            SQLClient client = jdbc.getJDBCClient();
            String sql = "select adcode,name from " + TABLE_NAME + " where parent='" + code + "' order by adcode";
            client.getConnection(conn -> {
                if (conn.failed()) {
                    LOGGER.error("/api/district error.SQLClient.getConnection", conn.cause());
                    outJson(routingContext, new JsonObject().put("result", Collections.emptyList()));
                    return;
                }
                SQLConnection sqlConnection = conn.result();
                sqlConnection.query(sql, res -> {
                    if (res.succeeded()) {
                        List<JsonObject> rows = res.result().getRows();
                        if (rows != null && !rows.isEmpty()) {
                            LOGGER.info("/api/district query from db succeed, code:" + code);
                            cache.put(code, rows);
                            outJson(routingContext, new JsonObject().put("result", rows));
                        } else {
                            getDistrictFromBackup(routingContext, code);//主表没查到，查备份表
                        }
                    } else {
                        outJson(routingContext, new JsonObject().put("result", Collections.emptyList()));
                    }
                });
                sqlConnection.close();
            });
        }
    }

    private void getDistrictFromBackup(RoutingContext routingContext, String code) {
        JdbcUtil jdbc = JdbcUtil.create(vertx, config);
        SQLClient client = jdbc.getJDBCClient();
        String sql = "select adcode,name from " + TABLE_NAME_LAST + " where parent='" + code + "' order by adcode";
        client.getConnection(conn -> {
            if (conn.failed()) {
                LOGGER.error("/api/district error.SQLClient.getConnection", conn.cause());
                outJson(routingContext, new JsonObject().put("result", Collections.emptyList()));
                return;
            }
            SQLConnection sqlConnection = conn.result();
            sqlConnection.query(sql, res -> {
                if (res.succeeded()) {
                    List<JsonObject> rows = res.result().getRows();
                    if (rows != null && !rows.isEmpty()) {
                        LOGGER.info("/api/district get district from backup table succeed, code:" + code);
                        cache.put(code, rows);
                        outJson(routingContext, new JsonObject().put("result", rows));
                    } else {
                        outJson(routingContext, new JsonObject().put("result", Collections.emptyList()));
                    }
                }
                outJson(routingContext, new JsonObject().put("result", Collections.emptyList()));
            });
            sqlConnection.close();
        });
    }

    private void update(RoutingContext routingContext) {
        getRemoteApi().setHandler(result -> {
            if (result.succeeded()) {
                updateDistrict(result.result()).setHandler(handler -> {
                    if (handler.succeeded()) {
                        LOGGER.info("/api/update succeed.");
                        outJson(routingContext, new JsonObject().put("status", 1).put("msg", "ok"));
                    } else {
                        outJson(routingContext, new JsonObject().put("status", -1).put("msg", "failed").put("error", handler.cause()));
                        LOGGER.info("/api/update failed.", handler.cause());
                    }
                });
            } else {
                LOGGER.error("/api/update error ", result.cause());
                outJson(routingContext, new JsonObject().put("status", -1).put("msg", "failed").put("error", result.cause()));
            }
        });
    }

    private void updateJob() {
        LOGGER.info("Update job start at " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        clearDistrict(TABLE_NAME_LAST).setHandler(result -> {//清空备份表
            if (result.succeeded()) {
                backupDistrict().setHandler(backupResult -> {//复制正式表数据到备份表
                    if (backupResult.succeeded()) {
                        clearDistrict(TABLE_NAME).setHandler(clearDistrictResult -> {//清空正式表
                            if (clearDistrictResult.succeeded()) {
                                remoteUpdate();//从远程更新数据到正式表
                            } else {
                                LOGGER.error("Clear table " + TABLE_NAME + " error", clearDistrictResult.cause());
                            }
                        });
                    } else {
                        LOGGER.error("Backup district error", backupResult.cause());
                    }
                });
            } else {
                LOGGER.error("Clear table " + TABLE_NAME_LAST + " error", result.cause());
            }
        });
    }

    private void remoteUpdate() {
        getRemoteApi().setHandler(result -> {
            if (result.succeeded()) {
                JsonObject resultJson = result.result();
                LOGGER.info("Update from remote server.{status:" + resultJson.getString("status") + ",info:" + resultJson.getString("info") + "}");
                updateDistrict(result.result());
            } else {
                LOGGER.error("Update job error ", result.cause());
            }
        });
    }

    private Future<JsonObject> getRemoteApi() {
        Future<JsonObject> future = Future.future();
        WebClient client = WebClient.create(vertx);
        String param = config.getString("api.param");
        String host = config.getString("api.host", "restapi.amap.com");
        String requestURI = config.getString("api.uri", "/v3/config/district");
        client.get(config.getInteger("api.port", 80), host, requestURI + param).send(ar -> {
            if (ar.succeeded()) {
                io.vertx.ext.web.client.HttpResponse<Buffer> response = ar.result();
                JsonObject obj = new JsonObject(response.body().toString("utf-8"));
                if (response.statusCode() == 200 && "1".equals(obj.getString("status"))) {
                    future.complete(obj);
                } else {
                    future.fail("Get remote api error");
                }
            } else {
                future.fail(ar.cause());
            }
        });
        return future;
    }

    private Future<Integer> updateDistrict(JsonObject obj) {
        Future<Integer> future = Future.future();
        StringBuilder sql = new StringBuilder("insert into " + TABLE_NAME + "(id, adcode,name,parent,level,update_time) values ");
        JsonArray districts = obj.getJsonArray("districts");
        if (null != districts && districts.size() > 0) {
            buildSqlList(districts, null, sql);
        }
        JdbcUtil jdbc = JdbcUtil.create(vertx, config);
        SQLClient client = jdbc.getJDBCClient();
        client.getConnection(conn -> {
            if (conn.failed()) {
                LOGGER.error("Update district error.SQLClient.getConnection failed",conn.cause());
                future.fail(conn.cause());
                return;
            }
            SQLConnection sqlConnection = conn.result();
            sqlConnection.execute(sql.substring(0, sql.length() - 1), handler -> {
                if (handler.succeeded()) {
                    LOGGER.info("Update job end, save districts to table[" + TABLE_NAME + "] succeed");
                    cache.clear();
                    future.complete(FUTURE_CODE_OK);
                } else {
                    LOGGER.error("Update district save districts to table[" + TABLE_NAME + "] failed", handler.cause());
                    future.fail(handler.cause());
                }
            });
            sqlConnection.close();
        });
        return future;
    }

    private Future<Integer> backupDistrict() {
        Future<Integer> future = Future.future();
        String sql = "insert into " + TABLE_NAME_LAST + "(id, adcode,name,parent,level,update_time) select id, adcode,name,parent,level,update_time from " + TABLE_NAME;
        JdbcUtil jdbc = JdbcUtil.create(vertx, config);
        SQLClient client = jdbc.getJDBCClient();
        client.getConnection(conn -> {
            if (conn.failed()) {
                LOGGER.error("Backup district error.SQLClient.getConnection failed", conn.cause());
                return;
            }
            SQLConnection sqlConnection = conn.result();
            sqlConnection.execute(sql, handler -> {
                if (handler.succeeded()) {
                    LOGGER.info("Backup districts succeed");
                    future.complete(FUTURE_CODE_OK);
                } else {
                    LOGGER.error("Backup district failed", handler.cause());
                    future.fail(handler.cause());
                }
            });
            sqlConnection.close();
        });
        return future;
    }

    private Future<Integer> clearDistrict(String table) {
        Future<Integer> future = Future.future();
        JdbcUtil jdbc = JdbcUtil.create(vertx, config);
        SQLClient client = jdbc.getJDBCClient();
        client.getConnection(conn -> {
            if (conn.failed()) {
                LOGGER.error("Clear table[" + table + "] error.SQLClient.getConnection failed", conn.cause());
                return;
            }
            SQLConnection sqlConnection = conn.result();
            sqlConnection.execute("delete from " + table, handler -> {
                if (handler.succeeded()) {
                    LOGGER.info("Delete districts from table[" + table + "] succeed");
                    future.complete(FUTURE_CODE_OK);
                } else {
                    LOGGER.error("Delete districts from table[" + table + "] failed", handler.cause());
                    future.fail(handler.cause());
                }
            });
            sqlConnection.close();
        });
        return future;
    }

    private void buildSqlList(JsonArray array, JsonObject parent, StringBuilder sqlList) {
        if (parent == null) {
            JsonObject object = array.getJsonObject(0);
            buildSqlList(object.getJsonArray("districts"), object, sqlList);
        } else {
            if (null != array && array.size() > 0) {
                for (int i = 0; i < array.size(); i++) {
                    JsonObject obj = array.getJsonObject(i);
                    String sql = genSql(obj, parent.getString("adcode"));
                    if (null != sql) {
                        sqlList.append(sql);
                    }
                    buildSqlList(obj.getJsonArray("districts"), obj, sqlList);
                }
            }
        }
    }

    private String genSql(JsonObject obj, String parentCode) {
        if (null != obj && null != parentCode) {
            StringBuilder sql = new StringBuilder("(");
            sql.append("'").append(randomUUID()).append("'");
            sql.append(",'").append(obj.getString("adcode")).append("'");
            sql.append(",'").append(obj.getString("name")).append("'");
            sql.append(",'").append(parentCode).append("'");
            sql.append(",'").append(obj.getString("level")).append("'");
            sql.append(",'").append(new Timestamp(System.currentTimeMillis())).append("'");
            sql.append("),");
            return sql.toString();
        }
        return null;
    }

    private void outJson(RoutingContext routingContext, Object obj) {
        routingContext.response()
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(Json.encodePrettily(obj));
    }

    private String randomUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
