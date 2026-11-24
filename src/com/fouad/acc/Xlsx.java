package com.fouad.acc;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

// Minimal .xlsx writer: inline strings, no shared strings, up to 26 columns per row.
public class Xlsx {
    private final List<String> names = new ArrayList<String>();
    private final List<List<String[]>> sheets = new ArrayList<List<String[]>>();

    public Xlsx sheet(String name, List<String[]> rows) {
        names.add(name.length() > 28 ? name.substring(0, 28) : name);
        sheets.add(rows);
        return this;
    }

    public byte[] bytes() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream z = new ZipOutputStream(bos);
        put(z, "[Content_Types].xml", contentTypes());
        put(z, "_rels/.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
        put(z, "xl/workbook.xml", workbook());
        put(z, "xl/_rels/workbook.xml.rels", workbookRels());
        for (int i = 0; i < sheets.size(); i++)
            put(z, "xl/worksheets/sheet" + (i + 1) + ".xml", sheetXml(sheets.get(i)));
        z.close();
        return bos.toByteArray();
    }

    private void put(ZipOutputStream z, String name, String content) throws Exception {
        z.putNextEntry(new ZipEntry(name));
        z.write(content.getBytes("UTF-8"));
        z.closeEntry();
    }

    private String contentTypes() {
        StringBuilder s = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>");
        for (int i = 0; i < sheets.size(); i++)
            s.append("<Override PartName=\"/xl/worksheets/sheet").append(i + 1).append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
        return s.append("</Types>").toString();
    }

    private String workbook() {
        StringBuilder s = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>");
        for (int i = 0; i < names.size(); i++)
            s.append("<sheet name=\"").append(esc(names.get(i))).append("\" sheetId=\"").append(i + 1).append("\" r:id=\"rId").append(i + 1).append("\"/>");
        return s.append("</sheets></workbook>").toString();
    }

    private String workbookRels() {
        StringBuilder s = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
        for (int i = 0; i < sheets.size(); i++)
            s.append("<Relationship Id=\"rId").append(i + 1).append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet").append(i + 1).append(".xml\"/>");
        return s.append("</Relationships>").toString();
    }

    private String sheetXml(List<String[]> rows) {
        StringBuilder s = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
        for (int r = 0; r < rows.size(); r++) {
            s.append("<row r=\"").append(r + 1).append("\">");
            String[] row = rows.get(r);
            for (int c = 0; c < row.length; c++) {
                String ref = col(c) + (r + 1);
                s.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">").append(esc(row[c] == null ? "" : row[c])).append("</t></is></c>");
            }
            s.append("</row>");
        }
        return s.append("</sheetData></worksheet>").toString();
    }

    private String col(int i) { return String.valueOf((char) ('A' + Math.min(i, 25))); }

    private String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
