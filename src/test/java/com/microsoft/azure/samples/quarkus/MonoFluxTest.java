package com.microsoft.azure.samples.quarkus;

import com.microsoft.azure.samples.quarkus.jsonmapper.Person;
import io.quarkus.test.junit.QuarkusTest;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.*;
import reactor.core.scheduler.Schedulers;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@QuarkusTest
public class MonoFluxTest {

    private static final Logger LOGGER = Logger.getLogger(MonoFluxTest.class);

    // Mono インスタンスの生成
    // Mono 0 or 1 のデータ・ソース
    @Test
    public void monoCreate() {
        String hello = "Hello";
        Mono<String> monoHello = Mono.just(hello); // String から生成

        Person person = new Person();
        person.setAge(47);
        person.setFirstName("Yoshio");
        person.setLastName("Terada");
        Mono<Person> just = Mono.just(person); // Person オブジェクトから生成

        Mono empty = Mono.empty(); // 空の Mono を生成
    }

    /* 実行結果
2020-10-24 00:45:13,269 INFO  [io.quarkus] (main) Quarkus 1.9.0.CR1 on JVM started in 4.270s. Listening on: http://0.0.0.0:8081
2020-10-24 00:45:13,276 INFO  [io.quarkus] (main) Profile test activated.
2020-10-24 00:45:13,276 INFO  [io.quarkus] (main) Installed features: [cdi, mutiny, resteasy-jackson, resteasy-mutiny, smallrye-context-propagation, smallrye-openapi, swagger-ui, vertx, vertx-web]
2020-10-24 00:45:13,970 INFO  [io.quarkus] (main) Quarkus stopped in 0.030s

Process finished with exit code 0
     */
    
    // Flux インスタンスの生成
    // Flux 複数のデータ・ソース
    @Test
    public void fluxCreate() {
        Publisher<String> publisher = Flux.just("Hello", "World");
        Flux<String> fromPublisher = Flux.from(publisher);// Publisher から生成

        String[] strArray = {"string1", "string2"};
        Flux<String> fromArray = Flux.fromArray(strArray);

        List<String> list = Arrays.asList(strArray);
        Flux<String> fromItterable = Flux.fromIterable(list); // Iterable から生成
        Flux<List<String>> fromJust = Flux.just(list); // List<String> から生成

        Stream<String> strStream = list.stream();
        Flux<String> fromStream = Flux.fromStream(strStream); // Stream から生成

        Flux<Integer> integerFlux = Flux.range(0, 100); //range から生成
        Flux<Void> empty = Flux.empty(); //空の Flux を生成
    }

    /* 実行結果
2020-10-24 00:46:13,148 INFO  [io.quarkus] (main) Quarkus 1.9.0.CR1 on JVM started in 4.518s. Listening on: http://0.0.0.0:8081
2020-10-24 00:46:13,157 INFO  [io.quarkus] (main) Profile test activated.
2020-10-24 00:46:13,157 INFO  [io.quarkus] (main) Installed features: [cdi, mutiny, resteasy-jackson, resteasy-mutiny, smallrye-context-propagation, smallrye-openapi, swagger-ui, vertx, vertx-web]
2020-10-24 00:46:14,057 INFO  [io.quarkus] (main) Quarkus stopped in 0.035s

Process finished with exit code 0
     */

    @Test
    public void subscribeMonoTest() {
        Person person = new Person();
        person.setAge(47);
        person.setFirstName("Yoshio");
        person.setLastName("Terada");
        Mono<Person> just = Mono.just(person); // Person オブジェクトから生成

        // Publisher だけを生成して Subscribe は別の所でするならばコチラ
        Mono<Person> personMono = just.doOnNext(requestPerson -> LOGGER.info("Last Name: " + person.getLastName()))
                .doOnError(LOGGER::error)
                .doOnSuccess(succeedPerson -> LOGGER.info("SUCCEEDED PERSON Operation: " + succeedPerson));
        //別の所で Subscribe
        personMono.subscribe();

        //自分のコード内で Publish して Subscribe するならばコチラ
        just.subscribe(requestPerson -> LOGGER.info("Last Name: " + person.getLastName()),
                LOGGER::error,
                () -> LOGGER.info("SUCCEEDED PERSON Operation"));
    }

    /* 実行結果
2020-10-24 00:47:05,111 INFO  [io.quarkus] (main) Quarkus 1.9.0.CR1 on JVM started in 4.214s. Listening on: http://0.0.0.0:8081
2020-10-24 00:47:05,126 INFO  [io.quarkus] (main) Profile test activated.
2020-10-24 00:47:05,126 INFO  [io.quarkus] (main) Installed features: [cdi, mutiny, resteasy-jackson, resteasy-mutiny, smallrye-context-propagation, smallrye-openapi, swagger-ui, vertx, vertx-web]
2020-10-24 00:47:05,869 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Last Name: Terada
2020-10-24 00:47:05,913 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) SUCCEEDED PERSON Operation: Person(id=null, firstName=Yoshio, lastName=Terada, age=47)
2020-10-24 00:47:05,914 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Last Name: Terada
2020-10-24 00:47:05,914 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) SUCCEEDED PERSON Operation
2020-10-24 00:47:05,958 INFO  [io.quarkus] (main) Quarkus stopped in 0.031s

Process finished with exit code 0
     */

