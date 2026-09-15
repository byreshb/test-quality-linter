package io.github.byreshb.tql.benchmark;

/** Where orders are stored, abstracted so tests can mock it. */
public interface OrderRepository {

  /**
   * Saves an order.
   *
   * @param order the order to save
   * @return the saved order
   */
  Order save(Order order);

  /**
   * How many orders are stored.
   *
   * @return the count
   */
  int count();
}
