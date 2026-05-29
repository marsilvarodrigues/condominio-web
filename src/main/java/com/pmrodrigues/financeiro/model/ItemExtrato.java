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
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Single transaction line from an imported bank statement, partitioned by condominio_id.
 * {@code lancamento} is populated when the item is matched to an existing entry.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@Entity
@Table(name = "itens_extrato")
@SQLDelete(sql = "UPDATE itens_extrato SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
@Filter(name = TenantFilterAspect.CONDOMINIO_FILTER, condition = "condominio_id = :condominioId")
@EntityListeners(AuditingEntityListener.class)
public class ItemExtrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condominio_id", nullable = false)
    private Condominio condominio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "extrato_importacao_id", nullable = false)
    private ExtratoImportacao extratoImportacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lancamento_id")
    private LancamentoBancario lancamento;

    @Column(name = "data_lancamento", nullable = false)
    private LocalDate dataLancamento;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoLancamento tipo;

    @Column(length = 500)
    private String descricao;

    @Column(name = "numero_documento", length = 100)
    private String numeroDocumento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private StatusItemExtrato status = StatusItemExtrato.PENDENTE;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