    @Test
    public void subscribeFluxTest() throws Exception {

        List<String> list = Arrays.asList("dog", "cat", "bird", "mouse", "fish", "lion");
        Flux<List<String>> fromList1 = Flux.just(list);
        Flux<List<String>> fromList2 = Flux.just(list);

        fromList1.doOnNext(listString -> listString.forEach(LOGGER::info))
                .doOnError(LOGGER::error)
                .doOnComplete(() -> LOGGER.info("COMPLETED Operation for Flux 1"))
                .subscribe();

        fromList2.subscribe(listString -> listString.forEach(LOGGER::info),
                LOGGER::error,
                () -> LOGGER.info("COMPLETED Operation for Flux 2"));

        Flux.interval(Duration.ofMillis(100))
                .subscribe(intVal -> LOGGER.info("Number: " + intVal));
        new CountDownLatch(1).await(5000, TimeUnit.MILLISECONDS);
    }

    /* 実行結果
2020-10-24 00:49:31,975 INFO  [io.quarkus] (main) Quarkus 1.9.0.CR1 on JVM started in 4.066s. Listening on: http://0.0.0.0:8081
2020-10-24 00:49:31,985 INFO  [io.quarkus] (main) Profile test activated.
2020-10-24 00:49:31,985 INFO  [io.quarkus] (main) Installed features: [cdi, mutiny, resteasy-jackson, resteasy-mutiny, smallrye-context-propagation, smallrye-openapi, swagger-ui, vertx, vertx-web]
2020-10-24 00:49:32,838 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) dog
2020-10-24 00:49:32,838 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) cat
2020-10-24 00:49:32,838 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) bird
2020-10-24 00:49:32,838 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) mouse
2020-10-24 00:49:32,838 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) fish
2020-10-24 00:49:32,838 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) lion
2020-10-24 00:49:32,839 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) COMPLETED Operation for Flux 1
2020-10-24 00:49:32,843 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) dog
2020-10-24 00:49:32,843 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) cat
2020-10-24 00:49:32,843 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) bird
2020-10-24 00:49:32,843 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) mouse
2020-10-24 00:49:32,843 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) fish
2020-10-24 00:49:32,843 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) lion
2020-10-24 00:49:32,843 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) COMPLETED Operation for Flux 2
2020-10-24 00:49:32,961 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-1) Number: 0
2020-10-24 00:49:33,057 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-1) Number: 1
2020-10-24 00:49:33,161 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-1) Number: 2
2020-10-24 00:49:33,259 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-1) Number: 3
2020-10-24 00:49:33,358 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-1) Number: 4
2020-10-24 00:49:33,460 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-1) Number: 5
2020-10-24 00:49:33,560 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-1) Number: 6
2020-10-24 00:49:33,660 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-1) Number: 7
2020-10-24 00:49:33,759 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-1) Number: 8
2020-10-24 00:49:33,860 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-1) Number: 9
2020-10-24 00:49:33,961 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-1) Number: 10
......
     */

    @Test
    public void backPressureTest() throws Exception {
        Flux<Integer> integerFlux = Flux.range(1, 100);
//                .take(10); //Publisher 側でのリクエスト制限

        // Back Pressure Subscriber
        integerFlux.subscribe(
                num -> LOGGER.info("With Number: " + num),
                error -> LOGGER.error(error),
                () -> LOGGER.info("FINISHED"),
                subscription -> subscription.request(5)); //Subscriber 側でのリクエスト制限
    }

    /* 実行結果
2020-10-24 00:50:31,519 INFO  [io.quarkus] (main) Quarkus 1.9.0.CR1 on JVM started in 4.025s. Listening on: http://0.0.0.0:8081
2020-10-24 00:50:31,529 INFO  [io.quarkus] (main) Profile test activated.
2020-10-24 00:50:31,529 INFO  [io.quarkus] (main) Installed features: [cdi, mutiny, resteasy-jackson, resteasy-mutiny, smallrye-context-propagation, smallrye-openapi, swagger-ui, vertx, vertx-web]
2020-10-24 00:50:32,338 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) With Number: 1
2020-10-24 00:50:32,339 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) With Number: 2
2020-10-24 00:50:32,339 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) With Number: 3
2020-10-24 00:50:32,339 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) With Number: 4
2020-10-24 00:50:32,340 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) With Number: 5
2020-10-24 00:50:32,387 INFO  [io.quarkus] (main) Quarkus stopped in 0.035s

Process finished with exit code 0
     */

