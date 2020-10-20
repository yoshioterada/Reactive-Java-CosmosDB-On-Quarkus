package com.microsoft.azure.samples.quarkus;

import com.azure.cosmos.*;
import com.azure.cosmos.models.*;
import com.azure.cosmos.util.CosmosPagedFlux;
import com.microsoft.azure.samples.quarkus.jsonmapper.*;
import io.quarkus.vertx.web.Body;
import io.quarkus.vertx.web.Param;
import io.quarkus.vertx.web.ReactiveRoutes;
import io.quarkus.vertx.web.Route;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.multi.MultiReactorConverters;
import io.smallrye.mutiny.converters.uni.UniReactorConverters;
import io.vertx.core.http.HttpMethod;
import org.jboss.logging.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@ApplicationScoped
public class ReactiveRouteApp {
    private static final Logger LOGGER = Logger.getLogger(ReactiveRouteApp.class);

    // 本来は MicroProfile Config や Azure App Configuration 等を利用して
    // 設定情報を外に書き出すべき、機密情報は Azure KeyVault に格納すべき
    private final static String COSMOS_DB_ENDPOINT = "https://HOSTNAME-cosmosdb.documents.azure.com:443/";
    private final static String CONNECTION_KEY_STRING = "CONNECTION-KEY-STRING-FOR_COSMOSDB";
    private final static String LOCATION = "Japan East";

    private final static String QUERY_SELECT_ALL = "SELECT * FROM Person P";
    private final static String QUERY_SELECT_BY_ID = "SELECT * FROM Person P WHERE P.id = \"%s\"";
    private final static String QUERY_SELECT_OFFSET_LIMIT = "SELECT * FROM Person P ORDER BY P.age OFFSET %s LIMIT 30";

    private CosmosAsyncClient asyncClient;
    private final static int PREFERRED_PAGE_SIZE = 10;

    /**
     * Initialize Operation If this Application is started, This will be created the
     * instance of Async client for Cosmos DB
     */

    @PostConstruct
    public void init() {
        asyncClient = new CosmosClientBuilder().endpoint(COSMOS_DB_ENDPOINT)
                .directMode(DirectConnectionConfig.getDefaultConfig()).key(CONNECTION_KEY_STRING)
                .preferredRegions(Collections.singletonList(LOCATION)).consistencyLevel(ConsistencyLevel.EVENTUAL) // パフォーマンス優先の
                                                                                                                   // EVENTUAL
                .contentResponseOnWriteEnabled(true) // true でレスポンスから作成したオブジェクトを取得可能(データ転送量を少なくするためには false の方が良く推奨)
                .buildAsyncClient();
    }

    /**
     * Before shutdown the Application This will close the connection of Azure
     * Cosmos DB
     */

    @PreDestroy
    public void destroy() {
        asyncClient.close();
    }

    /*
     * 1. Quarkus Route の基本的な実装方法の紹介
     *
     * @Route アノテーションを使い、Path の Pram を利用可能 JAX-RS に似たような感じで定義ができる。 Route
     * を利用するために下記の依存関係をついか
     *
     * <dependency> <groupId>io.quarkus</groupId>
     * <artifactId>quarkus-vertx-web</artifactId> </dependency>
     *
     * 2. Uni というオブジェクトを返している。 これは、下の実装部で詳しく説明
     * 
     * curl -X GET
     * http://localhost:8080/react-route/database/PERSON_DB/container/personmanage/
     * item
     */

    @Route(path = "/react-route/database/:database/container/:container/item", methods = HttpMethod.GET, produces = "application/json")
    public Uni<List<Person>> listAllPersonGet(@Param("database") String databaseName,
            @Param("container") String containerName) {
        return listPersonFromQuery(databaseName, containerName, QUERY_SELECT_ALL);
    }

