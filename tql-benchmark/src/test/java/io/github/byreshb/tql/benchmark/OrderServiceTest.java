package io.github.byreshb.tql.benchmark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class OrderServiceTest {

  @Test
  void placesAWidgetOrder() {
    OrderRepository repository = mock(OrderRepository.class);
    OrderService service = new OrderService(repository);
    Order order = new Order("widget", 2);
    when(repository.save(order)).thenReturn(order);
    Order saved = service.place(order);
    assertEquals(order, saved);
  }

  @Weak({"TQL008"})
  @Test
  void placesAWidgetOrderAgain() {
    OrderRepository repository = mock(OrderRepository.class);
    OrderService service = new OrderService(repository);
    Order order = new Order("widget", 2);
    when(repository.save(order)).thenReturn(order);
    Order saved = service.place(order);
    assertEquals(order, saved);
  }

  @Test
  void placesAValidOrderAndReturnsTheSavedOrder() {
    OrderRepository repository = mock(OrderRepository.class);
    OrderService service = new OrderService(repository);
    Order order = new Order("gadget", 1);
    when(repository.save(order)).thenReturn(order);
    Order saved = service.place(order);
    assertEquals(order, saved);
    verify(repository).save(order);
  }

  @Test
  void placingAnInvalidOrderThrows() {
    OrderRepository repository = mock(OrderRepository.class);
    OrderService service = new OrderService(repository);
    assertThrows(IllegalArgumentException.class, () -> service.place(new Order("", 0)));
  }

  @Test
  void backlogSizeReturnsTheRepositoryCount() {
    OrderRepository repository = mock(OrderRepository.class);
    when(repository.count()).thenReturn(3);
    OrderService service = new OrderService(repository);
    int backlog = service.backlogSize();
    assertEquals(3, backlog);
  }

  @Weak({"TQL004"})
  @Test
  void countsBacklogOrders() {
    OrderRepository repository = mock(OrderRepository.class);
    when(repository.count()).thenReturn(5);
    assertEquals(5, repository.count());
  }

  @Weak({"TQL004"})
  @Test
  void savesAndAssertsStubbedOrder() {
    OrderRepository repository = mock(OrderRepository.class);
    Order stubbed = new Order("widget", 1);
    when(repository.save(any())).thenReturn(stubbed);
    assertEquals(stubbed, repository.save(any()));
  }

  @Weak({"TQL005"})
  @Test
  void placesOrderAndOnlyVerifiesSave() {
    OrderRepository repository = mock(OrderRepository.class);
    OrderService service = new OrderService(repository);
    Order order = new Order("widget", 2);
    when(repository.save(order)).thenReturn(order);
    service.place(order);
    verify(repository).save(order);
  }

  @Weak({"TQL005"})
  @Test
  void placesOrderAndOnlyVerifiesNoExtraInteractions() {
    OrderRepository repository = mock(OrderRepository.class);
    OrderService service = new OrderService(repository);
    Order order = new Order("widget", 3);
    when(repository.save(order)).thenReturn(order);
    service.place(order);
    verify(repository).save(order);
    verifyNoMoreInteractions(repository);
  }

  @Weak({"TQL006"})
  @Test
  void placedOrderEventuallyAppearsInBacklog() throws InterruptedException {
    OrderRepository repository = mock(OrderRepository.class);
    OrderService service = new OrderService(repository);
    service.place(new Order("widget", 1));
    Thread.sleep(100);
    assertTrue(service.backlogSize() >= 0);
  }

  @Weak({"TQL007"})
  @Test
  void placingAnInvalidOrderIsHandledSilently() {
    OrderRepository repository = mock(OrderRepository.class);
    OrderService service = new OrderService(repository);
    try {
      service.place(new Order("", 0));
    } catch (IllegalArgumentException e) {
      System.out.println("failed: " + e.getMessage());
    }
    assertNotNull(service);
  }

  @Weak({"TQL009"})
  @Disabled
  @Test
  void handlesConcurrentOrderPlacement() {
    OrderRepository repository = mock(OrderRepository.class);
    OrderService service = new OrderService(repository);
    assertNotNull(service);
  }

  @Weak({"TQL011"})
  @Test
  void computesBacklogButIgnoresIt() {
    OrderRepository repository = mock(OrderRepository.class);
    when(repository.count()).thenReturn(3);
    OrderService service = new OrderService(repository);
    service.backlogSize();
    assertNotNull(service);
  }

  @Weak({"TQL012"})
  @Test
  void placesOrderThroughAMockedService() {
    OrderService mockedService = mock(OrderService.class);
    Order order = new Order("widget", 2);
    Order result = mockedService.place(order);
    assertNull(result);
  }

  @Weak({"TQL012"})
  @Test
  void backlogSizeFromAMockedService() {
    OrderService mockedService = Mockito.mock(OrderService.class);
    assertEquals(0, mockedService.backlogSize());
  }
}
