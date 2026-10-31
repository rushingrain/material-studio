package cn.learn.lfy.mtr.api;

public record SimulationConfigResponse(
        String failStage,
        long assetCheckDelayMs,
        long simulatingDelayMs,
        long resultPackagingDelayMs
) {
}
