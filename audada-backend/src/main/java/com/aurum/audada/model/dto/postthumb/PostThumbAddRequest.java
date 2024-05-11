package com.aurum.audada.model.dto.postthumb;

import java.io.Serializable;

import lombok.Data;

/**
 * 帖子点赞请求
 */
@Data
public class PostThumbAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 帖子 id
     */
    private Long postId;
}