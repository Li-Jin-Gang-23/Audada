package com.aurum.audada.common;

import java.io.Serializable;

import lombok.Data;

/**
 * 删除请求
 */
@Data
public class DeleteRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    private Long id;
}