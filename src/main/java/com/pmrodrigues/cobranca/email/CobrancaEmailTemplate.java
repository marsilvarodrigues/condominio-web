package com.pmrodrigues.cobranca.email;

import com.pmrodrigues.commons.email.Template;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Email template for the billing notification sent to a resident when a charge is issued.
 *
 * <p>Template file: {@code templates/email/cobranca.vm} The record component names match the {@code
 * $variableName} placeholders in the template exactly. Monetary and date formatting is encapsulated
 * in {@link #model()} so the template itself remains logic-free.
 *
 * @param moradorNome resident display name
 * @param condominioNome condominium name used in the subject and template header
 * @param apartamentoDesc human-readable apartment label, e.g. "Apt 101 · Bloco A"
 * @param valor charge amount in BRL (formatted to "R$ X.XXX,XX" in the model)
 * @param vencimento due date (formatted to "dd/MM/yyyy" in the model)
 * @param boletoUrl URL to download the boleto PDF (empty string if absent)
 * @param boletoCodBarras boleto barcode / nosso número (empty string if absent)
 * @param pixCopiaCola Pix copia-e-cola EMV string (empty string if absent)
 * @param pixQrCodeBase64 Base64-encoded PNG QR Code image (empty string if absent)
 */
public record CobrancaEmailTemplate(
    String moradorNome,
    String condominioNome,
    String apartamentoDesc,
    BigDecimal valor,
    LocalDate vencimento,
    String boletoUrl,
    String boletoCodBarras,
    String pixCopiaCola,
    String pixQrCodeBase64)
    implements Template {

  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  @Override
  public String templatePath() {
    return "templates/email/cobranca.vm";
  }

  @Override
  public String subject() {
    return "Taxa Condominial — " + condominioNome;
  }

  @Override
  public Map<String, Object> model() {
    var map = new HashMap<String, Object>();
    map.put("moradorNome", moradorNome);
    map.put("condominioNome", condominioNome);
    map.put("apartamentoDesc", apartamentoDesc);
    map.put("valor", formatValor(valor));
    map.put("vencimento", vencimento != null ? vencimento.format(DATE_FMT) : "");
    map.put("boletoUrl", nullSafe(boletoUrl));
    map.put("boletoCodBarras", nullSafe(boletoCodBarras));
    map.put("pixCopiaCola", nullSafe(pixCopiaCola));
    map.put("pixQrCodeBase64", nullSafe(pixQrCodeBase64));
    map.put("anoCorrente", String.valueOf(LocalDate.now().getYear()));
    return map;
  }

  private static String formatValor(BigDecimal v) {
    if (v == null) {
      return "R$ 0,00";
    }
    return "R$ " + String.format("%,.2f", v).replace(",", "X").replace(".", ",").replace("X", ".");
  }

  private static String nullSafe(String s) {
    return s != null ? s : "";
  }
}
