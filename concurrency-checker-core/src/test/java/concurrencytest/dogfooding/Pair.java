package concurrencytest.dogfooding;

public record Pair(long offset, int size) {
    public boolean overlaps(Pair other) {
        if (this.offset < other.offset()) {
            return this.offset + size < other.offset();
        }
        return other.offset + other.size < this.offset;
    }
}
