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
        new CountDownLatch(1).await(10000, TimeUnit.MILLISECONDS);
    }

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

    @Test
    public void testGenerate() throws Exception {
        AtomicInteger atomicInteger = new AtomicInteger();
        //Flux.generate は Lambda 式から Flux を生成

        //Flux generate sequence
        //sink は同期で subscriber にバインドし、Subscriber がデータを要求すると
        //Consumer のメソッドが呼び出される
        //sink を利用して１度に１つのイベントを生成可能
        Flux<Integer> integerFlux = Flux.generate((SynchronousSink<Integer> sink) -> {
            LOGGER.info("Flux generate" + atomicInteger);
            sink.next(atomicInteger.getAndIncrement());
        });

        //Observer
        integerFlux.delayElements(Duration.ofMillis(30))
                .subscribe(i -> LOGGER.info("First consumed ::" + i));

        new CountDownLatch(1).await(3000, TimeUnit.MILLISECONDS);
    }


    @Test
    public void testCreate() throws Exception {
        //Flux.create は Lambda 式から Flux を生成

        //generate との違いは、Subscriber 側の呼び出しに関係なく
        //処理を実施
        Flux<Integer> integerFlux = Flux.create((FluxSink<Integer> fluxSink) -> {
            LOGGER.info("Flux create");
            IntStream.range(0, 100)
                    .peek(i -> LOGGER.info("Emitting: " + i))
                    .forEach(fluxSink::next);
        });
        integerFlux.delayElements(Duration.ofMillis(30))
                .subscribe(i -> {
                    LOGGER.info("Consumed: " + i);
                });
        new CountDownLatch(1).await(5000, TimeUnit.MILLISECONDS);
    }


    @Test
    public void filterTest() {
        String[] animals = {"dog", "cat", "bird", "mouse", "fish"};
        Flux<String> fluxAnimals = Flux.fromArray(animals);
        fluxAnimals.filter(animal -> !animal.equals("bird"))
                .subscribe(filteredAnimal -> LOGGER.info("Filtered Animals: " + filteredAnimal));
    }

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


    @Test
    public void takeTest() {
        String[] animals = {"dog", "cat", "bird", "mouse", "fish"};
        Flux<String> fluxAnimals = Flux.fromArray(animals);
        fluxAnimals.filter(animal -> !animal.equals("bird"))
                .take(2)
                .subscribe(take2Animals -> LOGGER.info(take2Animals));
    }

    @Test
    public void takeLastTest() {
        String[] animals = {"dog", "cat", "bird", "mouse", "fish"};
        Flux<String> fluxAnimals = Flux.fromArray(animals);
        fluxAnimals.filter(animal -> !animal.equals("bird"))
                .takeLast(2)
                .subscribe(takeLast2Animals -> LOGGER.info(takeLast2Animals));
    }

    @Test
    public void skipTest() {
        String[] animals = {"dog", "cat", "bird", "mouse", "fish"};
        Flux<String> fluxAnimals = Flux.fromArray(animals);
        fluxAnimals.filter(animal -> !animal.equals("bird"))
                .skip(2)
                .subscribe(skip2Animals -> LOGGER.info(skip2Animals));
    }

    @Test
    public void reduceTest() {
        String[] animals = {"dog", "cat", "bird", "mouse", "fish"};
        Flux<String> fluxAnimals = Flux.fromArray(animals);
        fluxAnimals.reduce((s1, s2) -> s1 + ", " + s2)
                .subscribe(LOGGER::info);
    }

    @Test
    public void collectTest() {
        String[] animals = {"dog", "cat", "bird", "mouse", "fish"};
        Flux<String> fluxAnimals = Flux.fromArray(animals);
        Mono<List<String>> filterdAnimals = fluxAnimals.filter(anm -> !anm.equals("bird"))
                .collectList();
        filterdAnimals.subscribe(listAnimals -> LOGGER.info(listAnimals.toString()));
    }

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
}