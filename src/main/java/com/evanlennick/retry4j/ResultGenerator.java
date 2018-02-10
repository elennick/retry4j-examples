package com.evanlennick.retry4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ResultGenerator<T> {

    private Random random;

    private List<Class> exceptions;

    private List<T> values;

    public ResultGenerator() {
        this.random = new Random();
        this.exceptions = new ArrayList<>();
        this.values = new ArrayList<>();
    }

    public void setPossibleExceptions(Class... exceptionClasses) {
        this.exceptions = Arrays.asList(exceptionClasses);
    }

    public void setPossibleValues(T... values) {
        this.values = Arrays.asList(values);
    }

    public T generateRandomResult() throws Exception {
        boolean shouldThrowException = random.nextBoolean();
        if (shouldThrowException) {
            Class exceptionClass = exceptions.get(random.nextInt(exceptions.size()));
            throw (Exception) exceptionClass.newInstance();
        } else {
            return values.get(random.nextInt(values.size()));
        }
    }
}
