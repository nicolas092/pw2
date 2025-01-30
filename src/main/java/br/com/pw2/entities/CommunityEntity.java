package br.com.pw2.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Cacheable
public class CommunityEntity extends PanacheEntity {
    @Column(nullable = false, unique = true, length = 23)
    public String number;
    @NotNull
    public String name;
    @NotNull
    public Integer numberOfContacts;
    @NotNull
    public Boolean receivingNewContacts;
    @Enumerated(EnumType.STRING)
    public Category category;

}