/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.greglturnquist.learningspringboot.chat;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Greg Turnquist
 */
// tag::code-1[]
@Service
@EnableBinding(Sink.class)
public class CommentService implements WebSocketHandler {

	private final static Logger log =
		LoggerFactory.getLogger(CommentService.class);
	// end::code-1[]

	// tag::code-2[]
	private ObjectMapper mapper;
	private Flux<Comment> flux;
	private FluxSink<Comment> webSocketCommentSink;

	CommentService(ObjectMapper mapper) {
		this.mapper = mapper;
		this.flux = Flux.<Comment>create(
			emitter -> this.webSocketCommentSink = emitter,
			FluxSink.OverflowStrategy.IGNORE)
				.publish()
				.autoConnect();
	}
	// end::code-2[]

	// tag::broadcast[]
	@StreamListener(Sink.INPUT)
	public void broadcast(Comment comment) {
		if (webSocketCommentSink != null) {
			log.info("Publishing " + comment.toString() +
				" to websocket...");
			webSocketCommentSink.next(comment);
		}
	}
	// end::broadcast[]

	// tag::handle[]
	@Override
	public Mono<Void> handle(WebSocketSession session) {
		return session.send(this.flux
			.map(comment -> {
				try {
					return mapper.writeValueAsString(comment);
				} catch (JsonProcessingException e) {
					throw new RuntimeException(e);
				}
			})
			.log("encode-as-json")
			.map(session::textMessage)
			.log("wrap-as-websocket-message"))
		.log("publish-to-websocket");
	}
	// end::handle[]

}
// end::code[]
