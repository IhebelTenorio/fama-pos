package com.van_uber.famapos.controller;

import java.io.PrintWriter;
import javafx.stage.FileChooser;
import java.io.File;

import com.van_uber.famapos.domain.Comprar;
import com.van_uber.famapos.domain.Ticket;
import com.van_uber.famapos.repository.ComprarRepository;
import com.van_uber.famapos.repository.TicketRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
@FxmlView("history.fxml")
public class HistoryController {

    private final TicketRepository ticketRepository;
    private final ComprarRepository comprarRepository;
    private final FxWeaver fxWeaver;

    @FXML
    private TableView<Ticket> ticketTable;
    @FXML
    private TableColumn<Ticket, Integer> colId;
    @FXML
    private TableColumn<Ticket, LocalDate> colFecha;
    @FXML
    private TableColumn<Ticket, LocalTime> colHora;
    @FXML
    private TableColumn<Ticket, BigDecimal> colTotal;

    @FXML
    private TextField searchField;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;

    private ObservableList<Ticket> ticketList = FXCollections.observableArrayList();

    @Autowired
    public HistoryController(TicketRepository ticketRepository, ComprarRepository comprarRepository, FxWeaver fxWeaver) {
        this.ticketRepository = ticketRepository;
        this.comprarRepository = comprarRepository;
        this.fxWeaver = fxWeaver;
    }

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idTicket"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colHora.setCellValueFactory(new PropertyValueFactory<>("hora"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        
        ticketTable.setPlaceholder(new Label("No hay tickets en el historial."));

        loadTickets();

