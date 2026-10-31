package cn.learn.lfy.mtr.service;

import cn.learn.lfy.mtr.api.ApiErrorCode;
import cn.learn.lfy.mtr.api.BusinessException;
import cn.learn.lfy.mtr.api.SimulationConfigRequest;
import cn.learn.lfy.mtr.api.SimulationConfigResponse;
import cn.learn.lfy.mtr.config.SimulationConfig;
import cn.learn.lfy.mtr.config.SimulationSettings;
import org.springframework.stereotype.Service;

@Service
public class SimulationConfigService {
    private final SimulationSettings simulationSettings;

    public SimulationConfigService(SimulationSettings simulationSettings) {
        this.simulationSettings = simulationSettings;
    }

    public SimulationConfigResponse current() {
        return toResponse(simulationSettings.snapshot());
    }

    public SimulationConfigResponse update(SimulationConfigRequest request) {
        try {
            SimulationConfig config = simulationSettings.update(
                    request.failStage(),
                    request.assetCheckDelayMs(),
                    request.simulatingDelayMs(),
                    request.resultPackagingDelayMs()
            );
            return toResponse(config);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ApiErrorCode.INVALID_REQUEST, exception.getMessage());
        }
    }

    private static SimulationConfigResponse toResponse(SimulationConfig config) {
        return new SimulationConfigResponse(
                config.failStage() == null ? null : config.failStage().name(),
                config.assetCheckDelayMs(),
                config.simulatingDelayMs(),
                config.resultPackagingDelayMs()
        );
    }
}
