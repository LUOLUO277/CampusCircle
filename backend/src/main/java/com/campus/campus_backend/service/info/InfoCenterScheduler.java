package com.campus.campus_backend.service.info;

import com.campus.campus_backend.domain.SubscriptionSource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InfoCenterScheduler {
    private final InfoCenterService infoCenterService;
    private final CanvasBindingService canvasBindingService;

    public InfoCenterScheduler(InfoCenterService infoCenterService, CanvasBindingService canvasBindingService) {
        this.infoCenterService = infoCenterService;
        this.canvasBindingService = canvasBindingService;
    }

    // 加 initialDelay，避免应用启动时与数据初始化/清理并发导致外键约束异常
    @Scheduled(fixedDelay = 1800000, initialDelay = 60000)
    public void pollSources() {
        for (SubscriptionSource source : infoCenterService.listActiveSources()) {
            try {
                infoCenterService.fetchSource(source.getId());
            } catch (Exception ignore) {
            }
        }
    }

    @Scheduled(fixedDelay = 3600000, initialDelay = 600000)
    public void pollCanvasBindings() {
        canvasBindingService.syncActiveBindings();
    }
}
