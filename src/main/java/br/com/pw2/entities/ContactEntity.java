package br.com.pw2.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

@Entity
@Cacheable
public class ContactEntity extends PanacheEntity {
    @Column(nullable = false, length = 20)
    public String number;

    public String name;

    @ManyToOne
    @JoinColumn(referencedColumnName = "id")
    public CommunityEntity communityEntity;

    public ContactEntity() {
    }

    public ContactEntity(String number, String name) {
        this.number = number;
        this.name = name;
    }

    public static ContactEntity findByNumber(String number) {
        return find("number", number).firstResult();
    }
}