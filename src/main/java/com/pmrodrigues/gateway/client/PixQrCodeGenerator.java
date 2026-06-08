package com.pmrodrigues.gateway.client;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import io.micrometer.core.annotation.Timed;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Generates QR Code PNG images from Pix copia-e-cola (EMV) payloads using the ZXing library.
 *
 * <p>The output is a Base64-encoded PNG suitable for embedding in HTML email bodies as {@code
 * &lt;img src="data:image/png;base64,..."&gt;}.
 */
@Slf4j
@Component
public class PixQrCodeGenerator {

  private static final int QR_SIZE = 300;

  /**
   * Generates a 300×300 PNG QR Code from the given Pix EMV payload and returns it as Base64.
   *
   * @param pixPayload Pix copia-e-cola string (EMV format) from Asaas
   * @return Base64-encoded PNG string (no data URI prefix)
   * @throws IllegalArgumentException if {@code pixPayload} is null or blank
   * @throws RuntimeException if ZXing encoding or PNG writing fails
   */
  @Timed(value = "pix.qrcode.generator", description = "Generate Pix QR Code as Base64 PNG")
  public String gerarBase64(String pixPayload) {
    if (pixPayload == null || pixPayload.isBlank()) {
      throw new IllegalArgumentException("pixPayload must not be null or blank");
    }
    try {
      var hints =
          Map.<EncodeHintType, Object>of(
              EncodeHintType.CHARACTER_SET,
              "UTF-8",
              EncodeHintType.ERROR_CORRECTION,
              ErrorCorrectionLevel.M,
              EncodeHintType.MARGIN,
              1);

      BitMatrix matrix =
          new QRCodeWriter().encode(pixPayload, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints);

      var out = new ByteArrayOutputStream();
      MatrixToImageWriter.writeToStream(matrix, "PNG", out);
      String base64 = Base64.getEncoder().encodeToString(out.toByteArray());
      log.debug(
          "Pix QR Code generated: {}x{} pixels, {} Base64 chars",
          QR_SIZE,
          QR_SIZE,
          base64.length());
      return base64;
    } catch (Exception e) {
      log.error("Failed to generate Pix QR Code: {}", e.getMessage(), e);
      throw new RuntimeException("Erro ao gerar QR Code Pix", e);
    }
  }
}
