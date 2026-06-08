package com.pmrodrigues.morador.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Creation payload for a {@code Proprietario}. The {@code tipo} discriminator selects the concrete
 * sub-type ({@code PROP_PF} or {@code PROP_PJ}). Cross-field constraints ensure CPF is present for
 * natural persons and CNPJ for legal entities.
 *
 * @param tipo discriminator: {@code PROP_PF} or {@code PROP_PJ}
 * @param cpf required when tipo = PROP_PF
 * @param cnpj required when tipo = PROP_PJ
 * @param razaoSocial optional corporate name (PROP_PJ only)
 */
public record CreateProprietarioDTO(
    @NotBlank String nome,
    @NotBlank @Email String email,
    String telefone,
    @NotNull String tipo,
    String cpf,
    String cnpj,
    String razaoSocial) {

  /**
   * Returns {@code true} when tipo is not {@code PROP_PF}, or when CPF is present and non-blank.
   *
   * @return validation result
   */
  @AssertTrue(message = "CPF é obrigatório para proprietário pessoa física")
  public boolean isCpfValidoParaFisica() {
    return !"PROP_PF".equalsIgnoreCase(tipo) || (cpf != null && !cpf.isBlank());
  }

  /**
   * Returns {@code true} when tipo is not {@code PROP_PJ}, or when CNPJ is present and non-blank.
   *
   * @return validation result
   */
  @AssertTrue(message = "CNPJ é obrigatório para proprietário pessoa jurídica")
  public boolean isCnpjValidoParaJuridica() {
    return !"PROP_PJ".equalsIgnoreCase(tipo) || (cnpj != null && !cnpj.isBlank());
  }
}
