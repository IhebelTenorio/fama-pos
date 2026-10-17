package com.van_uber.famapos.controller;

import com.van_uber.famapos.domain.PivotRow;
import com.van_uber.famapos.repository.ProductRepository;
import com.van_uber.famapos.service.SummaryService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.PrintWriter;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Component
@FxmlView("summary.fxml")
public class SummaryController {

    private final SummaryService summaryService;
    private final ProductRepository productRepository;
    private final FxWeaver fxWeaver;

    @FXML private Label lblKpiHoy;
    @FXML private Label lblKpiHoyTickets;
    @FXML private Label lblKpiSemana;
    @FXML private Label lblKpiMesAnt;
    @FXML private Label lblKpiGanancia;

    @FXML private ComboBox<String> cmbPeriodo;
    @FXML private DatePicker dpStart;
    @FXML private DatePicker dpEnd;
    @FXML private ComboBox<String> cmbAgrupacion;
    @FXML private ComboBox<String> cmbMetrica;
    @FXML private ComboBox<String> cmbFiltroLab;
    @FXML private ComboBox<String> cmbFiltroCat;
    @FXML private TextField txtSearch;

    @FXML private TableView<PivotRow> pivotTable;
    @FXML private Label lblStatus;

    private SummaryService.PivotResult lastResult;
    private boolean updatingControls = false;

    @Autowired
    public SummaryController(SummaryService summaryService, ProductRepository productRepository, FxWeaver fxWeaver) {
        this.summaryService = summaryService;
        this.productRepository = productRepository;
        this.fxWeaver = fxWeaver;
    }

    @FXML
    public void initialize() {
        initControls();
        initListeners();
        loadKpis();
        refreshPivot();
    }

    private void initControls() {
        updatingControls = true;

        cmbPeriodo.setItems(FXCollections.observableArrayList(
                "Esta Semana", "Este Mes", "Mes Anterior", "Año Actual", "Personalizado"
        ));
        cmbPeriodo.setValue("Esta Semana");

        cmbAgrupacion.setItems(FXCollections.observableArrayList(
                "Por Producto", "Por Laboratorio", "Por Categoría"
        ));
        cmbAgrupacion.setValue("Por Producto");

        cmbMetrica.setItems(FXCollections.observableArrayList(
                "Piezas Vendidas", "Ingresos ($)", "Ganancia Estimada ($)"
        ));
        cmbMetrica.setValue("Ingresos ($)");

        // Cargar Laboratorios
        ObservableList<String> labs = FXCollections.observableArrayList("Todos los laboratorios");
        List<String> distinctLabs = productRepository.findAllDistinctLaboratories();
        if (distinctLabs != null) labs.addAll(distinctLabs);
        cmbFiltroLab.setItems(labs);
        cmbFiltroLab.setValue("Todos los laboratorios");

        // Cargar Categorías
        ObservableList<String> cats = FXCollections.observableArrayList("Todas las categorías");
        List<String> distinctCats = productRepository.findAllDistinctCategories();
        if (distinctCats != null) cats.addAll(distinctCats);
        cmbFiltroCat.setItems(cats);
        cmbFiltroCat.setValue("Todas las categorías");

        applyPeriodPreset("Esta Semana");

        updatingControls = false;
    }

    private void initListeners() {
        cmbPeriodo.valueProperty().addListener((obs, oldV, newV) -> {
            if (updatingControls || newV == null) return;
            applyPeriodPreset(newV);
            refreshPivot();
        });

        dpStart.valueProperty().addListener((obs, oldV, newV) -> {
            if (!updatingControls) {
                cmbPeriodo.setValue("Personalizado");
                refreshPivot();
            }
        });

        dpEnd.valueProperty().addListener((obs, oldV, newV) -> {
            if (!updatingControls) {
                cmbPeriodo.setValue("Personalizado");
                refreshPivot();
            }
        });

        cmbAgrupacion.valueProperty().addListener((obs, oldV, newV) -> {
            if (!updatingControls) refreshPivot();
        });

        cmbMetrica.valueProperty().addListener((obs, oldV, newV) -> {
            if (!updatingControls) refreshPivot();
        });

        cmbFiltroLab.valueProperty().addListener((obs, oldV, newV) -> {
            if (!updatingControls) refreshPivot();
        });

        cmbFiltroCat.valueProperty().addListener((obs, oldV, newV) -> {
            if (!updatingControls) refreshPivot();
        });

        txtSearch.textProperty().addListener((obs, oldV, newV) -> {
            if (!updatingControls) refreshPivot();
        });
    }

