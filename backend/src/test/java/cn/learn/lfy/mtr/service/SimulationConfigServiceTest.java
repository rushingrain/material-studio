package cn.learn.lfy.mtr.service;

import cn.learn.lfy.mtr.api.ApiErrorCode;
import cn.learn.lfy.mtr.api.BusinessException;
import cn.learn.lfy.mtr.api.SimulationConfigRequest;
import cn.learn.lfy.mtr.api.SimulationConfigResponse;
import cn.learn.lfy.mtr.config.SimulationSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SimulationConfigServiceTest {
    private SimulationSettings settings;
    private SimulationConfigService service;

    @BeforeEach
    void setUp() {
        settings = new SimulationSettings(100, "", "", "", "");
        service = new SimulationConfigService(settings);
    }

    @Test
    void updatesRuntimeConfiguration() {
        SimulationConfigResponse response = service.update(
                new SimulationConfigRequest("SIMULATING", 10L, 20L, 30L)
        );

        assertEquals("SIMULATING", response.failStage());
        assertEquals(10, response.assetCheckDelayMs());
        assertEquals(20, response.simulatingDelayMs());
        assertEquals(30, response.resultPackagingDelayMs());
        assertEquals("SIMULATING", settings.snapshot().failStage().name());
    }

    @Test
    void clearsFailureWhenStageIsBlank() {
        SimulationConfigResponse response = service.update(
                new SimulationConfigRequest(" ", 0L, 0L, 0L)
        );

        assertNull(response.failStage());
        assertNull(settings.snapshot().failStage());
    }

    @Test
    void translatesInvalidStageToBusinessError() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.update(new SimulationConfigRequest("COMPLETED", 0L, 0L, 0L))
        );

        assertEquals(ApiErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }
}
