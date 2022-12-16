package fr.openent.statistics_presences;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import fr.openent.presences.common.helper.SharedDataHelper;
import fr.openent.statistics_presences.bean.Failure;
import fr.openent.statistics_presences.bean.Report;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.powermock.reflect.Whitebox;

import java.util.List;

@RunWith(PowerMockRunner.class) //Using the PowerMock runner
@PowerMockRunnerDelegate(VertxUnitRunner.class) //And the Vertx runner
@PrepareForTest({Vertx.class}) //Prepare the static class you want to test
@PowerMockIgnore("javax.management.*")
public class SharedDataTest {

    private final HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(new Config());
    private final static String MAP_NAME = "map";
    private final static String KEY = "key";

    @Test
    public void testWithoutClusterWhitJson(TestContext ctx) {
        Async async = ctx.async();
        Vertx vertx = Vertx.vertx();
        //Get object first time return null
        SharedDataHelper.getObjectFromAsyncMap(vertx, MAP_NAME, KEY, JsonObject.class, false)
                .compose(jsonObject -> {
                    ctx.assertNull(jsonObject);
                    //Set the object in vertx
                    return SharedDataHelper.setObjectFromAsyncMap(vertx, MAP_NAME, KEY, new JsonObject().put("name", "my super name"), false);
                }).compose(unused -> {
                    //Get the object from vertx (shared map)
                    return SharedDataHelper.getObjectFromAsyncMap(vertx, MAP_NAME, KEY, JsonObject.class, false);
                }).compose(jsonObject -> {
                    //Test we have good value in json
                    ctx.assertEquals(jsonObject.getString("name"), "my super name");
                    //Edit json and update it with vertx
                    jsonObject.put("name", "other name");
                    return SharedDataHelper.setObjectFromAsyncMap(vertx, MAP_NAME, KEY, jsonObject, false);
                }).compose(unused -> {
                    //Get the object from vertx (shared map)
                    return SharedDataHelper.getObjectFromAsyncMap(vertx, MAP_NAME, KEY, JsonObject.class, false);
                }).compose(jsonObject -> {
                    //Test we have good value in json
                    ctx.assertEquals(jsonObject.getString("name"), "other name");
                    //Set value in local map in vertx
                    return SharedDataHelper.setObjectFromAsyncMap(vertx, MAP_NAME, KEY, 3, true);
                }).compose(unused -> {
                    //Get the object in local map from vertx
                    return SharedDataHelper.getObjectFromAsyncMap(vertx, MAP_NAME, KEY, Integer.class, true);
                }).compose(integer -> {
                    //We have the good value save in the vertx local map
                    ctx.assertEquals(integer, 3);
                    //Get the object from vertx (shared map)
                    return SharedDataHelper.getObjectFromAsyncMap(vertx, MAP_NAME, KEY, Integer.class, false);
                }).onSuccess(integer -> {
                    //In non cluster mod, the local map is the map that the share map,
                    //so we must not use the same name of map and key between local and share.
                    //Otherwise, the data will be overwritten, like here.
                    ctx.assertEquals(integer, 3);
                    async.complete();
                }).onFailure(ctx::fail);

        async.awaitSuccess(10000);
    }