    @Test
    public void testGenerate() throws Exception {
        AtomicInteger atomicInteger = new AtomicInteger();
        //Flux.generate は Lambda 式から Flux を生成

        //Flux generate sequence
        //sink は同期で subscriber にバインドし、Subscriber がデータを要求すると
        //Consumer のメソッドが呼び出される
        //sink を利用して１度に１つのイベントを生成可能
        Flux<Integer> integerFlux = Flux.generate((SynchronousSink<Integer> sink) -> {
            LOGGER.info("Publisher: published" + atomicInteger);
            sink.next(atomicInteger.getAndIncrement());
        });

        integerFlux.delayElements(Duration.ofMillis(20))
                .subscribe(i -> LOGGER.info("Subscriber: consumed ::" + i));

        new CountDownLatch(1).await(3000, TimeUnit.MILLISECONDS);
    }

    /*
Picked up JAVA_TOOL_OPTIONS: -Dfile.encoding=UTF-8
2020-10-24 00:54:48,366 INFO  [io.quarkus] (main) Quarkus 1.9.0.CR1 on JVM started in 4.058s. Listening on: http://0.0.0.0:8081
2020-10-24 00:54:48,380 INFO  [io.quarkus] (main) Profile test activated.
2020-10-24 00:54:48,380 INFO  [io.quarkus] (main) Installed features: [cdi, mutiny, resteasy-jackson, resteasy-mutiny, smallrye-context-propagation, smallrye-openapi, swagger-ui, vertx, vertx-web]
2020-10-24 00:54:49,137 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher: published0
2020-10-24 00:54:49,178 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher: published1
2020-10-24 00:54:49,178 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher: published2
2020-10-24 00:54:49,178 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher: published3
2020-10-24 00:54:49,178 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher: published4
2020-10-24 00:54:49,178 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher: published5
2020-10-24 00:54:49,178 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher: published6
2020-10-24 00:54:49,178 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher: published7
2020-10-24 00:54:49,179 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher: published8
2020-10-24 00:54:49,179 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher: published9
2020-10-24 00:54:49,179 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher: published10
2020-10-24 00:54:49,179 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher: published11
2020-10-24 00:54:49,179 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher: published12
2020-10-24 00:54:49,179 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher: published13
2020-10-24 00:54:49,179 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher: published14
2020-10-24 00:54:49,179 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher: published15
2020-10-24 00:54:49,179 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher: published16
2020-10-24 00:54:49,179 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher: published17
2020-10-24 00:54:49,179 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher: published18
2020-10-24 00:54:49,180 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher: published19
2020-10-24 00:54:49,180 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher: published20
2020-10-24 00:54:49,180 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher: published21
2020-10-24 00:54:49,182 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher: published22
2020-10-24 00:54:49,183 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher: published23
2020-10-24 00:54:49,183 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher: published24
2020-10-24 00:54:49,183 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher: published25
2020-10-24 00:54:49,183 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher: published26
2020-10-24 00:54:49,183 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher: published27
2020-10-24 00:54:49,183 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher: published28
2020-10-24 00:54:49,183 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher: published29
2020-10-24 00:54:49,183 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher: published30
2020-10-24 00:54:49,184 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher: published31
2020-10-24 00:54:49,199 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-1) Subscriber: consumed ::0
2020-10-24 00:54:49,221 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-2) Subscriber: consumed ::1
2020-10-24 00:54:49,243 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-3) Subscriber: consumed ::2
2020-10-24 00:54:49,266 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-4) Subscriber: consumed ::3
2020-10-24 00:54:49,286 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-1) Subscriber: consumed ::4
2020-10-24 00:54:49,312 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-2) Subscriber: consumed ::5
2020-10-24 00:54:49,337 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-3) Subscriber: consumed ::6
2020-10-24 00:54:49,363 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-4) Subscriber: consumed ::7
2020-10-24 00:54:49,388 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-1) Subscriber: consumed ::8
2020-10-24 00:54:49,413 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-2) Subscriber: consumed ::9
2020-10-24 00:54:49,436 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-3) Subscriber: consumed ::10
2020-10-24 00:54:49,461 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-4) Subscriber: consumed ::11
2020-10-24 00:54:49,486 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-1) Subscriber: consumed ::12
2020-10-24 00:54:49,510 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-2) Subscriber: consumed ::13
2020-10-24 00:54:49,535 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-3) Subscriber: consumed ::14
2020-10-24 00:54:49,561 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-4) Subscriber: consumed ::15
2020-10-24 00:54:49,586 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-1) Subscriber: consumed ::16
2020-10-24 00:54:49,610 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-2) Subscriber: consumed ::17
2020-10-24 00:54:49,636 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-3) Subscriber: consumed ::18
2020-10-24 00:54:49,659 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-4) Subscriber: consumed ::19
2020-10-24 00:54:49,680 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-1) Subscriber: consumed ::20
2020-10-24 00:54:49,703 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-2) Subscriber: consumed ::21
2020-10-24 00:54:49,729 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-3) Subscriber: consumed ::22
2020-10-24 00:54:49,729 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-3) Publisher: published32
2020-10-24 00:54:49,730 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-3) Publisher: published33
2020-10-24 00:54:49,730 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-3) Publisher: published34
2020-10-24 00:54:49,730 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-3) Publisher: published35
2020-10-24 00:54:49,730 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-3) Publisher: published36
2020-10-24 00:54:49,731 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-3) Publisher: published37
2020-10-24 00:54:49,731 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-3) Publisher: published38
2020-10-24 00:54:49,731 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-3) Publisher: published39
2020-10-24 00:54:49,731 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-3) Publisher: published40
.....
2020-10-24 00:54:52,062 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-3) Publisher: published149
2020-10-24 00:54:52,062 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-3) Publisher: published150
2020-10-24 00:54:52,063 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-3) Publisher: published151
2020-10-24 00:54:52,086 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-4) Subscriber: consumed ::119
2020-10-24 00:54:52,111 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-1) Subscriber: consumed ::120
2020-10-24 00:54:52,136 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-2) Subscriber: consumed ::121
2020-10-24 00:54:52,160 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-3) Subscriber: consumed ::122
2020-10-24 00:54:52,180 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-4) Subscriber: consumed ::123
2020-10-24 00:54:52,202 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-1) Subscriber: consumed ::124
2020-10-24 00:54:52,228 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-2) Subscriber: consumed ::125
2020-10-24 00:54:52,249 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-3) Subscriber: consumed ::126
2020-10-24 00:54:52,273 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-4) Subscriber: consumed ::127
2020-10-24 00:54:52,277 INFO  [io.quarkus] (main) Quarkus stopped in 0.064s

Process finished with exit code 0
     */


