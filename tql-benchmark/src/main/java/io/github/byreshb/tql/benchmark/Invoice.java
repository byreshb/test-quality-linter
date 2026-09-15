package io.github.byreshb.tql.benchmark;

import java.util.List;

/** A collection of billed lines with a total and a discount. */
public final class Invoice {

  private final List<Line> lines;

  /**
   * Creates an invoice.
   *
   * @param lines the lines it bills
   */
  public Invoice(List<Line> lines) {
    this.lines = List.copyOf(lines);
  }

  /**
   * The invoice total before discount.
   *
   * @return the sum of every line's amount, in cents
   */
  public int totalCents() {
    return lines.stream().mapToInt(Line::amountCents).sum();
  }

  /**
   * The total after a percentage discount.
   *
   * @param percent the discount, 0 to 100
   * @return the discounted total, in cents
   * @throws IllegalArgumentException when {@code percent} is out of range
   */
  public int applyDiscountCents(int percent) {
    if (percent < 0 || percent > 100) {
      throw new IllegalArgumentException("percent out of range: " + percent);
    }
    return totalCents() - (totalCents() * percent / 100);
  }

  /**
   * Whether the invoice has no lines.
   *
   * @return true when there are no lines
   */
  public boolean isEmpty() {
    return lines.isEmpty();
  }

  /**
   * How many lines the invoice has.
   *
   * @return the line count
   */
  public int lineCount() {
    return lines.size();
  }
}