    @Test
    public void testClusterWhitJson(TestContext ctx) {
        Async async = ctx.async();
        PowerMockito.spy(Vertx.class);

        Future<Vertx> vertxFuture1 = this.clusterVertx();

        vertxFuture1.compose(vertx -> this.clusterVertx())
                .onSuccess(vertx2 -> {
                    Vertx vertx1 = vertxFuture1.result();
                    //Now we have 2 vertx on the same cluster

                    PowerMockito.when(Vertx.vertx()).thenReturn(vertx1);
                    ctx.assertTrue(vertx1.isClustered());
                    ctx.assertTrue(vertx2.isClustered());

                    //Get object first time return null for 2 vertx
                    SharedDataHelper.getObjectFromAsyncMap(vertx1, MAP_NAME, KEY, JsonObject.class, false)
                            .compose(jsonObject -> {
                                ctx.assertNull(jsonObject);
                                return SharedDataHelper.getObjectFromAsyncMap(vertx2, MAP_NAME, KEY, JsonObject.class, false);
                            }).compose(jsonObject -> {
                                ctx.assertNull(jsonObject);
                                //Set the object in cluster by vertx1
                                return SharedDataHelper.setObjectFromAsyncMap(vertx1, MAP_NAME, KEY, new JsonObject().put("name", "my super name"), false);
                            }).compose(unused -> {
                                //Get the object in cluster from vertx1
                                return SharedDataHelper.getObjectFromAsyncMap(vertx1, MAP_NAME, KEY, JsonObject.class, false);
                            }).compose(jsonObject -> {
                                //Test we have good value in json
                                ctx.assertEquals(jsonObject.getString("name"), "my super name");
                                //Get the object in cluster from vertx2
                                return SharedDataHelper.getObjectFromAsyncMap(vertx2, MAP_NAME, KEY, JsonObject.class, false);
                            }).compose(jsonObject -> {
                                //Test we have good value in json
                                ctx.assertEquals(jsonObject.getString("name"), "my super name");
                                //Edit json and update it with vertx2
                                jsonObject.put("name", "other name");
                                return SharedDataHelper.setObjectFromAsyncMap(vertx2, MAP_NAME, KEY, jsonObject, false);
                            }).compose(unused -> {
                                //Get the object in cluster from vertx1
                                return SharedDataHelper.getObjectFromAsyncMap(vertx1, MAP_NAME, KEY, JsonObject.class, false);
                            }).compose(jsonObject -> {
                                //Test we have good value in json
                                ctx.assertEquals(jsonObject.getString("name"), "other name");
                                //Set value in local map in vertx2
                                return SharedDataHelper.setObjectFromAsyncMap(vertx2, MAP_NAME, KEY, 3, true);
                            }).compose(unused -> {
                                //Get the object in cluster from vertx1
                                return SharedDataHelper.getObjectFromAsyncMap(vertx1, MAP_NAME, KEY, JsonObject.class, false);
                            }).compose(jsonObject -> {
                                //Test we have not erased value in cluster
                                ctx.assertEquals(jsonObject.getString("name"), "other name");
                                //Get the object in local map from vertx1
                                return SharedDataHelper.getObjectFromAsyncMap(vertx1, MAP_NAME, KEY, Integer.class, true);
                            }).compose(jsonObject -> {
                                //We dont have value define in local from vertx1
                                ctx.assertNull(jsonObject);
                                //Get the object in local map from vertx2
                                return SharedDataHelper.getObjectFromAsyncMap(vertx2, MAP_NAME, KEY, Integer.class, true);
                            }).onSuccess(integer -> {
                                //We have the good value save in the vertx2 local map
                                ctx.assertEquals(integer, 3);
                                async.complete();
                            }).onFailure(ctx::fail);
                })
                .onFailure(ctx::fail);
        async.awaitSuccess(10000);
    }

    @Test
    public void testClusterWhitSharedDataModel(TestContext ctx) {
        Async async = ctx.async();
        PowerMockito.spy(Vertx.class);

        Future<Vertx> vertxFuture1 = this.clusterVertx();

        vertxFuture1.compose(vertx -> this.clusterVertx())
                .onSuccess(vertx2 -> {
                    Vertx vertx1 = vertxFuture1.result();
                    PowerMockito.when(Vertx.vertx()).thenReturn(vertx1);
                    ctx.assertTrue(vertx1.isClustered());
                    SharedDataHelper.loadPresenceData(vertx1, new Report())
                            .compose(report -> report.start().fail(new Failure("user", "structure", new Throwable("message"))).save(vertx1))
                            .compose(report -> {
                                List<Failure> list = Whitebox.getInternalState(report, "failures");
                                ctx.assertEquals(list.size(), 1);
                                return SharedDataHelper.loadPresenceData(vertx1, new Report());
                            }).compose(report -> {
                                List<Failure> list = Whitebox.getInternalState(report, "failures");
                                ctx.assertEquals(list.size(), 1);
                                PowerMockito.when(Vertx.vertx()).thenReturn(vertx2);
                                return SharedDataHelper.loadPresenceData(vertx2, new Report());
                            }).onSuccess(report -> {
                                List<Failure> list = Whitebox.getInternalState(report, "failures");
                                ctx.assertEquals(list.size(), 1);
                                async.complete();
                            }).onFailure(ctx::fail);
                })
                .onFailure(ctx::fail);
        async.awaitSuccess(10000);
    }

    private Future<Vertx> clusterVertx() {
        Promise<Vertx> promise = Promise.promise();

        ClusterManager mgr = new HazelcastClusterManager(this.hazelcastInstance);

        VertxOptions options = new VertxOptions().setClusterManager(mgr);

        Vertx.clusteredVertx(options, res -> {
            if (res.succeeded()) {
                promise.complete(res.result());
            } else {
                promise.fail(res.cause());
            }
        });
        return promise.future();
    }
}