    /*
     * 1. 実際の実装部分のコードはコチラ、ここで説明したい事
     *
     * Uni, Multi は Project Reactor でいう所の Mono, Flux に相当
     *
     * Uni, Multi は SmallRye の Mutiny 内で実装されている。 Mutiny は reactive programming
     * のライブラリ
     *
     * Quarkus の ROUTE では Mono, Flux をそのまま返す事はできないため、 Uni, Multi を返す必要がある
     *
     * Mono, Flux で実装したコードは、コンバータを利用して Uni, Multi に変換できる
     *
     * 変換には下記のライブラリを利用 <dependency> <groupId>io.smallrye.reactive</groupId>
     * <artifactId>mutiny-reactor</artifactId> <version>0.9.0</version>
     * </dependency>
     *
     * 2. Cosmos DB のクエリの実装例の紹介
     *
     * Azure CosmosDB Client Library for Java の API はコチラから参照可能
     * https://azure.github.io/azure-sdk-for-java/cosmos.html
     *
     * 基本的には、Project Reactor の Mono や Flux を拡張したクラスを提供し、 これを利用し、Publisher 側の実装を行う
     *
     * 特に、getCosmosDiagnostics() で処理に対する統計情報を取得できるので、 コチラの情報は取得した方が良い。実際にクエリで、どの位の
     * RU (Request Unit)を 消費しているか、また処理にどの程度の時間を要しているのかを把握するために重要。
     *
     * この計測結果を元に、コンテナ作成時の RU を決定するか（金額に影響）、 もしくは、金額内におさえたいならば、消費する RU
     * からどの程度のリクエスト/秒を 実行するかを計測できるようになる（金額とパフォーマンスのバランス調整に利用）。
     *
     */

    private Uni<List<Person>> listPersonFromQuery(String databaseName, String containerName, String query) {
        CosmosAsyncDatabase database = asyncClient.getDatabase(databaseName);
        CosmosAsyncContainer container = database.getContainer(containerName);

        // Execute Cosmos DB Query
        CosmosQueryRequestOptions queryOptions = new CosmosQueryRequestOptions();
        queryOptions.setQueryMetricsEnabled(true);
        CosmosPagedFlux<Person> fluxResponse = container.queryItems(query, queryOptions, Person.class);

        Mono<List<Person>> listMono = fluxResponse
                .handle(response -> LOGGER.info(response.getCosmosDiagnostics().toString())).collectList();
        return Uni.createFrom().converter(UniReactorConverters.fromMono(), listMono);
    }

    /*
     * ここでは、通常の DB と同じように OFFSET や LIMIT でページネータの作成例の紹介
     *
     * QUERY_SELECT_OFFSET_LIMIT =
     * "SELECT * FROM Person P ORDER BY P.age OFFSET %s LIMIT 30";
     *
     * curl -X GET
     * http://localhost:8080/react-route/database/PERSON_DB/container/personmanage/
     * item/offset/3
     */

    @Route(path = "/react-route/database/:database/container/:container/item/offset/:offset", methods = HttpMethod.GET, produces = "application/json")
    public Uni<List<Person>> listOffsetPersonGet(@Param("database") String databaseName,
            @Param("container") String containerName, @Param("offset") String offset) {
        try {
            Integer.parseInt(offset);
        } catch (NumberFormatException nume) {
            return Uni.createFrom().nullItem();
        }

        String query = String.format(QUERY_SELECT_OFFSET_LIMIT, offset);
        return listPersonFromQuery(databaseName, containerName, query);
    }

    /**
     * ここでは、
     * <p>
     * 1. PREFERRED_PAGE_SIZE で指定したサイズ事にデータをまとめ Flux として返している
     * <p>
     * 2. take(5) を指定する事で、最初から 5 番目までのデータを取得している
     * <p>
     * 3. Multi を ReactiveRoutes.asEventStream(multiPersons) でラップして返す事で イベント・ストリームを
     * Server Sent Event (SSE) で返すことができる
     * 
     * curl -X GET
     * http://localhost:8080/react-route/database/PERSON_DB/container/personmanage/item/preferred
     */

    @Route(path = "/react-route/database/:database/container/:container/item/preferred", methods = HttpMethod.GET, produces = "application/json")
    public Multi<List<Person>> listPageNatePersonGet(@Param("database") String databaseName,
            @Param("container") String containerName) {
        CosmosAsyncDatabase database = asyncClient.getDatabase(databaseName);
        CosmosAsyncContainer container = database.getContainer(containerName);

        // Configure the QueryOption in order to get the Diagnostic info
        CosmosQueryRequestOptions queryOptions = new CosmosQueryRequestOptions();
        queryOptions.setQueryMetricsEnabled(true);
        CosmosPagedFlux<Person> pagedFluxResponse = container.queryItems(QUERY_SELECT_ALL, queryOptions, Person.class);

        // Preferred size Request
        Flux<List<Person>> listFlux = pagedFluxResponse
                .handle(response -> LOGGER.info(response.getCosmosDiagnostics().toString())).byPage(PREFERRED_PAGE_SIZE)
                // .take(5)
                .flatMap(response -> Flux.just(response.getResults()));

        // Return the Result as Event Stream (SSE)
        Multi<List<Person>> multiPersons = Multi.createFrom().converter(MultiReactorConverters.fromFlux(), listFlux);
        return ReactiveRoutes.asEventStream(multiPersons);
    }

