package dps.adminserver.rest;

import dps.common.ProductionLineInfo;
import dps.adminserver.store.ProductionLineRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/lines")
public class RegistrationController {
    private final ProductionLineRegistry registry;

    @Autowired
    public RegistrationController(ProductionLineRegistry registry) {
        this.registry = registry;
    }

    @PostMapping
    public ResponseEntity<List<ProductionLineInfo>> registerLine(@RequestBody ProductionLineInfo newLine) {
        List<ProductionLineInfo> existingLine = registry.getAllLines();

        boolean addedLine = registry.addLine(newLine);
        if (!addedLine) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        return ResponseEntity.ok(existingLine);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeLine(@PathVariable int id) {
        boolean removed = registry.removeLine(id);

        if (!removed) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
