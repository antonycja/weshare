package weshare.controller;

import io.javalin.http.Handler;

import org.javamoney.moneta.Money;
import org.javamoney.moneta.function.MonetaryFunctions;
import org.jetbrains.annotations.NotNull;
import weshare.model.Expense;
import weshare.model.Payment;
import weshare.model.PaymentRequest;
import weshare.model.Person;
import weshare.persistence.ExpenseDAO;
import weshare.server.Routes;
import weshare.server.ServiceRegistry;
import weshare.server.WeShareServer;

import javax.money.MonetaryAmount;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.UUID;

import static weshare.model.MoneyHelper.ZERO_RANDS;

public class PaymentRequestsReceivedController {

    public static final Handler view = context -> {
        ExpenseDAO expensesDAO = ServiceRegistry.lookup(ExpenseDAO.class);
        Person personLoggedIn = WeShareServer.getPersonLoggedIn(context);

        Collection<PaymentRequest> paymentRequestsReceived = expensesDAO.findPaymentRequestsReceived(personLoggedIn);

        MonetaryAmount grandAmount = ZERO_RANDS;
        for (PaymentRequest payment : paymentRequestsReceived) {
            grandAmount = grandAmount.add(payment.getAmountToPay());
        }
        Map<String, Object> viewModel = new HashMap<>();
        viewModel.put("paymentRequests", paymentRequestsReceived);
        viewModel.put("grandTotal", grandAmount);

        context.render("paymentrequests_received.html", viewModel);
    };

    public static final Handler action = context -> {
        ExpenseDAO expensesDAO = ServiceRegistry.lookup(ExpenseDAO.class);
        Person personLoggedIn = WeShareServer.getPersonLoggedIn(context);

        String idString = context.formParam("id");
        UUID id = UUID.fromString(idString);

        Collection<PaymentRequest> paymentRequests = expensesDAO.findPaymentRequestsReceived(personLoggedIn);

        PaymentRequest paymentRequest = paymentRequests.stream()
                .filter(request -> request.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("PaymentRequest with ID " + id + " not found."));
        Payment payment = paymentRequest.pay(personLoggedIn, LocalDate.now());
        Expense expense = new Expense(personLoggedIn, payment.getExpenseForPersonPaying().getDescription(),
                payment.getAmountPaid(),
                payment.getPaymentDate());
        expensesDAO.save(expense);
        context.redirect(Routes.PAYMENT_REQUESTS_RECEIVED);
    };
}
