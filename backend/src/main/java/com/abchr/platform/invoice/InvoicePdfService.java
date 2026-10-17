package com.abchr.platform.invoice;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class InvoicePdfService {

    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 9);
    private static final Font SMALL = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);
    private static final Color HEADER_BG = new Color(0xEE, 0xF0, 0xF4);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    public byte[] render(Invoice inv) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();
            Paragraph title = new Paragraph("TAX INVOICE", TITLE);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            doc.add(title);

            PdfPTable parties = new PdfPTable(2);
            parties.setWidthPercentage(100);
            parties.addCell(partyCell("From", inv.sellerName, inv.sellerAddress, inv.sellerState, inv.sellerGstin,
                    inv.sellerPan));
            parties.addCell(partyCell("Bill to", inv.buyerName, inv.buyerAddress, inv.buyerState, inv.buyerGstin,
                    null));
            doc.add(parties);

            PdfPTable meta = new PdfPTable(4);
            meta.setWidthPercentage(100);
            meta.setSpacingBefore(8);
            meta.addCell(metaCell("Invoice no.", inv.number));
            meta.addCell(metaCell("Invoice date", DAY.format(inv.invoiceDate)));
            meta.addCell(metaCell("Due date", DAY.format(inv.dueDate)));
            meta.addCell(metaCell("SAC", inv.sacCode));
            doc.add(meta);

            PdfPTable lines = new PdfPTable(new float[] {5f, 2f});
            lines.setWidthPercentage(100);
            lines.setSpacingBefore(12);
            lines.addCell(headCell("Description", Element.ALIGN_LEFT));
            lines.addCell(headCell("Amount (Rs.)", Element.ALIGN_RIGHT));
            lines.addCell(bodyCell(inv.description + "\nCandidate: " + inv.candidateName + "  |  Position: "
                    + inv.jobTitle + " (" + inv.jobCode + ")  |  Joined: " + DAY.format(inv.joiningDate),
                    Element.ALIGN_LEFT, false));
            lines.addCell(bodyCell(money(inv.baseAmount), Element.ALIGN_RIGHT, false));
            if (inv.cgstAmount.signum() > 0) {
                lines.addCell(bodyCell("CGST @ " + rate(inv.gstRate.divide(new BigDecimal("2"))) + "%",
                        Element.ALIGN_RIGHT, false));
                lines.addCell(bodyCell(money(inv.cgstAmount), Element.ALIGN_RIGHT, false));
                lines.addCell(bodyCell("SGST @ " + rate(inv.gstRate.divide(new BigDecimal("2"))) + "%",
                        Element.ALIGN_RIGHT, false));
                lines.addCell(bodyCell(money(inv.sgstAmount), Element.ALIGN_RIGHT, false));
            }
            if (inv.igstAmount.signum() > 0) {
                lines.addCell(bodyCell("IGST @ " + rate(inv.gstRate) + "%", Element.ALIGN_RIGHT, false));
                lines.addCell(bodyCell(money(inv.igstAmount), Element.ALIGN_RIGHT, false));
            }
            lines.addCell(bodyCell("Total", Element.ALIGN_RIGHT, true));
            lines.addCell(bodyCell(money(inv.totalAmount), Element.ALIGN_RIGHT, true));
            doc.add(lines);

            Paragraph words = new Paragraph("Amount in words: " + AmountInWords.rupees(inv.totalAmount), NORMAL);
            words.setSpacingBefore(8);
            doc.add(words);

            if (inv.tdsAmount.signum() > 0) {
                Paragraph tds = new Paragraph("TDS @ " + rate(inv.tdsRate) + "% on Rs. " + money(inv.baseAmount)
                        + " = Rs. " + money(inv.tdsAmount) + " may be deducted at payment. Net receivable: Rs. "
                        + money(inv.netReceivable), SMALL);
                tds.setSpacingBefore(4);
                doc.add(tds);
            }

            if (inv.sellerBank != null && !inv.sellerBank.isBlank()) {
                Paragraph bank = new Paragraph("Bank details\n" + inv.sellerBank, NORMAL);
                bank.setSpacingBefore(14);
                doc.add(bank);
            }

            Paragraph terms = new Paragraph("Payment due by " + DAY.format(inv.dueDate)
                    + ". This is a computer generated invoice.", SMALL);
            terms.setSpacingBefore(14);
            doc.add(terms);
            doc.close();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Could not generate the invoice PDF", ex);
        }
        return out.toByteArray();
    }

    private static PdfPCell partyCell(String label, String name, String address, String state, String gstin,
            String pan) {
        StringBuilder text = new StringBuilder();
        if (address != null && !address.isBlank()) {
            text.append(address).append('\n');
        }
        if (state != null && !state.isBlank()) {
            text.append("State: ").append(state).append('\n');
        }
        if (gstin != null && !gstin.isBlank()) {
            text.append("GSTIN: ").append(gstin).append('\n');
        }
        if (pan != null && !pan.isBlank()) {
            text.append("PAN: ").append(pan).append('\n');
        }
        PdfPCell cell = new PdfPCell();
        cell.setPadding(6);
        cell.addElement(new Paragraph(label, SMALL));
        cell.addElement(new Paragraph(name, BOLD));
        cell.addElement(new Paragraph(text.toString().trim(), NORMAL));
        return cell;
    }

    private static PdfPCell metaCell(String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(5);
        cell.addElement(new Paragraph(label, SMALL));
        cell.addElement(new Paragraph(value, BOLD));
        return cell;
    }

    private static PdfPCell headCell(String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, BOLD));
        cell.setBackgroundColor(HEADER_BG);
        cell.setHorizontalAlignment(align);
        cell.setPadding(6);
        return cell;
    }

    private static PdfPCell bodyCell(String text, int align, boolean bold) {
        PdfPCell cell = new PdfPCell(new Phrase(text, bold ? BOLD : NORMAL));
        cell.setHorizontalAlignment(align);
        cell.setPadding(6);
        cell.setBorder(Rectangle.BOX);
        return cell;
    }

    private static String money(BigDecimal value) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.of("en", "IN"));
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format.format(value);
    }

    private static String rate(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
