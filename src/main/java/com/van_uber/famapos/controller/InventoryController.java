package com.van_uber.famapos.controller;

import com.van_uber.famapos.domain.Product;
import com.van_uber.famapos.repository.ProductRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.io.PrintWriter;
import javafx.stage.FileChooser;
import java.io.File;

@Component
@FxmlView("inventory.fxml")
public class InventoryController {

    private final ProductRepository productRepository;
    private final FxWeaver fxWeaver;

    @FXML
    private TableView<Product> productTable;
    @FXML
    private TableColumn<Product, String> colBarcode;
    @FXML
    private TableColumn<Product, String> colName;
    @FXML
    private TableColumn<Product, String> colCategory;
    @FXML
    private TableColumn<Product, String> colLaboratory;
    @FXML
    private TableColumn<Product, String> colCompounds;
    @FXML
    private TableColumn<Product, Integer> colStock;
    @FXML
    private TableColumn<Product, Integer> colThreshold;
    @FXML
    private TableColumn<Product, java.time.LocalDate> colExpiration;
    @FXML
    private TableColumn<Product, BigDecimal> colCost;
    @FXML
    private TableColumn<Product, BigDecimal> colPrice;
    @FXML
    private TableColumn<Product, Boolean> colActive;

    @FXML
    private TextField searchField;
    
    @FXML private ToggleButton btnFilterExpiration;
    @FXML private ToggleButton btnFilterStock;

    private ObservableList<Product> productList = FXCollections.observableArrayList();

    @Autowired
    public InventoryController(ProductRepository productRepository, FxWeaver fxWeaver) {
        this.productRepository = productRepository;
        this.fxWeaver = fxWeaver;
    }

