package bg.sirma.keycloak.external;

public class Pair<L, R> {
    private L left;
    private R right;

    private Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    public L getLeft() {
        return left;
    }

    public R getRight() {
        return right;
    }

    public static <T, P> Pair<T, P> of(T left, P right) {
        return new Pair<>(left, right);
    }
}
