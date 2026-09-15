package io.github.byreshb.tql.benchmark;

/**
 * One invoice line.
 *
 * @param description what was billed
 * @param quantity how many units
 * @param unitPriceCents the price of one unit, in cents
 */
public record Line(String description, int quantity, int unitPriceCents) {

  /**
   * The line's total.
   *
   * @return quantity times unit price, in cents
   */
  public int amountCents() {
    return quantity * unitPriceCents;
  }
}
