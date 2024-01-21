package tester;

import static org.junit.Assert.*;

import edu.princeton.cs.algs4.StdRandom;
import org.junit.Test;
import student.StudentArrayDeque;

public class TestArrayDequeEC {
    private static final int N = 1000;
    private static String message = "";

    private void randomAdd(double firstOrLast, Integer i, ArrayDequeSolution<Integer> correct, StudentArrayDeque<Integer> broken) {
        if (firstOrLast < 0.5) {
            correct.addFirst(i);
            broken.addFirst(i);
            message += "\naddFirst(" + i + ")";
        } else {
            correct.addLast(i);
            broken.addLast(i);
            message += "\naddLast(" + i + ")";
        }
    }

    private void randomRemove(double random, Integer i, ArrayDequeSolution<Integer> correct, StudentArrayDeque<Integer> broken) {
        Integer expected;
        Integer actual;
        if (random < 0.5) {
            expected = correct.removeFirst();
            actual = broken.removeFirst();
            message += "\nremoveFirst()";
        } else {
            expected = correct.removeLast();
            actual = broken.removeLast();
            message += "\nremoveLast()";
        }
        assertEquals(message, expected, actual);
    }

    @Test
    public void testRandomized() {
        ArrayDequeSolution<Integer> correct = new ArrayDequeSolution<>();
        StudentArrayDeque<Integer> broken = new StudentArrayDeque<>();

        for (Integer i = 0; i < N; i += 1) {
            if (correct.isEmpty()) {
                double firstOrLast = StdRandom.uniform();
                randomAdd(firstOrLast, i, correct, broken);
            } else {
                double addOrRemove = StdRandom.uniform();
                double firstOrLast = StdRandom.uniform();
                if (addOrRemove < 0.5) {
                    randomAdd(firstOrLast, i, correct, broken);
                } else {
                    randomRemove(firstOrLast, i, correct, broken);
                }
            }
        }
    }
}
