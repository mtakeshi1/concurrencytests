package concurrencytest.util;

import java.io.Serializable;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Either<L, R> extends Serializable {

    default boolean isLeft() {
        return maybeGetLeft().isPresent();
    }

    default boolean isRight() {
        return maybeGetLeft().isPresent();
    }

    Optional<L> maybeGetLeft();

    Optional<R> maybeGetRight();

    <L2> Either<L2, R> mapLeft(Function<L, L2> mapper);

    <R2> Either<L, R2> mapRight(Function<R, R2> mapper);

    default void ifLeft(Consumer<L> consumer) {
        Function<L, Void> m = a -> {
            consumer.accept(a);
            return null;
        };
        mapLeft(m).maybeGetLeft();
    }

    default void ifRight(Consumer<R> consumer) {
        Function<R, Void> m = a -> {
            consumer.accept(a);
            return null;
        };
        mapRight(m).maybeGetRight();
    }

    interface Left<L, R> extends Either<L, R> {
        @Override
        default Optional<R> maybeGetRight() {
            return Optional.empty();
        }

        @Override
        default <R2> Either<L, R2> mapRight(Function<R, R2> mapper) {
            return (Left<L, R2>) this::maybeGetLeft;
        }

        @Override
        default <L2> Either<L2, R> mapLeft(Function<L, L2> mapper) {
            return (Left<L2, R>) () -> this.maybeGetLeft().map(mapper);
        }
    }


    interface Right<L, R> extends Either<L, R> {

        @Override
        default Optional<L> maybeGetLeft() {
            return Optional.empty();
        }

        @Override
        default <R2> Either<L, R2> mapRight(Function<R, R2> mapper) {
            return (Right<L, R2>) () -> this.maybeGetRight().map(mapper);
        }

        @Override
        default <L2> Either<L2, R> mapLeft(Function<L, L2> mapper) {
            return (Right<L2, R>) this::maybeGetRight;
        }
    }

    static <LL, RR> Either<LL, RR> left(LL value) {
        return (Left<LL, RR>) () -> Optional.of(value);
    }

    static <LL, RR> Either<LL, RR> right(RR value) {
        return (Right<LL, RR>) () -> Optional.of(value);
    }

}
