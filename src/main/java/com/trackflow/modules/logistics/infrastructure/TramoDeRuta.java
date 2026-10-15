package com.trackflow.modules.logistics.infrastructure;

import com.trackflow.modules.logistics.domain.Tramo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * El tramo tal como está en la base. Se carga con las migraciones y la aplicación solo
 * lo lee, igual que los centros.
 */
@Entity
@Table(name = "logistics_route_legs")
class TramoDeRuta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long fromCityId;

    @Column(nullable = false)
    private Long toCityId;

    @Column(nullable = false)
    private int hours;

    @Column(nullable = false)
    private boolean active;

    protected TramoDeRuta() {
    }

    Tramo toTramo() {
        return new Tramo(fromCityId, toCityId, hours);
    }
}
