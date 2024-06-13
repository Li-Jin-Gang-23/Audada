package com.aurum.audada.scoring;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.aurum.audada.manager.AiManager;
import com.aurum.audada.model.dto.question.QuestionAnswerDTO;
import com.aurum.audada.model.dto.question.QuestionContentDTO;
import com.aurum.audada.model.entity.App;
import com.aurum.audada.model.entity.Question;
import com.aurum.audada.model.entity.UserAnswer;
import com.aurum.audada.model.vo.QuestionVO;
import com.aurum.audada.service.QuestionService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.aurum.audada.constant.QuestionSystemConstant.AI_TEST_SCORING_SYSTEM_MESSAGE;

/**
 * + AI测评类应用打分策略
 * +
 * + @Author Li_Jin_Gang
 * + @Date 2024/5/12 0012 13:40
 */

@ScoringStrategyConfig(appType = 1, scoringStrategy = 1)
public class AiTestScoringStrategy implements ScoringStrategy {

    private static final String AI_ANSWER_LOCK = "AI_ANSWER_LOCK";
    private final Cache<String, String> answerCacheMap =
            Caffeine.newBuilder().initialCapacity(1024)
                    // 缓存5分钟移除
                    .expireAfterAccess(5L, TimeUnit.MINUTES)
                    .build();
    @Resource
    private QuestionService questionService;
    @Resource
    private AiManager aiManager;
    @Resource
    private RedissonClient redissonClient;

    private String getAiTestScoringUserMessage(App app, List<QuestionContentDTO> questionContentDTOList, List<String> choices) {
        StringBuilder userMessage = new StringBuilder();
        userMessage.append(app.getAppName()).append("\n");
        userMessage.append(app.getAppDesc()).append("\n");
        List<QuestionAnswerDTO> questionAnswerDTOList = new ArrayList<>();
        for (int i = 0; i < questionContentDTOList.size(); i++) {
            QuestionAnswerDTO questionAnswerDTO = new QuestionAnswerDTO();
            questionAnswerDTO.setTitle(questionContentDTOList.get(i).getTitle());
            questionAnswerDTO.setUserAnswer(choices.get(i));
            questionAnswerDTOList.add(questionAnswerDTO);
        }
        userMessage.append(JSONUtil.toJsonStr(questionAnswerDTOList));
        return userMessage.toString();
    }

    @Override
    public UserAnswer doScore(List<String> choices, App app) throws Exception {
        Long appId = app.getId();
        String choicesStr = JSONUtil.toJsonStr(choices);
        // 生成缓存 key
        String key = buildCacheKey(appId, choicesStr);
        String answerJson = answerCacheMap.getIfPresent(key);
        // 命中缓存则直接返回结果
        if (StrUtil.isNotBlank(answerJson)) {
            UserAnswer userAnswer = JSONUtil.toBean(answerJson, UserAnswer.class);
            userAnswer.setAppId(appId);
            userAnswer.setAppType(app.getAppType());
            userAnswer.setScoringStrategy(app.getScoringStrategy());
            userAnswer.setChoices(choicesStr);
            return userAnswer;
        }
        // 定义锁
        RLock lock = redissonClient.getLock(AI_ANSWER_LOCK + key);
        try {
            // 竞争分布式锁，等待 3 秒，15 秒自动释放
            boolean res = lock.tryLock(3, 15, TimeUnit.SECONDS);
            if (res) {
                // 抢到锁的业务才能执行 AI 调用
                // 1. 根据 id 查询到题目
                Question question = questionService.getOne(
                        Wrappers.lambdaQuery(Question.class).eq(Question::getAppId, appId)
                );
                QuestionVO questionVO = QuestionVO.objToVo(question);
                List<QuestionContentDTO> questionContent = questionVO.getQuestionContent();
                // 2. 调用 AI 获取结果
                // 封装 Prompt
                String userMessage = getAiTestScoringUserMessage(app, questionContent, choices);
                // AI 生成
                String result = aiManager.doSyncStableRequest(AI_TEST_SCORING_SYSTEM_MESSAGE, userMessage);
                // 结果处理
                int start = result.indexOf("{");
                int end = result.lastIndexOf("}");
                String json = result.substring(start, end + 1);
                // 3. 缓存 AI 结果
                answerCacheMap.put(key, json);
                // 4. 构造返回值，填充答案对象的属性
                UserAnswer userAnswer = JSONUtil.toBean(json, UserAnswer.class);
                userAnswer.setAppId(appId);
                userAnswer.setAppType(app.getAppType());
                userAnswer.setScoringStrategy(app.getScoringStrategy());
                userAnswer.setChoices(choicesStr);
                return userAnswer;
            } else {
                // 未抢到锁，返回空
                return null;
            }
        } finally {
            if (lock != null && lock.isLocked()) {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
    }

    private String buildCacheKey(Long appId, String choicesStr) {
        return DigestUtil.md5Hex(appId + ":" + choicesStr);
    }
}
