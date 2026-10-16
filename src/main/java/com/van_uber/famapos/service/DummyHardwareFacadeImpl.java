package com.van_uber.famapos.service;

import com.van_uber.famapos.domain.Comprar;
import com.van_uber.famapos.domain.Ticket;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DummyHardwareFacadeImpl implements HardwareFacade {

    @Override
    public void printReceipt(Ticket ticket, List<Comprar> items) {
        System.out.println("===========================================");
        System.out.println("====== FARMACIA MANUEL - COMPROBANTE =========");
        System.out.println("Ticket ID: " + ticket.getIdTicket());
        System.out.println("Fecha: " + ticket.getFecha() + " Hora: " + ticket.getHora());
        System.out.println("-------------------------------------------");
        for (Comprar item : items) {
            System.out.println(item.getCantidad() + "x " + item.getProduct().getNombre() + " .... $" + item.getPrecioVenta());
        }
        System.out.println("-------------------------------------------");
        System.out.println("TOTAL: $" + ticket.getTotal());
        System.out.println("RECIBIDO: $" + ticket.getMontoRecibido());
        System.out.println("CAMBIO: $" + ticket.getCambio());
        System.out.println("===========================================");
    }

    @Override
    public void openCashDrawer() {
        System.out.println("====== (DUMMY) ABRIENDO CAJÓN DE DINERO ======");
    }
}
