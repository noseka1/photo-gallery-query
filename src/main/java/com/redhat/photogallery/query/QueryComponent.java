package com.redhat.photogallery.query;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.QueryParam;

import com.redhat.photogallery.common.Constants;
import com.redhat.photogallery.common.data.DataStore;
import com.redhat.photogallery.common.data.LikesItem;
import com.redhat.photogallery.common.data.PhotoItem;
import com.redhat.photogallery.common.data.QueryItem;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.core.eventbus.Message;
import io.vertx.reactivex.core.eventbus.MessageProducer;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;

@Path("/query")
public class QueryComponent {

    private static final Logger LOG = LoggerFactory.getLogger(QueryComponent.class);

    private DataStore<QueryItem> dataStore = new DataStore<>();

    MessageProducer<JsonObject> topic;

    private static final Comparator<QueryItem> orderByLikes = (o1, o2) -> {
        if (o1.getLikes() > o2.getLikes()) {
            return -1;
        } else if (o1.getLikes() < o2.getLikes()) {
            return 1;
        }
        return 0;
    };

    @Inject
    public void injectEventBus(EventBus eventBus) {
        eventBus.<JsonObject>consumer(Constants.PHOTOS_TOPIC_NAME).toObservable().subscribe(this::onNextPhoto,
                this::onErrorPhoto);
        eventBus.<JsonObject>consumer(Constants.LIKES_TOPIC_NAME).toObservable().subscribe(this::onNextLikes,
                this::onErrorLikes);
    }

    private void onNextPhoto(Message<JsonObject> photoObject) {
        PhotoItem item = photoObject.body().mapTo(PhotoItem.class);
        QueryItem savedItem = dataStore.getItem(item.getId());
        if (savedItem == null) {
            savedItem = new QueryItem();
            savedItem.setId(item.getId());
        }
        savedItem.setName(item.getName());
        savedItem.setCategory(item.getCategory());
        dataStore.putItem(savedItem);
        LOG.info("Updated in data store {}", savedItem);
    }

    private void onErrorPhoto(Throwable t) {
        LOG.error("Failed to receive photo", t);
    }

    private void onNextLikes(Message<JsonObject> likesObject) {
        LikesItem item = likesObject.body().mapTo(LikesItem.class);
        QueryItem savedItem = dataStore.getItem(item.getId());
        if (savedItem == null) {
            savedItem = new QueryItem();
            savedItem.setId(item.getId());
        }
        int likes = savedItem.getLikes() + item.getLikes();
        savedItem.setLikes(likes);
        dataStore.putItem(savedItem);
        LOG.info("Updated in data store {}", savedItem);
    }

    private void onErrorLikes(Throwable t) {
        LOG.error("Failed to receive likes", t);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response readCategoryOrderedByLikes(@QueryParam("category") String category) {

        List<QueryItem> items = dataStore.getAllItems();
        List<QueryItem> categoryItems = new ArrayList<>();
        for (QueryItem item : items) {
            if (item.getCategory().equals(category)) {
                categoryItems.add(item);
            }
        }
        categoryItems.sort(orderByLikes);
        LOG.info("Returned {} items in category {}", categoryItems.size(), category);
        return Response.ok(new GenericEntity<List<QueryItem>>(categoryItems){}).build();
    }

}