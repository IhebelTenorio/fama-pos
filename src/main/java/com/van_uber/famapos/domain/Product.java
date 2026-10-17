package com.van_uber.famapos.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "PRODUCTO")
public class Product {

    @Id
    @Column(name = "CodigoBarras", length = 50)
    private String codigoBarras;

    @Column(name = "Nombre", length = 150, nullable = false)
    private String nombre;

    @Column(name = "Categoria", length = 100)
    private String categoria;

    @Column(name = "UnidadesDisponibles", nullable = false)
    private Integer unidadesDisponibles;

    @Column(name = "UmbralUnidades", nullable = false)
    private Integer umbralUnidades;

    @Column(name = "FechaCaducidadProxima")
    private LocalDate fechaCaducidadProxima;

    @Column(name = "Costo", nullable = false, precision = 10, scale = 2)
    private BigDecimal costo;

    @Column(name = "PrecioPublico", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioPublico;

    @Column(name = "Laboratorio", length = 150)
    private String laboratorio;

    @Column(name = "Activo", nullable = false)
    private Boolean activo = true;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private java.util.List<CompuestoGramaje> compuestos = new java.util.ArrayList<>();

    public Product() {
    }

    public String getCompuestosString() {
        if (compuestos == null || compuestos.isEmpty()) {
            return "";
        }
        return compuestos.stream()
                .map(c -> c.getId().getCompuestoGramaje())
                .collect(java.util.stream.Collectors.joining(" - "));
    }

    public java.util.List<CompuestoGramaje> getCompuestos() {
        return compuestos;
    }

    public void setCompuestos(java.util.List<CompuestoGramaje> compuestos) {
        this.compuestos = compuestos;
    }

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public void setCodigoBarras(String codigoBarras) {
        this.codigoBarras = codigoBarras;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public Integer getUnidadesDisponibles() {
        return unidadesDisponibles;
    }

    public void setUnidadesDisponibles(Integer unidadesDisponibles) {
        this.unidadesDisponibles = unidadesDisponibles;
    }

    public Integer getUmbralUnidades() {
        return umbralUnidades;
    }

    public void setUmbralUnidades(Integer umbralUnidades) {
        this.umbralUnidades = umbralUnidades;
    }

    public LocalDate getFechaCaducidadProxima() {
        return fechaCaducidadProxima;
    }

    public void setFechaCaducidadProxima(LocalDate fechaCaducidadProxima) {
        this.fechaCaducidadProxima = fechaCaducidadProxima;
    }

    public BigDecimal getCosto() {
        return costo;
    }

    public void setCosto(BigDecimal costo) {
        this.costo = costo;
    }

    public BigDecimal getPrecioPublico() {
        return precioPublico;
    }

    public void setPrecioPublico(BigDecimal precioPublico) {
        this.precioPublico = precioPublico;
    }

    public String getLaboratorio() {
        return laboratorio;
    }

    public void setLaboratorio(String laboratorio) {
        this.laboratorio = laboratorio;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
