```java
package com.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.apiEach;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link Main#computeSecretFromFile(String)}.
 * <p>
 * This test verifies that a well‑formed JSON file containing the keys
 * {@code k} and {@code n} is parsed correctly and that the method
 * returns the expected secret value (k * n in this example).
 */
class ComputeSecretWithValidJSONTest {

    private Path tempFile;

    @BeforeEach
    void setUp() throws IOException {
        // Create a temporary file that will be deleted after the test
        tempFile = Files.createTempFile("secret", ".json");
    }

    @AfterEach
    void tearDown() throws IOException {
        // Ensure the temporary file is removed
        Files.deleteIfExists(tempFile);
    }

    @Test
    void testComputeSecretFromValidJSON() throws IOException {
        // Arrange: write a simple JSON object with k and n
        String jsonContent = """
                {
                    "k": 3,
                    "n": 5
                }
                """;
        Files.writeString(tempFile, jsonContent);

        // Act: compute the secret using the method under test
        String secret = Main.computeSecretFromFile(tempFile.toString());

        // Assert: the secret should not be null and should equal k * n
        assertNotNull(secret, "Secret should not be null");
        assertEquals("15", secret, "Secret should be the product of k and n");
    }
}
```