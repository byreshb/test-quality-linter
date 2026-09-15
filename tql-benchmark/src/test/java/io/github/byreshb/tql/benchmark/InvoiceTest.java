package io.github.byreshb.tql.benchmark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class InvoiceTest {

  @Test
  void computesTheTotal() {
    Invoice invoice = new Invoice(List.of(new Line("widget", 2, 500), new Line("gadget", 1, 300)));
    assertEquals(1300, invoice.totalCents());
  }

  @Test
  void appliesADiscount() {
    Invoice invoice = new Invoice(List.of(new Line("widget", 1, 1000)));
    assertEquals(750, invoice.applyDiscountCents(25));
  }

  @Test
  void rejectsADiscountOutOfRange() {
    Invoice invoice = new Invoice(List.of(new Line("widget", 1, 1000)));
    assertThrows(IllegalArgumentException.class, () -> invoice.applyDiscountCents(150));
  }

  @Test
  void anInvoiceWithNoLinesIsEmpty() {
    assertTrue(new Invoice(List.of()).isEmpty());
  }

  @Test
  void anInvoiceWithLinesIsNotEmpty() {
    Invoice invoice = new Invoice(List.of(new Line("widget", 1, 1000)));
    assertFalse(invoice.isEmpty());
  }

  @Test
  void countsTheLines() {
    Invoice invoice = new Invoice(List.of(new Line("widget", 1, 1000), new Line("gadget", 2, 200)));
    assertEquals(2, invoice.lineCount());
  }

  @Weak({"TQL003", "TQL011"})
  @Test
  void computesTotalWithoutChecking() {
    Invoice invoice = new Invoice(List.of(new Line("widget", 2, 500)));
    invoice.totalCents();
  }

  @Weak({"TQL007", "TQL011"})
  @Test
  void discountValidatesRange() {
    Invoice invoice = new Invoice(List.of(new Line("widget", 1, 1000)));
    try {
      invoice.applyDiscountCents(150);
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    }
    assertNotNull(invoice);
  }
}
