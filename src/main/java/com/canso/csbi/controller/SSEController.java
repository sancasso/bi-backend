package com.canso.csbi.controller;

import com.canso.csbi.model.entity.EventData;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;

import java.time.Duration;
import java.util.Date;
import java.util.stream.Stream;

@RestController
public class SSEController {
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<EventData> streamEvents() {
        Flux<EventData> eventDataFlux = Flux.fromStream(Stream.generate(() -> new EventData(new Date())));
        return Flux.zip(eventDataFlux, Flux.interval(Duration.ofSeconds(2)))
                .map(Tuple2::getT1);
    }
}