package com.pmrodrigues.morador.mapper;

import com.pmrodrigues.condominio.dto.ApartamentoResumoDTO;
import com.pmrodrigues.condominio.model.Apartamento;
import com.pmrodrigues.morador.dto.CreateProprietarioDTO;
import com.pmrodrigues.morador.dto.ProprietarioDTO;
import com.pmrodrigues.morador.dto.UpdateProprietarioDTO;
import com.pmrodrigues.morador.model.Proprietario;
import com.pmrodrigues.morador.model.ProprietarioPessoaFisica;
import com.pmrodrigues.morador.model.ProprietarioPessoaJuridica;
import jakarta.persistence.EntityManager;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MapStruct mapper for converting between {@link Proprietario} entities and their DTO representations.
 * Entity creation is handled manually because the concrete sub-type depends on the {@code tipo} discriminator.
 */
@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public abstract class ProprietarioMapper {

    protected EntityManager em;

    @Autowired(required = false)
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    /**
     * Returns a JPA proxy for the given apartment id, or null if id is null.
     *
     * @param id apartment primary key
     * @return proxy or null
     */
    public Apartamento apartamentoFromId(Long id) {
        if (id == null) return null;
        return em.getReference(Apartamento.class, id);
    }

    /**
     * Converts a {@link Proprietario} entity to its full DTO, including the list of apartments
     * and discriminated type.
     *
     * @param proprietario source entity
     * @return mapped DTO
     */
    @Mapping(target = "nome", source = "name")
    @Mapping(target = "userId", source = "proprietario", qualifiedByName = "resolveUserId")
    @Mapping(target = "tipo", source = "proprietario", qualifiedByName = "resolveTipo")
    @Mapping(target = "cpf", source = "proprietario", qualifiedByName = "resolveCpf")
    @Mapping(target = "cnpj", source = "proprietario", qualifiedByName = "resolveCnpj")
    @Mapping(target = "razaoSocial", source = "proprietario", qualifiedByName = "resolveRazaoSocial")
    @Mapping(target = "apartamentos", source = "proprietario", qualifiedByName = "resolveApartamentos")
    public abstract ProprietarioDTO toDTO(Proprietario proprietario);

    /**
     * Creates the appropriate concrete {@link Proprietario} sub-type from a creation payload.
     *
     * @param dto creation payload
     * @return new concrete entity
     * @throws IllegalArgumentException if the tipo value is unrecognised
     */
    public Proprietario toEntity(CreateProprietarioDTO dto) {
        if ("PROP_PF".equalsIgnoreCase(dto.tipo())) {
            ProprietarioPessoaFisica ppf = new ProprietarioPessoaFisica();
            ppf.setName(dto.nome());
            ppf.setEmail(dto.email());
            ppf.setTelefone(dto.telefone());
            ppf.setCpf(dto.cpf());
            return ppf;
        } else if ("PROP_PJ".equalsIgnoreCase(dto.tipo())) {
            ProprietarioPessoaJuridica ppj = new ProprietarioPessoaJuridica();
            ppj.setName(dto.nome());
            ppj.setEmail(dto.email());
            ppj.setTelefone(dto.telefone());
            ppj.setCnpj(dto.cnpj());
            ppj.setRazaoSocial(dto.razaoSocial());
            return ppj;
        }
        throw new IllegalArgumentException("Tipo de proprietário desconhecido: " + dto.tipo());
    }

    /**
     * Applies the non-null fields from {@code dto} onto the existing {@link Proprietario} entity.
     * The {@code tipo} discriminator, {@code id}, {@code condominio}, and audit timestamps are
     * never changed by this method.
     *
     * @param dto        partial-update payload
     * @param proprietario entity to update in-place
     */
    public void updateEntity(UpdateProprietarioDTO dto, @MappingTarget Proprietario proprietario) {
        if (dto.nome() != null) proprietario.setName(dto.nome());
        if (dto.email() != null) proprietario.setEmail(dto.email());
        if (dto.telefone() != null) proprietario.setTelefone(dto.telefone());
        if (proprietario instanceof ProprietarioPessoaFisica ppf && dto.cpf() != null) {
            ppf.setCpf(dto.cpf());
        }
        if (proprietario instanceof ProprietarioPessoaJuridica ppj) {
            if (dto.cnpj() != null) ppj.setCnpj(dto.cnpj());
            if (dto.razaoSocial() != null) ppj.setRazaoSocial(dto.razaoSocial());
        }
    }

    // ── Named helpers ──────────────────────────────────────────────────────────

    /**
     * Resolves the user id — equals the entity id since Proprietario IS-A User.
     *
     * @param proprietario source entity
     * @return entity id
     */
    @Named("resolveUserId")
    protected Long resolveUserId(Proprietario proprietario) {
        return proprietario.getId();
    }

    /**
     * Resolves the discriminator tipo string from the concrete runtime type.
     *
     * @param proprietario source entity
     * @return discriminator string (PROP_PF, PROP_PJ)
     */
    @Named("resolveTipo")
    protected String resolveTipo(Proprietario proprietario) {
        var dv = proprietario.getClass().getAnnotation(jakarta.persistence.DiscriminatorValue.class);
        return dv != null ? dv.value() : proprietario.getClass().getSimpleName();
    }

    /**
     * Extracts the CPF for {@link ProprietarioPessoaFisica}; returns {@code null} otherwise.
     *
     * @param proprietario source entity
     * @return cpf or {@code null}
     */
    @Named("resolveCpf")
    protected String resolveCpf(Proprietario proprietario) {
        if (proprietario instanceof ProprietarioPessoaFisica ppf) return ppf.getCpf();
        return null;
    }

    /**
     * Extracts the CNPJ for {@link ProprietarioPessoaJuridica}; returns {@code null} otherwise.
     *
     * @param proprietario source entity
     * @return cnpj or {@code null}
     */
    @Named("resolveCnpj")
    protected String resolveCnpj(Proprietario proprietario) {
        if (proprietario instanceof ProprietarioPessoaJuridica ppj) return ppj.getCnpj();
        return null;
    }

    /**
     * Extracts the razão social for {@link ProprietarioPessoaJuridica}; returns {@code null} otherwise.
     *
     * @param proprietario source entity
     * @return razaoSocial or {@code null}
     */
    @Named("resolveRazaoSocial")
    protected String resolveRazaoSocial(Proprietario proprietario) {
        if (proprietario instanceof ProprietarioPessoaJuridica ppj) return ppj.getRazaoSocial();
        return null;
    }

    /**
     * Maps the {@link Apartamento} set to a list of {@link ApartamentoResumoDTO}.
     *
     * @param proprietario source entity
     * @return list of apartment summaries
     */
    @Named("resolveApartamentos")
    protected List<ApartamentoResumoDTO> resolveApartamentos(Proprietario proprietario) {
        Set<Apartamento> apts = proprietario.getApartamentos();
        if (apts == null) return List.of();
        return apts.stream()
                .map(a -> new ApartamentoResumoDTO(
                        a.getId(),
                        a.getNumero(),
                        a.getBloco() != null ? a.getBloco().getBloco() : null))
                .collect(Collectors.toList());
    }
}
