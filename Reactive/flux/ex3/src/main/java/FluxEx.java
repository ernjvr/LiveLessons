import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import utils.BigFraction;
import utils.BigFractionUtils;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static utils.BigFractionUtils.sBigReducedFraction;
import static utils.BigFractionUtils.sMAX_FRACTIONS;
import static utils.MonosCollector.toMono;

@SuppressWarnings("ALL")

/**
 * This class shows how to apply Project Reactor features
 * asynchronously to perform a range of Flux operations, including
 * fromIterable(), create(), map(), flatMap(), collectList(), take(),
 * filter(), and various types of thread pools.  It also shows various
 * Mono operations, such as first(), when(), flatMap(), subscribeOn(),
 * and the parallel thread pool.  It also demonstrates how to combine
 * the Java streams framework with the Project Reactor framework.
 */
public class FluxEx {
    /**
     * Test BigFraction exception handling using an asynchronous Flux
     * stream and a pool of threads.
     */
    public static Mono<Void> testFractionExceptions() {
        StringBuilder sb =
            new StringBuilder(">> Calling testFractionExceptions1()\n");

        // Create a function to handle an ArithmeticException.
        Function<Throwable,
                 Mono<? extends BigFraction>> errorHandler = t -> {
            // If exception occurred return 0.
            sb.append("     exception = "
                      + t.getMessage()
                      + "\n");

            // Convert error to 0.
            return Mono
            // https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Mono.html#just-T-
            .just(BigFraction.ZERO);
        };

        // Create a function that multiplies big fractions.
        Function<BigFraction,
                 BigFraction> multiplyBigFractions = fraction -> {
            sb.append("     "
                      + fraction.toMixedString()
                      + " x "
                      + sBigReducedFraction.toMixedString()
                      + "\n");
            // When mono completes multiply it.
            return fraction.multiply(sBigReducedFraction);
        };

        // Create a list of denominators, including 0 that
        // will trigger an ArithmeticException.
        List<Integer> denominators = List.of(3, 4, 2, 0, 1);

        return Flux
            // Use a Flux to generate a stream from the denominators list.
            // https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Flux.html#just-T...-
            .fromIterable(denominators)

            // Iterate through the elements using the flatMap()
            // concurrency idiom.
            // https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Flux.html#flatMap-java.util.function.Function-
            .flatMap(denominator -> {
                    // Create/process each denominator asynchronously via an
                    // "inner publisher".
                    return Mono
                        // https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Mono.html#fromCallable-java.util.concurrent.Callable-
                        .fromCallable(() ->
                                      // Throws ArithmeticException if denominator is 0.
                                      BigFraction.valueOf(100,
                                                          denominator))

                        // Run all the processing in a pool of
                        // background threads.
                        // https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Mono.html#subscribeOn-reactor.core.scheduler.Scheduler-
                        .subscribeOn(Schedulers.parallel())

                        // Convert ArithmeticException to 0.
                        // https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Mono.html#onErrorResume-java.util.function.Function-
                        .onErrorResume(errorHandler)

                        // Perform a multiplication.
                        // https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Flux.html#map-java.util.function.Function-
                        .map(multiplyBigFractions);
                })

            // Remove any big fractions that are <= 0.
            .filter(fraction -> fraction.compareTo(0) > 0)

            // Collect the BigFractions into a list.
            // https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Flux.html#collectList--
            .collectList()

            // Process the collected list and return a mono used
            // to synchronize with the AsyncTester framework.
            // https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Mono.html#flatMap-java.util.function.Function-
            .flatMap(list ->
                     // Sort and print the results after all async
                     // fraction reductions complete.
                     BigFractionUtils.sortAndPrintList(list, sb));
    }

