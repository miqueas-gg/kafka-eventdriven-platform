package com.kafkaeventdriven.notification;

import com.kafkaeventdriven.notification.entities.NotificationEntity;
import com.kafkaeventdriven.notification.repositories.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository repository;

    @GetMapping
    public Page<NotificationEntity> getNotifications(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String eventType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "80") int size) {
        
        var pageable = PageRequest.of(page, size);
        
        if (status != null && eventType != null) {
            return repository.findByStatusAndEventType(status, eventType, pageable);
        } else if (status != null) {
            return repository.findByStatus(status, pageable);
        } else if (eventType != null) {
            return repository.findByEventType(eventType, pageable);
        }
        return repository.findAll(pageable);
    }
}