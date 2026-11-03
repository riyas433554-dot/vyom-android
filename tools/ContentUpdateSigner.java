import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;

public final class ContentUpdateSigner {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Usage: ContentUpdateSigner canonical-payload signature-output");
        String storePassword = requiredEnv("ANDROID_KEYSTORE_PASSWORD");
        String alias = requiredEnv("ANDROID_KEY_ALIAS");
        String keyPassword = requiredEnv("ANDROID_KEY_PASSWORD");
        Path keystorePath = Path.of(requiredEnv("ANDROID_KEYSTORE_PATH"));
        KeyStore keystore = KeyStore.getInstance("JKS");
        try (var input = Files.newInputStream(keystorePath)) {
            keystore.load(input, storePassword.toCharArray());
        }
        Key key = keystore.getKey(alias, keyPassword.toCharArray());
        if (!(key instanceof PrivateKey)) throw new IllegalStateException("Release alias is not a private key");
        String algorithm = key.getAlgorithm();
        String signatureAlgorithm = switch (algorithm.toUpperCase()) {
            case "EC", "ECDSA" -> "SHA256withECDSA";
            case "RSA" -> "SHA256withRSA";
            default -> throw new IllegalStateException("Unsupported release key algorithm: " + algorithm);
        };
        Signature signer = Signature.getInstance(signatureAlgorithm);
        signer.initSign((PrivateKey) key);
        signer.update(Files.readAllBytes(Path.of(args[0])));
        Files.write(Path.of(args[1]), signer.sign());
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing required environment variable " + name);
        return value;
    }
}
