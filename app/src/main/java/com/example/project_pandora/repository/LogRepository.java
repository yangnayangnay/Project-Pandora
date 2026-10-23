package com.example.project_pandora.repository;

import android.content.Context;

import com.example.project_pandora.core.exception.BusinessException;
import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.data.remote.LogApi;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class LogRepository {

    private final LogApi logApi;

    public LogRepository(Context context) {
        logApi = NetworkClient.getInstance(context).createService(LogApi.class);
    }

    public Observable<Map<String, Object>> createLog(String workItem, String workContent, String completionStatus,
                                                       Double timeCost, String logDate) {
        Map<String, Object> request = new HashMap<>();
        request.put("workItem", workItem);
        request.put("workContent", workContent == null ? "" : workContent);
        request.put("completionStatus", completionStatus);
        request.put("timeCost", timeCost);
        request.put("logDate", logDate);

        return logApi.createLog(request)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new BusinessException(response.getCode(), response.getMessage());
                });
    }

    public Observable<List<Map<String, Object>>> listLogs(String startDate, String endDate, String keyword) {
        return logApi.listLogs(startDate, endDate, keyword)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        List<Map<String, Object>> list = response.getData();
                        for (int i = 0; i < list.size(); i++) {
                            list.set(i, normalizeKeys(list.get(i)));
                        }
                        return list;
                    }
                    throw new BusinessException(response.getCode(), response.getMessage());
                });
    }

    public Observable<Map<String, Object>> getLogDetail(Long logId) {
        return logApi.getLogDetail(logId)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return normalizeKeys(response.getData());
                    }
                    throw new BusinessException(response.getCode(), response.getMessage());
                });
    }

    public Observable<Map<String, Object>> getLogStats() {
        return logApi.getLogStats()
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new BusinessException(response.getCode(), response.getMessage());
                });
    }

    // Round26 缺陷闭环：RxJava3 map 返回 null 抛 NPE，Void 方法统一改 Boolean 哨兵（调用方忽略元素）
    public Observable<Boolean> updateTop10(List<Map<String, Object>> top10List) {
        return logApi.updateTop10(top10List)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess()) return true;
                    throw new BusinessException(response.getCode(), response.getMessage());
                });
    }

    public Observable<List<Map<String, Object>>> getTop10(String workType) {
        return logApi.getTop10(workType)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new BusinessException(response.getCode(), response.getMessage());
                });
    }

    // Round26 B1（4.3 定稿方案）：个人十大增/改/删走 PersonalTop10Controller 端点（owner 校验 4046）
    public Observable<Map<String, Object>> injectTop10(String content, int rankOrder) {
        Map<String, Object> request = new HashMap<>();
        request.put("content", content);
        request.put("rankOrder", rankOrder);
        return logApi.injectTop10(request)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new BusinessException(response.getCode(), response.getMessage());
                });
    }

    // RxJava3 ObservableMap 对 null 返回值抛 NPE，map 必须返回非 null（Boolean 哨兵，调用方 concat+ignoreElements 忽略元素）
    public Observable<Boolean> updateTop10Content(Long id, String content) {
        Map<String, Object> request = new HashMap<>();
        request.put("content", content);
        return logApi.updateTop10Content(id, request)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess()) return true;
                    throw new BusinessException(response.getCode(), response.getMessage());
                });
    }

    public Observable<Boolean> deleteTop10(Long id) {
        return logApi.deleteTop10(id)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess()) return true;
                    throw new BusinessException(response.getCode(), response.getMessage());
                });
    }

    public Observable<Map<String, Object>> editLog(Long logId, String workItem, String workContent, String completionStatus,
                                                      Double timeCost, String logDate) {
        Map<String, Object> request = new HashMap<>();
        if (workItem != null) request.put("workItem", workItem);
        if (workContent != null) request.put("workContent", workContent);
        if (completionStatus != null) request.put("completionStatus", completionStatus);
        if (timeCost != null) request.put("timeCost", timeCost);
        if (logDate != null) request.put("logDate", logDate);

        return logApi.editLog(logId, request)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new BusinessException(response.getCode(), response.getMessage());
                });
    }

    // Round26 缺陷闭环：RxJava3 map 返回 null 抛 NPE（删除 HTTP 成功但 finish 不执行、误报删除失败），改 Boolean 哨兵
    public Observable<Boolean> deleteLog(Long logId) {
        return logApi.deleteLog(logId)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess()) return true;
                    throw new BusinessException(response.getCode(), response.getMessage());
                });
    }

    public Observable<List<Map<String, Object>>> getSubordinateLogs(Long subordinateId, String startDate, String endDate, String keyword) {
        return logApi.getSubordinateLogs(subordinateId, startDate, endDate, keyword)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        List<Map<String, Object>> list = response.getData();
                        for (int i = 0; i < list.size(); i++) {
                            list.set(i, normalizeKeys(list.get(i)));
                        }
                        return list;
                    }
                    throw new BusinessException(response.getCode(), response.getMessage());
                });
    }

    public Observable<List<Map<String, Object>>> getSubordinates() {
        return logApi.getSubordinates()
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        List<Map<String, Object>> list = response.getData();
                        for (int i = 0; i < list.size(); i++) {
                            list.set(i, normalizeKeys(list.get(i)));
                        }
                        return list;
                    }
                    throw new BusinessException(response.getCode(), response.getMessage());
                });
    }

    public Observable<Map<String, Object>> autoGenerateLogs() {
        return logApi.autoGenerateLogs()
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new BusinessException(response.getCode(), response.getMessage());
                });
    }

    public Observable<Map<String, Object>> generateDraft() {
        return logApi.generateDraft()
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return normalizeKeys(response.getData());
                    }
                    throw new BusinessException(response.getCode(), response.getMessage());
                });
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> normalizeKeys(Map<String, Object> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                value = normalizeKeys((Map<String, Object>) value);
            } else if (value instanceof List) {
                List<Object> list = (List<Object>) value;
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i) instanceof Map) {
                        list.set(i, normalizeKeys((Map<String, Object>) list.get(i)));
                    }
                }
            }
            result.put(entry.getKey().toLowerCase(), value);
        }
        return result;
    }
}