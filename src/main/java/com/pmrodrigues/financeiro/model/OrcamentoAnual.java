package com.pmrodrigues.financeiro.model;

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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Annual budget for a condominium, transitioning through RASCUNHO → APROVADO → ENCERRADO.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@Entity
@Table(name = "orcamento_anual")
@SQLDelete(sql = "UPDATE orcamento_anual SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
@Filter(name = "condominioFilter", condition = "condominio_id = :condominioId")
@EntityListeners(AuditingEntityListener.class)
public class OrcamentoAnual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condominio_id", nullable = false)
    private Condominio condominio;

    @Column(nullable = false)
    private Integer exercicio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private StatusOrcamento status = StatusOrcamento.RASCUNHO;

    @Column(name = "taxa_estimada_unidade", precision = 15, scale = 2)
    private BigDecimal taxaEstimadaUnidade;

    @OneToMany(mappedBy = "orcamentoAnual", fetch = FetchType.LAZY)
    @Builder.Default
    private List<ItemOrcamento> itens = new ArrayList<>();

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
