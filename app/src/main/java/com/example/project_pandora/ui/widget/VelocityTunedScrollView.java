package com.example.project_pandora.ui.widget;

/**
 * R43 REQ-B: 任务面板垂直滚动容器（NestedScrollView 子类）。
 * 根因：NSV 嵌套滚动协调（BottomSheet 父级拦截垂直 fling 用于展开/收起协调）
 * 导致垂直惯性弱于内层 HSV 的原生水平 fling——上下与左右灵敏度不一致。
 * 修复：fling() 起始速度乘系数补偿（VERTICAL_FACTOR=1.0 为原生行为，>1 增强惯性）。
 */
public class VelocityTunedScrollView extends androidx.core.widget.NestedScrollView {

    public static final float VERTICAL_FACTOR = 1.45f;

    public VelocityTunedScrollView(android.content.Context context) {
        super(context);
    }

    public VelocityTunedScrollView(android.content.Context context, android.util.AttributeSet attrs) {
        super(context, attrs);
    }

    public VelocityTunedScrollView(android.content.Context context, android.util.AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void fling(int velocityY) {
        super.fling((int) (velocityY * VERTICAL_FACTOR));
    }
}