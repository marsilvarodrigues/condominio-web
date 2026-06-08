package com.pmrodrigues.morador.mapper;

import com.pmrodrigues.condominio.model.Apartamento;
import com.pmrodrigues.morador.dto.CreatePessoaDTO;
import com.pmrodrigues.morador.dto.PessoaDTO;
import com.pmrodrigues.morador.dto.UpdatePessoaDTO;
import com.pmrodrigues.morador.model.Morador;
import com.pmrodrigues.morador.model.Pessoa;
import com.pmrodrigues.morador.model.ProprietarioPessoaFisica;
import jakarta.persistence.EntityManager;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * MapStruct mapper for converting between {@link Pessoa} entities and their DTO representations.
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public abstract class PessoaMapper {

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
   * Converts a {@link Pessoa} entity to its full DTO representation. The {@code tipo} field carries
   * the SINGLE_TABLE discriminator value.
   *
   * @param pessoa source entity
   * @return mapped DTO
   */
  @Mapping(target = "nome", source = "name")
  @Mapping(target = "userId", source = "pessoa", qualifiedByName = "resolveUserId")
  @Mapping(target = "tipo", source = "pessoa", qualifiedByName = "resolveTipo")
  @Mapping(target = "cpf", source = "pessoa", qualifiedByName = "resolveCpf")
  @Mapping(target = "apartamentoId", source = "apartamento.id")
  @Mapping(target = "apartamentoNumero", source = "apartamento.numero")
  public abstract PessoaDTO toDTO(Pessoa pessoa);

  /**
   * Creates a new {@link Morador} from the creation payload.
   *
   * @param dto creation payload
   * @return new Morador entity (id, condominio, deleted, audit, and user fields are ignored)
   */
  public Pessoa toEntity(CreatePessoaDTO dto) {
    Morador m = new Morador();
    m.setName(dto.nome());
    m.setEmail(dto.email());
    m.setTelefone(dto.telefone());
    m.setCpf(dto.cpf());
    return m;
  }

  /**
   * Applies non-null DTO fields onto an existing entity. {@code id}, {@code condominio},
   * soft-delete, and audit fields are never overwritten.
   *
   * @param pessoa target entity
   * @param dto source update data
   */
  public void updateEntity(Pessoa pessoa, UpdatePessoaDTO dto) {
    if (dto.nome() != null) pessoa.setName(dto.nome());
    if (dto.email() != null) pessoa.setEmail(dto.email());
    if (dto.telefone() != null) pessoa.setTelefone(dto.telefone());
    if (dto.cpf() != null && pessoa instanceof Morador m) m.setCpf(dto.cpf());
    if (dto.cpf() != null && pessoa instanceof ProprietarioPessoaFisica ppf) ppf.setCpf(dto.cpf());
  }

  // ── Named helpers ──────────────────────────────────────────────────────────

  /**
   * Resolves the user id (which is the same as the entity id since Pessoa IS-A User).
   *
   * @param pessoa source entity
   * @return entity id (used as userId in the DTO)
   */
  @Named("resolveUserId")
  protected Long resolveUserId(Pessoa pessoa) {
    return pessoa.getId();
  }

  /**
   * Resolves the discriminator tipo string from the concrete runtime type.
   *
   * @param pessoa source entity
   * @return discriminator string (MORADOR, PROP_PF, PROP_PJ)
   */
  @Named("resolveTipo")
  protected String resolveTipo(Pessoa pessoa) {
    return pessoa.getClass().getAnnotation(jakarta.persistence.DiscriminatorValue.class) != null
        ? pessoa.getClass().getAnnotation(jakarta.persistence.DiscriminatorValue.class).value()
        : pessoa.getClass().getSimpleName();
  }

  /**
   * Extracts the CPF from a {@link Morador} or {@link ProprietarioPessoaFisica}; returns {@code
   * null} for other types.
   *
   * @param pessoa source entity
   * @return cpf or {@code null}
   */
  @Named("resolveCpf")
  protected String resolveCpf(Pessoa pessoa) {
    if (pessoa instanceof Morador m) return m.getCpf();
    if (pessoa instanceof ProprietarioPessoaFisica ppf) return ppf.getCpf();
    return null;
  }
}
