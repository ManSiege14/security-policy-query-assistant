package com.vectordb.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class PdfService {

    public String extractText(MultipartFile file) throws IOException {

        try (PDDocument document =
                     Loader.loadPDF(file.getBytes())) {

            System.out.println("Pages = "
                    + document.getNumberOfPages());

            PDFTextStripper stripper =
                    new PDFTextStripper();

            String text = stripper.getText(document);

            System.out.println("Text length = "
                    + text.length());

            System.out.println(
                    text.substring(
                            0,
                            Math.min(500, text.length())
                    )
            );

            return text;
        }
    }
}