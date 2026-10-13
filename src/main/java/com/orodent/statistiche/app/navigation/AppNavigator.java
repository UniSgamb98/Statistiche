package com.orodent.statistiche.app.navigation;

public interface AppNavigator {

    void showHome();

    void showSalesImport();

    void showCustomerImport();
    void showCustomers();
    default void showCustomer(String code, int year) { showCustomers(); }
    void showProducts();
    void showDiscounts();
    void showReturns();
    void showSalesArchive();
}
