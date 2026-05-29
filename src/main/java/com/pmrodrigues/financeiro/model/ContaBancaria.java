package com.pmrodrigues.financeiro.model;

import com.pmrodrigues.commons.config.TenantFilterAspect;
import com.pmrodrigues.commons.model.Banco;
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

/**
 * Bank account belonging to a condominium, partitioned by condominio_id.
 * At most one account may have tipo {@link TipoContaBancaria#FUNDO_RESERVA} per condominium.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@Entity
@Table(name = "contas_bancarias")
@SQLDelete(sql = "UPDATE contas_bancarias SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
@Filter(name = TenantFilterAspect.CONDOMINIO_FILTER, condition = "condominio_id = :condominioId")
@EntityListeners(AuditingEntityListener.class)
public class ContaBancaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condominio_id", nullable = false)
    private Condominio condominio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banco_id", nullable = false)
    private Banco banco;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoContaBancaria tipo;

    @Column(nullable = false, length = 10)
    private String agencia;

    @Column(nullable = false, length = 20)
    private String conta;

    @Column(length = 2)
    private String digito;

    @Column(length = 255)
    private String descricao;

    @Column(name = "chave_pix", length = 255)
    private String chavePix;

    @Column(name = "saldo_contabil", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal saldoContabil = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private boolean ativa = true;

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
