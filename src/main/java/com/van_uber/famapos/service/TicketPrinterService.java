package com.van_uber.famapos.service;

import com.van_uber.famapos.domain.Comprar;
import com.van_uber.famapos.domain.Ticket;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

@Service
@Primary 
public class TicketPrinterService implements HardwareFacade {

    private static final String PRINTER_MODEL = "eQual"; 
    private static final byte[] INIT = {0x1B, 0x40}; 
    private static final byte[] CUT_PAPER = {0x1D, 0x56, 0x42, 0x00}; 
    private static final byte[] OPEN_DRAWER = {0x1B, 0x70, 0x00, 0x19, 0x19}; 

    @Override
    public void printReceipt(Ticket ticket, List<Comprar> items) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(INIT);
            
            StringBuilder sb = new StringBuilder();
            sb.append(centerText("FARMACIA MANUEL")).append("\n");
            sb.append(centerText("Comprobante de Venta")).append("\n");
            sb.append("================================\n");
            sb.append(String.format("Folio: %d\n", ticket.getIdTicket()));
            sb.append(String.format("Fecha: %s Hora: %s\n", ticket.getFecha(), ticket.getHora()));
            sb.append("================================\n");
            
            for (Comprar item : items) {
                String qtyX = item.getCantidad() + "x ";
                String name = item.getProduct().getNombre();
                String priceStr = "$" + item.getPrecioVenta();
                
                if (name.length() > 15) {
                    name = name.substring(0, 15);
                }
                
                String line = String.format("%-3s %-15s %12s\n", qtyX, name, priceStr);
                sb.append(line);
            }
            sb.append("--------------------------------\n");
            sb.append(String.format("%-15s %16s\n", "TOTAL:", "$" + ticket.getTotal()));
            sb.append(String.format("%-15s %16s\n", "RECIBIDO:", "$" + ticket.getMontoRecibido()));
            sb.append(String.format("%-15s %16s\n", "CAMBIO:", "$" + ticket.getCambio()));
            sb.append("================================\n");
            sb.append(centerText("¡Gracias por su compra!")).append("\n\n\n\n");
            
            outputStream.write(sb.toString().getBytes("CP437"));
            outputStream.write(CUT_PAPER);
            
            sendRawToPrinter(outputStream.toByteArray());
            
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public void openCashDrawer() {
        try {
            sendRawToPrinter(OPEN_DRAWER);
        } catch (Exception e) {
            throw new RuntimeException("Error abriendo cajón de dinero: " + e.getMessage(), e);
        }
    }

    private void sendRawToPrinter(byte[] data) throws Exception {
        // Enfoque 1: Conexión nativa limpia y directa desde Sistema Operativo Linux (Recomendado para ESC/POS)
        File linuxUsbPort = new File("/dev/usb/lp0");
        if (linuxUsbPort.exists()) {
            if (linuxUsbPort.canWrite()) {
                try (FileOutputStream fos = new FileOutputStream(linuxUsbPort)) {
                    fos.write(data);
                    fos.flush();
                    return; // Éxito con archivo crudo /dev/usb/lp0
                }
            } else {
                throw new Exception("eQual detectada en puerto USB.\nSin embargo, el sistema bloquea el acceso (Faltan permisos de dispositivo).\n\nContacte a su administrador de sistemas para configurar los accesos de escritura a periféricos.");
            }
        }
        
        // Enfoque 2: Buscar en Cola de Impresión CUPS Clásica (si los permisos USB directos fallan o si no está en /dev/usb)
        PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
        if (printServices.length == 0) {
            throw new Exception("No hay ninguna impresora instalada en el sistema de impresiones (CUPS) ni se detectó cable físico en /dev/usb/lp0.");
        }
        
        PrintService defaultPrinter = PrintServiceLookup.lookupDefaultPrintService();
        PrintService targetPrinter = defaultPrinter != null ? defaultPrinter : printServices[0];
        
        for (PrintService p : printServices) {
            if (p.getName().toUpperCase().contains(PRINTER_MODEL.toUpperCase()) || p.getName().toUpperCase().contains("EQUAL") || p.getName().toUpperCase().contains("POS58")) {
                targetPrinter = p;
                break;
            }
        }
        
        DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
        Doc doc = new SimpleDoc(data, flavor, null);
        DocPrintJob printJob = targetPrinter.createPrintJob();
        printJob.print(doc, null);
    }
    
    private String centerText(String text) {
        int lineWidth = 32;
        if (text.length() >= lineWidth) return text;
        int padding = (lineWidth - text.length()) / 2;
        return " ".repeat(Math.max(0, padding)) + text;
    }
}
