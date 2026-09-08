package dps.adminserver.rest;

import dps.adminserver.store.ProductionLineRegistry;
import dps.adminserver.store.TelemetryRegistry;
import dps.common.OperationalState;
import dps.common.ProductionLineInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/lines")
public class QueryController {
    private final ProductionLineRegistry productionLineRegistry;
    private final TelemetryRegistry telemetryRegistry;

    @Autowired
    public QueryController(ProductionLineRegistry productionLineRegistry, TelemetryRegistry telemetryRegistry) {
        this.productionLineRegistry = productionLineRegistry;
        this.telemetryRegistry = telemetryRegistry;
    }

    @GetMapping
    public List<ProductionLineInfo> getAllLines() {
        return productionLineRegistry.getAllLines();
    }

    @GetMapping("/{id}/state")
    public ResponseEntity<OperationalState> getState(@PathVariable int id) {
        OperationalState state = telemetryRegistry.getState(id);

        if (state == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(state);
    }

    @GetMapping("/{id}/average")
    public ResponseEntity<Double> getAverage(@PathVariable int id, @RequestParam long from, @RequestParam long to) {
        Double avg = telemetryRegistry.getAverage(id, from, to);

        if (avg == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(avg);
    }
}