    @FXML
    public void initialize() {
        colBarcode.setCellValueFactory(new PropertyValueFactory<>("codigoBarras"));
        colName.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        
        if (colCategory != null) {
            colCategory.setCellValueFactory(new PropertyValueFactory<>("categoria"));
            colCategory.setCellFactory(col -> new TableCell<Product, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null || item.trim().isEmpty()) {
                        setText("-");
                        setStyle("-fx-text-fill: #888888; -fx-alignment: CENTER;");
                    } else {
                        setText(item);
                        setStyle("-fx-alignment: CENTER_LEFT;");
                    }
                }
            });
        }
        
        colLaboratory.setCellValueFactory(new PropertyValueFactory<>("laboratorio"));
        colCompounds.setCellValueFactory(new PropertyValueFactory<>("compuestosString"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("unidadesDisponibles"));
        colThreshold.setCellValueFactory(new PropertyValueFactory<>("umbralUnidades"));
        colExpiration.setCellValueFactory(new PropertyValueFactory<>("fechaCaducidadProxima"));
        colCost.setCellValueFactory(new PropertyValueFactory<>("costo"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("precioPublico"));
        colActive.setCellValueFactory(new PropertyValueFactory<>("activo"));
        colActive.setCellFactory(column -> new TableCell<Product, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item ? "Activo" : "Inactivo");
                    setStyle(item ? "-fx-text-fill: #2ecc71; -fx-font-weight: bold;" : "-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                }
            }
        });
        
        productTable.setPlaceholder(new Label("No hay productos registrados."));

        loadProducts();

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });
    }

    private void applyFilters() {
        boolean filterExp = btnFilterExpiration != null && btnFilterExpiration.isSelected();
        boolean filterStock = btnFilterStock != null && btnFilterStock.isSelected();
        String query = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();

        java.time.LocalDate threeWeeksFromNow = java.time.LocalDate.now().plusWeeks(3);
        ObservableList<Product> filtered = FXCollections.observableArrayList();

        for (Product p : productList) {
            boolean matchesSearch = query.isEmpty() 
                    || (p.getNombre() != null && p.getNombre().toLowerCase().contains(query))
                    || (p.getCodigoBarras() != null && p.getCodigoBarras().toLowerCase().contains(query))
                    || (p.getCategoria() != null && p.getCategoria().toLowerCase().contains(query))
                    || (p.getLaboratorio() != null && p.getLaboratorio().toLowerCase().contains(query));
                    
            boolean matchesExp = !filterExp || (p.getFechaCaducidadProxima() != null && !p.getFechaCaducidadProxima().isAfter(threeWeeksFromNow));
            boolean matchesStock = !filterStock || (p.getUnidadesDisponibles() <= p.getUmbralUnidades());

            if (matchesSearch && matchesExp && matchesStock) {
                filtered.add(p);
            }
        }
        productTable.setItems(filtered);
    }

    private void loadProducts() {
        List<Product> products = productRepository.findAll();
        productList.setAll(products);
        productTable.setItems(productList);
    }

    @FXML
    public void onExportCsv(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar Inventario a CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo CSV", "*.csv"));
        fileChooser.setInitialFileName("inventario_" + java.time.LocalDate.now().toString() + ".csv");
        File file = fileChooser.showSaveDialog(((Node) event.getSource()).getScene().getWindow());
        
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file, "UTF-8")) {
                writer.println("CodigoBarras,Nombre,Categoria,Laboratorio,Compuestos,Stock,Umbral,Caducidad,Costo,PrecioPublico,Activo");
                for (Product p : productTable.getItems()) { // Emulate what is currently visible/filtered!
                    writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%d,%d,%s,%s,%s,%s\n",
                        p.getCodigoBarras(), 
                        p.getNombre() != null ? p.getNombre().replace("\"", "\"\"") : "", 
                        p.getCategoria() != null ? p.getCategoria().replace("\"", "\"\"") : "",
                        p.getLaboratorio() != null ? p.getLaboratorio().replace("\"", "\"\"") : "", 
                        p.getCompuestosString() != null ? p.getCompuestosString().replace("\"", "\"\"") : "",
                        p.getUnidadesDisponibles(), 
                        p.getUmbralUnidades(), 
                        p.getFechaCaducidadProxima() != null ? p.getFechaCaducidadProxima().toString() : "",
                        p.getCosto(), 
                        p.getPrecioPublico(), 
                        p.getActivo() ? "Activo" : "Inactivo");
                }
                showAlert(Alert.AlertType.INFORMATION, "Exportación Exitosa", "El inventario ha sido exportado correctamente.");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error de Exportación", "Hubo un error al guardar el archivo: " + e.getMessage());
            }
        }
    }

    @FXML
    public void onNewProduct(ActionEvent event) {
        openProductForm(null);
    }

    @FXML
    public void onEditProduct(ActionEvent event) {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openProductForm(selected);
        } else {
            showAlert(Alert.AlertType.WARNING, "Selección", "Por favor seleccione un producto para editar.");
        }
    }

    private void openProductForm(Product product) {
        net.rgielen.fxweaver.core.FxControllerAndView<ProductFormController, javafx.scene.Node> cav = 
                fxWeaver.load(ProductFormController.class);
        
        cav.getController().setProduct(product);
        
        Stage stage = new Stage();
        stage.setTitle(product == null ? "Nuevo Producto" : "Editar Producto");
        stage.setScene(new Scene((javafx.scene.Parent) cav.getView().get()));
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        stage.showAndWait();
        
        loadProducts();
    }

    @FXML
    public void onExpirationAlerts(ActionEvent event) {
        applyFilters();
    }

    @FXML
    public void onLowStockAlerts(ActionEvent event) {
        applyFilters();
    }

    @FXML
    public void onSummary(ActionEvent event) {
        Parent root = fxWeaver.loadView(SummaryController.class);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    public void onToggleStatus(ActionEvent event) {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            boolean isCurrentlyActive = selected.getActivo();
            String accion = isCurrentlyActive ? "dar de baja" : "REACTIVAR";
            
            javafx.scene.control.Dialog<String> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle(isCurrentlyActive ? "Confirmar Baja" : "Confirmar Reactivación");
            dialog.setHeaderText("¿Desea " + accion + " el producto: " + selected.getNombre() + "?\nIngrese autorización maestra:");
            dialog.getDialogPane().getButtonTypes().addAll(javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);
            
            javafx.scene.control.PasswordField pwd = new javafx.scene.control.PasswordField();
            pwd.setPromptText("Contraseña...");
            dialog.getDialogPane().setContent(pwd);
            
            javafx.application.Platform.runLater(() -> pwd.requestFocus());

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == javafx.scene.control.ButtonType.OK) {
                    return pwd.getText();
                }
                return null;
            });
            
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent() && result.get().equals("1234")) { 
                selected.setActivo(!isCurrentlyActive);
                productRepository.save(selected);
                loadProducts();
                if (isCurrentlyActive) {
                    showAlert(Alert.AlertType.INFORMATION, "Baja Exitosa", "El producto fue desactivado. Ya no se podrá vender en caja, pero se conserva en historiales.");
                } else {
                    showAlert(Alert.AlertType.INFORMATION, "Reactivación Exitosa", "El producto nuevamente está activo y es elegible para ventas.");
                }
            } else if (result.isPresent()) {
                showAlert(Alert.AlertType.ERROR, "Error de Permisos", "Contraseña incorrecta. Operación cancelada.");
            }
        } else {
            showAlert(Alert.AlertType.WARNING, "Selección", "Por favor seleccione un producto de la tabla.");
        }
    }

    @FXML
    public void onBackToSales(ActionEvent event) {
        Parent root = fxWeaver.loadView(SalesController.class);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    public void onDeleteProduct(ActionEvent event) {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            javafx.scene.control.Dialog<String> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle("Confirmar Eliminación Definitiva");
            dialog.setHeaderText("⚠️ PELIGRO: ESTA ACCIÓN NO SE PUEDE DESHACER ⚠️\n\n¿Eliminar PERMANENTEMENTE: " + selected.getNombre() + "?\n\nSi este producto tiene un historial guardado de ventas,\nel sistema bloqueará el borrado por seguridad.\nIngrese contraseña:");
            dialog.getDialogPane().getButtonTypes().addAll(javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);
            
            javafx.scene.control.PasswordField pwd = new javafx.scene.control.PasswordField();
            pwd.setPromptText("Contraseña...");
            dialog.getDialogPane().setContent(pwd);
            javafx.application.Platform.runLater(() -> pwd.requestFocus());
            dialog.setResultConverter(d -> d == javafx.scene.control.ButtonType.OK ? pwd.getText() : null);
            
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent() && result.get().equals("1234")) {
                try {
                    productRepository.delete(selected);
                    productRepository.flush(); // Force the delete immediately to trigger exceptions if constraint fails
                    loadProducts();
                    showAlert(Alert.AlertType.INFORMATION, "Borrado Físico Exitoso", "El producto fue eliminado definitivamente del disco duro.");
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Base de Datos Protegida", "❌ ELIMINACIÓN DENEGADA ❌\n\nEl producto ya pertenece a tickets del historial.\nBorrarlo corrompería las ganancias de los meses anteriores.\n\nPor favor utilice 'Cambiar Estado' para dar de Baja el producto.");
                }
            } else if (result.isPresent()) {
                showAlert(Alert.AlertType.ERROR, "Error de Permisos", "Contraseña incorrecta.");
            }
        } else {
            showAlert(Alert.AlertType.WARNING, "Selección", "Primero seleccione un producto de la tabla.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type, content);
        alert.setHeaderText(title);
        alert.show();
    }
}
