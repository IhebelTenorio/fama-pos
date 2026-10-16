package com.van_uber.famapos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CompuestoGramajeId implements Serializable {

    @Column(name = "CodigoBarras")
    private String codigoBarras;

    @Column(name = "CompuestoGramaje")
    private String compuestoGramaje;

    public CompuestoGramajeId() {}

    public CompuestoGramajeId(String codigoBarras, String compuestoGramaje) {
        this.codigoBarras = codigoBarras;
        this.compuestoGramaje = compuestoGramaje;
    }

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public void setCodigoBarras(String codigoBarras) {
        this.codigoBarras = codigoBarras;
    }

    public String getCompuestoGramaje() {
        return compuestoGramaje;
    }

    public void setCompuestoGramaje(String compuestoGramaje) {
        this.compuestoGramaje = compuestoGramaje;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompuestoGramajeId that = (CompuestoGramajeId) o;
        return Objects.equals(codigoBarras, that.codigoBarras) &&
               Objects.equals(compuestoGramaje, that.compuestoGramaje);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigoBarras, compuestoGramaje);
    }
}
