package com.aurum.audada.scoring;

import com.aurum.audada.model.entity.App;
import com.aurum.audada.model.entity.UserAnswer;

import java.util.List;

/**
 * 评分策略
 */
public interface ScoringStrategy {

    /**
     * 执行评分
     */
    UserAnswer doScore(List<String> choices, App app) throws Exception;
}