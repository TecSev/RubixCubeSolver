import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ReproduceBug {

    @Test
    public void testBugReproduction() {
        CubeState cube = new CubeState();

        String[] moves = {"R", "L", "U", "D", "F", "B", "R2", "L2", "U2", "D2", "F2", "B2", "R", "L", "U"};

        for (String move : moves) {
            cube.doTurn(move);
        }

        String result = cube.generateString();
        System.out.println("Result: " + result);

        assertEquals(54, result.length(), "Result length should be 54");
        assertFalse(result.contains("X"), "Result should not contain X: " + result);
        assertFalse(result.contains("?"), "Result should not contain ?: " + result);
    }
}
