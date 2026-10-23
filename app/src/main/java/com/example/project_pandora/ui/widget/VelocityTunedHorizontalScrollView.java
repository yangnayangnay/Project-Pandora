package com.example.project_pandora.ui.widget;

/**
 * R43 REQ-B: 任务面板水平滚动容器（HorizontalScrollView 子类）。
 * 与 VelocityTunedScrollView 成对管理两方向 fling 系数（水平保持原生 1.0——
 * 水平无嵌套协调截断，作为灵敏度基准；垂直系数在伴生类中补偿对齐）。
 */
public class VelocityTunedHorizontalScrollView extends android.widget.HorizontalScrollView {

    public static final float HORIZONTAL_FACTOR = 1.0f;

    public VelocityTunedHorizontalScrollView(android.content.Context context) {
        super(context);
    }

    public VelocityTunedHorizontalScrollView(android.content.Context context, android.util.AttributeSet attrs) {
        super(context, attrs);
    }

    public VelocityTunedHorizontalScrollView(android.content.Context context, android.util.AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void fling(int velocityX) {
        super.fling((int) (velocityX * HORIZONTAL_FACTOR));
    }
}