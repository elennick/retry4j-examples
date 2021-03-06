package com.evanlennick.retry4j;

import com.evanlennick.retry4j.config.RetryConfig;
import com.evanlennick.retry4j.config.RetryConfigBuilder;
import com.evanlennick.retry4j.exception.RetriesExhaustedException;
import com.evanlennick.retry4j.exception.UnexpectedException;

import java.net.ConnectException;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

public class Main {

    public static void main(String[] args) {
        superSimple();
        retryOnAllExceptions_usingListeners();
        retryOnAllExceptions_usingExceptions();
        asyncCallExecutor();
        generalExample_usingListeners();
    }

    private static void superSimple() {
        Callable<String> callable = () -> "success!";

        RetryConfig config = new RetryConfigBuilder()
                .fixedBackoff5Tries10Sec()
                .build();

        new CallExecutorBuilder<String>()
                .config(config)
                .onSuccessListener(s -> {
                    System.out.println("Status: " + s);
                })
                .onFailureListener(s -> System.out.println("Failed! All retries exhausted..."))
                .build()
                .execute(callable);
    }

    /**
     * This is a simple example of a retry situation where all exceptions cause retries and any return value counts as a
     * success. The call executor uses lambda-style listeners to specify how to behave.
     */
    private static void retryOnAllExceptions_usingListeners() {
        ResultGenerator<String> generator = new ResultGenerator<>();
        generator.setPossibleExceptions(ConnectException.class, RuntimeException.class);
        generator.setPossibleValues("this was a success!");

        Callable<String> callable = generator::generateRandomResult;

        RetryConfig config = new RetryConfigBuilder()
                .retryOnAnyException()
                .withNoWaitBackoff()
                .withDelayBetweenTries(Duration.ZERO) //TODO shouldnt have to specify this, see issue #50
                .withMaxNumberOfTries(10)
                .build();

        new CallExecutorBuilder<String>()
                .config(config)
                .onSuccessListener(s -> {
                    System.out.println("Success!");
                    System.out.println("Status: " + s);
                })
                .onCompletionListener(s -> System.out.println("Retry execution complete!"))
                .onFailureListener(s -> System.out.println("Failed! All retries exhausted..."))
                .afterFailedTryListener(s -> System.out.println("Try failed! Will try again in 0ms."))
                .beforeNextTryListener(s -> System.out.println("Trying again..."))
                .build()
                .execute(callable);
    }

    /**
     * This is a simple example of a retry situation where all exceptions cause retries and any return value counts as a
     * success. This example uses try/catch to detect when an error state has been encountered.
     */
    private static void retryOnAllExceptions_usingExceptions() {
        ResultGenerator<String> generator = new ResultGenerator<>();
        generator.setPossibleExceptions(ConnectException.class, RuntimeException.class);
        generator.setPossibleValues("this was a success!");

        Callable<String> callable = generator::generateRandomResult;

        RetryConfig config = new RetryConfigBuilder()
                .retryOnAnyException()
                .withNoWaitBackoff()
                .withMaxNumberOfTries(10)
                .build();

        try {
            Status<String> result = new CallExecutorBuilder<String>().config(config).build().execute(callable);
            System.out.println("Success!");
            System.out.println("Status: " + result);
        } catch (RetriesExhaustedException e) {
            System.out.println("Failed! All retries exhausted...");
        } catch (UnexpectedException e) {
            System.out.println("Failed! An unexpected exception was encountered...");
            e.printStackTrace();
        }
    }

    /**
     * This example demonstrates the AsyncCallExecutor executing three Callable's in parallel
     */
    private static void asyncCallExecutor() {
        ResultGenerator<String> generator = new ResultGenerator<>();
        generator.setPossibleExceptions(RuntimeException.class);
        generator.setPossibleValues("this was a success!");

        Callable<String> callable1 = generator::generateRandomResult;
        Callable<String> callable2 = generator::generateRandomResult;
        Callable<String> callable3 = generator::generateRandomResult;

        RetryConfig config = new RetryConfigBuilder()
                .retryOnAnyException()
                .withRandomExponentialBackoff()
                .withDelayBetweenTries(Duration.ofMillis(100))
                .withMaxNumberOfTries(5)
                .build();

        AsyncCallExecutor<String> executor = new CallExecutorBuilder<String>()
                .config(config)
                .onSuccessListener(s -> {
                    System.out.println("[" + s.getId() + "] Success!");
                    System.out.println("[" + s.getId() + "] Status: " + s);
                })
                .onCompletionListener(s -> System.out.println("[" + s.getId() + "] Retry execution complete!"))
                .onFailureListener(s -> System.out.println("[" + s.getId() + "] Failed!"))
                .afterFailedTryListener(s -> System.out.println("[" + s.getId() + "] Try failed! Will try again in 250ms."))
                .beforeNextTryListener(s -> System.out.println("[" + s.getId() + "] Trying again..."))
                .buildAsync();

        CompletableFuture<Status<String>> result1 = executor.execute(callable1);
        CompletableFuture<Status<String>> result2 = executor.execute(callable2);
        CompletableFuture<Status<String>> result3 = executor.execute(callable3);

        CompletableFuture.allOf(result1, result2, result3).join();
    }

    /**
     * This example has four possible outcomes from each call: (1) "this was a failure!" is returned and a retry is
     * triggered (2) a ConnectException is thrown and a retry is triggered (3) "this was a success!" is returned and the
     * executor finishes with a success (4) a RuntimeException is thrown and the executor stops due to an unexpected
     * exception occurring
     */
    private static void generalExample_usingListeners() {
        ResultGenerator<String> generator = new ResultGenerator<>();
        generator.setPossibleExceptions(ConnectException.class, RuntimeException.class);
        generator.setPossibleValues("this was a failure!", "this was a success!");

        Callable<String> callable = generator::generateRandomResult;

        RetryConfig config = new RetryConfigBuilder()
                .retryOnSpecificExceptions(ConnectException.class)
                .retryOnReturnValue("this was a failure!")
                .withDelayBetweenTries(Duration.ofMillis(250))
                .withFixedBackoff()
                .withMaxNumberOfTries(5)
                .build();

        new CallExecutorBuilder<String>()
                .config(config)
                .onSuccessListener(s -> {
                    System.out.println("Success!");
                    System.out.println("Status: " + s);
                })
                .onCompletionListener(s -> System.out.println("Retry execution complete!"))
                .onFailureListener(s -> System.out.println("Failed!"))
                .afterFailedTryListener(s -> System.out.println("Try failed! Will try again in 250ms."))
                .beforeNextTryListener(s -> System.out.println("Trying again..."))
                .build()
                .execute(callable);

    }
}
