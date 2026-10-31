package cn.learn.lfy.mtr.config;

import cn.learn.lfy.mtr.domain.enums.TaskStage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SimulationSettingsTest {

    @Test
    void loadsPerStageDelaysAndFallsBackToSharedDelay() {
        SimulationSettings settings = new SimulationSettings(
                500,
                "",
                "1200",
                "",
                "simulating"
        );

        SimulationConfig config = settings.snapshot();

        assertEquals(TaskStage.SIMULATING, config.failStage());
        assertEquals(500, config.assetCheckDelayMs());
        assertEquals(1200, config.simulatingDelayMs());
        assertEquals(500, config.resultPackagingDelayMs());
        assertEquals(500, config.delayFor(TaskStage.ASSET_CHECK));
        assertEquals(1200, config.delayFor(TaskStage.SIMULATING));
        assertEquals(500, config.delayFor(TaskStage.RESULT_PACKAGING));
    }

    @Test
    void updatesOnlyNewSnapshots() {
        SimulationSettings settings = new SimulationSettings(100, "", "", "", "");
        SimulationConfig original = settings.snapshot();

        SimulationConfig updated = settings.update(null, 10, 20, 30);

        assertNotSame(original, updated);
        assertEquals(100, original.assetCheckDelayMs());
        assertEquals(10, updated.assetCheckDelayMs());
        assertEquals(20, updated.simulatingDelayMs());
        assertEquals(30, updated.resultPackagingDelayMs());
        assertEquals(updated, settings.snapshot());
    }

    @Test
    void rejectsUnsupportedStageAndNegativeDelay() {
        SimulationSettings settings = new SimulationSettings(100, "", "", "", "");

        assertThrows(
                IllegalArgumentException.class,
                () -> settings.update("COMPLETED", 0, 0, 0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> settings.update("ASSET_CHECK", -1, 0, 0)
        );
    }
}