    @Test
    public void testCreate() throws Exception {
        //Flux.create は Lambda 式から Flux を生成

        //generate との違いは、Subscriber 側の呼び出しに関係なく
        //処理を実施
        Flux<Integer> integerFlux = Flux.create((FluxSink<Integer> fluxSink) -> {
            LOGGER.info("Flux create");
            IntStream.range(0, 100)
                    .peek(i -> LOGGER.info("Publisher Emitting: " + i))
                    .forEach(fluxSink::next);
        });
        integerFlux.delayElements(Duration.ofMillis(20))
                .subscribe(i -> {
                    LOGGER.info("Consumer Consumed: " + i);
                });
        new CountDownLatch(1).await(5000, TimeUnit.MILLISECONDS);
    }

    /* 実行結果
2020-10-24 00:57:10,116 INFO  [io.quarkus] (main) Quarkus 1.9.0.CR1 on JVM started in 3.764s. Listening on: http://0.0.0.0:8081
2020-10-24 00:57:10,124 INFO  [io.quarkus] (main) Profile test activated.
2020-10-24 00:57:10,124 INFO  [io.quarkus] (main) Installed features: [cdi, mutiny, resteasy-jackson, resteasy-mutiny, smallrye-context-propagation, smallrye-openapi, swagger-ui, vertx, vertx-web]
2020-10-24 00:57:10,829 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Flux create
2020-10-24 00:57:10,833 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher Emitting: 0
2020-10-24 00:57:10,867 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher Emitting: 1
2020-10-24 00:57:10,867 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher Emitting: 2
2020-10-24 00:57:10,867 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher Emitting: 3
2020-10-24 00:57:10,867 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher Emitting: 4
2020-10-24 00:57:10,867 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher Emitting: 5
2020-10-24 00:57:10,867 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher Emitting: 6
2020-10-24 00:57:10,867 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher Emitting: 7
2020-10-24 00:57:10,867 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher Emitting: 8
2020-10-24 00:57:10,868 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher Emitting: 9
2020-10-24 00:57:10,868 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher Emitting: 10
2020-10-24 00:57:10,868 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher Emitting: 11
2020-10-24 00:57:10,868 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher Emitting: 12
2020-10-24 00:57:10,868 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher Emitting: 13
2020-10-24 00:57:10,868 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher Emitting: 14
2020-10-24 00:57:10,868 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher Emitting: 15
.....
2020-10-24 00:57:10,875 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher Emitting: 97
2020-10-24 00:57:10,876 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher Emitting: 98
2020-10-24 00:57:10,876 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Publisher Emitting: 99
2020-10-24 00:57:10,894 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-1) Consumer Consumed: 0
2020-10-24 00:57:10,920 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-2) Consumer Consumed: 1
2020-10-24 00:57:10,944 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-3) Consumer Consumed: 2
2020-10-24 00:57:10,969 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-4) Consumer Consumed: 3
2020-10-24 00:57:10,994 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-1) Consumer Consumed: 4
2020-10-24 00:57:11,019 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-2) Consumer Consumed: 5
.....
2020-10-24 00:57:13,164 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-4) Consumer Consumed: 95
2020-10-24 00:57:13,188 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-1) Consumer Consumed: 96
2020-10-24 00:57:13,211 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-2) Consumer Consumed: 97
2020-10-24 00:57:13,237 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-3) Consumer Consumed: 98
2020-10-24 00:57:13,263 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-4) Consumer Consumed: 99
2020-10-24 00:57:15,957 INFO  [io.quarkus] (main) Quarkus stopped in 0.064s

Process finished with exit code 0
     */

    @Test
    public void filterTest() {
        String[] animals = {"dog", "cat", "bird", "mouse", "fish"};
        Flux<String> fluxAnimals = Flux.fromArray(animals);
        fluxAnimals.filter(animal -> !animal.equals("bird"))
                .subscribe(filteredAnimal -> LOGGER.info("Filtered Animals: " + filteredAnimal));
    }

