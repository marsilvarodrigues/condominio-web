package com.pmrodrigues.morador.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Immutable snapshot of a morador's occupancy period in an apartment. Written once when a morador
 * is removed or replaced; never updated or soft-deleted. Denormalises nome, email and CPF so the
 * record remains readable even after the Pessoa is soft-deleted.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "historico_ocupacao")
public class HistoricoOcupacao {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Tenant discriminator — copied from the Pessoa's condominio at snapshot time. */
  @Column(name = "condominio_id", nullable = false)
  private Long condominioId;

  @Column(name = "apartamento_id", nullable = false)
  private Long apartamentoId;

  @Column(name = "pessoa_id", nullable = false)
  private Long pessoaId;

  @Column(name = "nome_morador", nullable = false, length = 255)
  private String nomeMorador;

  @Column(name = "email_morador", length = 255)
  private String emailMorador;

  @Column(name = "cpf_morador", length = 14)
  private String cpfMorador;

  @Column(name = "data_entrada", nullable = false)
  private LocalDate dataEntrada;

  @Column(name = "data_saida", nullable = false)
  private LocalDate dataSaida;

  @CreationTimestamp
  @Column(name = "criado_em", updatable = false)
  private LocalDateTime criadoEm;
}
