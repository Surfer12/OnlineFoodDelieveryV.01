package main;

import model.Driver;
import model.MenuItem;
import model.Order;
import model.Size;
import factory.MenuItemFactory;
import orderUtilities.OrderBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import notification.NotificationService;
import exception.OrderProcessingException;
import exception.PaymentException;
import exception.ValidationException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DeliverySystemTest {

    private DeliverySystem deliverySystem;
    private NotificationService notificationService;

    @BeforeEach
    public void setUp() {
        deliverySystem = new DeliverySystem();
        notificationService = mock(NotificationService.class);
    }

    @Test
    public void testSubmitOrder_Success() {
        MenuItemFactory factory = new MenuItemFactory();
        MenuItem pizza = factory.createMenuItem("hamburger", "Pepperoni Pizza", "Spicy pepperoni with cheese", 12.99, Size.MEDIUM, 1);

        Order order = new OrderBuilder()
                .withValidatedCustomerId(1L)
                .withCustomerEmail("jane.doe@example.com")
                .addItem(pizza)
                .withValidatedDeliveryLocation("456 Elm Street", "12345")
                .build();

        deliverySystem.submitOrder(order);

        assertTrue(deliverySystem.getOrderQueue().contains(order));
    }

    @Test
    public void testSubmitOrder_ValidationException() {
        Order order = new OrderBuilder()
                .withValidatedCustomerId(1L)
                .withCustomerEmail("jane.doe@example.com")
                .withValidatedDeliveryLocation("456 Elm Street", "12345")
                .build();

        assertThrows(OrderProcessingException.class, () -> deliverySystem.submitOrder(order));
    }

    @Test
    public void testSubmitOrder_PaymentException() {
        MenuItemFactory factory = new MenuItemFactory();
        MenuItem pizza = factory.createMenuItem("hamburger", "Pepperoni Pizza", "Spicy pepperoni with cheese", 12.99, Size.MEDIUM, 1);

        Order order = new OrderBuilder()
                .withValidatedCustomerId(1L)
                .withCustomerEmail("jane.doe@example.com")
                .addItem(pizza)
                .withValidatedDeliveryLocation("456 Elm Street", "12345")
                .build();

        doThrow(new PaymentException("Payment failed")).when(order).processPayment(anyString());

        assertThrows(OrderProcessingException.class, () -> deliverySystem.submitOrder(order));
    }

    @Test
    public void testAssignOrderToDriver_Success() {
        MenuItemFactory factory = new MenuItemFactory();
        MenuItem pizza = factory.createMenuItem("hamburger", "Pepperoni Pizza", "Spicy pepperoni with cheese", 12.99, Size.MEDIUM, 1);

        Order order = new OrderBuilder()
                .withValidatedCustomerId(1L)
                .withCustomerEmail("jane.doe@example.com")
                .addItem(pizza)
                .withValidatedDeliveryLocation("456 Elm Street", "12345")
                .build();

        Driver driver = new Driver(101L, "Bob Smith", "Car", "ABC123");

        deliverySystem.registerDriver(driver);
        deliverySystem.submitOrder(order);
        deliverySystem.assignOrderToDriver(order, driver);

        assertTrue(driver.getCurrentOrder().isPresent());
        assertEquals(order, driver.getCurrentOrder().get());
    }

    @Test
    public void testCompleteDelivery_Success() {
        MenuItemFactory factory = new MenuItemFactory();
        MenuItem pizza = factory.createMenuItem("hamburger", "Pepperoni Pizza", "Spicy pepperoni with cheese", 12.99, Size.MEDIUM, 1);

        Order order = new OrderBuilder()
                .withValidatedCustomerId(1L)
                .withCustomerEmail("jane.doe@example.com")
                .addItem(pizza)
                .withValidatedDeliveryLocation("456 Elm Street", "12345")
                .build();

        Driver driver = new Driver(101L, "Bob Smith", "Car", "ABC123");

        deliverySystem.registerDriver(driver);
        deliverySystem.submitOrder(order);
        deliverySystem.assignOrderToDriver(order, driver);
        deliverySystem.completeDelivery(order.getOrderId(), driver.getId());

        assertFalse(driver.getCurrentOrder().isPresent());
        assertEquals(OrderStatus.DELIVERED, order.getStatus());
    }
}
