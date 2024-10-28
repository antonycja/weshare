package weshare.controller;

import io.javalin.http.Handler;

import org.javamoney.moneta.Money;
import org.javamoney.moneta.function.MonetaryFunctions;
import org.jetbrains.annotations.NotNull;
import weshare.model.Expense;
import weshare.model.PaymentRequest;
import weshare.model.Person;
import weshare.persistence.ExpenseDAO;
import weshare.server.Routes;
import weshare.server.ServiceRegistry;
import weshare.server.WeShareServer;

import javax.money.MonetaryAmount;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Objects;
import java.time.LocalDate;
import java.util.HashMap;

import static weshare.model.MoneyHelper.ZERO_RANDS;

public class PaymentRequestController {

    public static final Handler view = context -> {
        ExpenseDAO expensesDAO = ServiceRegistry.lookup(ExpenseDAO.class);
        String expenseId = context.queryParam("expenseId");
        UUID id = UUID.fromString(expenseId);
        Expense expense = expensesDAO.get(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense with ID " + id + " not found."));
        MonetaryAmount grandAmount = ZERO_RANDS;
        for (PaymentRequest request : expense.listOfPaymentRequests()) {
            grandAmount = grandAmount.add(request.getAmountToPay());
        }
        Map<String, Object> viewModel = new HashMap<>();
        viewModel.put("id", expenseId);
        viewModel.put("expense", expense);
        viewModel.put("paymentRequest", expense.listOfPaymentRequests());
        viewModel.put("total", grandAmount);
        context.render("paymentrequest.html", viewModel);
    };

    public static final Handler action = context -> {
        ExpenseDAO expensesDAO = ServiceRegistry.lookup(ExpenseDAO.class);
        Person personLoggedIn = WeShareServer.getPersonLoggedIn(context);

        String email = context.formParamAsClass("email", String.class)
                .check(Objects::nonNull, "Email is required")
                .get();

        int amount = context.formParamAsClass("amount", Integer.class)
                .check(Objects::nonNull, "Amount is required")
                .get();

        LocalDate date = context.formParamAsClass("due_date", LocalDate.class)
                .check(Objects::nonNull, "Date is required")
                .get();

        String idString = context.formParam("expense_id");


        UUID id = UUID.fromString(idString);
        Expense expense = expensesDAO.get(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense with ID " + id + " not found."));

        PaymentRequest paymentRequest = expense.requestPayment(new Person(email), Money.of(amount, "ZAR"), date);

        context.sessionAttribute(WeShareServer.SESSION_USER_KEY, personLoggedIn);
        context.redirect(Routes.PAYMENT_REQUEST + "?expenseId=" + idString);

    };
}
