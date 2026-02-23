package cs.min2phase;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;

public class ToolsTest {

    @Test
    public void testRandomCubeProperties() {
        for (int i = 0; i < 100; i++) {
            String cube = Tools.randomCube();
            assertNotNull(cube);
            assertEquals(54, cube.length(), "Cube string should have length 54");

            // Centers are at indices 4, 13, 22, 31, 40, 49
            char[] centers = {
                cube.charAt(4),  // U
                cube.charAt(13), // R
                cube.charAt(22), // F
                cube.charAt(31), // D
                cube.charAt(40), // L
                cube.charAt(49)  // B
            };

            Map<Character, Integer> counts = new HashMap<>();
            for (char c : cube.toCharArray()) {
                counts.put(c, counts.getOrDefault(c, 0) + 1);
            }

            for (char center : centers) {
                assertEquals(9, counts.get(center), "Each color should appear 9 times");
            }

            assertEquals(0, Tools.verify(cube), "Cube should be solvable");
        }
    }

    @Test
    public void testRandomCubeDeterministic() {
        long seed = 12345L;
        String cube1 = Tools.randomCube(new Random(seed));
        String cube2 = Tools.randomCube(new Random(seed));
        assertEquals(cube1, cube2, "Same seed should produce same cube");
    }

    @Test
    public void testOtherRandomStates() {
        verifyState(Tools.randomLastLayer(), "randomLastLayer");
        verifyState(Tools.randomLastSlot(), "randomLastSlot");
        verifyState(Tools.randomZBLastLayer(), "randomZBLastLayer");
        verifyState(Tools.randomCornerOfLastLayer(), "randomCornerOfLastLayer");
        verifyState(Tools.randomEdgeOfLastLayer(), "randomEdgeOfLastLayer");
        verifyState(Tools.randomCrossSolved(), "randomCrossSolved");
        verifyState(Tools.randomEdgeSolved(), "randomEdgeSolved");
        verifyState(Tools.randomCornerSolved(), "randomCornerSolved");
    }

    private void verifyState(String cube, String name) {
        assertNotNull(cube, name + " should not be null");
        assertEquals(54, cube.length(), name + " should have length 54");
        assertEquals(0, Tools.verify(cube), name + " should be solvable");
    }
}
