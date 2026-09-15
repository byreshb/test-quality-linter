package io.github.byreshb.tql.benchmark;

/** Places orders through an {@link OrderRepository}, rejecting invalid ones. */
public final class OrderService {

  private final OrderRepository repository;

  /**
   * Creates the service.
   *
   * @param repository where orders are stored
   */
  public OrderService(OrderRepository repository) {
    this.repository = repository;
  }

  /**
   * Validates and saves an order.
   *
   * @param order the order to place
   * @return the saved order
   * @throws IllegalArgumentException when the order is not valid
   */
  public Order place(Order order) {
    if (!order.isValid()) {
      throw new IllegalArgumentException("invalid order: " + order);
    }
    return repository.save(order);
  }

  /**
   * How many orders are on the books.
   *
   * @return the repository's count
   */
  public int backlogSize() {
    return repository.count();
  }
}