    /*
     * ここでは、クエリの結果 Flux で返ってきた値を Mono に変換して返す例
     *
     *
     * curl -X GET
     * http://localhost:8080/react-route/database/PERSON_DB/container/personmanage/
     * item/5998c7db-0c90-48b3-be3a-ef8b55f84201
     */

    @Route(path = "/react-route/database/:database/container/:container/item/:id", methods = HttpMethod.GET, produces = "application/json")
    public Uni<Person> listPersonGet(@Param("database") String databaseName, @Param("container") String containerName,
            @Param("id") String id) {
        CosmosAsyncDatabase database = asyncClient.getDatabase(databaseName);
        CosmosAsyncContainer container = database.getContainer(containerName);

        String query = String.format(QUERY_SELECT_BY_ID, id);

        // Configure the QueryOption in order to get the Diagnostic info
        CosmosQueryRequestOptions queryOptions = new CosmosQueryRequestOptions();
        queryOptions.setQueryMetricsEnabled(true);
        CosmosPagedFlux<Person> pagedFluxResponse = container.queryItems(query, queryOptions, Person.class);

        Mono<Person> monoPerson = pagedFluxResponse
                .handle(response -> LOGGER.info(response.getCosmosDiagnostics().toString())).next();

        return Uni.createFrom().converter(UniReactorConverters.fromMono(), monoPerson);
    }

    /**
     * Create Database ここでは DB の作成例
     *
     * curl -X POST -H 'Content-Type:application/json' \
     * localhost:8080/react-route/database/create-database \ -d
     * '{"dbName":"PERSON_DB"}'
     */
    // @Components(requestBodies =
    // @RequestBody(name="dbName",required=true,description = "DatabaseName"))
    @Route(path = "/react-route/database/create-database", methods = HttpMethod.POST, produces = "application/json")
    public Uni<String> createDBCosmosDB(@Body RequestCosmosDB requestCosmosDb) {
        Mono<String> stringMono = asyncClient.createDatabaseIfNotExists(requestCosmosDb.getDbName())
                .doOnSuccess(response -> {
                    LOGGER.info("COMPLETES SUCCESSFULLY to Create Database " + response.getProperties().getId());
                    LOGGER.info(response.getDiagnostics().toString());
                }).doOnError(LOGGER::error).map(dbresponse -> {
                    String createdDBName = dbresponse.getProperties().getId();
                    Instant timestamp = dbresponse.getProperties().getTimestamp();
                    LocalDateTime localDateTime = LocalDateTime.ofInstant(timestamp, ZoneId.of("Asia/Tokyo"));

                    CreatedCosmosDB createdCosmos = new CreatedCosmosDB(createdDBName, localDateTime);
                    Jsonb jsonb = JsonbBuilder.newBuilder().build();
                    return jsonb.toJson(createdCosmos);
                });
        return Uni.createFrom().converter(UniReactorConverters.fromMono(), stringMono);
    }

    /**
     * List All of the Databases in the Cosmos DB
     *
     * curl -X GET http://localhost:8080/react-route/database
     * 
     * @return Uni<List<String>> List all of the Databases in Cosmos DB
     */
    @Route(path = "/react-route/database", methods = HttpMethod.GET, produces = "application/json")
    public Uni<List<String>> listAllDatabases() {
        CosmosPagedFlux<CosmosDatabaseProperties> readAllDbProp = asyncClient.readAllDatabases();
        Mono<List<String>> listMono = readAllDbProp
                .handle(response -> LOGGER.info(response.getCosmosDiagnostics().toString()))
                .flatMap(properties -> Flux.just(properties.getId())).collectList();
        return Uni.createFrom().converter(UniReactorConverters.fromMono(), listMono);
    }

    /**
     * Delete Cosomos DB
     *
     * curl -X DELETE -H 'Content-Type:application/json' \
     * localhost:8080/react-route//database/delete-database \ -d
     * '{"dbName":"PERSON_DB"}'
     *
     * @param database 削除用の DB 名を含む JSON オブジェクトのマッパークラス
     * @return 削除した DB と削除日時を返す文字列
     */

    @Route(path = "/react-route/database/delete-database", methods = HttpMethod.DELETE, produces = "application/json")
    public Uni<String> deleteDBCosmosDB(@Body RequestCosmosDB database) {
        Mono<String> stringMono = asyncClient.getDatabase(database.getDbName()).delete()
                .doOnSuccess(cosmosDatabaseResponse -> {
                    LOGGER.info("DELETED SUCCESSFULLY : " + database.getDbName());
                }).doOnError(LOGGER::error)
                .map(dbResponse -> convertDatabaseResponseToJSON(database.getDbName(), LocalDateTime.now()));
        return Uni.createFrom().converter(UniReactorConverters.fromMono(), stringMono);
    }