    /**
     * Test BigFraction multiplications using a stream of monos and a
     * pipeline of operations, including create(), take(), flatMap(),
     * collectList(), and first().
     */
    public static Mono<Void> testFractionMultiplications1() {
        StringBuilder sb =
            new StringBuilder(">> Calling testFractionMultiplications1()\n");

        sb.append("     Printing sorted results:");

        // A consumer that emits a stream of random big fractions.
        Consumer<FluxSink<BigFraction>> bigFractionEmitter = sink -> sink
            // https://projectreactor.io/docs/core/release/api/reactor/core/publisher/FluxSink.html#onRequest-java.util.function.LongConsumer-
            .onRequest(size -> sink
                       // Emit a random big fraction every time a request is made.
                       .next(BigFractionUtils.makeBigFraction(new Random(),
                                                              false)));

        // Process the function in a flux stream.
        return Flux
            // Generate a stream of random, large, and unreduced big
            // fractions.
            // https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Flux.html#create-java.util.function.Consumer-
            .create(bigFractionEmitter)

            // Stop after generating sMAX_FRACTIONS big fractions.
            // https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Flux.html#take-long-
            .take(sMAX_FRACTIONS)

            // Reduce and multiply these fractions asynchronously.
            // https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Flux.html#flatMap-java.util.function.Function-
            .flatMap(unreducedFraction ->
                     reduceAndMultiplyFraction(unreducedFraction,
                                               Schedulers.parallel()))

            // Collect the results into a list.
            // https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Flux.html#collectList--
            .collectList()

            // Process the results of the collected list and return a
            // mono that's used to synchronize with the AsyncTester
            // framework.
            // https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Mono.html#flatMap-java.util.function.Function-
            .flatMap(list ->
                     // Sort and print the results after all async
                     // fraction reductions complete.
                     BigFractionUtils.sortAndPrintList(list, sb));
    }

    /**
     * Test BigFraction multiplications by combining the Java streams
     * framework with the Project Reactor framework and the Java
     * common fork-join framework.
     */
    public static Mono<Void> testFractionMultiplications2() {
        StringBuilder sb =
            new StringBuilder(">> Calling testFractionMultiplications2()\n");

        sb.append("     Printing sorted results:");

        // Process the function in a sequential stream.
        return Stream
            // Generate a stream of random, large, and unreduced big
            // fractions.
            // https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.html#generate-java.util.function.Supplier-
            .generate(() ->
                      BigFractionUtils.makeBigFraction(new Random(),
                                                       false))

            // Stop after generating sMAX_FRACTIONS big fractions.
            // https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.html#limit-long-
            .limit(sMAX_FRACTIONS)

            // Reduce and multiply these fractions asynchronously.
            // https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.html#map-java.util.function.Function-
            .map(unreducedBigFraction ->
                 reduceAndMultiplyFraction(unreducedBigFraction,
                                           Schedulers
                                           // https://projectreactor.io/docs/core/release/api/reactor/core/scheduler/Schedulers.html#fromExecutor-java.util.concurrent.Executor-
                                           .fromExecutor(ForkJoinPool
                                                         .commonPool())))

            // Trigger intermediate operation processing and return a
            // mono to a list of big fractions that are being reduced
            // and multiplied asynchronously.
            // https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.html#collect-java.util.stream.Collector-
            .collect(toMono())

            // After all the asynchronous fraction reductions have
            // completed sort and print the results.
            // https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Mono.html#flatMap-java.util.function.Function-
            .flatMap(list ->
                     BigFractionUtils.sortAndPrintList(list, sb));
    }

    /**
     * This factory method returns a mono that's signaled after the
     * {@code unreducedFraction} is reduced/multiplied asynchronously
     * in background threads from the given {@code scheduler}.
     */
    private static Mono<BigFraction>
        reduceAndMultiplyFraction(BigFraction unreducedFraction,
                                  Scheduler scheduler) {
        return Mono
            // Omit one item that performs the reduction.
            // https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Mono.html#fromCallable-java.util.concurrent.Callable-
            .fromCallable(() ->
                          BigFraction.reduce(unreducedFraction))

            // Perform all processing asynchronously in a pool of
            // background threads.
            // https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Mono.html#subscribeOn-reactor.core.scheduler.Scheduler-
            .subscribeOn(scheduler)

            // Return a mono to a multiplied big fraction.
            // https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Mono.html#flatMap-java.util.function.Function-
            .flatMap(reducedFraction -> Mono
                     // Multiply the big fractions
                     .fromCallable(() -> reducedFraction
                                   .multiply(sBigReducedFraction))
                                   
                     // Perform all processing asynchronously in a
                     // pool of background threads.
                     .subscribeOn(scheduler));

    }
}
