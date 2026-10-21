package ged;

import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public final class RecognitionEngine {
    private final Path base;
    public RecognitionEngine(Path base){this.base=base;}

    @SuppressWarnings("unchecked") public List<Map<String,Object>> rules(){Object v=Json.read(base.resolve("config/rules.json"),new ArrayList<Object>()); return (List<Map<String,Object>>)(List<?>)v;}
    @SuppressWarnings("unchecked") public List<Map<String,Object>> companies(){Object v=Json.read(base.resolve("config/companies.json"),new ArrayList<Object>()); return (List<Map<String,Object>>)(List<?>)v;}

    private static final Map<String,String> MONTHS=new HashMap<String,String>();
    static {
        String[][] a={{"JAN","01"},{"JANEIRO","01"},{"FEV","02"},{"FEVEREIRO","02"},{"MAR","03"},{"MARCO","03"},{"ABR","04"},{"ABRIL","04"},{"MAI","05"},{"MAIO","05"},{"JUN","06"},{"JUNHO","06"},{"JUL","07"},{"JULHO","07"},{"AGO","08"},{"AGOSTO","08"},{"SET","09"},{"SETEMBRO","09"},{"OUT","10"},{"OUTUBRO","10"},{"NOV","11"},{"NOVEMBRO","11"},{"DEZ","12"},{"DEZEMBRO","12"}};
        for(String[]x:a)MONTHS.put(x[0],x[1]);
    }

    private static String mm(String m,String y){return String.format(Locale.ROOT,"%02d-%s",Integer.parseInt(m),y);}
    private static String str(Object o){return o==null?"":String.valueOf(o);}
    private static int integer(Object o,int d){try{return o instanceof Number?((Number)o).intValue():Integer.parseInt(str(o));}catch(Exception e){return d;}}
    private static List<Object> list(Object o){if(o instanceof List)return (List<Object>)o;return new ArrayList<Object>();}
    private static Map<String,Object> obj(){return new LinkedHashMap<String,Object>();}

    private Map<String,Object> classify(String text,String filename,String title,List<Map<String,Object>> rules){
        String nt=PdfEngine.normalize(text), nf=PdfEngine.normalize(filename), nm=PdfEngine.normalize(title), hay=nt+" "+nm+" "+nf;
        List<Object[]> matches=new ArrayList<Object[]>();
        for(Map<String,Object> r:rules){
            List<Object>sigs=list(r.get("signatures")); if(sigs.isEmpty())continue;
            List<String> found=new ArrayList<String>();
            for(Object so:sigs){String s=PdfEngine.normalize(str(so));if(!s.isEmpty()&&nt.contains(s))found.add(s);}
            int min=integer(r.get("min_signatures"),sigs.size());
            String source="texto"; int conf=found.size()>=min?100:0;
            if("metadata_filename".equals(str(r.get("fallback")))&&conf==0){
                found.clear();
                for(Object so:sigs){String s=PdfEngine.normalize(str(so));if(!s.isEmpty()&&hay.contains(s))found.add(s);}
                if(found.size()>=Math.min(min,1)){source="metadados/nome do arquivo";conf=88;}
            }
            if(conf>0)matches.add(new Object[]{integer(r.get("priority"),0),conf,r,found,source});
        }
        if(matches.isEmpty())return null;
        Collections.sort(matches,new Comparator<Object[]>(){public int compare(Object[]a,Object[]b){int c=Integer.compare((Integer)b[0],(Integer)a[0]);if(c!=0)return c;c=Integer.compare((Integer)b[1],(Integer)a[1]);if(c!=0)return c;return Integer.compare(((List<?>)b[3]).size(),((List<?>)a[3]).size());}});
        Object[] x=matches.get(0);Map<String,Object> out=obj();out.put("rule",x[2]);out.put("source",x[4]);out.put("confidence",x[1]);out.put("signatures",x[3]);return out;
    }

    private List<String> detectedCnpjs(String text){
        LinkedHashSet<String> out=new LinkedHashSet<String>();
        Matcher m=Pattern.compile("(?<!\\d)(\\d{2})[.]?(\\d{3})[.]?(\\d{3})[/]?(\\d{4})[-]?(\\d{2})(?!\\d)").matcher(text==null?"":text);
        while(m.find()){
            String c=CompanyCardParser.formatCnpj(m.group(1)+m.group(2)+m.group(3)+m.group(4)+m.group(5));
            if(CompanyCardParser.validCnpj(c))out.add(c);
        }
        return new ArrayList<String>(out);
    }

    private Map<String,Object> chooseCompany(String text,String filename,List<Map<String,Object>> companies){
        String nt=PdfEngine.normalize(text), nf=PdfEngine.normalize(filename);
        List<String> detected=detectedCnpjs(text);
        List<Map<String,Object>> registered=new ArrayList<Map<String,Object>>();
        for(Map<String,Object> c:companies)if(!str(c.get("cnpj")).isEmpty())registered.add(c);

        Map<String,Object> best=null;int bestPos=Integer.MAX_VALUE;
        for(Map<String,Object> c:registered){
            String cnpj=str(c.get("cnpj"));int p=nt.indexOf(PdfEngine.normalize(cnpj));
            if(p<0){String d=PdfEngine.digits(cnpj);String nd=PdfEngine.digits(nt);p=nd.indexOf(d);}
            if(p>=0&&p<bestPos){best=c;bestPos=p;}
        }
        if(best!=null)return choice(best,"CNPJ cadastrado encontrado no documento",100,detected);

        for(Map<String,Object> c:registered){
            String legal=PdfEngine.normalize(!str(c.get("legal_name")).isEmpty()?str(c.get("legal_name")):str(c.get("folder")));
            if(!legal.isEmpty()&&nt.contains(legal))return choice(c,"razão social cadastrada encontrada no documento",96,detected);
        }
        for(Map<String,Object> c:registered){
            LinkedHashSet<String> als=new LinkedHashSet<String>();als.add(str(c.get("folder")));als.add(str(c.get("legal_name")));for(Object a:list(c.get("aliases")))als.add(str(a));
            List<String> norm=new ArrayList<String>();for(String a:als)if(!a.isEmpty())norm.add(PdfEngine.normalize(a));
            Collections.sort(norm,new Comparator<String>(){public int compare(String a,String b){return Integer.compare(b.length(),a.length());}});
            for(String a:norm){
                if(a.length()<=2){if(Pattern.compile("(?:^|[^A-Z0-9])"+Pattern.quote(a)+"(?:[^A-Z0-9]|$)").matcher(nf).find())return choice(c,"alias cadastrado no nome do arquivo",86,detected);}
                else if(nf.contains(a))return choice(c,"alias cadastrado no nome do arquivo",88,detected);
            }
        }
        String src=detected.isEmpty()?"empresa não identificada":"CNPJ detectado, mas empresa não cadastrada por Cartão CNPJ";
        return choice(null,src,0,detected);
    }
    private Map<String,Object> choice(Map<String,Object> c,String src,int conf,List<String> detected){Map<String,Object> o=obj();o.put("company",c);o.put("source",src);o.put("confidence",conf);o.put("detected_cnpjs",detected);return o;}

    private Map<String,Object> competence(String text,String filename,String ruleId){
        String nt=PdfEngine.normalize(text),nf=PdfEngine.normalize(filename);Matcher m;

        if("declaracao_faturamento".equals(ruleId)){
            m=Pattern.compile("PERIODO DE\\s+(\\d{2})/(\\d{2})/(20\\d{2})\\s+(?:A|ATE)\\s+(\\d{2})/(\\d{2})/(20\\d{2})").matcher(nt);
            if(m.find())return comp(mm(m.group(5),m.group(6)),"texto: fim do período declarado",100);
        }
        if("relacao_declaracoes_nfse_tomados".equals(ruleId)){
            m=Pattern.compile("(?:^|\\s)(0?[1-9]|1[0-2])/(20\\d{2}).{0,180}DECLARACAO SERVICOS TOMADOS").matcher(nt);
            if(m.find())return comp(mm(m.group(1),m.group(2)),"texto: competência da declaração de serviços tomados",100);
            m=Pattern.compile("COMPETENCIA INICIAL:\\s*(20\\d{2})/(0?[1-9]|1[0-2])").matcher(nt);
            if(m.find())return comp(mm(m.group(2),m.group(1)),"texto: competência inicial",98);
        }
        if("aliquota_iss_icms".equals(ruleId)){
            m=Pattern.compile("COMPETENCIA.{0,220}?(0?[1-9]|1[0-2])/(20\\d{2})").matcher(nt);
            if(m.find())return comp(mm(m.group(1),m.group(2)),"texto: campo competência",100);
        }
        if("guia_issqn_municipal".equals(ruleId)){
            m=Pattern.compile("MES/ANO:\\s*(0?[1-9]|1[0-2])/(20\\d{2})").matcher(nt);
            if(m.find())return comp(mm(m.group(1),m.group(2)),"texto: lançamento mês/ano",100);
        }
        if("demonstrativo_issqn_debitos_creditos".equals(ruleId)){
            m=Pattern.compile("PERIODO DE\\s+([A-Z]{3,9})\\s*(20\\d{2})").matcher(nt);
            if(m.find()){String mo=MONTHS.get(m.group(1));if(mo==null&&m.group(1).length()>3)mo=MONTHS.get(m.group(1).substring(0,3));if(mo!=null)return comp(mm(mo,m.group(2)),"texto: período do demonstrativo",100);}
        }
        if("relatorio_lancamentos".equals(ruleId)){
            m=Pattern.compile("PERIODO:\\s*(\\d{2})/(\\d{2})/(20\\d{2})").matcher(nt);
            if(m.find())return comp(mm(m.group(2),m.group(3)),"texto: início do período",98);
        }

        String[] direct={
            "COMPETENCIA\\s*:?\\s*(0?[1-9]|1[0-2])/(20\\d{2})",
            "PERIODO\\s*:?\\s*(0?[1-9]|1[0-2])/(20\\d{2})",
            "REFERENCIA\\s*:?\\s*(0?[1-9]|1[0-2])/(20\\d{2})"
        };
        for(String p:direct){m=Pattern.compile(p).matcher(nt);if(m.find())return comp(mm(m.group(1),m.group(2)),"texto: competência/período",100);}

        String[] named={
            "REFERENTE A COMPETENCIA\\s+([A-Z]+)/(20\\d{2})",
            "COMPETENCIA\\s*:?\\s*([A-Z]+)/(20\\d{2})",
            "PERIODO\\s*:?\\s*([A-Z]+)/(20\\d{2})",
            "REFERENCIA\\s*:?\\s*([A-Z]+)/(20\\d{2})"
        };
        for(String p:named){m=Pattern.compile(p).matcher(nt);if(m.find()){String mo=MONTHS.get(m.group(1));if(mo!=null)return comp(mm(mo,m.group(2)),"texto: competência/período",100);}}

        String[] ranges={
            "PERIODO DE APURACAO:\\s*(\\d{2})/(\\d{2})/(20\\d{2})\\s+(?:A|ATE)\\s+\\d{2}/\\d{2}/20\\d{2}",
            "PERIODO:\\s*(\\d{2})/(\\d{2})/(20\\d{2})\\s+(?:A|ATE)\\s+\\d{2}/\\d{2}/20\\d{2}",
            "COMPETENCIA:\\s*(\\d{2})/(\\d{2})/(20\\d{2})\\s+(?:A|ATE)\\s+\\d{2}/\\d{2}/20\\d{2}"
        };
        for(String p:ranges){m=Pattern.compile(p).matcher(nt);if(m.find())return comp(mm(m.group(2),m.group(3)),"texto: início do período",100);}
        m=Pattern.compile("(\\d{2})/(\\d{2})/(20\\d{2})\\s+(?:A|ATE)\\s+(\\d{2})/(\\d{2})/(20\\d{2})").matcher(nt);
        while(m.find())if(m.group(2).equals(m.group(5))&&m.group(3).equals(m.group(6)))return comp(mm(m.group(2),m.group(3)),"texto: intervalo mensal",98);

        m=Pattern.compile("(?:^|\\D)(0[1-9]|1[0-2])[-_ ](20\\d{2})(?:\\D|$)").matcher(nf);
        if(m.find())return comp(mm(m.group(1),m.group(2)),"nome do arquivo",90);
        for(Map.Entry<String,String> e:MONTHS.entrySet())if(e.getKey().length()>3){m=Pattern.compile("\\b"+Pattern.quote(e.getKey())+"\\s+(20\\d{2})\\b").matcher(nf);if(m.find())return comp(mm(e.getValue(),m.group(1)),"nome do arquivo",88);}
        return comp(null,"não encontrada",0);
    }
    private Map<String,Object> comp(String v,String s,int c){Map<String,Object>o=obj();o.put("value",v);o.put("source",s);o.put("confidence",c);return o;}

    public Map<String,Object> analyze(Path path,Map<String,Object> settings,List<Map<String,Object>> companies,List<Map<String,Object>> rules){
        Map<String,Object> result=obj();result.put("name",path.getFileName().toString());result.put("path",path.toString());
        try{result.put("size",Files.size(path));}catch(Exception e){result.put("size",0);}
        String name=path.getFileName().toString();String ext=name.lastIndexOf('.')>=0?name.substring(name.lastIndexOf('.')).toLowerCase(Locale.ROOT):"";result.put("extension",ext);result.put("status","error");result.put("error",null);
        if(!".pdf".equals(ext)){result.put("status","review");result.put("error","Formato ainda fora do escopo da v0.0.6. Nesta etapa o GED processa apenas PDFs da categoria RELATÓRIOS.");return result;}
        try{
            PdfEngine.PdfData pd=PdfEngine.extract(path);String text=pd.text;result.put("hash",PdfEngine.sha256(path));result.put("text_chars",text.trim().length());result.put("metadata",pd.metadata);
            Map<String,Object> cl=classify(text,name,str(pd.metadata.get("title")),rules);Map<String,Object> ch=chooseCompany(text,name,companies);Map<String,Object> company=(Map<String,Object>)ch.get("company");Map<String,Object> rule=cl==null?null:(Map<String,Object>)cl.get("rule");Map<String,Object> co=competence(text,name,rule==null?null:str(rule.get("id")));
            Map<String,Object> doc=obj();doc.put("id",rule==null?null:rule.get("id"));doc.put("name",rule==null?null:rule.get("name"));doc.put("source",cl==null?"não reconhecido":cl.get("source"));doc.put("confidence",cl==null?0:cl.get("confidence"));doc.put("signatures",cl==null?new ArrayList<Object>():cl.get("signatures"));result.put("document",doc);
            Map<String,Object> cc=obj();cc.put("folder",company==null?null:company.get("folder"));cc.put("legal_name",company==null?null:company.get("legal_name"));cc.put("cnpj",company==null?null:company.get("cnpj"));cc.put("source",ch.get("source"));cc.put("confidence",ch.get("confidence"));cc.put("detected_cnpjs",ch.get("detected_cnpjs"));cc.put("registered",company!=null);result.put("company",cc);result.put("competence",co);
            int dc=integer(doc.get("confidence"),0), cconf=integer(cc.get("confidence"),0), kconf=integer(co.get("confidence"),0);int confidence=(int)Math.round(dc*.40+cconf*.35+kconf*.25);result.put("confidence",confidence);int threshold=integer(settings.get("threshold"),95);String cv=str(co.get("value"));
            if(rule!=null&&company!=null&&!cv.isEmpty()){
                String year=cv.substring(cv.indexOf('-')+1);String category=!str(rule.get("category")).isEmpty()?str(rule.get("category")):str(settings.get("destination_category"));if(category.isEmpty())category="RELATÓRIOS";
                String tpl=!str(rule.get("filename")).isEmpty()?str(rule.get("filename")):"{empresa} - {documento} - {competencia}.pdf";String newName=tpl.replace("{empresa}",str(company.get("folder"))).replace("{documento}",str(rule.get("name"))).replace("{competencia}",cv);newName=PdfEngine.safeFilename(newName);
                Path dest=Paths.get(str(settings.get("root")),str(company.get("folder")),year,cv,category);result.put("proposed_name",newName);result.put("destination_dir",dest.toString());result.put("destination",dest.resolve(newName).toString());result.put("category",category);result.put("status",confidence>=threshold?"recognized":"review");
                if(confidence<threshold)result.put("review_reason","confiança abaixo do limite de "+threshold+"%");
            } else if(rule!=null||company!=null||!cv.isEmpty()){
                result.put("status","review");
                if(rule==null)result.put("review_reason","tipo documental não reconhecido");
                else if(company==null)result.put("review_reason","empresa não cadastrada por Cartão CNPJ");
                else if(cv.isEmpty())result.put("review_reason","competência não identificada");
            } else {
                result.put("status","unconfigured");result.put("review_reason","documento ainda não parametrizado");
            }
            if(text.trim().isEmpty())result.put("notice","PDF sem camada de texto; classificação limitada a metadados/nome do arquivo. O GED não arquiva automaticamente sem confiança suficiente.");
        }catch(Exception e){result.put("status","error");result.put("error",e.getClass().getSimpleName()+": "+e.getMessage());}
        return result;
    }
}
