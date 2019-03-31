package com.redhat.photogallery.query;

import com.redhat.photogallery.common.Server;
import com.redhat.photogallery.common.VertxInit;

public class QueryServer {

	private static final int LISTEN_PORT = 8082;

	public static void main(String[] args) {
		VertxInit.createClusteredVertx(vertx -> {
			vertx.deployVerticle(new Server(LISTEN_PORT, new QueryComponent()));
		});
	}
}
