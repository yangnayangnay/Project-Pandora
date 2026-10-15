package com.pandora.backend.ai;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
public class AIChannelStateManager {

    public enum Channel { EXTERNAL_AVAILABLE, LOCAL_FALLBACK }

    private volatile Channel currentChannel = Channel.EXTERNAL_AVAILABLE;
    private volatile boolean externalAvailable = true;
    private volatile Long externalRemainingQuota = null;
    private volatile boolean fallbackAvailable = true;
    private volatile String lastSwitchReason = null;
    private volatile Instant lastSwitchTime = null;

    public synchronized Map<String, Object> getState() {
        Map<String, Object> state = new java.util.HashMap<>();
        state.put("currentChannel", currentChannel.name());
        state.put("externalAvailable", externalAvailable);
        state.put("externalRemainingQuota", externalRemainingQuota);
        state.put("fallbackAvailable", fallbackAvailable);
        state.put("lastSwitchReason", lastSwitchReason);
        state.put("lastSwitchTime", lastSwitchTime != null ? lastSwitchTime.toString() : null);
        return state;
    }

    public synchronized void switchToLocal(String reason) {
        this.currentChannel = Channel.LOCAL_FALLBACK;
        this.externalAvailable = false;
        this.lastSwitchReason = reason;
        this.lastSwitchTime = Instant.now();
    }

    public synchronized void switchToExternal() {
        this.currentChannel = Channel.EXTERNAL_AVAILABLE;
        this.externalAvailable = true;
        this.lastSwitchReason = "额度恢复/探测成功";
        this.lastSwitchTime = Instant.now();
    }

    public synchronized void markExternalUnavailable(String reason) {
        this.externalAvailable = false;
        this.lastSwitchReason = reason;
        this.lastSwitchTime = Instant.now();
    }

    public synchronized void updateQuota(Long remaining) {
        this.externalRemainingQuota = remaining;
    }

    public Channel getCurrentChannel() {
        return currentChannel;
    }

    public boolean isExternalAvailable() {
        return externalAvailable;
    }
}