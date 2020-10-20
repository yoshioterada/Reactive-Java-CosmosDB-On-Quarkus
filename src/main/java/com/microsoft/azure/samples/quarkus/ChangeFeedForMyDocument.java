package com.microsoft.azure.samples.quarkus;

import com.azure.cosmos.*;
import com.azure.cosmos.models.ChangeFeedProcessorOptions;
import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.azure.samples.quarkus.jsonmapper.TwitterMessageForMe;
import org.jboss.logging.Logger;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * 事前に DB(COSMOS_DB_NAME) を作成し、DB 内に２つのコンテナを作成します。
 *
 * 一つは、ドキュメントを保存するためのコンテナ(DOCUMENT_CONTAINER) もう一つは、Change Feed 用の Lease
 * コンテナ(DOCUMENT_LEASE_CONTAINER)
 *
 * ドキュメントに変更が加わって通知を受けた際の処理を handleChanges(Consumer(List<JsonNode>))内で実装します
 *
 * 単一コンテナの変更監視を行うため、ApplicationScoped のスコープ内で実装しています。
 *
 */

@ApplicationScoped
public class ChangeFeedForMyDocument {

    private static final Logger LOGGER = Logger.getLogger(ChangeFeedForMyDocument.class);

    // 本来は MicroProfile Config や Azure App Configuration 等を利用して
    // 設定情報を外に書き出すべき、機密情報は Azure KeyVault に格納すべきですが
    // ソースコード量を絞り、Cosmos DB の実装部分だけにフォーカスして説明したかったため
    // 今回は、設定情報系は定数に持たせています

    private final static String COSMOS_DB_ENDPOINT = "https://HOSTNAME-cosmosdb.documents.azure.com:443/";
    private final static String CONNECTION_KEY_STRING = "CONNECTION-KEY-STRING-FOR_COSMOSDB";
    private final static String LOGIC_APP_POST_URL = "https://LOGIC-APP.japaneast.logic.azure.com/workflows/006678******************";
    private final static String LOCATION = "Japan East";
    private static ChangeFeedProcessor changeFeedProcessor;
    private static CosmosAsyncClient asyncClient;
    private static String COSMOS_DB_NAME = "MESSAGES";
    private static String DOCUMENT_CONTAINER = "message";
    private static String DOCUMENT_LEASE_CONTAINER = "message-leases";
    private static String FEED_HOST_NAME = "change-feedhost";

    /**
     * Initialize Operation If this Application is started, This will be created the
     * instance of Async client for Cosmos DB
     */

    public void init(@Observes @Initialized(ApplicationScoped.class) Object init) {
        asyncClient = new CosmosClientBuilder().endpoint(COSMOS_DB_ENDPOINT)
                .directMode(DirectConnectionConfig.getDefaultConfig()).key(CONNECTION_KEY_STRING)
                .preferredRegions(Collections.singletonList(LOCATION)).consistencyLevel(ConsistencyLevel.EVENTUAL) // パフォーマンス優先の
                                                                                                                   // EVENTUAL
                .contentResponseOnWriteEnabled(true) // true でレスポンスから作成したオブジェクトを取得可能(データ転送量を少なくするためには false の方が良く推奨)
                .buildAsyncClient();

        CosmosAsyncDatabase asyncDatabase = asyncClient.getDatabase(COSMOS_DB_NAME);
        CosmosAsyncContainer feedContainer = asyncDatabase.getContainer(DOCUMENT_CONTAINER);
        CosmosAsyncContainer leaseContainer = asyncDatabase.getContainer(DOCUMENT_LEASE_CONTAINER);

        changeFeedProcessor = getChangeFeedProcessor(FEED_HOST_NAME, feedContainer, leaseContainer);
        changeFeedProcessor.start().subscribeOn(Schedulers.elastic()).subscribe();
    }

    /**
     * Pre Destroy Operation
     *
     * Close all of Resources
     *
     * @param destroy Destroy 用のオブジェクト
     */

    public void destroy(@Observes @Destroyed(ApplicationScoped.class) Object destroy) {
        changeFeedProcessor.stop().block();
        asyncClient.close();
    }

    /**
     * Create ChangeFeedProcessor Instance
     *
     * ChangeFeedProcessorOptions で指定可能なオプション
     * https://azuresdkdocs.blob.core.windows.net/$web/java/azure-cosmos/4.7.0/com/azure/cosmos/models/ChangeFeedProcessorOptions.html
     *
     * @param hostName       Change Feed 用のホスト名
     * @param feedContainer  ドキュメントを保存するためのコンテナ
     * @param leaseContainer id だけを持つ Change Feed の Lease コンテナ
     * @return ChangeFeedProcessor のインスタンス
     */

    private static ChangeFeedProcessor getChangeFeedProcessor(String hostName, CosmosAsyncContainer feedContainer,
            CosmosAsyncContainer leaseContainer) {
        ChangeFeedProcessorOptions options = new ChangeFeedProcessorOptions();
        options.setFeedPollDelay(Duration.ofSeconds(1)); // ポーリング感覚（デフォルト5秒毎）

        return new ChangeFeedProcessorBuilder().hostName(hostName).feedContainer(feedContainer)
                .leaseContainer(leaseContainer).options(options).handleChanges((List<JsonNode> docs) -> {
                    LOGGER.info("FROM-CHANGE-FEED: " + docs);
                    // データの追加・変更に通知を受信可能

                    Jsonb jsonb = JsonbBuilder.create();
                    docs.stream().map(doc -> jsonb.fromJson(doc.toString(), TwitterMessageForMe.class))
                            .forEach(message -> {
                                String jsonValue = jsonb.toJson(message);
                                LOGGER.info("CHANGE-FEED-UPDATE: " + jsonValue);
                                invokeLogicAppSendToTwitter(jsonValue);
                            });
                }).buildChangeFeedProcessor();
    }

    /**
     * invoke Logic App HTTP URL with JSON data Notify to Another Jobs from Cosmos
     * DB Change Feed
     *
     * @param jsonData Logic App に送信するための JSON データ
     */

    private static void invokeLogicAppSendToTwitter(String jsonData) {
        Mono.just(jsonData).doOnNext(json -> {
            Client client = ClientBuilder.newClient();
            client.target(LOGIC_APP_POST_URL)
                    // .target("http://localhost:8080/rest/check")
                    .request(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                    .post(Entity.json(json), String.class);
            client.close();
        }).doOnError(LOGGER::error).doOnSuccess(response -> LOGGER.info("SUCCESS RESPONSE: " + response)).subscribe();
    }
}
