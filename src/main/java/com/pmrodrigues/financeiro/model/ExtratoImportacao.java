package com.pmrodrigues.financeiro.model;

import com.pmrodrigues.commons.config.TenantFilterAspect;
import com.pmrodrigues.commons.tenant.TenantContext;
import com.pmrodrigues.condominio.model.Condominio;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Header record for a bank statement import (OFX or CSV), partitioned by condominio_id.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@Entity
@Table(name = "extrato_importacoes")
@SQLDelete(sql = "UPDATE extrato_importacoes SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
@Filter(name = TenantFilterAspect.CONDOMINIO_FILTER, condition = "condominio_id = :condominioId")
@EntityListeners(AuditingEntityListener.class)
public class ExtratoImportacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condominio_id", nullable = false)
    private Condominio condominio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conta_bancaria_id", nullable = false)
    private ContaBancaria contaBancaria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FormatoExtrato formato;

    @Column(name = "data_importacao", nullable = false)
    private LocalDateTime dataImportacao;

    @Column(name = "data_inicio")
    private LocalDate dataInicio;

    @Column(name = "data_fim")
    private LocalDate dataFim;

    @Column(name = "total_itens", nullable = false)
    @Builder.Default
    private int totalItens = 0;

    @Column(name = "itens_conciliados", nullable = false)
    @Builder.Default
    private int itensConciliados = 0;

    @Column(name = "itens_pendentes", nullable = false)
    @Builder.Default
    private int itensPendentes = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private StatusExtrato status = StatusExtrato.PROCESSANDO;

    @Column(name = "mensagem_erro")
    private String mensagemErro;

    @Column(name = "nome_arquivo", length = 500)
    private String nomeArquivo;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    /**
     * Sets the {@code condominio} from {@link TenantContext} before the entity is first persisted.
     */
    @PrePersist
    protected void prePersist() {
        Long condominioId = TenantContext.getCondominioId();
        if (condominioId != null) {
            var c = new Condominio();
            c.setId(condominioId);
            this.condominio = c;
        }
    }
}
