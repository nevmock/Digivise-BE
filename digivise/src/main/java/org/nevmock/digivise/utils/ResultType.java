package org.nevmock.digivise.utils;

public class ResultType<T, E> {
    private final T result;
    private final E error;

    public ResultType(T result, E error) {
        this.result = result;
        this.error = error;
    }

    public T getResult() {
        return result;
    }

    public E getError() {
        return error;
    }

    public boolean isSuccess() {
        return error == null;
    }

    public boolean isFailure() {
        return error != null;
    }
}