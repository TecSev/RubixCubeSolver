package cs.min2phase;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SearchTest {

    @Test
    public void testSolutionSolvedState() {
        String solved = "UUUUUUUUURRRRRRRRRFFFFFFFFFDDDDDDDDDLLLLLLLLLBBBBBBBBB";
        Search search = new Search();
        // 100000 probes is enough for testing
        String result = search.solution(solved, 21, 100000, 0, 0);
        assertNotNull(result);
        assertFalse(result.startsWith("Error"), "Should not error on solved state. Result: " + result);
    }
}
