package utils;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Consumer;

/**
 * A utility class containing helpful methods for manipulating various
 * BigFraction features.
 */
public class BigFractionUtils {
    /**
     * A utility class should always define a private constructor.
     */
    private BigFractionUtils() {
    }

    /**
     * Number of big fractions to process asynchronously in a Reactor
     * flux stream.
     */
    public static final int sMAX_FRACTIONS = 10;

    /**
     * These final strings are used to pass params to various lambdas in the
     * test methods below.
     */
    public static final String sF1 = "62675744/15668936";
    public static final String sF2 = "609136/913704";
    public static final String sBI1 = "846122553600669882";
    public static final String sBI2 = "188027234133482196";

    /**
     * A big reduced fraction constant.
     */
    public static final BigFraction sBigReducedFraction =
            BigFraction.valueOf(new BigInteger("846122553600669882"),
                    new BigInteger("188027234133482196"),
                    true);

    /**
     * Stores a completed mono with a value of sBigReducedFraction.
     */
    public static final Mono<BigFraction> mBigReducedFractionM =
            Mono.just(sBigReducedFraction);

    /**
     * Represents a test that's completed running when it returns.
     */
    public static final Mono<Void> sVoidM =
            Mono.empty();

    /**
     * A factory method that returns a large random BigFraction whose
     * creation is performed synchronously.
     *
     * @param random A random number generator
     * @param reduced A flag indicating whether to reduce the fraction or not
     * @return A large random BigFraction
     */
    public static BigFraction makeBigFraction(Random random,
                                              boolean reduced) {
        // Create a large random big integer.
        BigInteger numerator =
            new BigInteger(150000, random);

        // Create a denominator that's between 1 to 10 times smaller
        // than the numerator.
        BigInteger denominator =
            numerator.divide(BigInteger.valueOf(random.nextInt(10) + 1));

        // Return a big fraction.
        return BigFraction.valueOf(numerator,
                                   denominator,
                                   reduced);
    }

    /**
     * Sort the {@code list} in parallel using quicksort and mergesort
     * and then store the results in the {@code StringBuilder}
     * parameter.
     */
    public static Mono<Void> sortAndPrintList(List<BigFraction> list,
                                              StringBuilder sb) {
        // Quick sort the list asynchronously.
        Mono<List<BigFraction>> quickSortM = Mono
            // Use the fromCallable() factory method to obtain the
            // results of quick sorting the list.
            // https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Mono.html#fromCallable-java.util.concurrent.Callable-
            .fromCallable(() -> quickSort(list))

            // Use subscribeOn() to run all the processing in the
            // parallel thread pool.
            // https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Mono.html#subscribeOn-reactor.core.scheduler.Scheduler-
            .subscribeOn(Schedulers.parallel());

        // Heap sort the list asynchronously.
        Mono<List<BigFraction>> heapSortM =  Mono
            // Use the fromCallable() factory method to obtain the
            // results of heap sorting the list.
            .fromCallable(() -> heapSort(list))

            // Use subscribeOn() to run all the processing in the
            // parallel thread pool.
            .subscribeOn(Schedulers.parallel());

        // Display the results as mixed fractions.
        Consumer<List<BigFraction>> displayList = sortedList -> {
            // Iterate through each BigFraction in the sorted list.
            sortedList.forEach(fraction ->
                               sb.append("\n     "
                                         + fraction.toMixedString()));
            sb.append("\n");
            display(sb.toString());
        };

        return Mono
            // Use first() to select the result of whichever sort
            // finishes first and use it to print the sorted list.
            // https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Mono.html#first-reactor.core.publisher.Mono...-
            .first(quickSortM,
                   heapSortM)

            // Use doOnSuccess() to display the first sorted list.
            // https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Mono.html#doOnSuccess-java.util.function.Consumer
            .doOnSuccess(displayList)
                
            // Use then() to return an empty mono to synchronize with
            // the AsyncTester framework.
            // https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Mono.html#then--
            .then();
    }

    /**
     * Perform a quick sort on the {@code list}.
     */
    public static List<BigFraction> quickSort(List<BigFraction> list) {
        List<BigFraction> copy = new ArrayList<>(list);
    
        // Order the list with quick sort.
        Collections.sort(copy);

        return copy;
    }

    /*
     * Perform a heap sort on the {@code list}.
     */
    public static List<BigFraction> heapSort(List<BigFraction> list) {
        List<BigFraction> copy = new ArrayList<>(list);

        // Order the list with heap sort.
        HeapSort.sort(copy);

        return copy;
    }

    /**
     * Display the {@code string} after prepending the thread id.
     */
    public static void display(String string) {
        System.out.println("["
                           + Thread.currentThread().getId()
                           + "] "
                           + string);
    }
}
