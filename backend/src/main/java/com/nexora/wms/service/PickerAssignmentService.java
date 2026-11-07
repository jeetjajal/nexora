package com.nexora.wms.service;

import com.nexora.exception.BadRequestException;
import com.nexora.exception.ResourceNotFoundException;
import com.nexora.wms.entity.PickTask;
import com.nexora.wms.entity.PickerProfile;
import com.nexora.wms.entity.PickingEvent;
import com.nexora.wms.enums.PickerStatus;
import com.nexora.wms.enums.PickingEventType;
import com.nexora.wms.enums.PickTaskStatus;
import com.nexora.wms.repository.PickTaskRepository;
import com.nexora.wms.repository.PickerProfileRepository;
import com.nexora.wms.repository.PickingEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * SMART PICKER ASSIGNMENT ENGINE (NEXORA 3.0 WS4)
 *
 * Implements workload-balanced picker assignment, concurrency safety,
 * task queuing, and manual reassignment for dark-store warehouse management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PickerAssignmentService {

    private final PickerProfileRepository pickerProfileRepository;
    private final PickTaskRepository pickTaskRepository;
    private final PickingEventRepository pickingEventRepository;

    /**
     * Automatically assigns an available picker to a PickTask using workload-balancing heuristics.
     * Concurrency-safe: isolated transaction prevents double-allocation under burst traffic.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public synchronized Optional<PickerProfile> assignPickerToTask(PickTask task) {
        if (task == null || task.getStore() == null) {
            log.warn("Cannot assign picker: task or dark store is null");
            return Optional.empty();
        }

        Long storeId = task.getStore().getId();
        List<PickerProfile> availablePickers = pickerProfileRepository.findByStoreIdAndStatus(storeId, PickerStatus.AVAILABLE);

        if (availablePickers.isEmpty()) {
            log.info("No AVAILABLE pickers at store #{}. Task #{} queued as PENDING.", storeId, task.getTaskCode());
            task.setStatus(PickTaskStatus.PENDING);
            pickTaskRepository.save(task);
            return Optional.empty();
        }

        // Workload-balancing heuristic: Select picker with the lowest completed tasks during this shift
        PickerProfile selectedPicker = availablePickers.stream()
                .min(Comparator.comparing(p -> p.getCompletedTasksCount() != null ? p.getCompletedTasksCount() : 0))
                .orElse(availablePickers.get(0));

        // Mark picker as BUSY
        selectedPicker.setStatus(PickerStatus.BUSY);
        selectedPicker.setCurrentTaskId(task.getId());
        pickerProfileRepository.save(selectedPicker);

        // Assign to task
        task.setPicker(selectedPicker);
        task.setStatus(PickTaskStatus.ASSIGNED);
        task.setAssignedAt(LocalDateTime.now());
        pickTaskRepository.save(task);

        log.info("Assigned picker #{} ({}) to task #{} at store #{}",
                selectedPicker.getId(), selectedPicker.getEmployeeCode(), task.getTaskCode(), storeId);

        return Optional.of(selectedPicker);
    }

    /**
     * Reassigns an in-flight or stalled pick task to another picker.
     */
    @Transactional
    public PickTask reassignPicker(Long pickTaskId, Long newPickerProfileId, String reason, Long callerUserId) {
        PickTask task = pickTaskRepository.findById(pickTaskId)
                .orElseThrow(() -> new ResourceNotFoundException("PickTask not found with id: " + pickTaskId));

        PickerProfile newPicker = pickerProfileRepository.findById(newPickerProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Target picker not found with id: " + newPickerProfileId));

        if (!newPicker.getStore().getId().equals(task.getStore().getId())) {
            throw new BadRequestException("Target picker is registered at a different store");
        }

        PickerProfile oldPicker = task.getPicker();
        if (oldPicker != null) {
            oldPicker.setStatus(PickerStatus.AVAILABLE);
            if (task.getId().equals(oldPicker.getCurrentTaskId())) {
                oldPicker.setCurrentTaskId(null);
            }
            pickerProfileRepository.save(oldPicker);
        }

        newPicker.setStatus(PickerStatus.BUSY);
        newPicker.setCurrentTaskId(task.getId());
        pickerProfileRepository.save(newPicker);

        task.setPicker(newPicker);
        task.setStatus(PickTaskStatus.ASSIGNED);
        task.setAssignedAt(LocalDateTime.now());
        PickTask updated = pickTaskRepository.save(task);

        log.info("Reassigned pick task #{} from picker #{} to #{} (Reason: {})",
                task.getTaskCode(), oldPicker != null ? oldPicker.getId() : "NONE", newPicker.getId(), reason);

        return updated;
    }

    /**
     * Marks a picker as available after completing or relinquishing their task.
     */
    @Transactional
    public void releasePicker(Long pickerProfileId) {
        pickerProfileRepository.findById(pickerProfileId).ifPresent(p -> {
            p.setStatus(PickerStatus.AVAILABLE);
            p.setCurrentTaskId(null);
            if (p.getCompletedTasksCount() == null) {
                p.setCompletedTasksCount(1);
            } else {
                p.setCompletedTasksCount(p.getCompletedTasksCount() + 1);
            }
            pickerProfileRepository.save(p);
            log.debug("Released picker #{}. Status is now AVAILABLE.", pickerProfileId);
        });
    }

    /**
     * Updates picker status (e.g. going ON_BREAK, AVAILABLE, or OFFLINE).
     */
    @Transactional
    public PickerProfile updatePickerStatus(Long pickerProfileId, PickerStatus newStatus) {
        PickerProfile picker = pickerProfileRepository.findById(pickerProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Picker profile not found with id: " + pickerProfileId));

        if (newStatus == PickerStatus.AVAILABLE && picker.getStatus() == PickerStatus.OFFLINE) {
            picker.setShiftStartedAt(LocalDateTime.now());
        }

        picker.setStatus(newStatus);
        return pickerProfileRepository.save(picker);
    }
}
