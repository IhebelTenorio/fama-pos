package com.van_uber.famapos.controller;

import com.van_uber.famapos.service.HardwareFacade;
import com.van_uber.famapos.domain.Comprar;
import com.van_uber.famapos.domain.Product;
import com.van_uber.famapos.domain.Ticket;
import com.van_uber.famapos.repository.ComprarRepository;
import com.van_uber.famapos.repository.ProductRepository;
import com.van_uber.famapos.repository.TicketRepository;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@FxmlView("ticket_detail_modal.fxml")
public class TicketDetailModalController {

    private final TicketRepository ticketRepository;
    private final ComprarRepository comprarRepository;
    private final ProductRepository productRepository;

    @FXML
    private Label lblTicketId;
    @FXML
    private Label lblFecha;
    @FXML
    private Label lblHora;
    @FXML
    private VBox itemsContainer;
    @FXML
    private Label lblTotal;
    @FXML
    private Label lblRecibido;
    @FXML
    private Label lblCambio;

    private Ticket currentTicket;
    private List<Comprar> purchasedItems;
    private final HardwareFacade hardwareFacade;

    @Autowired
    public TicketDetailModalController(TicketRepository ticketRepository, ComprarRepository comprarRepository, ProductRepository productRepository, HardwareFacade hardwareFacade) {
        this.ticketRepository = ticketRepository;
        this.comprarRepository = comprarRepository;
        this.productRepository = productRepository;
        this.hardwareFacade = hardwareFacade;
    }

    public void setTicket(Ticket ticket) {
        this.currentTicket = ticket;
        this.purchasedItems = comprarRepository.findById_IdTicket(ticket.getIdTicket());
        
        lblTicketId.setText("Folio: #" + ticket.getIdTicket());
        lblFecha.setText("Fecha: " + ticket.getFecha().toString());
        lblHora.setText("Hora: " + ticket.getHora().toString());
        
        lblTotal.setText("$ " + ticket.getTotal().toString());
        lblRecibido.setText("$ " + ticket.getMontoRecibido().toString());
        lblCambio.setText("$ " + ticket.getCambio().toString());
        
        populateItems();
    }

    private void populateItems() {
        itemsContainer.getChildren().clear();
        for (Comprar compra : purchasedItems) {
            HBox row = new HBox(10);
            
            Label lblQty = new Label(compra.getCantidad() + "x");
            lblQty.setPrefWidth(30);
            
            Label lblName = new Label(compra.getProduct().getNombre());
            lblName.setWrapText(true);
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            
            Label lblPrice = new Label("$ " + compra.getPrecioVenta().toString());
            lblPrice.setStyle("-fx-font-weight: bold;");
            
            row.getChildren().addAll(lblQty, lblName, spacer, lblPrice);
            itemsContainer.getChildren().add(row);
        }
    }

    @FXML
    public void onPrint(ActionEvent event) {
        if (currentTicket == null) return;
        try {
            hardwareFacade.printReceipt(currentTicket, purchasedItems);
            showAlert(Alert.AlertType.INFORMATION, "Impresión en curso", "Ticket enviado correctamente a la impresora eQual.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Fallo al Imprimir", "Problema de comunicación con la impresora.\n\nDetalles:\n" + e.getMessage());
        }
    }

    @FXML
    public void onRefund(ActionEvent event) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "⚠️ ¿Está seguro que desea REEMBOLSAR este ticket?\n\n- Se devolverán los productos al inventario.\n- Se eliminará este ticket del historial.\n\nEsta acción no se puede deshacer.", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirmar Reembolso");
        Optional<ButtonType> result = confirm.showAndWait();
        
        if (result.isPresent() && result.get() == ButtonType.YES) {
            javafx.scene.control.Dialog<String> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle("Confirmar Reembolso");
            dialog.setHeaderText("Autorización Requerida para Reembolso");
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            javafx.scene.control.PasswordField pwdField = new javafx.scene.control.PasswordField();
            pwdField.setPromptText("Contraseña...");
            dialog.getDialogPane().setContent(pwdField);
            javafx.application.Platform.runLater(() -> pwdField.requestFocus());
            dialog.setResultConverter(b -> b == ButtonType.OK ? pwdField.getText() : null);
            Optional<String> pwd = dialog.showAndWait();
            
            if (pwd.isPresent() && pwd.get().equals("1234")) {
                // 1. Devolver existencias
            for (Comprar compra : purchasedItems) {
                Product p = compra.getProduct();
                p.setUnidadesDisponibles(p.getUnidadesDisponibles() + compra.getCantidad());
                productRepository.save(p);
            }
            
            // 2. Eliminar compras
            comprarRepository.deleteAll(purchasedItems);
            
            // 3. Eliminar ticket
            ticketRepository.delete(currentTicket);
            
            showAlert(Alert.AlertType.INFORMATION, "Reembolso Exitoso", "El ticket ha sido reembolsado y las existencias han sido restauradas.");
                closeModal(event);
            } else if (pwd.isPresent()) {
                showAlert(Alert.AlertType.ERROR, "Error de Permisos", "Contraseña incorrecta. Operación cancelada.");
            }
        }
    }

    @FXML
    public void onDelete(ActionEvent event) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "⚠️ ¿Está seguro que desea ELIMINAR este ticket del historial?\n\n- NO se devolverán los productos al inventario.\n- Se borrará cualquier rastro del ticket.\n\nEsta acción no se puede deshacer.", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Eliminar Ticket sin Reembolso");
        Optional<ButtonType> result = confirm.showAndWait();
        
        if (result.isPresent() && result.get() == ButtonType.YES) {
            javafx.scene.control.Dialog<String> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle("Confirmar Eliminación");
            dialog.setHeaderText("Autorización Requerida para Eliminar Ticket");
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            javafx.scene.control.PasswordField pwdField = new javafx.scene.control.PasswordField();
            pwdField.setPromptText("Contraseña...");
            dialog.getDialogPane().setContent(pwdField);
            javafx.application.Platform.runLater(() -> pwdField.requestFocus());
            dialog.setResultConverter(b -> b == ButtonType.OK ? pwdField.getText() : null);
            Optional<String> pwd = dialog.showAndWait();
            
            if (pwd.isPresent() && pwd.get().equals("1234")) {
                // 1. Eliminar compras
            comprarRepository.deleteAll(purchasedItems);
            
            // 2. Eliminar ticket
            ticketRepository.delete(currentTicket);
            
            showAlert(Alert.AlertType.INFORMATION, "Eliminado", "El ticket ha sido eliminado del historial.");
                closeModal(event);
            } else if (pwd.isPresent()) {
                showAlert(Alert.AlertType.ERROR, "Error de Permisos", "Contraseña incorrecta. Operación cancelada.");
            }
        }
    }

    @FXML
    public void onClose(ActionEvent event) {
        closeModal(event);
    }

    private void closeModal(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
    
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type, content);
        alert.setHeaderText(title);
        alert.showAndWait();
    }
}
