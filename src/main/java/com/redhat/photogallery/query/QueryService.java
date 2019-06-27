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
import com.redhat.photogallery.common.data.LikesAddedMessage;
import com.redhat.photogallery.common.data.PhotoCreatedMessage;

import io.quarkus.panache.common.Sort;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.eventbus.Message;

@Path("/query")
public class QueryService {

    private static final Logger LOG = LoggerFactory.getLogger(QueryService.class);

    @ConsumeEvent(value = Constants.PHOTOS_TOPIC_NAME, blocking = true)
    @Transactional
    public void onNextPhotoCreated(Message<JsonObject> photoObject) {
        PhotoCreatedMessage message = photoObject.body().mapTo(PhotoCreatedMessage.class);
        QueryItem savedItem = QueryItem.findById(message.getId());
        if (savedItem == null) {
            savedItem = new QueryItem();
            savedItem.id = message.getId();
            savedItem.persist();
        }
        savedItem.name = message.getName();
        savedItem.category = message.getCategory();
        LOG.info("Updated in data store {}", savedItem);
    }

    @ConsumeEvent(value = Constants.LIKES_TOPIC_NAME, blocking = true)
    @Transactional
    public void onNextLikesAdded(Message<JsonObject> likesObject) {
        LikesAddedMessage message = likesObject.body().mapTo(LikesAddedMessage.class);
        QueryItem savedItem = QueryItem.findById(message.getId());
        if (savedItem == null) {
            savedItem = new QueryItem();
            savedItem.id = message.getId();
            savedItem.persist();
        }
        int likes = message.getLikes();
        savedItem.likes = likes;
        LOG.info("Updated in data store {}", savedItem);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response readCategoryOrderedByLikes(@QueryParam("category") String category) {
        List<QueryItem> items = QueryItem.list("category", Sort.by("likes").descending(), category);
        LOG.info("Returned {} items in category {}", items.size(), category);
        return Response.ok(new GenericEntity<List<QueryItem>>(items){}).build();
    }

}