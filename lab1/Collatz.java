/** Class that prints the Collatz sequence starting from a given number.
 *  @author Yun Zhu
 */
public class Collatz {

    /** Return the next number */
    public static int nextNumber(int n) {
        if (n  == 1) {
            return n;
        } else if (n % 2 != 0) {
            return 3 * n + 1;
        } else {
            return n / 2;
        }
    }

    public static void main(String[] args) {
        int n = 5;
        System.out.print(n + " ");
        while (n != 1) {
            n = nextNumber(n);
            System.out.print(n + " ");
        }
        System.out.println();
    }
}
