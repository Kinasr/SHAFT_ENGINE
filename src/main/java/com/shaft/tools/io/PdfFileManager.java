package com.shaft.tools.io;

import com.shaft.cli.FileActions;
import io.github.shafthq.shaft.tools.io.FailureReporter;
import io.github.shafthq.shaft.tools.support.JavaHelper;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.io.RandomAccessBufferedFileInputStream;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;

@SuppressWarnings("unused")
public class PdfFileManager {

    private final File file;
    private RandomAccessBufferedFileInputStream stream = null;
    private PDFParser parser = null;
    private COSDocument cosDoc = null;
    private PDFTextStripper strip = null;

    public PdfFileManager(String folderName, String fileName, int numberOfRetries) {

        boolean doesFileExist = FileActions.getInstance().doesFileExist(folderName, fileName, numberOfRetries);

        file = new File(FileActions.getInstance().getAbsolutePath(folderName, fileName));

        if (!doesFileExist) {
            FailureReporter.fail("Couldn't find the provided file [" + file
                    + "]. It might need to wait more to download or the path isn't correct");
        }
    }

    public PdfFileManager(String pdfFilePath) {
        pdfFilePath = JavaHelper.appendTestDataToRelativePath(pdfFilePath);
        boolean doesFileExist = FileActions.getInstance().doesFileExist(pdfFilePath);
        file = new File(FileActions.getInstance().getAbsolutePath(pdfFilePath));
        if (!doesFileExist) {
            FailureReporter.fail("Couldn't find the provided file [" + file
                    + "]. It might need to wait more to download or the path isn't correct");
        }
    }

    public String readFileContent() {
        return PdfFileManager.readFileContent(file.getPath());
    }

    /**
     * Read PDF file content given relative path and optionally delete the file after reading it
     *
     * @param relativeFilePath       relative path to the PDF file
     * @param deleteFileAfterReading optional boolean to delete the file after reading it or not, default is to leave the file as is
     * @return a string value representing the entire content of the pdf file
     */
    public static String readFileContent(String relativeFilePath, boolean... deleteFileAfterReading) {
        if (FileActions.getInstance().doesFileExist(relativeFilePath)) {
            try {
                var randomAccessBufferedFileInputStream = new RandomAccessBufferedFileInputStream(new File(FileActions.getInstance().getAbsolutePath(relativeFilePath)));
                var pdfParser = new PDFParser(randomAccessBufferedFileInputStream);
                pdfParser.parse();
                var pdfTextStripper = new PDFTextStripper();
                pdfTextStripper.setSortByPosition(true);
                var fileContent = pdfTextStripper.getText(new PDDocument(pdfParser.getDocument()));
                randomAccessBufferedFileInputStream.close();

                if (deleteFileAfterReading != null
                        && deleteFileAfterReading.length > 0
                        && deleteFileAfterReading[0]) {
                    FileActions.getInstance().deleteFile(relativeFilePath);
                }
                return fileContent;
            } catch (java.io.IOException rootCauseException) {
                FailureReporter.fail(PdfFileManager.class, "Failed to read this PDF file [" + relativeFilePath + "].", rootCauseException);
            }

        } else {
            FailureReporter.fail("This PDF file [" + relativeFilePath + "] doesn't exist.");
        }
        return "";
    }

    /**
     * @param startPageNumber                 the starting page for the document to
     *                                        be validated
     * @param endPageNumber                   the ending page for the document to be
     *                                        validated
     * @param deleteFileAfterValidationStatus the status of deleting the file after
     *                                        get the document text
     * @return returns the pdf content in string so that can be validated
     */
    public String readPDFContentFromDownloadedPDF(int startPageNumber, int endPageNumber,
                                                  DeleteFileAfterValidationStatus deleteFileAfterValidationStatus) {

        stream = readFileInputStream(file);
        parser = parseStreamDocument(stream);

        cosDoc = getParsedDocument(parser);
        String content = getPdfText(cosDoc, startPageNumber, endPageNumber);
        closeStreamAndDeleteFile(file, stream, deleteFileAfterValidationStatus);

        return content;
    }

