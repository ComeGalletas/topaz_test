package com.example.supportorders.application;

import com.example.supportorders.domain.OrderStatus;
import com.example.supportorders.domain.StatusTransitionPolicy;
import com.example.supportorders.domain.SupportOrder;
import com.example.supportorders.persistence.SupportOrderRepository;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de órdenes de soporte.
 * <p>
 * Los logs registran identificadores y estados, nunca customerId ni la descripción, que pueden
 * contener datos personales.
 */
@Service
public class SupportOrderService {

    private static final Logger log = LoggerFactory.getLogger(SupportOrderService.class);
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
    private static final String ACTIVE_REFERENCE_CONSTRAINT = "uq_support_orders_active_reference";

    private final SupportOrderRepository repository;
    private final StatusTransitionPolicy transitionPolicy;
    private final Clock clock;

    public SupportOrderService(SupportOrderRepository repository, StatusTransitionPolicy transitionPolicy, Clock clock) {
        this.repository = repository;
        this.transitionPolicy = transitionPolicy;
        this.clock = clock;
    }

    @Transactional
    public SupportOrder create(NewSupportOrder request) {
        // La capa HTTP ya valida los obligatorios; con null, Spring Data derivaría "IS NULL" y
        // coincidiría con cualquier orden final, dando un falso duplicado.
        String externalReference = Objects.requireNonNull(request.externalReference(), "externalReference");
        if (repository.existsByActiveReference(externalReference)) {
            throw new DuplicateActiveOrderException(externalReference);
        }

        SupportOrder order = SupportOrder.open(externalReference, request.customerId(), request.category(),
                request.description(), request.channel(), request.priority(), clock.instant());

        SupportOrder saved;
        try {
            saved = repository.saveAndFlush(order);
        } catch (DataIntegrityViolationException ex) {
            // Dos peticiones simultáneas pueden pasar la comprobación previa; la restricción UNIQUE decide.
            if (isActiveReferenceViolation(ex)) {
                throw new DuplicateActiveOrderException(externalReference);
            }
            throw ex;
        }

        log.info("Support order created id={} externalReference={} category={} priority={} channel={}",
                saved.getId(), externalReference, saved.getCategory(), saved.getPriority(), saved.getChannel());
        return saved;
    }

    /** Órdenes más recientes primero, opcionalmente filtradas por estado. */
    @Transactional(readOnly = true)
    public List<SupportOrder> list(OrderStatus status) {
        return status == null
                ? repository.findAll(NEWEST_FIRST)
                : repository.findByStatus(status, NEWEST_FIRST);
    }

    @Transactional(readOnly = true)
    public SupportOrder get(long id) {
        return repository.findById(id).orElseThrow(() -> new SupportOrderNotFoundException(id));
    }

    @Transactional
    public SupportOrder changeStatus(long id, OrderStatus target) {
        SupportOrder order = get(id);
        OrderStatus previous = order.getStatus();

        order.transitionTo(target, transitionPolicy, clock.instant());
        SupportOrder saved = repository.saveAndFlush(order);

        log.info("Support order status changed id={} from={} to={}", id, previous, target);
        return saved;
    }

    /** Estados a los que puede pasar la orden; útil para que el cliente solo ofrezca acciones válidas. */
    public List<OrderStatus> allowedTransitions(SupportOrder order) {
        return transitionPolicy.allowedTransitions(order.getStatus()).stream().sorted().toList();
    }

    private static boolean isActiveReferenceViolation(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause().getMessage();
        return message != null && message.toLowerCase(Locale.ROOT).contains(ACTIVE_REFERENCE_CONSTRAINT);
    }
}
