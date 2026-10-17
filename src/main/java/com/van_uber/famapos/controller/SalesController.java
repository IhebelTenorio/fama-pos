package com.van_uber.famapos.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.cell.PropertyValueFactory;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import com.van_uber.famapos.domain.Product;
import org.springframework.beans.factory.annotation.Autowired;
import com.van_uber.famapos.repository.ProductRepository;
import com.van_uber.famapos.repository.TicketRepository;
import com.van_uber.famapos.repository.ComprarRepository;
import com.van_uber.famapos.service.HardwareFacade;
import com.van_uber.famapos.domain.Ticket;
import com.van_uber.famapos.domain.Comprar;
import com.van_uber.famapos.domain.ComprarId;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;
import java.time.format.DateTimeFormatter;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.van_uber.famapos.domain.CompuestoGramaje;

@Component
@FxmlView("sales.fxml")
public class SalesController {

    @Autowired
    private net.rgielen.fxweaver.core.FxWeaver fxWeaver;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private ComprarRepository comprarRepository;
    @Autowired
    private HardwareFacade hardwareFacade;

    @FXML
    private TextField searchField;

    @FXML
    private Label clockLabel;

    @FXML
    private TableView<CartItem> cartTable;
    @FXML
    private TableColumn<CartItem, String> colCodigo;
    @FXML
    private TableColumn<CartItem, String> colProducto;
    @FXML
    private TableColumn<CartItem, String> colCompuestos;
    @FXML
    private TableColumn<CartItem, String> colLaboratorio;
    @FXML
    private TableColumn<CartItem, String> colCategoria;
    @FXML
    private TableColumn<CartItem, Integer> colCantidad;
    @FXML
    private TableColumn<CartItem, String> colPrecio;

    @FXML
    private TableView<CompuestoGramaje> compuestosTable;
    @FXML
    private TableColumn<CompuestoGramaje, String> colCompuesto;

    @FXML
    private Label selectedProductLabel;
    @FXML
    private Label selectedQuantityLabel;
    @FXML
    private Label totalLabel;

    private CartItem currentSelectedItem;
    private BigDecimal currentTotal = BigDecimal.ZERO;

