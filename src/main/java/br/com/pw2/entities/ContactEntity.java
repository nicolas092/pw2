package br.com.pw2.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Cacheable
public class ContactEntity extends PanacheEntity {
    @Column(nullable = false, length = 20)
    public String number;

    public String name;

    @NotNull
    public Boolean authorizedToSendMessages;

    @ManyToOne
    @JoinColumn(referencedColumnName = "id")
    public CommunityEntity communityEntity;

    public ContactEntity() {
    }

    public ContactEntity(String number, String name) {
        this.number = number;
        this.name = name;
        this.authorizedToSendMessages = false;
    }

    public static ContactEntity findByNumber(String number) {
        return find("number", number).firstResult();
    }
}