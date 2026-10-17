package com.van_uber.famapos.domain;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

public class PivotRow {

    private String dimensionKey;
    private Map<String, BigDecimal> periodValues = new HashMap<>();
    private BigDecimal rowTotal = BigDecimal.ZERO;
    private boolean isCurrency = false;

    public PivotRow(String dimensionKey, boolean isCurrency) {
        this.dimensionKey = dimensionKey;
        this.isCurrency = isCurrency;
    }

    public String getDimensionKey() {
        return dimensionKey;
    }

    public void setDimensionKey(String dimensionKey) {
        this.dimensionKey = dimensionKey;
    }

    public Map<String, BigDecimal> getPeriodValues() {
        return periodValues;
    }

    public void addValue(String periodKey, BigDecimal amount) {
        if (amount == null) return;
        BigDecimal current = periodValues.getOrDefault(periodKey, BigDecimal.ZERO);
        BigDecimal updated = current.add(amount);
        periodValues.put(periodKey, updated);
        this.rowTotal = this.rowTotal.add(amount);
    }

    public BigDecimal getRawValue(String periodKey) {
        return periodValues.getOrDefault(periodKey, BigDecimal.ZERO);
    }

    public BigDecimal getRowTotal() {
        return rowTotal;
    }

    public String getFormattedValue(String periodKey) {
        BigDecimal val = periodValues.getOrDefault(periodKey, BigDecimal.ZERO);
        if (isCurrency) {
            return String.format("$ %.2f", val);
        } else {
            return String.valueOf(val.setScale(0, RoundingMode.HALF_UP));
        }
    }

    public String getFormattedTotal() {
        if (isCurrency) {
            return String.format("$ %.2f", rowTotal);
        } else {
            return String.valueOf(rowTotal.setScale(0, RoundingMode.HALF_UP));
        }
    }

    public ObservableValue<String> getObservableValue(String periodKey) {
        return new SimpleStringProperty(getFormattedValue(periodKey));
    }

    public ObservableValue<String> getObservableTotal() {
        return new SimpleStringProperty(getFormattedTotal());
    }
}
