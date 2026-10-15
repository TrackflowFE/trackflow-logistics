package com.trackflow.modules.logistics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Punto de la cadena logística: centro de distribución, punto de recolección u
 * oficina. Es propio de logistics — a diferencia de Ciudad, ningún otro módulo lo
 * necesita, así que no vive en shared/geografia. Se carga con las migraciones y la
 * aplicación solo lo lee: no hay caso de uso que cree centros.
 */
@Entity
@Table(name = "logistics_centers")
public class Centro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoCentro type;

    @Column(nullable = false)
    private Long cityId;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private boolean active;

    protected Centro() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public TipoCentro getType() {
        return type;
    }

    public Long getCityId() {
        return cityId;
    }

    public String getAddress() {
        return address;
    }

    public boolean isActive() {
        return active;
    }
}
