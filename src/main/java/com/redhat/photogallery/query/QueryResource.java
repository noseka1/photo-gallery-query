package com.redhat.photogallery.query;

import java.util.List;

import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.photogallery.common.Constants;
import com.redhat.photogallery.common.data.LikesMessage;
import com.redhat.photogallery.common.data.PhotoMessage;

import io.quarkus.panache.common.Sort;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.eventbus.Message;

@Path("/query")
public class QueryResource {

    private static final Logger LOG = LoggerFactory.getLogger(QueryResource.class);

    @ConsumeEvent(Constants.PHOTOS_TOPIC_NAME)
    @Transactional
    public void onNextPhoto(Message<JsonObject> photoObject) {
        PhotoMessage photoMessage = photoObject.body().mapTo(PhotoMessage.class);
        QueryItem savedItem = QueryItem.findById(photoMessage.getId());
        if (savedItem == null) {
            savedItem = new QueryItem();
            savedItem.id = photoMessage.getId();
        }
        savedItem.name = photoMessage.getName();
        savedItem.category = photoMessage.getCategory();
        savedItem.persist();
        LOG.info("Updated in data store {}", savedItem);
    }

    @ConsumeEvent(Constants.LIKES_TOPIC_NAME)
    @Transactional
    public void onNextLikes(Message<JsonObject> likesObject) {
        LikesMessage likesMessage = likesObject.body().mapTo(LikesMessage.class);
        QueryItem savedItem = QueryItem.findById(likesMessage.getId());
        if (savedItem == null) {
            savedItem = new QueryItem();
            savedItem.id = likesMessage.getId();
        }
        int likes = savedItem.likes + likesMessage.getLikes();
        savedItem.likes = likes;
        savedItem.persist();
        LOG.info("Updated in data store {}", savedItem);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response readCategoryOrderedByLikes(@QueryParam("category") String category) {
        List<QueryItem> items = QueryItem.find("category", Sort.descending("likes"), category).list();
        LOG.info("Returned {} items in category {}", items.size(), category);
        return Response.ok(new GenericEntity<List<QueryItem>>(items){}).build();
    }

}