package weshare.controller;

import io.javalin.core.validation.JavalinValidation;
import io.javalin.http.Handler;

import org.javamoney.moneta.Money;
import org.javamoney.moneta.function.MonetaryFunctions;
import org.jetbrains.annotations.NotNull;
import weshare.model.Expense;
import weshare.model.Person;
import weshare.persistence.ExpenseDAO;
import weshare.server.Routes;
import weshare.server.ServiceRegistry;
import weshare.server.WeShareServer;

import javax.money.MonetaryAmount;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static weshare.model.MoneyHelper.ZERO_RANDS;
import static weshare.model.MoneyHelper.amountOf;

public class ExpensesController {
    private static Collection<Expense> expenses;
    public static final Handler view = context -> {
        ExpenseDAO expensesDAO = ServiceRegistry.lookup(ExpenseDAO.class);
        Person personLoggedIn = WeShareServer.getPersonLoggedIn(context);

        expenses = expensesDAO.findExpensesForPerson(personLoggedIn);
        MonetaryAmount grandAmount = ZERO_RANDS;
        for (Expense expense : expenses) {
            grandAmount = grandAmount.add(expense.amountLessPaymentsReceived());
        }

        Map<String, Object> viewModel = new HashMap<>();
        if (grandAmount.isGreaterThan(ZERO_RANDS)) {
            viewModel.put("expenses", expenses);
            viewModel.put("grandTotal", grandAmount);
        }

        context.render("expenses.html", viewModel);
    };

    public static final Handler form = context -> {
        context.render("newexpense.html");
    };

    public static final Handler newexpense = context -> {
        Person personLoggedIn = WeShareServer.getPersonLoggedIn(context);
        ExpenseDAO expensesDAO = ServiceRegistry.lookup(ExpenseDAO.class);

        String description = context.formParamAsClass("description", String.class)
                .check(Objects::nonNull, "Description is required")
                .get();

        JavalinValidation.register(LocalDate.class, v -> LocalDate.parse(v, DateTimeFormatter.ISO_LOCAL_DATE));
        LocalDate date = context.formParamAsClass("date", LocalDate.class)
                .check(Objects::nonNull, "Date is required")
                .get();

        int amount = context.formParamAsClass("amount", Integer.class)
                .check(Objects::nonNull, "Amount is required")
                .get();
        Expense expense = new Expense(personLoggedIn, description, Money.of(amount, "ZAR"), date);
        expensesDAO.save(expense);

        context.sessionAttribute(WeShareServer.SESSION_USER_KEY, personLoggedIn);
        context.redirect(Routes.EXPENSES);

    };
}
