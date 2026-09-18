package com.example.project_pandora.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class DivinationResult implements Serializable {

    @SerializedName("originalHexagram")
    private String originalHexagram;

    @SerializedName("changedHexagram")
    private String changedHexagram;

    @SerializedName("suitable")
    private String suitable;

    @SerializedName("avoid")
    private String avoid;

    @SerializedName("fortuneLevel")
    private String fortuneLevel;

    @SerializedName("description")
    private String description;

    public DivinationResult() {}

    public String getOriginalHexagram() { return originalHexagram; }
    public void setOriginalHexagram(String originalHexagram) { this.originalHexagram = originalHexagram; }
    public String getChangedHexagram() { return changedHexagram; }
    public void setChangedHexagram(String changedHexagram) { this.changedHexagram = changedHexagram; }
    public String getSuitable() { return suitable; }
    public void setSuitable(String suitable) { this.suitable = suitable; }
    public String getAvoid() { return avoid; }
    public void setAvoid(String avoid) { this.avoid = avoid; }
    public String getFortuneLevel() { return fortuneLevel; }
    public void setFortuneLevel(String fortuneLevel) { this.fortuneLevel = fortuneLevel; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}