package com.kafkaeventdriven.ingest.infrastructure.rest;

import com.kafkaeventdriven.ingest.application.dtos.EventResponseDTO;
import com.kafkaeventdriven.ingest.application.dtos.EventStatsDTO;
import com.kafkaeventdriven.ingest.application.services.EventQueryService;
import com.kafkaeventdriven.ingest.infrastructure.repositories.specs.EventSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventQueryService eventQueryService;

    // GET /api/events?eventType=...&source=...&from=...&to=...&page=0&size=20
    @GetMapping
    public ResponseEntity<Page<EventResponseDTO>> getAllEvents(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 90) Pageable pageable) {
        
        var spec = EventSpecifications.withFilters(eventType, source, from, to);
        return ResponseEntity.ok(eventQueryService.findAll(spec, pageable));
    }

    // GET /api/events/{id} -> ID interno de la DB
    @GetMapping("/{id}")
    public ResponseEntity<EventResponseDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(eventQueryService.getById(id));
    }

    // GET /api/events/by-event-id/{eventId} -> UUID de Kafka
    @GetMapping("/by-event-id/{eventId}")
    public ResponseEntity<EventResponseDTO> getByEventId(@PathVariable UUID eventId) {
        return ResponseEntity.ok(eventQueryService.getByEventId(eventId));
    }

    // GET /api/events/stats
    @GetMapping("/stats")
    public ResponseEntity<List<EventStatsDTO>> getStats() {
        return ResponseEntity.ok(eventQueryService.getStats());
    }
}