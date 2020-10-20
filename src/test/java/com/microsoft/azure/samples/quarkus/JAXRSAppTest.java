package com.microsoft.azure.samples.quarkus;

import io.quarkus.test.junit.QuarkusTest;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.UnicastProcessor;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class JAXRSAppTest {

    private static final Logger LOGGER = Logger.getLogger(JAXRSAppTest.class);

    // Mono, Flux インスタンスの生成
    // Mono.jus, Flux.just
    @Test
    public void monoCreate() {
        String hello = "Hello";
        Mono<String> monoHello = Mono.just(hello);
        Mono empty = Mono.empty();
    }

    @Test
    public void fluxCreate() {
        Flux<Void> empty = Flux.empty();

        String[] strArray = { "string1", "string2" };
        Flux<String> fromArray = Flux.fromArray(strArray);

        List<String> list = Arrays.asList(strArray);
        Flux<String> fromItterable = Flux.fromIterable(list);

        Flux<List<String>> fromJust = Flux.just(list);

        Flux<Integer> integerFlux = Flux.range(0, 100);
    }

    /*
     * 
     * その他、このような
     * 
     * just delay add delayElement delaySubscription as cast create block empty
     * justOrEmpty first fromSupplier fromCallable fromRunnable fromCompletable zip
     * 
     */

    /*
     * Subscribe 用のメソッド
     * 
     * public final Disposable subscribe()
     * 
     * public final Disposable subscribe(Consumer<? super T> consumer)
     * 
     * public final Disposable subscribe(@Nullable Consumer<? super T> consumer,
     * Consumer<? super Throwable> errorConsumer)
     * 
     * public final Disposable subscribe(@Nullable Consumer<? super T> consumer,
     * 
     * @Nullable Consumer<? super Throwable> errorConsumer,
     * 
     * @Nullable Runnable completeConsumer)
     * 
     * public final Disposable subscribe(@Nullable Consumer<? super T> consumer,
     * 
     * @Nullable Consumer<? super Throwable> errorConsumer,
     * 
     * @Nullable Runnable completeConsumer,
     * 
     * @Nullable Consumer<? super Subscription> subscriptionConsumer)
     */

    @Test
    public void monoSubscribe() {
        String hello = "Hello";
        Mono<String> monoHello = Mono.just(hello);

        // monoHello.log().subscribe();

        monoHello.doOnSubscribe(subscription -> {
            System.out.println("ON SUBSCRIBE");
            subscription.request(5);
        }).doFirst(() -> System.out.println("DO FIRST")).doOnNext(next -> System.out.println("NEXT VALUE: " + next))
                .doOnError(error -> LOGGER.error(error))
                .doOnSuccess(onSuccess -> System.out.println("SUCCESS :" + onSuccess)).log().subscribe();

        // monoHello.subscribe(response -> LOGGER.info("SUBSCRIBE: " + response), //
        // error -> LOGGER.error(error), // エラー発生時
        // () -> LOGGER.info("COMPLETES SUCCESSFULLY"), // 処理が正常に完了した時
        // subscription -> subscription.request(5) // バックプレッシャー用
        // );
    }

    @Test
    public void fluxSubscribe() throws Exception {
        String[] strArray = { "string1", "string-hogehoge" };
        List<String> list = Arrays.asList(strArray);
        Flux<String> fromArray = Flux.fromArray(strArray);
        Flux<List<String>> fromJust = Flux.just(list);
        Flux<Integer> integerFlux = Flux.range(1, 100);

        fromArray.subscribe(value -> System.out.println("SUBSCRIBED: " + value));
        fromJust.subscribe(listValue -> System.out.println("LIST VALUE: " + listValue));
        integerFlux.subscribe(System.out::println);

        Flux<Long> interval = Flux.interval(Duration.ofMillis(100)).log();
        interval
                // .take(10)
                .subscribe(intVal -> LOGGER.info("Number: " + intVal));
        new CountDownLatch(1).await(3000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void backPressureTest() throws Exception {
        Flux<Integer> integerFlux = Flux.range(1, 100).take(10).log();

        // WithOut Back Pressure
        // integerFlux
        // .subscribe(num -> LOGGER.info("WithOut Number: "+ num));

        // With Back Pressure
        integerFlux.subscribe(num -> LOGGER.info("With Number: " + num), error -> LOGGER.error(error),
                () -> LOGGER.info("FINISHED"), subscription -> subscription.request(5));
    }

    @Test
    public void evaluateMapFlatMap() {
        String[] strArray = { "string1", "string-hogehoge" };
        List<String> list = Arrays.asList(strArray);
        Flux<String> fromArray = Flux.fromArray(strArray);
        Flux<List<String>> fromJust = Flux.just(list);

        // Flux.map
        fromArray.map(fluxStr -> fluxStr.length()).subscribe(System.out::println);

        // Flux.flatMap
        Flux<List<Integer>> listIntFlux = fromJust.flatMap(listStr -> {
            List<Integer> listInt = listStr.stream().map(strVal -> strVal.length()).collect(Collectors.toList());
            // 新しい Flux を生成
            return Flux.just(listInt);
        });
        // 変換された Flux の値を表示
        listIntFlux.subscribe(listInt -> System.out.println("String length: " + listInt.toString()));
    }

    /*
     * https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Mono.
     * html#concatWith-org.reactivestreams.Publisher-
     * 
     * concatWith public final Flux<T> concatWith(Publisher<? extends T> other)
     * Concatenate emissions of this Mono with the provided Publisher (no
     * interleave).
     */

    @Test
    public void concatTest() {
        String[] animals = { "dog", "cat", "bird", "mouse", "fish" };
        Flux<String> fluxAnimals = Flux.fromArray(animals);

        Flux<String> withoutBird = fluxAnimals.filter(animal -> !animal.equals("bird"))
                .flatMap(animal -> Flux.just(animal.concat(", ")));

        Flux<String> concatPublisher = Mono.just("[").concatWith(withoutBird).concatWith(Flux.just("]"));

        concatPublisher.subscribe(System.out::println);

        // Result : [dog, cat, mouse,fish,]
    }

    @Test
    public void coldPublisherTest() throws Exception {
        List<String> video = Arrays.asList("scene 1", "scene 2", "scene 3");

        Flux<String> videoDeliver1 = Flux.fromIterable(video);
        videoDeliver1.subscribe(str -> System.out.println("Audience 1: " + str));

        Flux<String> videoDeliver2 = Flux.fromIterable(video);
        videoDeliver2.subscribe(str -> System.out.println("Audience 2: " + str));

        Flux<String> videoDeliver3 = Flux.fromIterable(video);
        videoDeliver3.subscribe(str -> System.out.println("Audience 3: " + str));
    }

    @Test
    public void hotPublisherTest() throws Exception {
        UnicastProcessor<String> hotPublishSource = UnicastProcessor.create();

        Flux<String> tvProgram = hotPublishSource.publish().autoConnect().map(String::toUpperCase);

        tvProgram.subscribe(str -> System.out.println("Audience 1: " + str));
        hotPublishSource.onNext("scene 1");

        tvProgram.subscribe(str -> System.out.println("Audience 2: " + str));
        hotPublishSource.onNext("scene 2");

        tvProgram.subscribe(str -> System.out.println("Audience 3: " + str));
        hotPublishSource.onNext("scene 3");

        hotPublishSource.onComplete();
    }
}