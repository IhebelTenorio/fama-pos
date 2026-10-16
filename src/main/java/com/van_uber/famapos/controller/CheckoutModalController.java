package com.van_uber.famapos.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@FxmlView("checkout_modal.fxml")
public class CheckoutModalController {

    @FXML private Label totalLabel;
    @FXML private TextField receivedField;
    @FXML private Label changeLabel;

    private BigDecimal total = BigDecimal.ZERO;
    private BigDecimal change = BigDecimal.ZERO;
    private BigDecimal received = BigDecimal.ZERO;
    private boolean confirmed = false;
    private boolean isCard = false;

    public void setTotal(BigDecimal total) {
        this.total = total;
        totalLabel.setText("$ " + total.setScale(2, RoundingMode.HALF_UP).toString());
        
        // Listen to changes in received amount
        receivedField.textProperty().addListener((observable, oldValue, newValue) -> {
            calculateChange(newValue);
        });
        
        // Focus textfield automatically y capturar enter
        Platform.runLater(() -> receivedField.requestFocus());
        receivedField.setOnAction(event -> onConfirm(null));
    }
    
    public void setPaymentType(boolean isCard) {
        this.isCard = isCard;
        if (isCard) {
            receivedField.setText(total.toString());
            receivedField.setDisable(true);
        } else {
            receivedField.setDisable(false);
            receivedField.clear();
        }
    }

    private void calculateChange(String input) {
        try {
            if (input == null || input.trim().isEmpty()) {
                received = BigDecimal.ZERO;
            } else {
                received = new BigDecimal(input.trim());
            }
            
            change = received.subtract(total);
            if (change.compareTo(BigDecimal.ZERO) < 0) {
                changeLabel.setText("Monto insuficiente");
                changeLabel.setStyle("-fx-text-fill: red;");
            } else {
                changeLabel.setText("$ " + change.setScale(2, RoundingMode.HALF_UP).toString());
                changeLabel.setStyle("-fx-text-fill: #2ecc71;"); // Green
            }
        } catch (NumberFormatException e) {
            changeLabel.setText("Monto inválido");
            changeLabel.setStyle("-fx-text-fill: red;");
            received = BigDecimal.ZERO;
            change = BigDecimal.ZERO;
        }
    }

    @FXML
    public void onConfirm(ActionEvent event) {
        if (change.compareTo(BigDecimal.ZERO) >= 0 && received.compareTo(total) >= 0) {
            confirmed = true;
            closeWindow();
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING, "El monto recibido es menor al total a cobrar.");
            alert.setHeaderText("Monto Insuficiente");
            alert.show();
        }
    }

    @FXML
    public void onCancel(ActionEvent event) {
        confirmed = false;
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) receivedField.getScene().getWindow();
        stage.close();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public BigDecimal getReceived() {
        return received;
    }

    public BigDecimal getChange() {
        return change;
    }
}
