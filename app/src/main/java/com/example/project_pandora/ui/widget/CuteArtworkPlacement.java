package com.example.project_pandora.ui.widget;

/** Match business photos: cover the viewport without letterbox strips or stretching the artwork. */
final class CuteArtworkPlacement {
    private CuteArtworkPlacement() {}
    static float scale(int viewportWidth, int viewportHeight, int artworkWidth, int artworkHeight) {
        if (viewportWidth <= 0 || viewportHeight <= 0 || artworkWidth <= 0 || artworkHeight <= 0) return 0;
        return Math.max(viewportWidth / (float) artworkWidth, viewportHeight / (float) artworkHeight);
    }
}