    private String convertDatabaseResponseToJSON(String dbName, LocalDateTime localDateTime) {
        CreatedCosmosDB cosmosDB = new CreatedCosmosDB(dbName, localDateTime);
        Jsonb jsonb = JsonbBuilder.newBuilder().build();
        return jsonb.toJson(cosmosDB);

    }

    /**
     * Create Container
     *
     * ここでは、コンテナの作成例 curl -X POST -H 'Content-Type:application/json' \
     * localhost:8080/react-route/database/PERSON_DB/container/create-container \ -d
     * '{"dbName":"PERSON_DB","containerName": "personmanage", "partitionName":
     * "/lastName","requestUnit": 1000}'
     *
     */

    @Route(path = "/react-route/database/:database/container/create-container", methods = HttpMethod.POST, produces = "application/json")
    public Uni<String> createContainerCosmosDB(@Body RequestCosmosContainer container,
            @Param("database") String databaseName) {
        Long counter = asyncClient.readAllDatabases().filter(prop -> prop.getId().equals(databaseName)).count().block();
        if (counter == null || counter < 1) {
            return Uni.createFrom().nullItem();
        }

        CosmosAsyncDatabase database = asyncClient.getDatabase(databaseName);
        CosmosContainerProperties containerProperties = new CosmosContainerProperties(container.getContainerName(),
                container.getPartitionName());
        ThroughputProperties throughputProperties = ThroughputProperties
                .createManualThroughput(container.getRequestUnit());

        Mono<String> stringMono = database.createContainerIfNotExists(containerProperties, throughputProperties)
                .doOnSuccess(response -> {
                    LOGGER.info("COMPLETES SUCCESSFULLY to Create Container : " + response.getProperties().getId());
                    LOGGER.info(response.getDiagnostics().toString());
                }).doOnError(LOGGER::error).map(this::convertResponseToJSONString);
        return Uni.createFrom().converter(UniReactorConverters.fromMono(), stringMono);
    }

    /**
     * List All of the Containers in the DB
     *
     * curl -X GET http://localhost:8080/react-route/database/PERSON_DB/container
     * 
     * @param databaseName DataBase name as String
     * @return List&lt;String&gt; list all of the Container in DB as JSON
     */

    @Route(path = "/react-route/database/:database/container", methods = HttpMethod.GET, produces = "application/json")
    public Uni<List<String>> listAllContainers(@Param("database") String databaseName) {

        Long counter = asyncClient.readAllDatabases().filter(prop -> prop.getId().equals(databaseName)).count().block();
        if (counter == null || counter < 1) {
            Uni.createFrom().nullItem();
        }

        CosmosAsyncDatabase database = asyncClient.getDatabase(databaseName);
        CosmosPagedFlux<CosmosContainerProperties> readContainerProp = database.readAllContainers();

        Mono<List<String>> listMono = readContainerProp
                .handle(response -> LOGGER.info(response.getCosmosDiagnostics().toString()))
                .flatMap(properties -> Flux.just(properties.getId())).collectList();

        return Uni.createFrom().converter(UniReactorConverters.fromMono(), listMono);
    }

    /**
     * Delete Container
     * 
     * curl -X DELETE -H 'Content-Type:application/json'
     * localhost:8080/react-route/database/PERSON_DB/container/delete-container -d
     * '{"dbName":"PERSON_DB","containerName": "personmanage"}'
     *
     * @param container 削除するコンテナ名を含む JSON データのマッパークラス
     * @return 削除したコンテナ名と削除日時を返す
     */

    @Route(path = "/react-route/database/:database/container/delete-container", methods = HttpMethod.DELETE, produces = "application/json")
    public Uni<String> deleteContainer(@Body RequestCosmosContainer container, @Param("database") String databaseName) {
        Long counter = asyncClient.readAllDatabases().filter(prop -> prop.getId().equals(databaseName)).count().block();
        if (counter == null || counter < 1) {
            return Uni.createFrom().nullItem();
        }
        CosmosAsyncDatabase asyncDatabase = asyncClient.getDatabase(databaseName);
        CosmosAsyncContainer asyncContainer = asyncDatabase.getContainer(container.getContainerName());
        if (asyncContainer == null) {
            return Uni.createFrom().nullItem();
        }
        Mono<String> stringMono = asyncContainer.delete().doOnSuccess(response -> {
            LOGGER.info("COMPLETES SUCCESSFULLY to Delete Container : " + container.getContainerName());
            LOGGER.info(response.getDiagnostics().toString());
        }).doOnError(LOGGER::error)
                .map(response -> convertCreateObjectToString(container.getContainerName(), LocalDateTime.now()));
        return Uni.createFrom().converter(UniReactorConverters.fromMono(), stringMono);
    }

