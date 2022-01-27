package concurrencytest;

import concurrencytest.util.Murmur3A;

import java.nio.charset.StandardCharsets;

public class CheckpointPrototype {

    private final long id;
    private final String details;

    public CheckpointPrototype(long id, String details) {
        this.id = id;
        this.details = details;
    }

    public CheckpointPrototype(String extendedDetails) {
        this.details = extendedDetails;
        this.id = calculateHash(extendedDetails);
    }

    private static long calculateHash(String extendedDetails) {
        Murmur3A murmur3A = new Murmur3A(extendedDetails.length());
        murmur3A.update(extendedDetails.getBytes(StandardCharsets.UTF_8));
        return murmur3A.getValue();
    }
}