    /* 実行結果
2020-10-24 00:58:37,000 INFO  [io.quarkus] (main) Quarkus 1.9.0.CR1 on JVM started in 4.344s. Listening on: http://0.0.0.0:8081
2020-10-24 00:58:37,006 INFO  [io.quarkus] (main) Profile test activated.
2020-10-24 00:58:37,006 INFO  [io.quarkus] (main) Installed features: [cdi, mutiny, resteasy-jackson, resteasy-mutiny, smallrye-context-propagation, smallrye-openapi, swagger-ui, vertx, vertx-web]
2020-10-24 00:58:37,845 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Filtered Animals: dog
2020-10-24 00:58:37,845 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Filtered Animals: cat
2020-10-24 00:58:37,845 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Filtered Animals: mouse
2020-10-24 00:58:37,845 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Filtered Animals: fish
2020-10-24 00:58:37,898 INFO  [io.quarkus] (main) Quarkus stopped in 0.028s

Process finished with exit code 0

     */

    @Test
    public void filterWhenTest() {
        String[] animals = {"dog", "cat", "bird", "mouse", "fish"};
        Flux<String> fluxAnimals = Flux.fromArray(animals);

        fluxAnimals.filterWhen(str -> {
            if (!str.equals("bird")) {
                return Mono.just(true);
            } else {
                return Mono.just(false);
            }
        }).subscribe(filteredAnimals -> {
            LOGGER.info(filteredAnimals);
        });
    }
    /* 実行結果
2020-10-24 00:59:05,764 INFO  [io.quarkus] (main) Quarkus 1.9.0.CR1 on JVM started in 3.662s. Listening on: http://0.0.0.0:8081
2020-10-24 00:59:05,771 INFO  [io.quarkus] (main) Profile test activated.
2020-10-24 00:59:05,771 INFO  [io.quarkus] (main) Installed features: [cdi, mutiny, resteasy-jackson, resteasy-mutiny, smallrye-context-propagation, smallrye-openapi, swagger-ui, vertx, vertx-web]
2020-10-24 00:59:06,427 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) dog
2020-10-24 00:59:06,427 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) cat
2020-10-24 00:59:06,427 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) mouse
2020-10-24 00:59:06,427 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) fish
2020-10-24 00:59:06,468 INFO  [io.quarkus] (main) Quarkus stopped in 0.026s

Process finished with exit code 0
     */


    // 下記コード内の map() で利用する Function
    Function<String, String> mapper = (animal) -> {
        LOGGER.info("Publisher : " + animal + " is invoked: " + Thread.currentThread().getName());
        Random random = new Random();
        int longProcess = random.nextInt(10);
        try {
            Thread.sleep(100 * longProcess);
        } catch (InterruptedException ine) {

        }
        return animal.toUpperCase();
    };


    // map は同期オペレーション　
    // Transform the items emitted by this Flux by applying a synchronous function to each item.
    @Test
    public void mapTest() throws Exception {
        String[] animals = {"dog", "cat", "bird", "mouse", "fish", "lion"};
        Flux<String> fromIterable = Flux.fromIterable(Arrays.asList(animals));

        LOGGER.info("This method is running on thread: " + Thread.currentThread().getName());
        Flux<String> publisher = fromIterable
                .map(mapper)
                .subscribeOn(Schedulers.parallel());

        publisher.subscribe(returnAnimal -> {
            LOGGER.info("Subscriber: " + returnAnimal + " is invoked: " + Thread.currentThread().getName());
        });
        new CountDownLatch(1).await(10000, TimeUnit.MILLISECONDS);
    }
    /* 実行結果
2020-10-24 01:00:39,400 INFO  [io.quarkus] (main) Quarkus 1.9.0.CR1 on JVM started in 6.692s. Listening on: http://0.0.0.0:8081
2020-10-24 01:00:39,406 INFO  [io.quarkus] (main) Profile test activated.
2020-10-24 01:00:39,406 INFO  [io.quarkus] (main) Installed features: [cdi, mutiny, resteasy-jackson, resteasy-mutiny, smallrye-context-propagation, smallrye-openapi, swagger-ui, vertx, vertx-web]
2020-10-24 01:00:41,269 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) This method is running on thread: main
2020-10-24 01:00:41,395 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-1) Publisher : dog is invoked: parallel-1
2020-10-24 01:00:41,599 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-1) Subscriber: DOG is invoked: parallel-1
2020-10-24 01:00:41,599 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-1) Publisher : cat is invoked: parallel-1
2020-10-24 01:00:42,102 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-1) Subscriber: CAT is invoked: parallel-1
2020-10-24 01:00:42,102 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-1) Publisher : bird is invoked: parallel-1
2020-10-24 01:00:42,905 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-1) Subscriber: BIRD is invoked: parallel-1
2020-10-24 01:00:42,906 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-1) Publisher : mouse is invoked: parallel-1
2020-10-24 01:00:43,709 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-1) Subscriber: MOUSE is invoked: parallel-1
2020-10-24 01:00:43,710 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-1) Publisher : fish is invoked: parallel-1
2020-10-24 01:00:44,111 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-1) Subscriber: FISH is invoked: parallel-1
2020-10-24 01:00:44,112 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-1) Publisher : lion is invoked: parallel-1
2020-10-24 01:00:44,513 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-1) Subscriber: LION is invoked: parallel-1
2020-10-24 01:00:51,441 INFO  [io.quarkus] (main) Quarkus stopped in 0.035s

Process finished with exit code 0

     */


