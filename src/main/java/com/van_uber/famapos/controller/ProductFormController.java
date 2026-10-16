package com.van_uber.famapos.controller;

import com.van_uber.famapos.domain.CompuestoGramaje;
import com.van_uber.famapos.domain.CompuestoGramajeId;
import com.van_uber.famapos.domain.Product;
import com.van_uber.famapos.repository.ProductRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.stream.Collectors;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

@Component
@FxmlView("product_form.fxml")
public class ProductFormController {

    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private com.van_uber.famapos.repository.CompuestoGramajeRepository compuestoRepository;

    @FXML private TextField barcodeField;
    @FXML private TextField nameField;
    @FXML private TextField laboratoryField;
    @FXML private TextField stockField;
    @FXML private TextField thresholdField;
    @FXML private DatePicker expirationPicker;
    @FXML private TextField costField;
    @FXML private TextField priceField;
    @FXML private TextField compoundField;
    @FXML private TextField searchCompoundField;
    @FXML private ListView<CheckBox> compoundsList;

    private ObservableList<CheckBox> displayedCheckboxes = FXCollections.observableArrayList();
    private List<String> allDistinctCompounds = new ArrayList<>();
    private Set<String> selectedCompounds = new HashSet<>();
    private Product editingProduct;

    @FXML
    public void initialize() {
        compoundsList.setItems(displayedCheckboxes);
        searchCompoundField.textProperty().addListener((obs, oldV, newV) -> {
            refreshCompoundList(newV == null ? "" : newV);
        });
    }

    public void setProduct(Product product) {
        this.editingProduct = product;
        selectedCompounds.clear();
        searchCompoundField.clear();
        compoundField.clear();

        if (product != null && product.getCodigoBarras() != null) {
            barcodeField.setText(product.getCodigoBarras());
            barcodeField.setDisable(true); // Can't change PK
            nameField.setText(product.getNombre());
            laboratoryField.setText(product.getLaboratorio());
            stockField.setText(String.valueOf(product.getUnidadesDisponibles()));
            thresholdField.setText(String.valueOf(product.getUmbralUnidades()));
            expirationPicker.setValue(product.getFechaCaducidadProxima());
            costField.setText(product.getCosto().toString());
            priceField.setText(product.getPrecioPublico().toString());
            
            if (product.getCompuestos() != null) {
                product.getCompuestos().forEach(c -> selectedCompounds.add(c.getId().getCompuestoGramaje()));
            }
        } else {
            barcodeField.clear();
            barcodeField.setDisable(false);
            nameField.clear();
            laboratoryField.clear();
            stockField.clear();
            thresholdField.clear();
            expirationPicker.setValue(null);
            costField.clear();
            priceField.clear();
        }

        // Fetch from DB
        allDistinctCompounds = compuestoRepository.findAllDistinctCompuestos();
        if (allDistinctCompounds == null) {
            allDistinctCompounds = new ArrayList<>();
        }
        // Ensure currently selected are in allDistinctCompounds
        selectedCompounds.forEach(c -> {
            if (!allDistinctCompounds.contains(c)) allDistinctCompounds.add(c);
        });

        // Initialize checkboxes
        refreshCompoundList("");
    }

    private void refreshCompoundList(String filter) {
        displayedCheckboxes.clear();
        String lowerFilter = filter.toLowerCase();
        
        for (String comp : allDistinctCompounds) {
            if (comp.toLowerCase().contains(lowerFilter)) {
                CheckBox cb = new CheckBox(comp);
                cb.setSelected(selectedCompounds.contains(comp));
                cb.selectedProperty().addListener((obs, oldV, newV) -> {
                    if (newV) {
                        selectedCompounds.add(comp);
                    } else {
                        selectedCompounds.remove(comp);
                    }
                });
                displayedCheckboxes.add(cb);
            }
        }
    }

    @FXML
    public void onAddCompound(ActionEvent event) {
        String comp = compoundField.getText().trim();
        if (!comp.isEmpty() && !allDistinctCompounds.contains(comp)) {
            allDistinctCompounds.add(comp);
            selectedCompounds.add(comp);
            compoundField.clear();
            searchCompoundField.clear();
            refreshCompoundList("");
        } else if (!comp.isEmpty()) {
            selectedCompounds.add(comp);
            compoundField.clear();
            refreshCompoundList(searchCompoundField.getText());
        }
    }

    @FXML
    public void onSave(ActionEvent event) {
        try {
            Product p = (editingProduct != null && editingProduct.getCodigoBarras() != null) 
                    ? editingProduct : new Product();

            p.setCodigoBarras(barcodeField.getText().trim());
            p.setNombre(nameField.getText().trim());
            p.setLaboratorio(laboratoryField.getText().trim());
            p.setUnidadesDisponibles(Integer.parseInt(stockField.getText().trim()));
            p.setUmbralUnidades(Integer.parseInt(thresholdField.getText().trim()));
            p.setFechaCaducidadProxima(expirationPicker.getValue());
            p.setCosto(new BigDecimal(costField.getText().trim()));
            p.setPrecioPublico(new BigDecimal(priceField.getText().trim()));
            p.setActivo(true);

            // Update compounds
            p.getCompuestos().clear();
            for (String comp : selectedCompounds) {
                CompuestoGramajeId id = new CompuestoGramajeId(p.getCodigoBarras(), comp);
                CompuestoGramaje cg = new CompuestoGramaje(id, p);
                p.getCompuestos().add(cg);
            }

            productRepository.save(p);
            closeWindow();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error al guardar: Verifique los datos numéricos y campos obligatorios.");
            alert.show();
            e.printStackTrace();
        }
    }

    @FXML
    public void onCancel(ActionEvent event) {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) barcodeField.getScene().getWindow();
        stage.close();
    }
}
