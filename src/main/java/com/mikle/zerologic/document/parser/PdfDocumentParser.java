package com.mikle.zerologic.document.parser;

import com.mikle.zerologic.document.DocumentParser;
import com.mikle.zerologic.exception.BusinessException;
import com.mikle.zerologic.exception.ErrorCode;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Component
public class PdfDocumentParser implements DocumentParser {

    @Override
    public boolean supports(String extension, String contentType) {
        return "pdf".equalsIgnoreCase(extension);
    }

    @Override
    public String parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "";
        }

        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            if (document.isEncrypted()) {
                throw new BusinessException(
                        ErrorCode.PARAMS_ERROR,
                        "暂不支持加密或受密码保护的 PDF"
                );
            }

            PDFTextStripper stripper = new PDFTextStripper();
            // 可选：按页面顺序提取
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        } catch (InvalidPasswordException e) {
            throw new BusinessException(
                    ErrorCode.PARAMS_ERROR,
                    "暂不支持加密或受密码保护的 PDF"
            );
        } catch (IOException e) {
            throw new BusinessException(
                    ErrorCode.PARAMS_ERROR,
                    "PDF 文件损坏或无法解析"
            );
        }
    }

}
