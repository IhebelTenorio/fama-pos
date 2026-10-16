package com.van_uber.famapos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ComprarId implements Serializable {

    @Column(name = "CodigoBarras")
    private String codigoBarras;

    @Column(name = "IDTicket")
    private Integer idTicket;

    public ComprarId() {}

    public ComprarId(String codigoBarras, Integer idTicket) {
        this.codigoBarras = codigoBarras;
        this.idTicket = idTicket;
    }

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public void setCodigoBarras(String codigoBarras) {
        this.codigoBarras = codigoBarras;
    }

    public Integer getIdTicket() {
        return idTicket;
    }

    public void setIdTicket(Integer idTicket) {
        this.idTicket = idTicket;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComprarId comprarId = (ComprarId) o;
        return Objects.equals(codigoBarras, comprarId.codigoBarras) &&
               Objects.equals(idTicket, comprarId.idTicket);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigoBarras, idTicket);
    }
}
