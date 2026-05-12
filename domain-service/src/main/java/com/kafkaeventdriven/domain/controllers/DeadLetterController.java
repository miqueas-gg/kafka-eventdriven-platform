package com.kafkaeventdriven.domain.controllers;

import com.kafkaeventdriven.domain.entities.DeadLetterEvent;
import com.kafkaeventdriven.domain.services.DeadLetterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/dead-letters")
@RequiredArgsConstructor
public class DeadLetterController {

    private final DeadLetterService deadLetterService;

    // GET /api/admin/dead-letters?page=0&size=10
    @GetMapping
    public ResponseEntity<Page<DeadLetterEvent>> listDeadLetters(
        @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(deadLetterService.getAllDeadLetters(pageable));
    }

    // POST /api/admin/dead-letters/{id}/replay
    @PostMapping("/{id}/replay")
    public ResponseEntity<Void> replay(@PathVariable UUID id) {
        deadLetterService.replayEvent(id);
        return ResponseEntity.noContent().build();
    }
}