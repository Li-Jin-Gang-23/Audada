package com.aurum.audada.model.dto.question;

import lombok.Data;

import java.io.Serializable;

@Data
public class AiGenerateQuestionRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 题目数
     */
    int questionNumber = 10;
    /**
     * 选项数
     */
    int optionNumber = 2;
    /**
     * id
     */
    private Long appId;

}