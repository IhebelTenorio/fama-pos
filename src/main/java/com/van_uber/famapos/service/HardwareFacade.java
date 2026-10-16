package com.van_uber.famapos.service;

import com.van_uber.famapos.domain.Comprar;
import com.van_uber.famapos.domain.Ticket;
import java.util.List;

public interface HardwareFacade {
    void printReceipt(Ticket ticket, List<Comprar> items);
    void openCashDrawer();
}
