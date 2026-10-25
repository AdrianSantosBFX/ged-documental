package ged;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Self-contained test using a localhost stub: no third-party API calls. */
public final class BrasilApiIntegrationTest {
    private static void check(boolean ok, String label) { if(!ok)throw new AssertionError(label); }
    private static int freePort() throws Exception { try(ServerSocket s=new ServerSocket(0)){return s.getLocalPort();} }
    private static String response(HttpURLConnection c) throws Exception {
        InputStream in=(c.getResponseCode()<400?c.getInputStream():c.getErrorStream());
        try(InputStream body=in;ByteArrayOutputStream bytes=new ByteArrayOutputStream()){
            byte[] b=new byte[4096];int n;while((n=body.read(b))!=-1)bytes.write(b,0,n);
            return new String(bytes.toByteArray(),StandardCharsets.UTF_8);
        }
    }
    private static final class Answer {
        int code;Map<String,Object> json;
        Answer(int code,String body){this.code=code;this.json=Json.object(body);}
    }
    private static Answer call(int port,String path,Map<String,Object> input) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL("http://127.0.0.1:"+port+path).openConnection();
        c.setRequestMethod("POST");c.setConnectTimeout(2000);c.setReadTimeout(5000);c.setDoOutput(true);
        c.setRequestProperty("Content-Type","application/json");
        try(OutputStream out=c.getOutputStream()){out.write(Json.stringify(input).getBytes(StandardCharsets.UTF_8));}
        Answer a=new Answer(c.getResponseCode(),response(c));c.disconnect();return a;
    }
    private static Map<String,Object> map(Object... kv){Map<String,Object>m=new LinkedHashMap<String,Object>();for(int i=0;i<kv.length;i+=2)m.put(String.valueOf(kv[i]),kv[i+1]);return m;}

    public static void main(String[] args) throws Exception {
        check(BrasilApiClient.validCnpj("19.131.243/0001-97"),"valid numeric CNPJ");
        check(BrasilApiClient.validCnpj("AB.123.456/0001-10"),"valid alphanumeric CNPJ");
        check(!BrasilApiClient.validCnpj("00.000.000/0000-00"),"reject repeated digits");
        check(!BrasilApiClient.validCnpj("19.131.243/0001-98"),"reject incorrect DV");
        check(!BrasilApiClient.validCnpj("AB.123.456/0001-AA"),"reject alphanumeric check digits");
        final int mockPort=freePort();
        HttpServer mock=HttpServer.create(new InetSocketAddress("127.0.0.1",mockPort),0);
        mock.createContext("/cnpj/v1/",new HttpHandler(){public void handle(HttpExchange x)throws IOException {
            String code=x.getRequestURI().getPath().replace("/cnpj/v1/","");
            int http=200;String json;
            if("00000000000191".equals(code)){http=429;json="{\"message\":\"Too many requests\"}";}
            else if("99999999000191".equals(code)){http=404;json="{\"message\":\"not found\"}";}
            else json=Json.stringify(map("cnpj",code,"razao_social","EMPRESA EXEMPLO LTDA","nome_fantasia","EXEMPLO","municipio","VARGINHA","uf","MG","situacao_cadastral",2,"cnae_fiscal",6201501,"cnae_fiscal_descricao","Desenvolvimento de programas","opcao_pelo_simples",false,"natureza_juridica","Sociedade Empresária Limitada"));
            byte[] b=json.getBytes(StandardCharsets.UTF_8);x.getResponseHeaders().set("Content-Type","application/json");x.sendResponseHeaders(http,b.length);
            try(OutputStream out=x.getResponseBody()){out.write(b);}
        }});
        mock.start();
        Path home=Files.createTempDirectory("ged-test-");Path root=home.resolve("SERVIDOR");Files.createDirectories(root);
        Files.createDirectories(home.resolve("config"));Json.write(home.resolve("config/settings.json"),map("version","0.1.1","root",root.toString(),"inbox","AAA","auto_process",false,"create_folders",true));
        Json.write(home.resolve("config/companies.json"),new ArrayList<Object>());
        int gedPort=freePort();
        GedServer ged=new GedServer(home,new BrasilApiClient("http://127.0.0.1:"+mockPort+"/cnpj/v1/"));
        ged.start("127.0.0.1",gedPort,false);
        try{
            Answer invalid=call(gedPort,"/api/companies/preview-api",map("cnpj","123"));
            check(invalid.code==400,"invalid CNPJ gives 400");
            Answer limited=call(gedPort,"/api/companies/preview-api",map("cnpj","00000000000191"));
            check(limited.code==429,"rate limit gives 429");
            Answer missing=call(gedPort,"/api/companies/preview-api",map("cnpj","99999999000191"));
            check(missing.code==404,"missing gives 404");
            Answer preview=call(gedPort,"/api/companies/preview-api",map("cnpj","19.131.243/0001-97"));
            check(preview.code==200,"preview status 200");
            check(Boolean.TRUE.equals(preview.json.get("can_register")),"preview available");
            check(!Files.isDirectory(root.resolve("EMPRESA EXEMPLO LTDA")),"preview does not create folder");
            check(((List<?>)Json.read(home.resolve("config/companies.json"),Collections.emptyList())).isEmpty(),"preview does not register");
            String token=String.valueOf(preview.json.get("preview_token"));
            Answer forged=call(gedPort,"/api/companies/register-api",map("cnpj","AB123456000110","preview_token",token));
            check(forged.code==400,"token must match CNPJ");
            Answer registered=call(gedPort,"/api/companies/register-api",map("cnpj","19131243000197","preview_token",token,"legal_name","SPOOF"));
            check(registered.code==200,"register status 200");
            check(!Boolean.TRUE.equals(registered.json.get("already_registered")),"new registration");
            check(Files.isDirectory(root.resolve("EMPRESA EXEMPLO LTDA")),"correct folder created");
            check(!Files.exists(root.resolve("SPOOF")),"client cannot replace API data");
            Answer reused=call(gedPort,"/api/companies/register-api",map("cnpj","19131243000197","preview_token",token));
            check(reused.code==400,"preview token one-time");
            Answer again=call(gedPort,"/api/companies/preview-api",map("cnpj","19131243000197"));
            check(again.code==200&&again.json.get("existing_company")!=null,"existing company reported");
            Answer duplicate=call(gedPort,"/api/companies/register-api",map("cnpj","19.131.243/0001-97","preview_token",again.json.get("preview_token")));
            check(duplicate.code==200&&Boolean.TRUE.equals(duplicate.json.get("already_registered")),"no duplicate registration");
            Answer alpha=call(gedPort,"/api/companies/preview-api",map("cnpj","AB.123.456/0001-10"));
            check(alpha.code==200&&"AB.123.456/0001-10".equals(((Map<?,?>)alpha.json.get("card")).get("cnpj")),"alphanumeric response");
            List<?> companies=(List<?>)Json.read(home.resolve("config/companies.json"),Collections.emptyList());
            check(companies.size()==1,"exactly one saved company");
            check("brasilapi".equals(((Map<?,?>)companies.get(0)).get("source")),"record has source");
            check(Boolean.FALSE.equals(((Map<?,?>)companies.get(0)).get("opcao_pelo_simples")),"save Simples false without inventing tax regime");
            System.out.println("OK: 19 assertions, BrasilAPI mocked / cadastro integrado + CNPJ alfanumérico.");
        }finally{mock.stop(0);}
        System.exit(0); // GED webserver has a persistent executor by design.
    }
}
