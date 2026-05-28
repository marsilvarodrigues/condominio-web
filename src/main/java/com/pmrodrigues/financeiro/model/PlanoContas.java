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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Hierarchical chart of accounts node for a condominium, supporting tree structure via self-reference.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@Entity
@Table(name = "plano_contas")
@SQLDelete(sql = "UPDATE plano_contas SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
@Filter(name = "condominioFilter", condition = "condominio_id = :condominioId")
@EntityListeners(AuditingEntityListener.class)
public class PlanoContas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condominio_id", nullable = false)
    private Condominio condominio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pai_id")
    private PlanoContas pai;

    @OneToMany(mappedBy = "pai", fetch = FetchType.LAZY)
    @Builder.Default
    private List<PlanoContas> filhos = new ArrayList<>();

    @Column(nullable = false, length = 20)
    private String codigo;

    @Column(nullable = false)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoConta tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_rateio", length = 20)
    private TipoRateio tipoRateio;

    @Enumerated(EnumType.STRING)
    @Column(name = "escopo_rateio", length = 20)
    private EscopoRateio escopoRateio;

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
