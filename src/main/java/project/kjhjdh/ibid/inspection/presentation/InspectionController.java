package project.kjhjdh.ibid.inspection.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.inspection.application.InspectionService;

@RestController
@RequestMapping("/api/inspections")
@RequiredArgsConstructor
public class InspectionController {

    private final InspectionService inspectionService;

    @PostMapping("/{orderId}/receive")
    public ResponseEntity<Void> receive(@PathVariable Long orderId) {
        inspectionService.receive(orderId);
        return ResponseEntity.ok().build();
    }
}
