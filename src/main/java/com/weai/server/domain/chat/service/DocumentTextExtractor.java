package com.weai.server.domain.chat.service;

import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xslf.extractor.XSLFExtractor;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Component
public class DocumentTextExtractor {

	private static final int MAX_EXTRACTED_TEXT_LENGTH = 50_000;

	public String extract(MultipartFile file, String extension) {
		try {
			String text = switch (extension) {
				case "txt", "md" -> new String(file.getBytes(), StandardCharsets.UTF_8);
				case "pdf" -> extractPdf(file);
				case "docx" -> extractDocx(file);
				case "pptx" -> extractPptx(file);
				default -> null;
			};
			return normalize(text);
		} catch (IOException | RuntimeException exception) {
			throw new ApiException(ErrorCode.DOCUMENT_UPLOAD_FAILED, "Failed to extract text from the document.");
		}
	}

	private String extractPdf(MultipartFile file) throws IOException {
		try (PDDocument document = Loader.loadPDF(file.getBytes())) {
			return new PDFTextStripper().getText(document);
		}
	}

	private String extractDocx(MultipartFile file) throws IOException {
		try (XWPFDocument document = new XWPFDocument(file.getInputStream());
			 XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
			return extractor.getText();
		}
	}

	private String extractPptx(MultipartFile file) throws IOException {
		try (XMLSlideShow slideShow = new XMLSlideShow(file.getInputStream());
			 XSLFExtractor extractor = new XSLFExtractor(slideShow)) {
			return extractor.getText();
		}
	}

	private String normalize(String text) {
		if (!StringUtils.hasText(text)) {
			return null;
		}
		String normalized = text.trim();
		return normalized.length() > MAX_EXTRACTED_TEXT_LENGTH
			? normalized.substring(0, MAX_EXTRACTED_TEXT_LENGTH)
			: normalized;
	}
}