    private String convertResponseToJSONString(CosmosContainerResponse response) {
        String containerName = response.getProperties().getId();
        Instant timestamp = response.getProperties().getTimestamp();
        LocalDateTime localDateTime = LocalDateTime.ofInstant(timestamp, ZoneId.of("Asia/Tokyo"));
        return convertCreateObjectToString(containerName, localDateTime);
    }

    private String convertCreateObjectToString(String containerName, LocalDateTime localDateTime) {
        CreatedCosmosContainer cosmosContainer = new CreatedCosmosContainer(containerName, localDateTime);
        Jsonb jsonb = JsonbBuilder.newBuilder().build();
        return jsonb.toJson(cosmosContainer);
    }

    /**
     * Create One Person Item
     *
     * curl -X POST -H 'Content-Type:application/json' http://localhost:8080/react-route/database/PERSON_DB/container/personmanage/item/addItem -d '{"firstName": "a", "lastName": "b","age": 39}'
     *
     * @param person Person data convert from JSON of HTTP Body
     * @param databaseName  Database Name
     * @param containerName Container Name
     * @return Person data which succeeded
     */
    @Route(path = "/react-route/database/:database/container/:container/item/addItem", methods = HttpMethod.POST, produces = "application/json")
    public Uni<Person> createItemCosmosDB(@Body Person person, @Param("database") String databaseName,
            @Param("container") String containerName) {
        Long counter = asyncClient.readAllDatabases().filter(prop -> prop.getId().equals(databaseName)).count().block();
        if (counter == null || counter < 1) {
            return Uni.createFrom().nullItem();
        }
        CosmosAsyncDatabase asyncDatabase = asyncClient.getDatabase(databaseName);
        CosmosAsyncContainer asyncContainer = asyncDatabase.getContainer(containerName);
        if (asyncContainer == null) {
            return Uni.createFrom().nullItem();
        }

        person.setId(UUID.randomUUID().toString());

        Mono<Person> successPerson = asyncContainer.createItem(person)
                .doOnSuccess(cosmosItemResponse -> {
                    LOGGER.info(cosmosItemResponse.getDiagnostics());
                    Person returnedPerson = cosmosItemResponse.getItem();
                    LOGGER.info("SUCCEEDED to Create Item: " + returnedPerson);
                }).doOnError(LOGGER::error)
                .map(cosmosItemResponse -> cosmosItemResponse.getItem());
        return Uni.createFrom().converter(UniReactorConverters.fromMono(), successPerson);
    }

    /**
     * Create Dummy Item into Container in CosmosDB <p> curl -X POST
     * "http://localhost:8080/react-route/database/PERSON_DB/container/personmanage/item/addDummyItems"
     * \ -H "accept: application/json" \ -H "Content-Type: application/json"
     */
/*
    @Route(path = "/react-route/database/:database/container/:container/item/addDummyItems", methods = HttpMethod.POST, produces = "application/json")
    public Uni<List<Person>> createDummyItemCosmosDB(@Param("database") String databaseName,
            @Param("container") String containerName) {
        List<Person> persons = createDummyPersons();

        CosmosAsyncDatabase database = asyncClient.getDatabase(databaseName);
        CosmosAsyncContainer container = database.getContainer(containerName);

        Mono<List<Person>> listMono = Flux.fromIterable(persons).delayElements(Duration.ofMillis(10))
                .flatMap(container::createItem).map(response -> {
                    LOGGER.info(response.getDiagnostics().toString());
                    return response.getItem();
                }).collectList();
        return Uni.createFrom().converter(UniReactorConverters.fromMono(), listMono);
    }*/

    /*
     * This method create Dummy Data.
     */
    static List<Person> createDummyPersons() {
        int counter = 1;
        int range = 1000;

        List<Person> persons = new ArrayList<>();
        IntStream.rangeClosed(counter, counter + range).forEach(value -> {
            Person person = new Person();
            person.setId(UUID.randomUUID().toString());
            person.setFirstName("Yoshio" + value);
            person.setLastName("Terada" + value);
            person.setAge(value);
            persons.add(person);
        });
        return persons;
    }
}
