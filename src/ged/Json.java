package ged;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Minimal JSON reader/writer so the GED does not require Jackson/Gson. */
public final class Json {
    private Json() {}

    public static Object parse(String text) {
        return new Parser(text == null ? "" : text).parse();
    }

    @SuppressWarnings("unchecked")
    public static Map<String,Object> object(String text) {
        Object v = parse(text);
        return v instanceof Map ? (Map<String,Object>) v : new LinkedHashMap<String,Object>();
    }

    public static Object read(Path path, Object fallback) {
        try {
            byte[] data = Files.readAllBytes(path);
            return parse(new String(data, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return fallback;
        }
    }

    public static void write(Path path, Object value) throws IOException {
        Path parent = path.getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.write(path, stringifyPretty(value).getBytes(StandardCharsets.UTF_8));
    }

    public static String stringify(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value, 0, false);
        return sb.toString();
    }

    public static String stringifyPretty(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value, 0, true);
        sb.append('\n');
        return sb.toString();
    }

    private static void writeValue(StringBuilder sb, Object value, int depth, boolean pretty) {
        if (value == null) { sb.append("null"); return; }
        if (value instanceof String) { quote(sb, (String)value); return; }
        if (value instanceof Number || value instanceof Boolean) { sb.append(String.valueOf(value)); return; }
        if (value instanceof Map) {
            @SuppressWarnings("unchecked") Map<String,Object> map = (Map<String,Object>)value;
            sb.append('{');
            boolean first = true;
            for (Map.Entry<String,Object> e : map.entrySet()) {
                if (!first) sb.append(',');
                if (pretty) { sb.append('\n'); indent(sb, depth + 1); }
                quote(sb, e.getKey()); sb.append(pretty ? ": " : ":");
                writeValue(sb, e.getValue(), depth + 1, pretty);
                first = false;
            }
            if (!map.isEmpty() && pretty) { sb.append('\n'); indent(sb, depth); }
            sb.append('}'); return;
        }
        if (value instanceof Iterable) {
            sb.append('['); boolean first = true;
            for (Object item : (Iterable<?>)value) {
                if (!first) sb.append(',');
                if (pretty) { sb.append('\n'); indent(sb, depth + 1); }
                writeValue(sb, item, depth + 1, pretty); first = false;
            }
            if (!first && pretty) { sb.append('\n'); indent(sb, depth); }
            sb.append(']'); return;
        }
        quote(sb, String.valueOf(value));
    }

    private static void indent(StringBuilder sb, int depth) { for (int i=0;i<depth;i++) sb.append("  "); }
    private static void quote(StringBuilder sb, String s) {
        sb.append('"');
        for (int i=0;i<s.length();i++) {
            char c=s.charAt(i);
            switch(c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 32) sb.append(String.format("\\u%04x", (int)c));
                    else sb.append(c);
            }
        }
        sb.append('"');
    }

    private static final class Parser {
        private final String s; private int i;
        Parser(String s){this.s=s;}
        Object parse(){ skip(); Object v=value(); skip(); if(i!=s.length()) throw err("Conteúdo após o JSON"); return v; }
        private Object value(){ skip(); if(i>=s.length()) throw err("JSON vazio"); char c=s.charAt(i);
            if(c=='{') return object(); if(c=='[') return array(); if(c=='"') return string();
            if(c=='t'&&word("true")) return Boolean.TRUE; if(c=='f'&&word("false")) return Boolean.FALSE; if(c=='n'&&word("null")) return null;
            if(c=='-'||Character.isDigit(c)) return number(); throw err("Valor inválido"); }
        private Map<String,Object> object(){ LinkedHashMap<String,Object> m=new LinkedHashMap<String,Object>(); i++; skip(); if(peek('}')){i++;return m;} while(true){skip(); if(!peek('"'))throw err("Chave esperada"); String k=string(); skip(); expect(':'); Object v=value(); m.put(k,v); skip(); if(peek('}')){i++;return m;} expect(',');}}
        private List<Object> array(){ ArrayList<Object> a=new ArrayList<Object>(); i++; skip(); if(peek(']')){i++;return a;} while(true){a.add(value()); skip(); if(peek(']')){i++;return a;} expect(',');}}
        private String string(){ expect('"'); StringBuilder b=new StringBuilder(); while(i<s.length()){char c=s.charAt(i++); if(c=='"') return b.toString(); if(c=='\\'){if(i>=s.length())throw err("Escape incompleto"); char e=s.charAt(i++); switch(e){case '"':b.append('"');break;case '\\':b.append('\\');break;case '/':b.append('/');break;case 'b':b.append('\b');break;case 'f':b.append('\f');break;case 'n':b.append('\n');break;case 'r':b.append('\r');break;case 't':b.append('\t');break;case 'u': if(i+4>s.length())throw err("Unicode inválido"); b.append((char)Integer.parseInt(s.substring(i,i+4),16)); i+=4;break;default:throw err("Escape inválido");}} else b.append(c);} throw err("String não terminada");}
        private Number number(){int st=i; if(peek('-'))i++; while(i<s.length()&&Character.isDigit(s.charAt(i)))i++; boolean dec=false; if(peek('.')){dec=true;i++;while(i<s.length()&&Character.isDigit(s.charAt(i)))i++;} if(i<s.length()&&(s.charAt(i)=='e'||s.charAt(i)=='E')){dec=true;i++;if(peek('+')||peek('-'))i++;while(i<s.length()&&Character.isDigit(s.charAt(i)))i++;} String n=s.substring(st,i); try{return dec?Double.valueOf(n):Long.valueOf(n);}catch(Exception e){throw err("Número inválido");}}
        private boolean word(String w){if(s.regionMatches(i,w,0,w.length())){i+=w.length();return true;}return false;}
        private void skip(){while(i<s.length()&&Character.isWhitespace(s.charAt(i)))i++;}
        private boolean peek(char c){return i<s.length()&&s.charAt(i)==c;}
        private void expect(char c){skip();if(!peek(c))throw err("Esperado '"+c+"'");i++;}
        private RuntimeException err(String m){return new IllegalArgumentException(m+" na posição "+i);}
    }
}
