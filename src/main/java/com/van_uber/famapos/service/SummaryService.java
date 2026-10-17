package com.van_uber.famapos.service;

import com.van_uber.famapos.domain.Comprar;
import com.van_uber.famapos.domain.PivotRow;
import com.van_uber.famapos.domain.Ticket;
import com.van_uber.famapos.repository.ComprarRepository;
import com.van_uber.famapos.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SummaryService {

    private final TicketRepository ticketRepository;
    private final ComprarRepository comprarRepository;

    @Autowired
    public SummaryService(TicketRepository ticketRepository, ComprarRepository comprarRepository) {
        this.ticketRepository = ticketRepository;
        this.comprarRepository = comprarRepository;
    }

    public static class KpiData {
        public BigDecimal ventasHoy = BigDecimal.ZERO;
        public int ticketsHoy = 0;
        public BigDecimal ventasSemana = BigDecimal.ZERO;
        public BigDecimal ventasMesAnterior = BigDecimal.ZERO;
        public BigDecimal gananciaPeriodo = BigDecimal.ZERO;
    }

    public static class PivotResult {
        public List<String> periodKeys = new ArrayList<>();
        public List<PivotRow> rows = new ArrayList<>();
        public PivotRow totalRow;
        public boolean isCurrency;

        public PivotResult(List<String> periodKeys, List<PivotRow> rows, PivotRow totalRow, boolean isCurrency) {
            this.periodKeys = periodKeys;
            this.rows = rows;
            this.totalRow = totalRow;
            this.isCurrency = isCurrency;
        }
    }

    public KpiData calculateKpis(LocalDate rangeStart, LocalDate rangeEnd) {
        KpiData data = new KpiData();
        LocalDate today = LocalDate.now();

        // 1. Hoy
        List<Ticket> ticketsHoy = ticketRepository.findByFechaBetween(today, today);
        data.ticketsHoy = ticketsHoy.size();
        data.ventasHoy = ticketsHoy.stream()
                .map(Ticket::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Semana actual (Lunes a hoy)
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        List<Ticket> ticketsSemana = ticketRepository.findByFechaBetween(startOfWeek, today);
        data.ventasSemana = ticketsSemana.stream()
                .map(Ticket::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Mes anterior (1er al último día del mes pasado)
        LocalDate prevMonth = today.minusMonths(1);
        LocalDate startOfPrevMonth = prevMonth.withDayOfMonth(1);
        LocalDate endOfPrevMonth = prevMonth.withDayOfMonth(prevMonth.lengthOfMonth());
        List<Ticket> ticketsMesAnt = ticketRepository.findByFechaBetween(startOfPrevMonth, endOfPrevMonth);
        data.ventasMesAnterior = ticketsMesAnt.stream()
                .map(Ticket::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. Ganancia en el rango seleccionado
        if (rangeStart != null && rangeEnd != null) {
            List<Comprar> comprasRango = comprarRepository.findByTicketFechaBetween(rangeStart, rangeEnd);
            BigDecimal totalGanancia = BigDecimal.ZERO;
            for (Comprar c : comprasRango) {
                if (c.getProduct() != null && c.getProduct().getCosto() != null && c.getPrecioVenta() != null) {
                    BigDecimal costoTotal = c.getProduct().getCosto().multiply(BigDecimal.valueOf(c.getCantidad()));
                    BigDecimal ganancia = c.getPrecioVenta().subtract(costoTotal);
                    totalGanancia = totalGanancia.add(ganancia);
                }
            }
            data.gananciaPeriodo = totalGanancia;
        }

        return data;
    }

    public PivotResult generatePivotTable(
            LocalDate startDate,
            LocalDate endDate,
            String agrupacion,      // "Por Producto", "Por Laboratorio", "Por Categoría"
            String metrica,         // "Piezas Vendidas", "Ingresos ($)", "Ganancia Estimada ($)"
            String filtroLab,       // null / "Todos"
            String filtroCat,       // null / "Todas"
            String busqueda) {

        if (startDate == null) startDate = LocalDate.now().minusDays(7);
        if (endDate == null) endDate = LocalDate.now();
        if (endDate.isBefore(startDate)) {
            LocalDate tmp = startDate;
            startDate = endDate;
            endDate = tmp;
        }

        boolean isCurrency = !"Piezas Vendidas".equalsIgnoreCase(metrica);
        long daysCount = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        boolean groupByMonth = daysCount > 35;

        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MM/yyyy");

        // 1. Construir lista ordenada de columnas temporales
        List<String> periodKeys = new ArrayList<>();
        if (groupByMonth) {
            LocalDate curr = startDate.withDayOfMonth(1);
            LocalDate lastMonth = endDate.withDayOfMonth(1);
            while (!curr.isAfter(lastMonth)) {
                String mKey = curr.format(monthFormatter);
                if (!periodKeys.contains(mKey)) {
                    periodKeys.add(mKey);
                }
                curr = curr.plusMonths(1);
            }
        } else {
            LocalDate curr = startDate;
            while (!curr.isAfter(endDate)) {
                periodKeys.add(curr.format(dayFormatter));
                curr = curr.plusDays(1);
            }
        }

        // 2. Consultar compras del rango
        List<Comprar> compras = comprarRepository.findByTicketFechaBetween(startDate, endDate);

        // 3. Aplicar filtros secundarios
        String lowerQuery = busqueda == null ? "" : busqueda.toLowerCase().trim();
        List<Comprar> filtradas = compras.stream().filter(c -> {
            if (c.getProduct() == null) return false;
            
            // Filtro Lab
            if (filtroLab != null && !filtroLab.isEmpty() && !filtroLab.equalsIgnoreCase("Todos los laboratorios") && !filtroLab.equalsIgnoreCase("Todos")) {
                if (c.getProduct().getLaboratorio() == null || !c.getProduct().getLaboratorio().equalsIgnoreCase(filtroLab)) {
                    return false;
                }
            }
            
            // Filtro Cat
            if (filtroCat != null && !filtroCat.isEmpty() && !filtroCat.equalsIgnoreCase("Todas las categorías") && !filtroCat.equalsIgnoreCase("Todas")) {
                if (c.getProduct().getCategoria() == null || !c.getProduct().getCategoria().equalsIgnoreCase(filtroCat)) {
                    return false;
                }
            }

            // Filtro búsqueda de texto
            if (!lowerQuery.isEmpty()) {
                boolean matchName = c.getProduct().getNombre() != null && c.getProduct().getNombre().toLowerCase().contains(lowerQuery);
                boolean matchCode = c.getProduct().getCodigoBarras() != null && c.getProduct().getCodigoBarras().toLowerCase().contains(lowerQuery);
                boolean matchLab = c.getProduct().getLaboratorio() != null && c.getProduct().getLaboratorio().toLowerCase().contains(lowerQuery);
                boolean matchCat = c.getProduct().getCategoria() != null && c.getProduct().getCategoria().toLowerCase().contains(lowerQuery);
                if (!matchName && !matchCode && !matchLab && !matchCat) {
                    return false;
                }
            }

            return true;
        }).collect(Collectors.toList());

        // 4. Mapear a PivotRow
        Map<String, PivotRow> rowMap = new LinkedHashMap<>();

        for (Comprar c : filtradas) {
            String dimKey;
            if ("Por Laboratorio".equalsIgnoreCase(agrupacion)) {
                dimKey = c.getProduct().getLaboratorio() != null && !c.getProduct().getLaboratorio().trim().isEmpty() 
                        ? c.getProduct().getLaboratorio().trim() : "Sin Laboratorio";
            } else if ("Por Categoría".equalsIgnoreCase(agrupacion)) {
                dimKey = c.getProduct().getCategoria() != null && !c.getProduct().getCategoria().trim().isEmpty() 
                        ? c.getProduct().getCategoria().trim() : "Sin Categoría";
            } else {
                dimKey = c.getProduct().getNombre() != null ? c.getProduct().getNombre() : c.getProduct().getCodigoBarras();
            }

            PivotRow row = rowMap.computeIfAbsent(dimKey, k -> new PivotRow(k, isCurrency));

            // Período
            LocalDate ticketFecha = c.getTicket().getFecha();
            String pKey = groupByMonth ? ticketFecha.format(monthFormatter) : ticketFecha.format(dayFormatter);

            // Calcular monto según métrica
            BigDecimal valor;
            if ("Piezas Vendidas".equalsIgnoreCase(metrica)) {
                valor = BigDecimal.valueOf(c.getCantidad());
            } else if ("Ganancia Estimada ($)".equalsIgnoreCase(metrica)) {
                BigDecimal costoUnidad = c.getProduct().getCosto() != null ? c.getProduct().getCosto() : BigDecimal.ZERO;
                BigDecimal costoTotal = costoUnidad.multiply(BigDecimal.valueOf(c.getCantidad()));
                BigDecimal venta = c.getPrecioVenta() != null ? c.getPrecioVenta() : BigDecimal.ZERO;
                valor = venta.subtract(costoTotal);
            } else {
                // Ingresos ($)
                valor = c.getPrecioVenta() != null ? c.getPrecioVenta() : BigDecimal.ZERO;
            }

            row.addValue(pKey, valor);
        }

        List<PivotRow> rows = new ArrayList<>(rowMap.values());
        // Ordenar por total descendente (los más vendidos primero)
        rows.sort((r1, r2) -> r2.getRowTotal().compareTo(r1.getRowTotal()));

        // 5. Fila de Totales Generales
        PivotRow totalRow = new PivotRow("TOTALES GENERALES", isCurrency);
        for (PivotRow r : rows) {
            for (String pKey : periodKeys) {
                totalRow.addValue(pKey, r.getRawValue(pKey));
            }
        }

        return new PivotResult(periodKeys, rows, totalRow, isCurrency);
    }
}
