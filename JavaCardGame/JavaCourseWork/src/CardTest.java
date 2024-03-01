import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CardTest {

    @Test
    void getValue() {

        // Arrange
        int expectedValue = 5;

        // Act
        Card card = new Card(expectedValue);

        // Assert
        assertEquals(expectedValue, card.getValue(), "value should match the expected value.");

    }

}