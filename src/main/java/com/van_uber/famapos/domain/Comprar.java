package com.van_uber.famapos.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "COMPRAR")
public class Comprar {

    @EmbeddedId
    private ComprarId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("codigoBarras")
    @JoinColumn(name = "CodigoBarras")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idTicket")
    @JoinColumn(name = "IDTicket")
    private Ticket ticket;

    @Column(name = "Cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "PrecioVenta", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioVenta;

    public Comprar() {}

    public ComprarId getId() {
        return id;
    }

    public void setId(ComprarId id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(BigDecimal precioVenta) {
        this.precioVenta = precioVenta;
    }
}
