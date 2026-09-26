package com.mikle.zerologic.app.download.service;

import jakarta.servlet.http.HttpServletResponse;

public interface ProjectDownloadService {

    /**
     * 下载项目为压缩包
     */
    void downloadProjectAsZip(String projectPath, String downloadFileName, HttpServletResponse response);
}
