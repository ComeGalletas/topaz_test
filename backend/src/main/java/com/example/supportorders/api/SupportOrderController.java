package com.example.supportorders.api;

import com.example.supportorders.api.dto.CreateSupportOrderRequest;
import com.example.supportorders.api.dto.SupportOrderDetailResponse;
import com.example.supportorders.api.dto.SupportOrderSummaryResponse;
import com.example.supportorders.api.dto.UpdateStatusRequest;
import com.example.supportorders.application.SupportOrderService;
import com.example.supportorders.domain.OrderStatus;
import com.example.supportorders.domain.SupportOrder;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Capa HTTP: valida la entrada, delega en el servicio y traduce a DTOs. Sin reglas de negocio. */
@RestController
@RequestMapping("/api/support-orders")
public class SupportOrderController {

    private final SupportOrderService service;

    public SupportOrderController(SupportOrderService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<SupportOrderDetailResponse> create(@Valid @RequestBody CreateSupportOrderRequest request) {
        SupportOrder created = service.create(request.toNewSupportOrder());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(toDetail(created));
    }

    @GetMapping
    public List<SupportOrderSummaryResponse> list(@RequestParam(required = false) OrderStatus status) {
        return service.list(status).stream()
                .map(SupportOrderSummaryResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public SupportOrderDetailResponse get(@PathVariable long id) {
        return toDetail(service.get(id));
    }

    @PatchMapping("/{id}/status")
    public SupportOrderDetailResponse changeStatus(@PathVariable long id,
                                                   @Valid @RequestBody UpdateStatusRequest request) {
        return toDetail(service.changeStatus(id, request.status()));
    }

    private SupportOrderDetailResponse toDetail(SupportOrder order) {
        return SupportOrderDetailResponse.from(order, service.allowedTransitions(order));
    }
}