        // Double click
        ticketTable.setRowFactory(tv -> {
            TableRow<Ticket> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
                    Ticket rowData = row.getItem();
                    openTicketModal(rowData);
                }
            });
            return row;
        });

        searchField.textProperty().addListener((obs, oldV, newV) -> filterTickets());
        startDatePicker.valueProperty().addListener((obs, oldV, newV) -> filterTickets());
        endDatePicker.valueProperty().addListener((obs, oldV, newV) -> filterTickets());
    }

    private void loadTickets() {
        // Find all tickets and sort by ID descending (newest first)
        List<Ticket> tickets = ticketRepository.findAll();
        tickets.sort((t1, t2) -> t2.getIdTicket().compareTo(t1.getIdTicket()));
        ticketList.setAll(tickets);
        ticketTable.setItems(ticketList);
    }

    private void filterTickets() {
        String idFilter = searchField.getText() == null ? "" : searchField.getText().trim();
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        ObservableList<Ticket> filtered = FXCollections.observableArrayList();
        for (Ticket t : ticketList) {
            boolean matchesId = idFilter.isEmpty() || String.valueOf(t.getIdTicket()).contains(idFilter);
            boolean matchesDate = (start == null || !t.getFecha().isBefore(start)) 
                    && (end == null || !t.getFecha().isAfter(end));
            
            if (matchesId && matchesDate) {
                filtered.add(t);
            }
        }
        ticketTable.setItems(filtered);
    }

    @FXML
    public void onPresetToday(ActionEvent event) {
        LocalDate today = LocalDate.now();
        startDatePicker.setValue(today);
        endDatePicker.setValue(today);
    }

    @FXML
    public void onPresetWeek(ActionEvent event) {
        LocalDate today = LocalDate.now();
        startDatePicker.setValue(today.minusDays(7));
        endDatePicker.setValue(today);
    }

    @FXML
    public void onPresetMonth(ActionEvent event) {
        LocalDate today = LocalDate.now();
        startDatePicker.setValue(today.minusMonths(1));
        endDatePicker.setValue(today);
    }

    @FXML
    public void onPresetYear(ActionEvent event) {
        LocalDate today = LocalDate.now();
        startDatePicker.setValue(today.minusYears(1));
        endDatePicker.setValue(today);
    }

    @FXML
    public void onClearFilters(ActionEvent event) {
        searchField.clear();
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
    }

    @FXML
    public void onViewDetails(ActionEvent event) {
        Ticket selected = ticketTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openTicketModal(selected);
        } else {
            showAlert(Alert.AlertType.WARNING, "Selección", "Por favor seleccione un ticket de la tabla.");
        }
    }

    private void openTicketModal(Ticket ticket) {
        net.rgielen.fxweaver.core.FxControllerAndView<TicketDetailModalController, Node> cav = 
                fxWeaver.load(TicketDetailModalController.class);
        
        cav.getController().setTicket(ticket);
        
        Stage stage = new Stage();
        stage.setTitle("Detalles del Ticket #" + ticket.getIdTicket());
        stage.setScene(new Scene((Parent) cav.getView().get()));
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        stage.showAndWait();
        
        // Refresh when modal closes (in case ticket was deleted/refunded)
        loadTickets();
    }

    @FXML
    public void onExportCsv(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar Historial a CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo CSV", "*.csv"));
        fileChooser.setInitialFileName("historial_tickets_" + java.time.LocalDate.now().toString() + ".csv");
        File file = fileChooser.showSaveDialog(((Node) event.getSource()).getScene().getWindow());
        
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file, "UTF-8")) {
                writer.println("FolioID,Fecha,Hora,Total,Recibido,Cambio");
                for (Ticket t : ticketTable.getItems()) { // Respects current filters (Date/Folio search)
                    writer.printf("%d,%s,%s,%s,%s,%s\n",
                        t.getIdTicket(), 
                        t.getFecha() != null ? t.getFecha().toString() : "", 
                        t.getHora() != null ? t.getHora().toString() : "", 
                        t.getTotal(), 
                        t.getMontoRecibido(), 
                        t.getCambio());
                }
                showAlert(Alert.AlertType.INFORMATION, "Exportación Exitosa", "El historial de tickets ha sido exportado correctamente.");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error de Exportación", "Hubo un error al guardar el archivo: " + e.getMessage());
            }
        }
    }

    @FXML
    public void onExportDetailedSalesCsv(ActionEvent event) {
        if (ticketTable.getItems().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Sin Registros", "No hay tickets visibles para exportar el detalle.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar Detalle de Ventas a CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo CSV", "*.csv"));
        fileChooser.setInitialFileName("detalle_ventas_" + java.time.LocalDate.now().toString() + ".csv");
        File file = fileChooser.showSaveDialog(((Node) event.getSource()).getScene().getWindow());

        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file, "UTF-8")) {
                writer.println("FolioTicket,Fecha,Hora,CodigoBarras,Producto,Categoria,Laboratorio,Cantidad,PrecioVenta");
                for (Ticket t : ticketTable.getItems()) {
                    List<Comprar> purchasedItems = comprarRepository.findById_IdTicket(t.getIdTicket());
                    for (Comprar c : purchasedItems) {
                        String prodName = c.getProduct() != null && c.getProduct().getNombre() != null ? c.getProduct().getNombre().replace("\"", "\"\"") : "";
                        String prodCat = c.getProduct() != null && c.getProduct().getCategoria() != null ? c.getProduct().getCategoria().replace("\"", "\"\"") : "";
                        String prodLab = c.getProduct() != null && c.getProduct().getLaboratorio() != null ? c.getProduct().getLaboratorio().replace("\"", "\"\"") : "";
                        String prodCode = c.getProduct() != null && c.getProduct().getCodigoBarras() != null ? c.getProduct().getCodigoBarras() : "";

                        writer.printf("%d,%s,%s,\"%s\",\"%s\",\"%s\",\"%s\",%d,%s\n",
                                t.getIdTicket(),
                                t.getFecha() != null ? t.getFecha().toString() : "",
                                t.getHora() != null ? t.getHora().toString() : "",
                                prodCode,
                                prodName,
                                prodCat,
                                prodLab,
                                c.getCantidad(),
                                c.getPrecioVenta());
                    }
                }
                showAlert(Alert.AlertType.INFORMATION, "Exportación Exitosa", "El detalle completo de ventas ha sido exportado correctamente a CSV.");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error de Exportación", "Hubo un error al guardar el archivo: " + e.getMessage());
            }
        }
    }

    @FXML
    public void onBackToSales(ActionEvent event) {
        Parent root = fxWeaver.loadView(SalesController.class);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type, content);
        alert.setHeaderText(title);
        alert.show();
    }
}
