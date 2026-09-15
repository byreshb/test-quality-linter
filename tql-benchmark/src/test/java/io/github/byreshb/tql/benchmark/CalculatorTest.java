package io.github.byreshb.tql.benchmark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class CalculatorTest {

  @Test
  void addsTwoNumbers() {
    assertEquals(7, Calculator.add(3, 4));
  }

  @Test
  void subtractsTwoNumbers() {
    assertEquals(1, Calculator.subtract(4, 3));
  }

  @Test
  void multipliesTwoNumbers() {
    assertEquals(12, Calculator.multiply(3, 4));
  }

  @Test
  void dividesTwoNumbers() {
    assertEquals(4, Calculator.divide(12, 3));
  }

  @Test
  void divideByZeroThrows() {
    assertThrows(ArithmeticException.class, () -> Calculator.divide(1, 0));
  }

  @Test
  void recognisesEvenNumbers() {
    assertTrue(Calculator.isEven(4));
  }

  @Test
  void recognisesOddNumbers() {
    assertFalse(Calculator.isEven(3));
  }

  @Test
  void maxReturnsTheLargerNumber() {
    assertEquals(9, Calculator.max(9, 2));
  }

  @Test
  void addsNegativeNumbers() {
    int result = Calculator.add(-1, -2);
    assertEquals(-3, result);
  }

  @Weak({"TQL008"})
  @Test
  void addsTwoNegativeNumbers() {
    int result = Calculator.add(-1, -2);
    assertEquals(-3, result);
  }

  @Weak({"TQL001"})
  @Test
  void addCompletesWithoutError() {
    int sum = Calculator.add(2, 2);
    assertEquals(sum, sum);
  }

  @Weak({"TQL002"})
  @Test
  void addMatchesArithmetic() {
    assertEquals(4, 2 + 2);
  }

  @Weak({"TQL006"})
  @Test
  void addIsFastEnough() throws InterruptedException {
    Thread.sleep(50);
    assertTrue(Calculator.add(1, 1) >= 0);
  }

  @Weak({"TQL009"})
  @Disabled
  @Test
  void handlesOverflow() {
    assertTrue(Calculator.isEven(4));
  }

  @Weak({"TQL010"})
  @Test
  void isEvenChecksParity() {
    assertTrue(Calculator.isEven(4), "4 should not be even");
  }
}
