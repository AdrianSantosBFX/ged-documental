package ged;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Consulta publica de CNPJ. O endereco da API nao e fornecido pelo navegador. */
public final class BrasilApiClient {
    private static final String PUBLIC_ENDPOINT = "https://brasilapi.com.br/api/cnpj/v1/";
    private static final int MAX_RESPONSE = 1024 * 1024;
    private static final int[] W1 = {5,4,3,2,9,8,7,6,5,4,3,2};
    private static final int[] W2 = {6,5,4,3,2,9,8,7,6,5,4,3,2};
    private final String endpoint;

    public BrasilApiClient() { this(PUBLIC_ENDPOINT); }
    // Construtor de injecao usado exclusivamente em testes locais com servidor simulado.
    BrasilApiClient(String endpoint) { this.endpoint = endpoint; }

    public static String normalizeCnpj(String input) {
        return input == null ? "" : input.toUpperCase(Locale.ROOT).replaceAll("[.\\/\\-\\s]", "");
    }

    private static int checkDigit(String prefix, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) sum += (prefix.charAt(i) - 48) * weights[i];
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    public static boolean validCnpj(String input) {
        String value = normalizeCnpj(input);
        if (!value.matches("[0-9A-Z]{12}[0-9]{2}")) return false;
        if (value.matches("([0-9])\\1{13}")) return false;
        int d1 = checkDigit(value.substring(0, 12), W1);
        int d2 = checkDigit(value.substring(0, 12) + d1, W2);
        return value.substring(12).equals("" + d1 + d2);
    }

    public static String formatCnpj(String value) {
        String v = normalizeCnpj(value);
        if (v.length() != 14) return value == null ? "" : value;
        return v.substring(0,2) + "." + v.substring(2,5) + "." + v.substring(5,8)
            + "/" + v.substring(8,12) + "-" + v.substring(12);
    }

    public static final class ApiException extends Exception {
        private final int status;
        public ApiException(int status, String message) { super(message); this.status = status; }
        public int getStatus() { return status; }
    }

    private static String str(Object data) { return data == null ? "" : String.valueOf(data).trim(); }
    private static Map<String,Object> obj() { return new LinkedHashMap<String,Object>(); }
    private static void copyIf(Map<String,Object> dst, String target, Map<String,Object> src, String key) {
        Object v = src.get(key);
        if (v != null && !str(v).isEmpty()) dst.put(target, v);
    }
    private static String status(Object code) {
        String v = str(code);
        if ("2".equals(v)) return "ATIVA";
        if ("1".equals(v)) return "NULA";
        if ("3".equals(v)) return "SUSPENSA";
        if ("4".equals(v)) return "INAPTA";
        if ("8".equals(v)) return "BAIXADA";
        return "NÃO INFORMADA";
    }

    private static String readBounded(InputStream stream) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n, size = 0;
        while ((n = stream.read(chunk)) != -1) {
            size += n;
            if (size > MAX_RESPONSE) throw new IOException("A resposta da BrasilAPI excede o limite de tamanho permitido.");
            out.write(chunk, 0, n);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    public Map<String,Object> consult(String input) throws ApiException {
        String cnpj = normalizeCnpj(input);
        if (!validCnpj(cnpj)) throw new ApiException(400, "CNPJ inválido. Informe 14 caracteres com dígitos verificadores válidos.");
        HttpURLConnection connection = null;
        try {
            URL url = new URL(endpoint + cnpj);
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(6000);
            connection.setReadTimeout(9000);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "ElevarGED/0.1.3 (+consulta cadastral local)");
            int http = connection.getResponseCode();
            if (http != 200) {
                if (http == 400) throw new ApiException(400, "A BrasilAPI rejeitou o CNPJ informado.");
                if (http == 404) throw new ApiException(404, "CNPJ não encontrado na BrasilAPI. Confira os dados e tente novamente.");
                if (http == 429) throw new ApiException(429, "Limite de consultas da BrasilAPI atingido. Aguarde antes de tentar novamente ou utilize o Cartão CNPJ.");
                if (http == 503 || http == 502 || http == 504) throw new ApiException(503, "A BrasilAPI está temporariamente indisponível. Utilize o Cartão CNPJ ou tente mais tarde.");
                throw new ApiException(502, "A BrasilAPI respondeu com HTTP " + http + ". Cadastro não realizado.");
            }
            String body;
            try (InputStream in = connection.getInputStream()) { body = readBounded(in); }
            Map<String,Object> response = Json.object(body);
            return mapResponse(cnpj, response);
        } catch (ApiException ex) {
            throw ex;
        } catch (SocketTimeoutException ex) {
            throw new ApiException(504, "Tempo limite de consulta à BrasilAPI. Tente novamente ou utilize o Cartão CNPJ.");
        } catch (IOException ex) {
            throw new ApiException(503, "Não foi possível conectar à BrasilAPI. Verifique a internet ou utilize o Cartão CNPJ.");
        } catch (RuntimeException ex) {
            throw new ApiException(502, "A BrasilAPI retornou dados inválidos. Cadastro não realizado.");
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    /** Transforma o esquema externo em um cadastro minimo do Elevar GED. Não usa QSA/dados pessoais. */
    static Map<String,Object> mapResponse(String requestedCnpj, Map<String,Object> data) throws ApiException {
        String returnedCnpj = normalizeCnpj(str(data.get("cnpj")));
        String legalName = str(data.get("razao_social"));
        if (!requestedCnpj.equals(returnedCnpj) || !validCnpj(returnedCnpj))
            throw new ApiException(502, "CNPJ da resposta da BrasilAPI não corresponde ao CNPJ consultado.");
        if (legalName.isEmpty()) throw new ApiException(502, "A BrasilAPI não retornou uma razão social válida.");
        String folder = PdfEngine.safeFolderName(legalName);
        if (folder.trim().isEmpty()) throw new ApiException(502, "Razão social inválida para criação de pasta.");
        Map<String,Object> card = obj();
        card.put("valid", true);
        card.put("confidence", 100); // Refere-se ao preenchimento da consulta, não à autenticidade dos dados.
        card.put("errors", Collections.emptyList());
        card.put("document_type", "consulta_brasilapi");
        card.put("cnpj", formatCnpj(returnedCnpj));
        card.put("legal_name", legalName);
        card.put("folder", folder);
        card.put("trade_name", str(data.get("nome_fantasia")));
        card.put("status", status(data.get("situacao_cadastral")));
        card.put("municipality", str(data.get("municipio")));
        card.put("uf", str(data.get("uf")));
        String cnae = str(data.get("cnae_fiscal"));
        String cnaeDescription = str(data.get("cnae_fiscal_descricao"));
        card.put("main_activity", cnaeDescription.isEmpty() ? cnae : cnae + " - " + cnaeDescription);
        copyIf(card, "cnae_fiscal", data, "cnae_fiscal");
        copyIf(card, "natureza_juridica", data, "natureza_juridica");
        copyIf(card, "porte", data, "porte");
        copyIf(card, "data_inicio_atividade", data, "data_inicio_atividade");
        copyIf(card, "logradouro", data, "logradouro");
        copyIf(card, "numero", data, "numero");
        copyIf(card, "bairro", data, "bairro");
        copyIf(card, "cep", data, "cep");
        copyIf(card, "complemento", data, "complemento");
        copyIf(card, "opcao_pelo_simples", data, "opcao_pelo_simples");
        copyIf(card, "opcao_pelo_mei", data, "opcao_pelo_mei");
        card.put("consulted_at", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        card.put("source", "brasilapi");
        return card;
    }
}
