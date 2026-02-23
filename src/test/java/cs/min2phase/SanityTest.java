package cs.min2phase;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SanityTest {
    @Test
    public void testSearchInit() {
        Search.init();
        assertTrue(Search.isInited());
    }
}
