package cn.learn.lfy.mtr.controller;

import cn.learn.lfy.mtr.api.ApiResponse;
import cn.learn.lfy.mtr.api.SimulationConfigRequest;
import cn.learn.lfy.mtr.api.SimulationConfigResponse;
import cn.learn.lfy.mtr.service.SimulationConfigService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("dev")
@RestController
@RequestMapping("/dev/simulation")
public class SimulationDevController {
    private final SimulationConfigService simulationConfigService;

    public SimulationDevController(SimulationConfigService simulationConfigService) {
        this.simulationConfigService = simulationConfigService;
    }

    @GetMapping
    public ApiResponse<SimulationConfigResponse> get() {
        return ApiResponse.success(simulationConfigService.current());
    }

    @PutMapping
    public ApiResponse<SimulationConfigResponse> update(
            @Valid @RequestBody SimulationConfigRequest request
    ) {
        return ApiResponse.success(simulationConfigService.update(request));
    }
}