    // flatMap は非同期オペレーション＆返り値の順番保証はなし
    // 結果として単一の Flux にマージ
    @Test
    public void flatMapTest() throws Exception {
        String[] animals = {"dog", "cat", "bird", "mouse", "fish", "lion"};
        Flux<String> fromIterable = Flux.fromIterable(Arrays.asList(animals));

        // flatMap は Flux から新しい 複数の　Flux<> を生成(別のスレッドで処理を動作可能)
        // Flux.flatMap は 1:n の変換に有効

        LOGGER.info("This method is running on thread: " + Thread.currentThread().getName());
        Flux<String> publisher = fromIterable.flatMap(animal -> {
            return Mono.just(animal)
                    .map(mapper)
                    .subscribeOn(Schedulers.parallel());
        }, 4);

        publisher.subscribe(returnAnimal -> {
            LOGGER.info("Subscriber: " + returnAnimal + " is invoked: " + Thread.currentThread().getName());
        });
        new CountDownLatch(1).await(10000, TimeUnit.MILLISECONDS);
    }

    /* 実行結果
020-10-24 01:02:38,255 INFO  [io.quarkus] (main) Quarkus 1.9.0.CR1 on JVM started in 4.267s. Listening on: http://0.0.0.0:8081
2020-10-24 01:02:38,263 INFO  [io.quarkus] (main) Profile test activated.
2020-10-24 01:02:38,264 INFO  [io.quarkus] (main) Installed features: [cdi, mutiny, resteasy-jackson, resteasy-mutiny, smallrye-context-propagation, smallrye-openapi, swagger-ui, vertx, vertx-web]
2020-10-24 01:02:39,132 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) This method is running on thread: main
2020-10-24 01:02:39,221 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-2) Publisher : cat is invoked: parallel-2
2020-10-24 01:02:39,221 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-1) Publisher : dog is invoked: parallel-1
2020-10-24 01:02:39,221 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-3) Publisher : bird is invoked: parallel-3
2020-10-24 01:02:39,223 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-4) Publisher : mouse is invoked: parallel-4
2020-10-24 01:02:39,430 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-1) Subscriber: DOG is invoked: parallel-1
2020-10-24 01:02:39,431 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-1) Publisher : fish is invoked: parallel-1
2020-10-24 01:02:39,727 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-2) Subscriber: CAT is invoked: parallel-2
2020-10-24 01:02:39,728 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-2) Publisher : lion is invoked: parallel-2
2020-10-24 01:02:39,927 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-3) Subscriber: BIRD is invoked: parallel-3
2020-10-24 01:02:40,128 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-4) Subscriber: MOUSE is invoked: parallel-4
2020-10-24 01:02:40,229 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-2) Subscriber: LION is invoked: parallel-2
2020-10-24 01:02:40,235 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (parallel-1) Subscriber: FISH is invoked: parallel-1
2020-10-24 01:02:49,262 INFO  [io.quarkus] (main) Quarkus stopped in 0.034s

Process finished with exit code 0
     */



    @Test
    public void filterAndConcatTest() {
        String[] animals = {"dog", "cat", "bird", "mouse", "fish"};
        Flux<String> fluxAnimals = Flux.fromArray(animals);

        Flux<String> withoutBird = fluxAnimals
                .filter(animal -> !animal.equals("bird"))
                .map(animal -> animal.concat(", "));

        Flux<String> concatPublisher = Mono.just("[")
                .concatWith(withoutBird)
                .concatWith(Flux.just("]"));

        concatPublisher.subscribe(LOGGER::info);
        // Result : [dog, cat, mouse,fish,]
    }

    /* 実行結果
2020-10-24 01:01:30,411 INFO  [io.quarkus] (main) Quarkus 1.9.0.CR1 on JVM started in 6.574s. Listening on: http://0.0.0.0:8081
2020-10-24 01:01:30,417 INFO  [io.quarkus] (main) Profile test activated.
2020-10-24 01:01:30,417 INFO  [io.quarkus] (main) Installed features: [cdi, mutiny, resteasy-jackson, resteasy-mutiny, smallrye-context-propagation, smallrye-openapi, swagger-ui, vertx, vertx-web]
2020-10-24 01:01:31,731 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) [
2020-10-24 01:01:31,733 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) dog,
2020-10-24 01:01:31,734 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) cat,
2020-10-24 01:01:31,734 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) mouse,
2020-10-24 01:01:31,734 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) fish,
2020-10-24 01:01:31,734 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) ]
2020-10-24 01:01:31,807 INFO  [io.quarkus] (main) Quarkus stopped in 0.063s

Process finished with exit code 0
     */


