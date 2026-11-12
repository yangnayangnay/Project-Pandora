package com.example.project_pandora.ui.ai;

import com.example.project_pandora.R;

public final class MbtiAvatars {
    private MbtiAvatars() {}
    public static int resource(String type) {
        switch (type.toUpperCase(java.util.Locale.ROOT)) {
            case "INTJ": return R.drawable.mbti_intj; case "INTP": return R.drawable.mbti_intp;
            case "ENTJ": return R.drawable.mbti_entj; case "ENTP": return R.drawable.mbti_entp;
            case "INFJ": return R.drawable.mbti_infj; case "INFP": return R.drawable.mbti_infp;
            case "ENFJ": return R.drawable.mbti_enfj; case "ENFP": return R.drawable.mbti_enfp;
            case "ISTJ": return R.drawable.mbti_istj; case "ISFJ": return R.drawable.mbti_isfj;
            case "ESTJ": return R.drawable.mbti_estj; case "ESFJ": return R.drawable.mbti_esfj;
            case "ISTP": return R.drawable.mbti_istp; case "ISFP": return R.drawable.mbti_isfp;
            case "ESTP": return R.drawable.mbti_estp; case "ESFP": return R.drawable.mbti_esfp;
            default: return R.drawable.mbti_four_groups;
        }
    }
}
