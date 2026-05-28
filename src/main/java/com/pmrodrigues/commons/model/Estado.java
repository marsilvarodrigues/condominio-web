package com.pmrodrigues.commons.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.Accessors;

/**
 * JPA entity representing a Brazilian state with its name and two-letter UF abbreviation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@Entity
@Table(name = "estados")
public class Estado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, length = 2, unique = true)
    private String uf;
}