    private void applyPeriodPreset(String preset) {
        boolean prev = updatingControls;
        updatingControls = true;
        LocalDate today = LocalDate.now();

        switch (preset) {
            case "Esta Semana":
                dpStart.setValue(today.with(DayOfWeek.MONDAY));
                dpEnd.setValue(today);
                break;
            case "Este Mes":
                dpStart.setValue(today.withDayOfMonth(1));
                dpEnd.setValue(today);
                break;
            case "Mes Anterior":
                LocalDate prevMonth = today.minusMonths(1);
                dpStart.setValue(prevMonth.withDayOfMonth(1));
                dpEnd.setValue(prevMonth.withDayOfMonth(prevMonth.lengthOfMonth()));
                break;
            case "Año Actual":
                dpStart.setValue(today.withDayOfYear(1));
                dpEnd.setValue(today);
                break;
            case "Personalizado":
            default:
                break;
        }
        updatingControls = prev;
    }

    private void loadKpis() {
        SummaryService.KpiData kpis = summaryService.calculateKpis(dpStart.getValue(), dpEnd.getValue());
        lblKpiHoy.setText(String.format("$ %.2f", kpis.ventasHoy));
        lblKpiHoyTickets.setText(kpis.ticketsHoy + " ticket(s) emitido(s)");
        lblKpiSemana.setText(String.format("$ %.2f", kpis.ventasSemana));
        lblKpiMesAnt.setText(String.format("$ %.2f", kpis.ventasMesAnterior));
        lblKpiGanancia.setText(String.format("$ %.2f", kpis.gananciaPeriodo));
    }

    @FXML
    public void refreshPivot() {
        LocalDate start = dpStart.getValue();
        LocalDate end = dpEnd.getValue();
        if (start == null || end == null) return;

        String agrupacion = cmbAgrupacion.getValue();
        String metrica = cmbMetrica.getValue();
        String filtroLab = cmbFiltroLab.getValue();
        String filtroCat = cmbFiltroCat.getValue();
        String busqueda = txtSearch.getText();

        lastResult = summaryService.generatePivotTable(
                start, end, agrupacion, metrica, filtroLab, filtroCat, busqueda
        );

        rebuildTableColumns(agrupacion);

        ObservableList<PivotRow> items = FXCollections.observableArrayList();
        items.addAll(lastResult.rows);
        if (lastResult.totalRow != null && !lastResult.rows.isEmpty()) {
            items.add(lastResult.totalRow);
        }
        pivotTable.setItems(items);

        lblStatus.setText(String.format("Mostrando %d renglones agrupados por %s | Período: %s a %s",
                lastResult.rows.size(), agrupacion, start, end));

        loadKpis();
    }

