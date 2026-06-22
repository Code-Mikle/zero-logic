package com.mikle.zerologic.document;

import org.springframework.web.multipart.MultipartFile;

public interface DocumentParser {

    boolean supports(String extension, String contentType);

    String parse(MultipartFile file);
}
