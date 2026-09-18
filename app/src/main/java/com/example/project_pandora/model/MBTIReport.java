package com.example.project_pandora.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class MBTIReport implements Serializable {

    @SerializedName("id")
    private Long id;

    @SerializedName("user_id")
    private Long userId;

    @SerializedName("personality_type")
    private String personalityType;

    @SerializedName("answer_data")
    private String answerData;

    @SerializedName("report_content")
    private String reportContent;

    @SerializedName("generated_at")
    private String generatedAt;

    @SerializedName("expire_at")
    private String expireAt;

    public MBTIReport() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getPersonalityType() { return personalityType; }
    public void setPersonalityType(String personalityType) { this.personalityType = personalityType; }
    public String getAnswerData() { return answerData; }
    public void setAnswerData(String answerData) { this.answerData = answerData; }
    public String getReportContent() { return reportContent; }
    public void setReportContent(String reportContent) { this.reportContent = reportContent; }
    public String getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(String generatedAt) { this.generatedAt = generatedAt; }
    public String getExpireAt() { return expireAt; }
    public void setExpireAt(String expireAt) { this.expireAt = expireAt; }

    public String getTypeNickname() {
        if (personalityType == null) return "未知";
        switch (personalityType) {
            case "INTJ": return "建筑师";
            case "INTP": return "逻辑学家";
            case "ENTJ": return "指挥官";
            case "ENTP": return "辩论家";
            case "INFJ": return "提倡者";
            case "INFP": return "调停者";
            case "ENFJ": return "主人公";
            case "ENFP": return "竞选者";
            case "ISTJ": return "物流师";
            case "ISFJ": return "守卫者";
            case "ESTJ": return "总经理";
            case "ESFJ": return "执政官";
            case "ISTP": return "鉴赏家";
            case "ISFP": return "探险家";
            case "ESTP": return "企业家";
            case "ESFP": return "表演者";
            default: return personalityType;
        }
    }
}