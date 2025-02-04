package br.com.pw2.resources;

import br.com.pw2.entities.CommunityEntity;
import br.com.pw2.entities.ContactEntity;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.WebApplicationException;

import java.util.List;

@ApplicationScoped
@Path("community")
public class CommunityEntityResource {

    @GET
    @Path("all")
    public List<CommunityEntity> get() {
        return CommunityEntity.listAll(Sort.by("name"));
    }

    @GET
    @Path("{id}")
    public CommunityEntity get(@PathParam("id") Long id) {
        return CommunityEntity.findById(id);
    }

    @GET
    @Path("{id}/participants")
    public List<ContactEntity> getParticipantIds(@PathParam("id") Long id) {
        CommunityEntity communityEntity = CommunityEntity.findById(id);
        if (communityEntity == null) {
            throw new WebApplicationException("Community with id " + id + " does not exist.", 404);
        }
        return ContactEntity.find("communityEntity", communityEntity).list();
    }
}