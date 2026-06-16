package com.campus.campus_backend.service.info;

import com.campus.campus_backend.domain.UserCanvasBinding;
import org.springframework.stereotype.Service;

@Service
public class TongjiSsoSessionService {
    private final CanvasSessionFetcher canvasSessionFetcher;

    public TongjiSsoSessionService(CanvasSessionFetcher canvasSessionFetcher) {
        this.canvasSessionFetcher = canvasSessionFetcher;
    }

    public CanvasSessionFetcher.LoginResult login(UserCanvasBinding binding) {
        return canvasSessionFetcher.login(binding);
    }

    public CanvasSessionFetcher.LoginResult login(UserCanvasBinding binding, boolean forceRelogin) {
        return canvasSessionFetcher.login(binding, forceRelogin);
    }
}
