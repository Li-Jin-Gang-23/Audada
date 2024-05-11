package com.aurum.audada.model.dto.file;

import java.io.Serializable;

import lombok.Data;

/**
 * 文件上传请求
 */
@Data
public class UploadFileRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 业务
     */
    private String biz;
}