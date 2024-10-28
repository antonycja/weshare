package weshare.controller;

import io.javalin.http.Handler;
import org.javamoney.moneta.function.MonetaryFunctions;
import org.jetbrains.annotations.NotNull;
import weshare.model.Expense;
import weshare.model.PaymentRequest;
import weshare.model.Person;
import weshare.persistence.ExpenseDAO;
import weshare.server.ServiceRegistry;
import weshare.server.WeShareServer;

import javax.money.MonetaryAmount;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;
import static weshare.model.MoneyHelper.ZERO_RANDS;

public class PaymentRequestsSentController {

    public static final Handler view = context -> {
        ExpenseDAO expensesDAO = ServiceRegistry.lookup(ExpenseDAO.class);
        Person personLoggedIn = WeShareServer.getPersonLoggedIn(context);

        Collection<PaymentRequest> payments = expensesDAO.findPaymentRequestsSent(personLoggedIn);

        MonetaryAmount grandAmount = ZERO_RANDS;
        for (PaymentRequest payment : payments) {
            grandAmount = grandAmount.add(payment.getAmountToPay());
        }
        Map<String, Object> viewModel = new HashMap<>();
        viewModel.put("payments", payments);
        viewModel.put("grandTotal", grandAmount);

        context.render("paymentrequests_sent.html", viewModel);
    };
}