    public String readPDFContentFromDownloadedPDF(DeleteFileAfterValidationStatus deleteFileAfterValidationStatus) {

        stream = readFileInputStream(file);
        parser = parseStreamDocument(stream);

        cosDoc = getParsedDocument(parser);
        String content = getPdfText(cosDoc);
        closeStreamAndDeleteFile(file, stream, deleteFileAfterValidationStatus);

        return content;
    }

    private RandomAccessBufferedFileInputStream readFileInputStream(File file) {
        try {
            stream = new RandomAccessBufferedFileInputStream(file);
        } catch (IOException rootCauseException) {
            FailureReporter.fail(PdfFileManager.class, "Couldn't read the data from the provided file [" + file + "].", rootCauseException);
        }
        return stream;
    }

    private PDFParser parseStreamDocument(RandomAccessBufferedFileInputStream stream) {
        try {
            parser = new PDFParser(stream);
            parser.parse();
        } catch (IOException rootCauseException) {
            FailureReporter.fail(PdfFileManager.class, "Couldn't parse the stream that opened the document to be prepared to populate the COSDocument object.", rootCauseException);
        }
        return parser;
    }

    private COSDocument getParsedDocument(PDFParser parser) {
        try {
            cosDoc = parser.getDocument();
        } catch (IOException rootCauseException) {
            FailureReporter.fail(PdfFileManager.class, "Couldn't get the document that was parsed. Check that the document parsed before get the document.", rootCauseException);
        }
        return cosDoc;
    }

    private String getPdfText(COSDocument cosDoc, int startPageNumber, int endPageNumber) {
        try {
            strip = new PDFTextStripper();
            // By default, text extraction is done in the same sequence as the text in the
            // PDF page content stream. PDF is a graphic format, not a text format, and
            // unlike HTML, it has no requirements that text one on page be rendered in a
            // certain order. The order is the one that was determined by the software that
            // created the PDF
            // To get text sorted from left to right and top to bottom
            strip.setSortByPosition(true);
        } catch (IOException rootCauseException) {
            FailureReporter.fail(PdfFileManager.class, "Couldn't load PDFTextStripper properties.", rootCauseException);
        }

        strip.setStartPage(startPageNumber);
        strip.setEndPage(endPageNumber);
        PDDocument pdDoc = new PDDocument(cosDoc);

        String content = null;
        try {
            content = strip.getText(pdDoc);
        } catch (IOException rootCauseException) {
            FailureReporter.fail(PdfFileManager.class, "Couldn't get document text. Document state is invalid or it is encrypted.", rootCauseException);
        }
        return content;
    }

    private String getPdfText(COSDocument cosDoc) {
        try {
            strip = new PDFTextStripper();
            strip.setSortByPosition(true);
        } catch (IOException rootCauseException) {
            FailureReporter.fail(PdfFileManager.class, "Couldn't load PDFTextStripper properties.", rootCauseException);
        }

        PDDocument pdDoc = new PDDocument(cosDoc);

        String content = null;
        try {
            content = strip.getText(pdDoc);
        } catch (IOException rootCauseException) {
            FailureReporter.fail(PdfFileManager.class, "Couldn't get document text. Document state is invalid or it is encrypted.", rootCauseException);
        }
        return content;
    }

    private void closeStreamAndDeleteFile(File file, RandomAccessBufferedFileInputStream stream,
                                          DeleteFileAfterValidationStatus deleteFileAfterValidation) {
        try {
            stream.close();
        } catch (IOException rootCauseException) {
            FailureReporter.fail(PdfFileManager.class, "Couldn't close the stream, check if it already opened.", rootCauseException);
        }

        // Delete the file from target folder for next run

        if (deleteFileAfterValidation == DeleteFileAfterValidationStatus.TRUE) {
            try {
                FileUtils.forceDelete(file);
            } catch (IOException rootCauseException) {
                FailureReporter.fail(PdfFileManager.class, "Couldn't find the file, File directory may be null or file is not found.", rootCauseException);
            }
        }

    }

    @SuppressWarnings("unused")
    public enum DeleteFileAfterValidationStatus {
        TRUE, FALSE
    }

    // Will implement same for preview pdf file
}
