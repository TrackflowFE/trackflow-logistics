package com.trackflow.shared.geografia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * El catálogo tal como está en la base. Se carga con las migraciones y la aplicación
 * solo lo lee: no hay caso de uso que cree municipios.
 */
@Entity
@Table(name = "cities")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CiudadRegistrada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 120)
    private String department;

    Ciudad toCiudad() {
        return new Ciudad(id, name, department);
    }
}
