package com.netflix.streaming_service.controller;

import com.netflix.common_lib.dto.response.StreamingResponse;
import com.netflix.streaming_service.service.StreamingService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/v1/streaming")
public class StreamingController {
    private final StreamingService streamingService;

    @GetMapping(path = "/{movieId}")
    public ResponseEntity<StreamingResponse> getStreamingUrl(@PathVariable(name = "movieId") UUID movieId) {
        StreamingResponse streamingResponse = streamingService.getStreamingUrl(movieId);
        return ResponseEntity.status(HttpStatus.OK).body(streamingResponse);
    }

    /*
    * GET /api/v1/streaming/{movieId}/playlist?path=encoded/movieId/1080p/playlist.m3u8
    * */
    @GetMapping(path = "/{movieId}/playlist")
    public ResponseEntity<String> getSignedPlaylist(
            @PathVariable(name = "movieId") UUID movieId,
            @RequestParam(name = "path") String path
    ) {
        log.info("playlist request for movieId: {}, path: {}", movieId, path);
        String signedPlaylist = streamingService.getSignedPlaylist(movieId, path);
        return ResponseEntity.status(HttpStatus.OK)
                .header("Content-Type", "application/x-mpegURL")
                .body(signedPlaylist);
    }
}