    @FXML
    public void initialize() {
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colProducto.setCellValueFactory(new PropertyValueFactory<>("nombreProducto"));
        colCompuestos.setCellValueFactory(new PropertyValueFactory<>("compuestos"));
        colLaboratorio.setCellValueFactory(new PropertyValueFactory<>("laboratorio"));
        if (colCategoria != null) {
            colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        }
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));

        if (colCompuesto != null) {
            colCompuesto.setCellValueFactory(new PropertyValueFactory<>("compuestoNombre"));
            compuestosTable.setPlaceholder(new Label("No hay compuestos"));
        }
        cartTable.setPlaceholder(new Label("El carrito está vacío. Escanee un producto."));

        cartTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            currentSelectedItem = newSelection;
            updateSelectionUI();
        });

        // The barcode scanner will hit ENTER, triggering onAction on the textfield
        searchField.setOnAction(event -> handleScan());
        initClock();
    }

    private void updateSelectionUI() {
        if (currentSelectedItem != null) {
            selectedProductLabel.setText(currentSelectedItem.getNombreProducto());
            selectedQuantityLabel.setText(String.valueOf(currentSelectedItem.getCantidad()));
            if (compuestosTable != null) {
                compuestosTable.getItems().clear();
                if (currentSelectedItem.getProduct().getCompuestos() != null) {
                    compuestosTable.getItems().addAll(currentSelectedItem.getProduct().getCompuestos());
                }
            }
        } else {
            selectedProductLabel.setText("");
            selectedQuantityLabel.setText("0");
            if (compuestosTable != null) {
                compuestosTable.getItems().clear();
            }
        }
    }

    private void updateTotal() {
        currentTotal = cartTable.getItems().stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalLabel.setText("$ " + currentTotal.toString());
    }

    private void initClock() {
        if (clockLabel == null) return;
        
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            LocalDate date = LocalDate.now();
            LocalTime time = LocalTime.now();
            
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            
            clockLabel.setText(date.format(dateFormatter) + " - " + time.format(timeFormatter));
        }), new KeyFrame(Duration.seconds(1)));
        
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    @FXML
    public void onSearch(ActionEvent event) {
        handleScan();
    }

    private void handleScan() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) return;

        Optional<Product> pOpt = productRepository.findById(query);
        if (pOpt.isPresent()) {
            Product p = pOpt.get();
            if (!p.getActivo()) {
                showAlert(Alert.AlertType.WARNING, "Producto Descontinuado", "El producto se encuentra inactivo/dado de baja y no puede venderse.");
                return;
            }
            if (p.getUnidadesDisponibles() <= 0) {
                showAlert(Alert.AlertType.WARNING, "Sin stock", "El producto no tiene stock disponible.");
                return;
            }

            // Check if already in cart
            Optional<CartItem> existing = cartTable.getItems().stream()
                    .filter(item -> item.getProduct().getCodigoBarras().equals(p.getCodigoBarras()))
                    .findFirst();

            if (existing.isPresent()) {
                boolean success = existing.get().increment();
                if (!success) {
                    showAlert(Alert.AlertType.WARNING, "Tope de stock", "No se puede agregar más: el stock disponible es de " + existing.get().getProduct().getUnidadesDisponibles() + " unidad(es).");
                }
            } else {
                cartTable.getItems().add(new CartItem(p, 1));
            }

            cartTable.refresh();
            updateTotal();
            searchField.clear();
        } else {
            showAlert(Alert.AlertType.ERROR, "No Encontrado", "No se encontró el producto con código: " + query);
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type, content);
        alert.setHeaderText(title);
        alert.show();
    }

    @FXML
    public void onPlus(ActionEvent event) {
        if (currentSelectedItem != null) {
            boolean success = currentSelectedItem.increment();
            if (!success) {
                showAlert(Alert.AlertType.WARNING, "Tope de stock", "No se puede agregar más cantidad de este producto.");
            }
            cartTable.refresh();
            updateSelectionUI();
            updateTotal();
        }
    }

    @FXML
    public void onMinus(ActionEvent event) {
        if (currentSelectedItem != null) {
            currentSelectedItem.decrement();
            if (currentSelectedItem.getCantidad() == 0) {
                cartTable.getItems().remove(currentSelectedItem);
                cartTable.getSelectionModel().clearSelection();
            }
            cartTable.refresh();
            updateSelectionUI();
            updateTotal();
        }
    }

    @FXML
    public void onSession(ActionEvent event) {
        net.rgielen.fxweaver.core.FxControllerAndView<SessionSettingsModalController, javafx.scene.Node> cav = 
                fxWeaver.load(SessionSettingsModalController.class);
        
        Stage stage = new Stage();
        stage.setTitle("Sesión y Configuración");
        stage.setScene(new Scene((javafx.scene.Parent) cav.getView().get()));
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        stage.showAndWait();
    }

    @FXML
    public void onHistory(ActionEvent event) {
        javafx.scene.Parent root = fxWeaver.loadView(HistoryController.class);
        javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    public void onWarehouse(ActionEvent event) {
        javafx.scene.Parent root = fxWeaver.loadView(InventoryController.class);
        javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    public void onCash(ActionEvent event) {
        processPayment(false);
    }

    @FXML
    public void onCard(ActionEvent event) {
        processPayment(true);
    }

    private void processPayment(boolean isCard) {
        if (cartTable.getItems().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Carrito Vacío", "No hay productos para cobrar.");
            return;
        }

        net.rgielen.fxweaver.core.FxControllerAndView<CheckoutModalController, javafx.scene.Node> cav = 
                fxWeaver.load(CheckoutModalController.class);
        
        cav.getController().setTotal(currentTotal);
        cav.getController().setPaymentType(isCard);
        
        Stage stage = new Stage();
        stage.setTitle("Cobrar");
        stage.setScene(new Scene((javafx.scene.Parent) cav.getView().get()));
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        stage.showAndWait();

        if (cav.getController().isConfirmed()) {
            BigDecimal received = cav.getController().getReceived();
            BigDecimal change = cav.getController().getChange();
            finalizeSale(received, change);
        }
    }

    private void finalizeSale(BigDecimal received, BigDecimal change) {
        // 1. Guardar Ticket
        Ticket ticket = new Ticket();
        ticket.setFecha(LocalDate.now());
        ticket.setHora(LocalTime.now());
        ticket.setTotal(currentTotal);
        ticket.setMontoRecibido(received);
        ticket.setCambio(change);
        ticket = ticketRepository.save(ticket);

        // 2. Guardar Detalle (COMPRAR) y Restar Stock
        for (CartItem item : cartTable.getItems()) {
            Product p = item.getProduct();
            
            // Restar stock
            p.setUnidadesDisponibles(p.getUnidadesDisponibles() - item.getCantidad());
            productRepository.save(p);

            // Crear compra
            ComprarId id = new ComprarId(p.getCodigoBarras(), ticket.getIdTicket());
            Comprar compra = new Comprar();
            compra.setId(id);
            compra.setProduct(p);
            compra.setTicket(ticket);
            compra.setCantidad(item.getCantidad());
            compra.setPrecioVenta(item.getSubtotal());
            comprarRepository.save(compra);
        }

        // 3. Abrir cajón de dinero
        hardwareFacade.openCashDrawer();

        // 4. Limpiar UI
        cartTable.getItems().clear();
        currentSelectedItem = null;
        updateSelectionUI();
        updateTotal();

        // 5. Ofrecer imprimir ticket al usuario (para ahorrar papel/tinta)
        Alert confirmPrint = new Alert(Alert.AlertType.CONFIRMATION);
        confirmPrint.setTitle("Venta Exitosa");
        confirmPrint.setHeaderText("¡Venta registrada con éxito! (Folio #" + ticket.getIdTicket() + ")");
        confirmPrint.setContentText("¿Desea imprimir el ticket de compra?");

        ButtonType btnImprimir = new ButtonType("Imprimir Ticket");
        ButtonType btnNoImprimir = new ButtonType("No Imprimir", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmPrint.getButtonTypes().setAll(btnImprimir, btnNoImprimir);

        Optional<ButtonType> printChoice = confirmPrint.showAndWait();
        if (printChoice.isPresent() && printChoice.get() == btnImprimir) {
            try {
                List<Comprar> purchasedItems = comprarRepository.findById_IdTicket(ticket.getIdTicket());
                hardwareFacade.printReceipt(ticket, purchasedItems);
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Fallo al Imprimir", "No se pudo comunicar con la impresora: " + e.getMessage());
            }
        }
    }
}
