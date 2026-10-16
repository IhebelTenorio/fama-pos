package com.van_uber.famapos.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "COMPUESTOGRAMAJE")
public class CompuestoGramaje {

    @EmbeddedId
    private CompuestoGramajeId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("codigoBarras")
    @JoinColumn(name = "CodigoBarras")
    private Product product;

    public CompuestoGramaje() {}

    public CompuestoGramaje(CompuestoGramajeId id, Product product) {
        this.id = id;
        this.product = product;
    }

    public CompuestoGramajeId getId() {
        return id;
    }

    public void setId(CompuestoGramajeId id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getCompuestoNombre() {
        return id != null ? id.getCompuestoGramaje() : "";
    }
}
