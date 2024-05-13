package com.aurum.audada.scoring;

import cn.hutool.json.JSONUtil;
import com.aurum.audada.model.dto.question.QuestionContentDTO;
import com.aurum.audada.model.entity.App;
import com.aurum.audada.model.entity.Question;
import com.aurum.audada.model.entity.ScoringResult;
import com.aurum.audada.model.entity.UserAnswer;
import com.aurum.audada.model.vo.QuestionVO;
import com.aurum.audada.service.QuestionService;
import com.aurum.audada.service.ScoringResultService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * + 自定义测评类应用打分策略
 * +
 * + @Author Li_Jin_Gang
 * + @Date 2024/5/12 0012 13:40
 */

@ScoringStrategyConfig(appType = 1, scoringStrategy = 0)
public class CustomTestScoringStrategy implements ScoringStrategy {

    @Resource
    private QuestionService questionService;

    @Resource
    private ScoringResultService scoringResultService;

    @Override
    public UserAnswer doScore(List<String> choices, App app) throws Exception {
        Long appId = app.getId();
        // 1. 根据 id 查询到题目和题目结果信息
        Question question = questionService.getOne(
                Wrappers.lambdaQuery(Question.class)
                        .eq(Question::getAppId, appId)
        );
        List<ScoringResult> scoringResultList = scoringResultService.list(
                Wrappers.lambdaQuery(ScoringResult.class)
                        .eq(ScoringResult::getAppId, appId)
        );
        // 2. 统计用户每个选择对应的属性个数，如 I = 10 个，E = 5 个
        // 初始化Map，用于存储每个选项、的个数
        Map<String, Integer> optionCount = new HashMap<>();
        QuestionVO questionVO = QuestionVO.objToVo(question);
        List<QuestionContentDTO> questionContent = questionVO.getQuestionContent();
        // 遍历题目列表
        for (QuestionContentDTO questionContentDTO : questionContent) {
            // 遍历答案列表
            for (String answer : choices) {
                // 遍历题目选项
                for (QuestionContentDTO.Option option : questionContentDTO.getOptions()) {
                    // 判断用户选择的答案是否与题目选项相等
                    if (option.getKey().equals(answer)) {
                        // 获得答案的 result 属性
                        String result = option.getResult();
                        // 判断Map中是否已经存在该答案的计数
                        if (!optionCount.containsKey(result)) {
                            optionCount.put(result, 0);
                        }
                        // 增加答案的计数
                        optionCount.put(result, optionCount.get(result) + 1);
                    }
                }
            }
        }
        // 3. 遍历每种评分结果，计算哪个结果的得分更高
        int maxScore = 0;
        ScoringResult maxScoringResult = scoringResultList.get(0);
        // 遍历所有评分结果
        for (ScoringResult scoringResult : scoringResultList) {
            List<String> resultProp = JSONUtil.toList(scoringResult.getResultProp(), String.class);
            // 计算得分 [I, E] -> [10, 5] -> 15
            int score = resultProp.stream()
                    .mapToInt(prop -> optionCount.getOrDefault(prop, 0))
                    .sum();
            // 更新最大得分和对应的结果
            if (score > maxScore) {
                maxScore = score;
                maxScoringResult = scoringResult;
            }
        }
        // 4. 构造返回值，填充答案对象的属性
        UserAnswer userAnswer = new UserAnswer();
        userAnswer.setAppId(appId);
        userAnswer.setAppType(app.getAppType());
        userAnswer.setScoringStrategy(app.getScoringStrategy());
        userAnswer.setChoices(JSONUtil.toJsonStr(choices));
        userAnswer.setResultId(maxScoringResult.getId());
        userAnswer.setResultName(maxScoringResult.getResultName());
        userAnswer.setResultDesc(maxScoringResult.getResultDesc());
        userAnswer.setResultPicture(maxScoringResult.getResultPicture());
        return userAnswer;
    }
}
