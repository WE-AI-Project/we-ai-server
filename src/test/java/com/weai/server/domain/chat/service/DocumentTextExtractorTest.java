package com.weai.server.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class DocumentTextExtractorTest {

	private final DocumentTextExtractor extractor = new DocumentTextExtractor();

	@Test
	void extractsTextFromPdf() throws Exception {
		byte[] content;
		try (PDDocument document = new PDDocument();
			 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			PDPage page = new PDPage();
			document.addPage(page);
			try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
				stream.beginText();
				stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
				stream.newLineAtOffset(50, 700);
				stream.showText("PDF project briefing text");
				stream.endText();
			}
			document.save(output);
			content = output.toByteArray();
		}

		String text = extractor.extract(file("briefing.pdf", "application/pdf", content), "pdf");

		assertThat(text).contains("PDF project briefing text");
	}

	@Test
	void extractsTextFromDocx() throws Exception {
		byte[] content;
		try (XWPFDocument document = new XWPFDocument();
			 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			document.createParagraph().createRun().setText("DOCX project briefing text");
			document.write(output);
			content = output.toByteArray();
		}

		String text = extractor.extract(
			file("briefing.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", content),
			"docx"
		);

		assertThat(text).contains("DOCX project briefing text");
	}

	@Test
	void extractsTextFromPptx() throws Exception {
		byte[] content;
		try (XMLSlideShow slideShow = new XMLSlideShow();
			 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			XSLFSlide slide = slideShow.createSlide();
			slide.createTextBox().setText("PPTX project briefing text");
			slideShow.write(output);
			content = output.toByteArray();
		}

		String text = extractor.extract(
			file("briefing.pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation", content),
			"pptx"
		);

		assertThat(text).contains("PPTX project briefing text");
	}

	private MockMultipartFile file(String name, String contentType, byte[] content) {
		return new MockMultipartFile("file", name, contentType, content);
	}
}
