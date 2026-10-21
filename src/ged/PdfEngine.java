package ged;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.util.PDFTextStripper;

public final class PdfEngine {
    private PdfEngine() {}

    public static final class PdfData {
        public final String text;
        public final Map<String,Object> metadata;
        PdfData(String text, Map<String,Object> metadata){this.text=text;this.metadata=metadata;}
    }

    public static PdfData extract(Path path) throws IOException {
        PDDocument doc = null;
        try {
            doc = PDDocument.load(path.toFile());
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            PDDocumentInformation info = doc.getDocumentInformation();
            Map<String,Object> meta = new LinkedHashMap<String,Object>();
            meta.put("title", info == null || info.getTitle() == null ? "" : info.getTitle());
            meta.put("author", info == null || info.getAuthor() == null ? "" : info.getAuthor());
            meta.put("pages", String.valueOf(doc.getNumberOfPages()));
            return new PdfData(text == null ? "" : text, meta);
        } finally {
            if (doc != null) try { doc.close(); } catch (IOException ignored) {}
        }
    }

    public static String sha256(Path path) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            InputStream in = Files.newInputStream(path);
            try {
                byte[] buf = new byte[1024 * 1024]; int n;
                while((n=in.read(buf))!=-1) md.update(buf,0,n);
            } finally { in.close(); }
            StringBuilder sb=new StringBuilder(); for(byte b:md.digest()) sb.append(String.format("%02x",b&0xff)); return sb.toString();
        } catch (NoSuchAlgorithmException e) { throw new IOException(e); }
    }

    public static String normalize(String value) {
        if (value == null || value.isEmpty()) return "";
        Matcher tok=Pattern.compile("#U([0-9A-Fa-f]{4})").matcher(value); StringBuffer sb=new StringBuffer();
        while(tok.find()){ char c=(char)Integer.parseInt(tok.group(1),16); tok.appendReplacement(sb,Matcher.quoteReplacement(String.valueOf(c))); } tok.appendTail(sb);
        value=Normalizer.normalize(sb.toString(), Normalizer.Form.NFKD).replaceAll("\\p{M}+", "");
        value=value.toUpperCase(Locale.ROOT).replace('×','X').replace('–','-').replace('—','-');
        return value.replaceAll("\\s+"," ").trim();
    }

    public static String digits(String s){ return s==null?"":s.replaceAll("\\D",""); }
    public static String safeFilename(String value){String v=value.replaceAll("[<>:\"/\\\\|?*]","-").replaceAll("\\s+"," ").trim(); while(v.endsWith(".")||v.endsWith(" "))v=v.substring(0,v.length()-1); if(v.length()>220)v=v.substring(0,220); return v.isEmpty()?"documento.pdf":v;}
    public static String safeFolderName(String value){String v=value==null?"":value.replaceAll("[<>:\"/\\\\|?*]","-").replaceAll("\\s+"," ").trim(); while(v.endsWith(".")||v.endsWith(" "))v=v.substring(0,v.length()-1); Set<String> r=new HashSet<String>(Arrays.asList("CON","PRN","AUX","NUL","COM1","COM2","COM3","COM4","COM5","COM6","COM7","COM8","COM9","LPT1","LPT2","LPT3","LPT4","LPT5","LPT6","LPT7","LPT8","LPT9")); if(r.contains(v.toUpperCase(Locale.ROOT)))v=v+" - EMPRESA"; if(v.length()>180)v=v.substring(0,180); return v.isEmpty()?"EMPRESA SEM NOME":v;}
}