    @Test
    public void takeTest() throws Exception{
        String[] animals = {"dog", "cat", "bird", "mouse", "fish"};
        Flux<String> fluxAnimals = Flux.fromArray(animals);
        fluxAnimals.filter(animal -> !animal.equals("bird"))
                .take(2)
                .subscribe(take2Animals -> LOGGER.info(take2Animals));
        new CountDownLatch(1).await(2000, TimeUnit.MILLISECONDS);
    }
    /* 実行結果
2020-10-24 01:06:45,442 INFO  [io.quarkus] (main) Quarkus 1.9.0.CR1 on JVM started in 8.105s. Listening on: http://0.0.0.0:8081
2020-10-24 01:06:45,448 INFO  [io.quarkus] (main) Profile test activated.
2020-10-24 01:06:45,448 INFO  [io.quarkus] (main) Installed features: [cdi, mutiny, resteasy-jackson, resteasy-mutiny, smallrye-context-propagation, smallrye-openapi, swagger-ui, vertx, vertx-web]
2020-10-24 01:06:46,600 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) dog
2020-10-24 01:06:46,600 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) cat
2020-10-24 01:06:48,690 INFO  [io.quarkus] (main) Quarkus stopped in 0.078s

Process finished with exit code 0
     */

    @Test
    public void takeLastTest() throws Exception {
        String[] animals = {"dog", "cat", "bird", "mouse", "fish"};
        Flux<String> fluxAnimals = Flux.fromArray(animals);
        fluxAnimals.filter(animal -> !animal.equals("bird"))
                .takeLast(2)
                .subscribe(takeLast2Animals -> LOGGER.info(takeLast2Animals));
        new CountDownLatch(1).await(2000, TimeUnit.MILLISECONDS);
    }
    /* 実行結果
2020-10-24 01:07:38,012 INFO  [io.quarkus] (main) Quarkus 1.9.0.CR1 on JVM started in 4.769s. Listening on: http://0.0.0.0:8081
2020-10-24 01:07:38,028 INFO  [io.quarkus] (main) Profile test activated.
2020-10-24 01:07:38,029 INFO  [io.quarkus] (main) Installed features: [cdi, mutiny, resteasy-jackson, resteasy-mutiny, smallrye-context-propagation, smallrye-openapi, swagger-ui, vertx, vertx-web]
2020-10-24 01:07:39,496 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) mouse
2020-10-24 01:07:39,497 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) fish
2020-10-24 01:07:41,545 INFO  [io.quarkus] (main) Quarkus stopped in 0.028s

Process finished with exit code 0
     */

    @Test
    public void skipTest() throws Exception{
        String[] animals = {"dog", "cat", "bird", "mouse", "fish"};
        Flux<String> fluxAnimals = Flux.fromArray(animals);
        fluxAnimals.filter(animal -> !animal.equals("bird"))
                .skip(3)
                .subscribe(skip2Animals -> LOGGER.info(skip2Animals));
        new CountDownLatch(1).await(2000, TimeUnit.MILLISECONDS);
    }
    /*
2020-10-24 01:08:25,282 INFO  [io.quarkus] (main) Quarkus 1.9.0.CR1 on JVM started in 3.433s. Listening on: http://0.0.0.0:8081
2020-10-24 01:08:25,289 INFO  [io.quarkus] (main) Profile test activated.
2020-10-24 01:08:25,289 INFO  [io.quarkus] (main) Installed features: [cdi, mutiny, resteasy-jackson, resteasy-mutiny, smallrye-context-propagation, smallrye-openapi, swagger-ui, vertx, vertx-web]
2020-10-24 01:08:25,954 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) fish
2020-10-24 01:08:28,034 INFO  [io.quarkus] (main) Quarkus stopped in 0.051s

Process finished with exit code 0
     */


    @Test
    public void reduceTest() throws Exception{
        String[] animals = {"dog", "cat", "bird", "mouse", "fish"};
        Flux<String> fluxAnimals = Flux.fromArray(animals);
        fluxAnimals.reduce((s1, s2) -> s1 + ", " + s2)
                .subscribe(LOGGER::info);
        new CountDownLatch(1).await(2000, TimeUnit.MILLISECONDS);
    }
    /*
2020-10-24 01:09:28,103 INFO  [io.quarkus] (main) Quarkus 1.9.0.CR1 on JVM started in 4.155s. Listening on: http://0.0.0.0:8081
2020-10-24 01:09:28,117 INFO  [io.quarkus] (main) Profile test activated.
2020-10-24 01:09:28,117 INFO  [io.quarkus] (main) Installed features: [cdi, mutiny, resteasy-jackson, resteasy-mutiny, smallrye-context-propagation, smallrye-openapi, swagger-ui, vertx, vertx-web]
2020-10-24 01:09:28,953 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) dog, cat, bird, mouse, fish
2020-10-24 01:09:31,010 INFO  [io.quarkus] (main) Quarkus stopped in 0.039s

Process finished with exit code 0
     */

