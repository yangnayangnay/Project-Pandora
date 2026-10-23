package com.example.project_pandora.core.network;

/**
 * R44 REQ-A: 服务端列表条目（不可变值对象）。
 * url 恒为归一化形态（构造时经 ServerUrlManager.normalizeBaseUrl 校验）；
 * enabled=启停标志（false 时故障转移扫描跳过）；顺序由列表位置表达（不在字段内）。
 */
public class ServerEntry {

    private final String url;
    private final boolean enabled;

    public ServerEntry(String normalizedUrl, boolean enabled) {
        this.url = normalizedUrl;
        this.enabled = enabled;
    }

    public String getUrl() {
        return url;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServerEntry)) return false;
        return url.equals(((ServerEntry) o).url);
    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }
}