package br.com.pw2.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.QueryHint;

import java.time.ZonedDateTime;
import java.util.Base64;

@Entity
@NamedQuery(name = "AdvertisingEntity.findAll",
        query = "SELECT a.id, a.message, a.zonedDateTime FROM AdvertisingEntity a")
@NamedQuery(name = "AdvertisingEntity.findAllNotSentYet",
        query = "SELECT a.id, a.zonedDateTime FROM AdvertisingEntity a WHERE a.sent = false",
        hints = @QueryHint(name = "org.hibernate.cacheable", value = "false"))
public class AdvertisingEntity extends PanacheEntity {
    public String message;
    public ZonedDateTime zonedDateTime;
    public Boolean sent;
    @Column(columnDefinition = "bytea")
    public byte[] imageData;

    public AdvertisingEntity() {
    }

    public AdvertisingEntity(String message, byte[] imageData, ZonedDateTime zonedDateTime) {
        this.message = message;
        this.zonedDateTime = zonedDateTime;
        this.sent = false;
        this.imageData = imageData;
    }

    public String getImageDataAsBase64() {
        if (imageData != null && imageData.length > 0) {
            return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(imageData);
        }
        return null; // Return null if there's no image data
    }
}