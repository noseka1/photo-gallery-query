package com.redhat.photogallery.query;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.redhat.photogallery.common.Constants;
import com.redhat.photogallery.common.ServerComponent;
import com.redhat.photogallery.common.data.DataStore;
import com.redhat.photogallery.common.data.LikesItem;
import com.redhat.photogallery.common.data.PhotoItem;
import com.redhat.photogallery.common.data.QueryItem;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.core.eventbus.Message;
import io.vertx.reactivex.core.eventbus.MessageProducer;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;

public class QueryComponent implements ServerComponent {

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

	@Override
	public void registerRoutes(Router router) {
		router.get("/query").handler(this::readCategoryOrderedByLikes);
	}

	@Override
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

	private void readCategoryOrderedByLikes(RoutingContext rc) {
		String category = rc.request().getParam("category");
		if (category == null) {
			LOG.error("Parameter category is missing");
			rc.response().setStatusCode(400).end();
			return;
		}
		List<QueryItem> items = dataStore.getAllItems();
		List<QueryItem> categoryItems = new ArrayList<>();
		for (QueryItem item : items) {
			if (item.getCategory().equals(category)) {
				categoryItems.add(item);
			}
		}
		categoryItems.sort(orderByLikes);

		HttpServerResponse response = rc.response();
		response.putHeader("content-type", "application/json");
		response.end(Json.encodePrettily(categoryItems));
		LOG.info("Returned {} items in category {}", categoryItems.size(), category);
	}

}