    private void rebuildTableColumns(String agrupacion) {
        pivotTable.getColumns().clear();

        // 1. Columna de Dimensión
        TableColumn<PivotRow, String> colDim = new TableColumn<>(agrupacion);
        colDim.setCellValueFactory(new PropertyValueFactory<>("dimensionKey"));
        colDim.setPrefWidth(260);
        colDim.setCellFactory(column -> new TableCell<PivotRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("TOTALES GENERALES".equals(item)) {
                        setStyle("-fx-font-weight: bold; -fx-text-fill: #e67e22; -fx-background-color: #fcefe6;");
                    } else {
                        setStyle("-fx-font-weight: normal; -fx-text-fill: #2c3e50;");
                    }
                }
            }
        });
        pivotTable.getColumns().add(colDim);

        // 2. Columnas temporales
        for (String pKey : lastResult.periodKeys) {
            TableColumn<PivotRow, String> col = new TableColumn<>(pKey);
            col.setCellValueFactory(data -> data.getValue().getObservableValue(pKey));
            col.setPrefWidth(110);
            col.setStyle("-fx-alignment: CENTER_RIGHT;");
            col.setCellFactory(column -> new TableCell<PivotRow, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("-fx-alignment: CENTER_RIGHT;");
                    } else {
                        setText(item);
                        PivotRow row = getTableRow() != null ? getTableRow().getItem() : null;
                        if (row != null && "TOTALES GENERALES".equals(row.getDimensionKey())) {
                            setStyle("-fx-alignment: CENTER_RIGHT; -fx-font-weight: bold; -fx-background-color: #fcefe6;");
                        } else {
                            setStyle("-fx-alignment: CENTER_RIGHT;");
                        }
                    }
                }
            });
            pivotTable.getColumns().add(col);
        }

        // 3. Columna de Total
        TableColumn<PivotRow, String> colTotal = new TableColumn<>("TOTAL");
        colTotal.setCellValueFactory(data -> data.getValue().getObservableTotal());
        colTotal.setPrefWidth(130);
        colTotal.setCellFactory(column -> new TableCell<PivotRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-alignment: CENTER_RIGHT;");
                } else {
                    setText(item);
                    PivotRow row = getTableRow() != null ? getTableRow().getItem() : null;
                    if (row != null && "TOTALES GENERALES".equals(row.getDimensionKey())) {
                        setStyle("-fx-alignment: CENTER_RIGHT; -fx-font-weight: bold; -fx-text-fill: #d35400; -fx-background-color: #fcefe6;");
                    } else {
                        setStyle("-fx-alignment: CENTER_RIGHT; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
                    }
                }
            }
        });
        pivotTable.getColumns().add(colTotal);
    }

    @FXML
    public void onExportPivotCsv(ActionEvent event) {
        if (lastResult == null || lastResult.rows.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Sin Datos", "No hay datos en la matriz pivote para exportar.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar Matriz Pivote a CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo CSV", "*.csv"));
        fileChooser.setInitialFileName("resumen_pivote_" + LocalDate.now() + ".csv");
        File file = fileChooser.showSaveDialog(((Node) event.getSource()).getScene().getWindow());

        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file, "UTF-8")) {
                String dimName = cmbAgrupacion.getValue();
                
                // Cabecera CSV
                StringBuilder header = new StringBuilder();
                header.append("\"").append(dimName).append("\"");
                for (String pKey : lastResult.periodKeys) {
                    header.append(",\"").append(pKey).append("\"");
                }
                header.append(",\"TOTAL\"");
                writer.println(header);

                // Filas
                for (PivotRow row : lastResult.rows) {
                    StringBuilder line = new StringBuilder();
                    line.append("\"").append(row.getDimensionKey().replace("\"", "\"\"")).append("\"");
                    for (String pKey : lastResult.periodKeys) {
                        line.append(",").append(row.getRawValue(pKey));
                    }
                    line.append(",").append(row.getRowTotal());
                    writer.println(line);
                }

                // Fila de Totales
                if (lastResult.totalRow != null) {
                    StringBuilder totalLine = new StringBuilder();
                    totalLine.append("\"TOTALES GENERALES\"");
                    for (String pKey : lastResult.periodKeys) {
                        totalLine.append(",").append(lastResult.totalRow.getRawValue(pKey));
                    }
                    totalLine.append(",").append(lastResult.totalRow.getRowTotal());
                    writer.println(totalLine);
                }

                showAlert(Alert.AlertType.INFORMATION, "Exportación Exitosa", 
                        "La matriz pivotal ha sido exportada correctamente en formato CSV.");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error de Exportación", 
                        "Ocurrió un error al exportar la matriz: " + e.getMessage());
            }
        }
    }

    @FXML
    public void onBackToWarehouse(ActionEvent event) {
        Parent root = fxWeaver.loadView(InventoryController.class);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type, content);
        alert.setHeaderText(title);
        alert.show();
    }
}
