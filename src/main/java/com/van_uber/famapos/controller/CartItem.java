package com.van_uber.famapos.controller;

import com.van_uber.famapos.domain.Product;
import java.math.BigDecimal;

public class CartItem {
    private Product product;
    private int quantity;
    private BigDecimal subtotal;

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
        updateSubtotal();
    }

    public boolean increment() {
        if (quantity < product.getUnidadesDisponibles()) {
            quantity++;
            updateSubtotal();
            return true;
        }
        return false;
    }

    public void decrement() {
        if (quantity > 0) {
            quantity--;
            updateSubtotal();
        }
    }

    public void setQuantity(int newQuantity) {
        if (newQuantity <= product.getUnidadesDisponibles() && newQuantity >= 0) {
            this.quantity = newQuantity;
            updateSubtotal();
        }
    }

    private void updateSubtotal() {
        if (product != null && product.getPrecioPublico() != null) {
            this.subtotal = product.getPrecioPublico().multiply(BigDecimal.valueOf(quantity));
        } else {
            this.subtotal = BigDecimal.ZERO;
        }
    }

    public Product getProduct() {
        return product;
    }

    // Métodos para la TableView (PropertyValueFactory)
    public String getCodigo() {
        return product != null ? product.getCodigoBarras() : "";
    }

    public String getNombreProducto() {
        return product != null ? product.getNombre() : "";
    }

    public int getCantidad() {
        return quantity;
    }

    public String getCompuestos() {
        return product != null ? product.getCompuestosString() : "";
    }

    public String getLaboratorio() {
        return product != null && product.getLaboratorio() != null ? product.getLaboratorio() : "";
    }

    public String getCategoria() {
        return product != null && product.getCategoria() != null && !product.getCategoria().trim().isEmpty() 
                ? product.getCategoria() : "-";
    }

    public String getPrecio() {
        return "$ " + subtotal.toString();
    }
    
    public BigDecimal getSubtotal() {
        return subtotal;
    }
}