    @Test
    public void collectTest() throws Exception{
        String[] animals = {"dog", "cat", "bird", "mouse", "fish"};
        Flux<String> fluxAnimals = Flux.fromArray(animals);
        Mono<List<String>> filterdAnimals = fluxAnimals.filter(anm -> !anm.equals("bird"))
                .collectList();

        filterdAnimals.subscribe(listAnimals -> LOGGER.info(listAnimals.toString()));
        new CountDownLatch(1).await(2000, TimeUnit.MILLISECONDS);
    }
    /*
2020-10-24 01:10:42,616 INFO  [io.quarkus] (main) Quarkus 1.9.0.CR1 on JVM started in 4.556s. Listening on: http://0.0.0.0:8081
2020-10-24 01:10:42,626 INFO  [io.quarkus] (main) Profile test activated.
2020-10-24 01:10:42,626 INFO  [io.quarkus] (main) Installed features: [cdi, mutiny, resteasy-jackson, resteasy-mutiny, smallrye-context-propagation, smallrye-openapi, swagger-ui, vertx, vertx-web]
2020-10-24 01:10:43,519 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) [dog, cat, mouse, fish]
2020-10-24 01:10:45,578 INFO  [io.quarkus] (main) Quarkus stopped in 0.044s

Process finished with exit code 0
     */

    @Test
    public void coldPublisherTest() throws Exception {
        List<String> video = Arrays.asList("scene 1", "scene 2", "scene 3");

        Flux<String> videoDeliver1 = Flux.fromIterable(video);
        videoDeliver1.subscribe(str -> LOGGER.info("Audience 1: " + str));

        Flux<String> videoDeliver2 = Flux.fromIterable(video);
        videoDeliver2.subscribe(str -> LOGGER.info("Audience 2: " + str));

        Flux<String> videoDeliver3 = Flux.fromIterable(video);
        videoDeliver3.subscribe(str -> LOGGER.info("Audience 3: " + str));
    }

    /* 実行結果
2020-10-24 01:11:28,879 INFO  [io.quarkus] (main) Quarkus 1.9.0.CR1 on JVM started in 10.250s. Listening on: http://0.0.0.0:8081
2020-10-24 01:11:28,887 INFO  [io.quarkus] (main) Profile test activated.
2020-10-24 01:11:28,888 INFO  [io.quarkus] (main) Installed features: [cdi, mutiny, resteasy-jackson, resteasy-mutiny, smallrye-context-propagation, smallrye-openapi, swagger-ui, vertx, vertx-web]
2020-10-24 01:11:30,554 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Audience 1: scene 1
2020-10-24 01:11:30,554 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Audience 1: scene 2
2020-10-24 01:11:30,554 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Audience 1: scene 3
2020-10-24 01:11:30,596 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Audience 2: scene 1
2020-10-24 01:11:30,596 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Audience 2: scene 2
2020-10-24 01:11:30,596 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Audience 2: scene 3
2020-10-24 01:11:30,597 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Audience 3: scene 1
2020-10-24 01:11:30,597 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Audience 3: scene 2
2020-10-24 01:11:30,597 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Audience 3: scene 3
2020-10-24 01:11:30,861 INFO  [io.quarkus] (main) Quarkus stopped in 0.113s

Process finished with exit code 0
     */

    @Test
    public void hotPublisherTest() throws Exception {
        UnicastProcessor<String> hotPublishSource = UnicastProcessor.create();

        Flux<String> tvProgram = hotPublishSource
                .publish()
                .autoConnect()
                .map(String::toUpperCase);

        tvProgram.subscribe(str -> LOGGER.info("Audience 1: " + str));
        hotPublishSource.onNext("scene 1");

        tvProgram.subscribe(str -> LOGGER.info("Audience 2: " + str));
        hotPublishSource.onNext("scene 2");

        tvProgram.subscribe(str -> LOGGER.info("Audience 3: " + str));
        hotPublishSource.onNext("scene 3");

        hotPublishSource.onComplete();
    }
    /* 実行結果
2020-10-24 01:12:07,132 INFO  [io.quarkus] (main) Quarkus 1.9.0.CR1 on JVM started in 7.355s. Listening on: http://0.0.0.0:8081
2020-10-24 01:12:07,152 INFO  [io.quarkus] (main) Profile test activated.
2020-10-24 01:12:07,152 INFO  [io.quarkus] (main) Installed features: [cdi, mutiny, resteasy-jackson, resteasy-mutiny, smallrye-context-propagation, smallrye-openapi, swagger-ui, vertx, vertx-web]
2020-10-24 01:12:08,048 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Audience 1: SCENE 1
2020-10-24 01:12:08,049 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Audience 1: SCENE 2
2020-10-24 01:12:08,049 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Audience 2: SCENE 2
2020-10-24 01:12:08,049 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Audience 1: SCENE 3
2020-10-24 01:12:08,049 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Audience 2: SCENE 3
2020-10-24 01:12:08,050 INFO  [com.mic.azu.sam.qua.MonoFluxTest] (main) Audience 3: SCENE 3
2020-10-24 01:12:08,097 INFO  [io.quarkus] (main) Quarkus stopped in 0.034s

Process finished with exit code 0
     */
}