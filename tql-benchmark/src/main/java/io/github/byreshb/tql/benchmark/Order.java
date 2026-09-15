package io.github.byreshb.tql.benchmark;

import java.util.UUID;

/**
 * An order.
 *
 * @param id a unique id
 * @param product the product name
 * @param quantity how many were ordered
 */
public record Order(String id, String product, int quantity) {

  /**
   * Creates an order with a random id.
   *
   * @param product the product name
   * @param quantity how many were ordered
   */
  public Order(String product, int quantity) {
    this(UUID.randomUUID().toString(), product, quantity);
  }

  /**
   * Whether the order is well-formed.
   *
   * @return true when the quantity is positive and the product name is non-blank
   */
  public boolean isValid() {
    return quantity > 0 && product != null && !product.isBlank();
  }
}
