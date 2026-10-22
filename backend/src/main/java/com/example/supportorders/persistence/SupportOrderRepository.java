package com.example.supportorders.persistence;

import com.example.supportorders.domain.OrderStatus;
import com.example.supportorders.domain.SupportOrder;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportOrderRepository extends JpaRepository<SupportOrder, Long> {

    /** Usa la columna con restricción UNIQUE, que solo tiene valor para órdenes activas. */
    boolean existsByActiveReference(String externalReference);

    List<SupportOrder> findByStatus(OrderStatus status, Sort sort);
}
