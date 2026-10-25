package ged;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

/** End-to-end flow with a local BrasilAPI stub and real text-layer PDF documents. */
public final class AutoCompanyRegistrationTest {
    static int assertions=0;
    static void check(boolean ok,String label) {assertions++;if(!ok)throw new AssertionError(label);}
    static Map<String,Object> map(Object... pairs) {
        Map<String,Object> v=new LinkedHashMap<String,Object>();
        for(int i=0;i<pairs.length;i+=2)v.put(String.valueOf(pairs[i]),pairs[i+1]);
        return v;
    }
    static void createPdf(Path path,String... lines)throws Exception {
        Files.createDirectories(path.getParent());
        PDDocument doc=new PDDocument();
        try {
            PDPage page=new PDPage(PDPage.PAGE_SIZE_A4);doc.addPage(page);
            PDPageContentStream stream=new PDPageContentStream(doc,page);
            try {
                stream.beginText();stream.setFont(PDType1Font.HELVETICA,11);
                stream.moveTextPositionByAmount(45,750);
                for(String line:lines){stream.drawString(line);stream.moveTextPositionByAmount(0,-20);}
                stream.endText();
            } finally {stream.close();}
            doc.save(path.toFile());
        } finally {doc.close();}
    }
    @SuppressWarnings("unchecked")
    static Map<String,Object> sub(Map<String,Object> r,String key){return (Map<String,Object>)r.get(key);}
    @SuppressWarnings("unchecked")
    static List<Map<String,Object>> companies(Path home){return (List<Map<String,Object>>)(List<?>)Json.read(home.resolve("config/companies.json"),new ArrayList<Object>());}
    static void config(Path home,Path root,boolean auto,boolean create)throws Exception {
        Json.write(home.resolve("config/settings.json"),map("version","0.1.2","root",root.toString(),"inbox","AAA","review","AAA/REVISAO","errors","AAA/ERROS","auto_process",true,"auto_register_companies",auto,"create_folders",create,"threshold",95));
    }
    static Map<String,Object> process(GedServer app,Path root,String filename,String... content)throws Exception {
        Path input=root.resolve("AAA").resolve(filename);
        createPdf(input,content);
        Map<String,Object> result=app.processOne(input,"integration-test");
        check(result!=null,"document processed: "+filename);
        return result;
    }
    public static void main(String[]args)throws Exception {
        final AtomicInteger calls=new AtomicInteger();
        HttpServer stub=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        stub.createContext("/cnpj/v1/", x->{
            calls.incrementAndGet();String requested=x.getRequestURI().getPath().replace("/cnpj/v1/","");
            int status="99999999000191".equals(requested)?404:200;
            String payload=status==404?"{\"message\":\"not found\"}":Json.stringify(map("cnpj",requested,"razao_social","EMPRESA EXEMPLO LTDA","nome_fantasia","EXEMPLO","municipio","VARGINHA","uf","MG","situacao_cadastral",2,"cnae_fiscal","6201501","opcao_pelo_simples",true));
            byte[] response=payload.getBytes(StandardCharsets.UTF_8);
            x.getResponseHeaders().set("Content-Type","application/json");x.sendResponseHeaders(status,response.length);
            try(OutputStream out=x.getResponseBody()){out.write(response);}
        });
        stub.start();
        Path home=Files.createTempDirectory("ged-auto-test-");Path root=home.resolve("SERVIDOR");Files.createDirectories(root.resolve("AAA"));
        Files.createDirectories(home.resolve("config"));
        Files.copy(Paths.get("config/rules.json"),home.resolve("config/rules.json"));
        Json.write(home.resolve("config/companies.json"),new ArrayList<Object>());
        config(home,root,true,true);
        GedServer app=new GedServer(home,new BrasilApiClient("http://127.0.0.1:"+stub.getAddress().getPort()+"/cnpj/v1/"));
        try {
            Map<String,Object> first=process(app,root,"primeiro.pdf","ALIQUOTA ISS / ICMS","COMPETENCIA: 09/2026","CNPJ: 19.131.243/0001-97");
            check("recognized".equals(first.get("status")),"valid unknown CNPJ recognized after registration");
            check("archived".equals(first.get("archive_action")),"file archived");
            check("created".equals(sub(first,"auto_registration").get("status")),"automatic company created");
            check(Files.isDirectory(root.resolve("EMPRESA EXEMPLO LTDA")),"company folder created");
            check(Files.isRegularFile(Paths.get(String.valueOf(first.get("final_path")))),"file moved to final destination");
            check(String.valueOf(first.get("final_path")).contains("09-2026"),"competence folder used");
            check(companies(home).size()==1,"one company registered");
            check("brasilapi_auto".equals(companies(home).get(0).get("source")),"automatic source retained");
            check(Boolean.TRUE.equals(companies(home).get(0).get("opcao_pelo_simples")),"API field persisted");
            check(calls.get()==1,"single API request");

            Map<String,Object> second=process(app,root,"segundo.pdf","ALIQUOTA ISS / ICMS","COMPETENCIA: 10/2026","19.131.243/0001-97");
            check("archived".equals(second.get("archive_action")),"known company file archived");
            check(calls.get()==1,"known company does not trigger new API query");
            check(companies(home).size()==1,"no duplicate company registration");

            Map<String,Object> ambiguous=process(app,root,"ambiguo.pdf","ALIQUOTA ISS / ICMS","COMPETENCIA: 11/2026","19.131.243/0001-97","AB.123.456/0001-10");
            // There is a known CNPJ in this mixed document, so the current recognizer
            // matches the existing company. It must NEVER enroll an additional company.
            check(calls.get()==1,"no automatic registration from an ambiguous multi-CNPJ document");
            check(companies(home).size()==1,"no second company from mixed PDF");

            // Start a fresh registry to test unknown-company / unknown-company ambiguity.
            Json.write(home.resolve("config/companies.json"),new ArrayList<Object>());
            Map<String,Object> multi=process(app,root,"multiplos.pdf","ALIQUOTA ISS / ICMS","COMPETENCIA: 11/2026","19.131.243/0001-97","AB.123.456/0001-10");
            check("review".equals(multi.get("archive_action")),"two unknown CNPJs routed to review");
            check("skipped".equals(sub(multi,"auto_registration").get("status")),"ambiguity explicitly tracked");
            check(calls.get()==1,"no request for two unknown CNPJs");
            check(companies(home).isEmpty(),"no company created from ambiguous PDF");

            Map<String,Object> unknownType=process(app,root,"regra-ausente.pdf","DOCUMENTO NAO PARAMETRIZADO","COMPETENCIA: 08/2026","19.131.243/0001-97");
            check("review".equals(unknownType.get("archive_action")),"unknown document routed to review");
            check(calls.get()==1,"unknown document does not call BrasilAPI");

            Map<String,Object> fallback=process(app,root,"efd-reinf sem movimento.pdf","COMPETENCIA: 08/2026","CNPJ: 19.131.243/0001-97");
            check("review".equals(fallback.get("archive_action")),"filename-only rule routed to review");
            check(calls.get()==1,"metadata or filename fallback does not trigger API");

            Map<String,Object> noComp=process(app,root,"sem-competencia.pdf","ALIQUOTA ISS / ICMS","CNPJ: 19.131.243/0001-97");
            check("review".equals(noComp.get("archive_action")),"missing competence routed to review");
            check(calls.get()==1,"missing competence does not call BrasilAPI");

            Map<String,Object> notFound=process(app,root,"inexistente.pdf","ALIQUOTA ISS / ICMS","COMPETENCIA: 12/2026","CNPJ: 99.999.999/0001-91");
            check("review".equals(notFound.get("archive_action")),"404 routed to review");
            check("failed".equals(sub(notFound,"auto_registration").get("status")),"API failure tracked");
            check(calls.get()==2,"nonexistent CNPJ consulted once");
            check(companies(home).isEmpty(),"failed API does not register company");

            config(home,root,false,true);
            Map<String,Object> disabled=process(app,root,"desligado.pdf","ALIQUOTA ISS / ICMS","COMPETENCIA: 08/2026","CNPJ: 19.131.243/0001-97");
            check("review".equals(disabled.get("archive_action")),"auto registration disabled -> review");
            check(calls.get()==2,"disabled means no API request");
            check(companies(home).isEmpty(),"disabled means no company");

            config(home,root,true,false);
            Map<String,Object> folderOff=process(app,root,"pastas-desligadas.pdf","ALIQUOTA ISS / ICMS","COMPETENCIA: 08/2026","CNPJ: 19.131.243/0001-97");
            check("review".equals(folderOff.get("archive_action")),"no folder creation -> review");
            check(calls.get()==2,"no call when folder creation is disabled");
            check(companies(home).isEmpty(),"no new company when folder creation is disabled");

            check(Files.isRegularFile(home.resolve("data/history.jsonl")),"audit history was written");
            System.out.println("OK: "+assertions+" checks; automatic registration, fallback, idempotence, and safe routing tested.");
        }finally {stub.stop(0);}
    }
}
