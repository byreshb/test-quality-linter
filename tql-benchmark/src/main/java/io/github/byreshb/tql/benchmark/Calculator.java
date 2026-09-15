package io.github.byreshb.tql.benchmark;

/** Simple arithmetic, deliberately branchy so mutation testing has something to bite into. */
public final class Calculator {

  private Calculator() {}

  /**
   * Adds two numbers.
   *
   * @param a the first number
   * @param b the second number
   * @return the sum
   */
  public static int add(int a, int b) {
    return a + b;
  }

  /**
   * Subtracts one number from another.
   *
   * @param a the number to subtract from
   * @param b the number to subtract
   * @return the difference
   */
  public static int subtract(int a, int b) {
    return a - b;
  }

  /**
   * Multiplies two numbers.
   *
   * @param a the first number
   * @param b the second number
   * @return the product
   */
  public static int multiply(int a, int b) {
    return a * b;
  }

  /**
   * Divides one number by another.
   *
   * @param a the dividend
   * @param b the divisor
   * @return the quotient
   * @throws ArithmeticException when {@code b} is zero
   */
  public static int divide(int a, int b) {
    if (b == 0) {
      throw new ArithmeticException("division by zero");
    }
    return a / b;
  }

  /**
   * Whether a number is even.
   *
   * @param n the number
   * @return true when {@code n} is even
   */
  public static boolean isEven(int n) {
    return n % 2 == 0;
  }

  /**
   * The larger of two numbers.
   *
   * @param a the first number
   * @param b the second number
   * @return the maximum
   */
  public static int max(int a, int b) {
    return a > b ? a : b;
  }
}
