package sepoa.seal.util;

import java.awt.Color;
import java.io.File;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;

public class PDFUtils {
	
	final public static int LEFT_BOTTOM = 0;
	
	final public static int RIGHT_TOP = 1;	
	
	public static void writeTextToPDF(File srcFile, File targetFile, String text, float fontSize, int position) throws Exception {
		// PDF-BOX 라이브러리를 활용한 PDF문서에 텍스트 추가 샘플 코드

		// 텍스트의 불투명도 값
		float opacity = 1f;

		// 텍스트의 회전 각도 값
		float degree = 0f;

		// PDF 문서 파일 로드
		PDDocument document = PDDocument.load(srcFile);

		// 텍스트에서 사용할 폰트 객체 생성
		PDFont font = null;

		// PDF 문서의 기본 폰트 로드
		font = PDType1Font.HELVETICA; // PDType1Font 클래스 내부 static 변수 참조

		// OTF 타입의 폰트 로드
//		File OTFFont = new File("");
//		OTFParser otfParser = new OTFParser();
//		OpenTypeFont otf = otfParser.parse(OTFFont);
//		font = PDType0Font.load(document, otf, false);

		// TTF 타입의 폰트 로드
//		File TTFFont = new File("");
//		TTFParser ttfParser = new TTFParser();
//		TrueTypeFont ttf = ttfParser.parse(TTFFont);
//		font = PDType0Font.load(document, ttf, false);

		// 특정 페이지만 작업할 경우
//		PDPage page = document.getPage(0);

		// 전체 페이지 작업의 경우
		for (PDPage page : document.getPages()) {
			
			// 텍스트(컨텐츠)를 그릴 페이지의 스트림 객체 생성
			PDPageContentStream contentStream = new PDPageContentStream(document, page, AppendMode.APPEND, true, // compress
																													// content
					true // reset context(matrix)
			);

			// 스트림의 현재 상태 저장(좌표 원점, 색상, 폰트 등등)
			contentStream.saveGraphicsState();

			// 텍스트의 불투명도 지정 (0 ~ 1)
			if (opacity != 1f) {
				PDExtendedGraphicsState graphics = new PDExtendedGraphicsState();
				graphics.setNonStrokingAlphaConstant(opacity);
				contentStream.setGraphicsStateParameters(graphics);
			}

			// 텍스트의 색상 지정
			contentStream.setNonStrokingColor(Color.BLACK); // Color 객체 프리셋으로 설정 할 경우
//			contentStream.setNonStrokingColor(111f, 222f, 222f);		//RGB 색상 설정 할 경우
//			contentStream.setNonStrokingColor(0.5f, 0.5f, 0.1f, 0.2f);	//CMYK 색상 설정 할 경우

			// 폰트가 적용된 텍스트의 실제 렌더링 크기
			float textWidth = (font.getStringWidth(text) / 1000) * fontSize;
			float textHeight = (font.getFontDescriptor().getCapHeight() / 1000) * fontSize;

			float positionX = 0f;
			float positionY = 0f;
			switch (position) {
			case LEFT_BOTTOM:
				positionX = 0f;
				positionY = 0f;
				break;
			case RIGHT_TOP:
				float pageWidth = page.getMediaBox().getWidth();
				float pageHeight = page.getMediaBox().getHeight();
				positionX = pageWidth - textWidth - 10;
				positionY = pageHeight - textHeight - 12;
				break;
			default:
			}
			
			// 텍스트의 회전각도 지정
			Matrix matrix = Matrix.getTranslateInstance(positionX + (textWidth / 2), positionY + (textHeight / 2));
			matrix.rotate(Math.toRadians(degree));

			// 텍스트 삽입 부분
			contentStream.beginText(); // 텍스트 삽입 시작
			contentStream.setFont(font, fontSize); // 폰트 및 크기 지정
			contentStream.setTextMatrix(matrix); // 텍스트가 삽입될 좌표체계 원점 및 회전각 지정
			contentStream.newLineAtOffset(-(textWidth / 2), -(textHeight / 2)); // 텍스트가 위치할 좌표 지정
			contentStream.showText(text); // 텍스트 문구 지정
			contentStream.endText(); // 텍스트 삽입 종료

			// 스트림의 저장된 초기 상태 복구
			contentStream.restoreGraphicsState();

			contentStream.close();
		}

		// PDF 수정 작업이 끝난 후 결과물 저장
		document.save(targetFile);

		// 로드한 PDF 문서 객체 닫기
		document.close();
	}
}
