import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

public class QueueBenchmark {

    private static final int NUM_OPERATIONS = 5000000;
    private static final int WARMUP_ITERATIONS = 5;
    private static final int BENCHMARK_ITERATIONS = 10;

    public static void main(String[] args) {
        System.out.println("Starting Queue Benchmark...");
        System.out.println("Comparing LinkedList vs ArrayDeque performance.");
        System.out.println("Operations: " + NUM_OPERATIONS + " adds and polls per iteration.");
        System.out.println("Warmup iterations: " + WARMUP_ITERATIONS);
        System.out.println("Benchmark iterations: " + BENCHMARK_ITERATIONS);
        System.out.println("--------------------------------------------------");

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            benchmark(new LinkedList<>());
            benchmark(new ArrayDeque<>());
        }

        long totalLinkedListTime = 0;
        long totalArrayDequeTime = 0;

        // Benchmark
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            totalLinkedListTime += benchmark(new LinkedList<>());
            totalArrayDequeTime += benchmark(new ArrayDeque<>());
        }

        double avgLinkedListTime = totalLinkedListTime / (double) BENCHMARK_ITERATIONS / 1_000_000.0;
        double avgArrayDequeTime = totalArrayDequeTime / (double) BENCHMARK_ITERATIONS / 1_000_000.0;

        System.out.println("--------------------------------------------------");
        System.out.printf("LinkedList Average Time: %.3f ms%n", avgLinkedListTime);
        System.out.printf("ArrayDeque Average Time: %.3f ms%n", avgArrayDequeTime);
        System.out.printf("Performance Improvement: %.2f%%%n", (avgLinkedListTime - avgArrayDequeTime) / avgLinkedListTime * 100);
    }

    private static long benchmark(Queue<String> queue) {
        long startTime = System.nanoTime();
        for (int i = 0; i < NUM_OPERATIONS; i++) {
            queue.add("Move" + i);
        }
        while (!queue.isEmpty()) {
            queue.poll();
        }
        return System.nanoTime() - startTime;
    }
}
