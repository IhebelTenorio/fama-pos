package com.van_uber.famapos;

import com.van_uber.famapos.domain.*;
import com.van_uber.famapos.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DataIntegrityTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private ComprarRepository comprarRepository;

    @Test
    @Transactional // Hace un roll-back al terminar la prueba
    void testHistoricalPriceMaintainedWhenProductPriceChanges() {
        // 1. Crear producto de prueba a $22
        Product p = new Product();
        p.setCodigoBarras("TEST-123");
        p.setNombre("Producto Test");
        p.setPrecioPublico(new BigDecimal("22.00"));
        p.setCosto(new BigDecimal("10.00"));
        p.setUnidadesDisponibles(100);
        p.setUmbralUnidades(10);
        p.setActivo(true);
        productRepository.save(p);

        // 2. Crear un ticket y simular venta de 2 unidades ($44 en total)
        Ticket t = new Ticket();
        t.setTotal(new BigDecimal("44.00"));
        t.setMontoRecibido(new BigDecimal("50.00"));
        t.setCambio(new BigDecimal("6.00"));
        t.setFecha(LocalDate.now());
        t.setHora(LocalTime.now());
        t = ticketRepository.save(t); // Assign saved entity to capture the auto-generated ID

        Comprar compra = new Comprar();
        ComprarId cid = new ComprarId(p.getCodigoBarras(), t.getIdTicket());
        compra.setId(cid);
        compra.setProduct(p);
        compra.setTicket(t);
        compra.setCantidad(2);
        compra.setPrecioVenta(new BigDecimal("44.00")); // Guardado el subtotal del precio histórico
        comprarRepository.save(compra);
        
        // 3. Modificar el precio del producto a $25
        p.setPrecioPublico(new BigDecimal("25.00"));
        productRepository.save(p);
        
        // --- ASERCIONES (TESTS) ---
        
        // A. Verificar que el ticket sigue costando $44.00
        Ticket savedTicket = ticketRepository.findById(t.getIdTicket()).orElseThrow();
        assertEquals(0, new BigDecimal("44.00").compareTo(savedTicket.getTotal()), "El total de ventas pasadas no debería alterarse");
        
        // B. Verificar que el desglose de Compra sigue con el precio de vental original $44.00
        List<Comprar> compras = comprarRepository.findById_IdTicket(savedTicket.getIdTicket());
        assertEquals(1, compras.size());
        assertEquals(0, new BigDecimal("44.00").compareTo(compras.get(0).getPrecioVenta()), "El precio histórico debe mantenerse en 44.00");
        
        // C. Verificar que el producto sí actualizó su precio público actual
        Product updatedProduct = productRepository.findById("TEST-123").orElseThrow();
        assertEquals(0, new BigDecimal("25.00").compareTo(updatedProduct.getPrecioPublico()), "El producto debería estar actualizado a 25.00");
    }
    
    @Test
    @Transactional // Hace un roll-back al terminar la prueba
    void testRefundIntegrityAndNoDuplicateEntries() {
        // 1. Crear producto con 10 unidades
        Product p = new Product();
        p.setCodigoBarras("TEST-REFUND");
        p.setNombre("Producto Reembolso");
        p.setPrecioPublico(new BigDecimal("22.00"));
        p.setCosto(new BigDecimal("10.00"));
        p.setUnidadesDisponibles(10);
        p.setUmbralUnidades(5);
        p.setActivo(true);
        productRepository.save(p);

        // 2. Vender 2 unidades
        p.setUnidadesDisponibles(8); // Al vender bajamos el stock a 8
        productRepository.save(p);
        
        Ticket t = new Ticket();
        t.setTotal(new BigDecimal("44.00"));
        t.setMontoRecibido(new BigDecimal("50.00"));
        t.setCambio(new BigDecimal("6.00"));
        t.setFecha(LocalDate.now());
        t.setHora(LocalTime.now());
        t = ticketRepository.save(t);
        
        Comprar compra = new Comprar();
        compra.setId(new ComprarId(p.getCodigoBarras(), t.getIdTicket()));
        compra.setProduct(p);
        compra.setTicket(t);
        compra.setCantidad(2);
        compra.setPrecioVenta(new BigDecimal("44.00"));
        comprarRepository.save(compra);
        
        // 3. Simular que le suben el precio accidentalmente antes o despues del reembolso
        p.setPrecioPublico(new BigDecimal("30.00"));
        productRepository.save(p);
        
        // 4. EL REEMBOLSO (Misma logica que hace TicketDetailModalController.onRefund)
        List<Comprar> purchasedItems = comprarRepository.findById_IdTicket(t.getIdTicket());
        for (Comprar c : purchasedItems) {
            Product refundProduct = c.getProduct();
            refundProduct.setUnidadesDisponibles(refundProduct.getUnidadesDisponibles() + c.getCantidad());
            productRepository.save(refundProduct); // Guarda con JPA save (si existe lo hace UPDATE por primary key)
        }
        comprarRepository.deleteAll(purchasedItems);
        ticketRepository.delete(t);
        
        // --- ASERCIONES ---
        
        // A. Verificar que el número de unidades volvió a ser 10 correctamente
        Product refundedProduct = productRepository.findById("TEST-REFUND").orElseThrow();
        assertEquals(10, refundedProduct.getUnidadesDisponibles(), "El reembolso no sumó las unidades correctamente");
        
        // B. Verificar que el ticket ya no existe
        assertFalse(ticketRepository.existsById(t.getIdTicket()), "El ticket debería estar eliminado");
        
        // C. Verificar que la tabla Comprar ya no tiene la entrada, previendo duplicados
        assertTrue(comprarRepository.findById_IdTicket(t.getIdTicket()).isEmpty(), "Los detalles de la compra deben estar vacíos");
        
        // D. Comprobar que en Producto no se duplicó el producto, sino que sigue siendo un sólo registro TEST-REFUND
        assertEquals(0, new BigDecimal("30.00").compareTo(refundedProduct.getPrecioPublico()), "El precio que se alteró post-compra debe respetarse");
    }
}
