package com.redhat.photogallery.query;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
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

import io.quarkus.vertx.ConsumeEvent;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.eventbus.Message;

@Path("/query")
public class QueryService {

    private static final Logger LOG = LoggerFactory.getLogger(QueryService.class);

    @Inject
    EntityManager entityManager;

    @ConsumeEvent(value = Constants.PHOTOS_TOPIC_NAME, blocking = true)
    @Transactional
    public void onNextPhotoCreated(Message<JsonObject> photoObject) {
        PhotoCreatedMessage message = photoObject.body().mapTo(PhotoCreatedMessage.class);
        QueryItem savedItem = entityManager.find(QueryItem.class, message.getId());
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
        QueryItem savedItem = entityManager.find(QueryItem.class, message.getId());
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
        Query query = entityManager.createQuery("FROM QueryItem WHERE category =?1 ORDER BY likes DESC");
        query.setParameter(1, category);
        @SuppressWarnings("unchecked")
        List<QueryItem> items = query.getResultList();
        LOG.info("Returned {} items in category {}", items.size(), category);
        return Response.ok(new GenericEntity<List<QueryItem>>(items){}).build();
    }

}