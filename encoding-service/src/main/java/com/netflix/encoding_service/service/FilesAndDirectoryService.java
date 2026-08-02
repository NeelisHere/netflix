package com.netflix.encoding_service.service;

import com.netflix.common_lib.dto.exception.CommonException;
import com.netflix.common_lib.utils.VideoQuality;
import com.netflix.common_lib.utils.VideoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilesAndDirectoryService {
    public void generateMasterPlaylist(String masterPlaylistPath) {
        /*
        *   #EXTM3U
            #EXT-X-VERSION:3

            #EXT-X-STREAM-INF:BANDWIDTH=5000000,RESOLUTION=1920x1080,CODECS="avc1.42e01e,mp4a.40.2"1080p/playlist.m3u8

            #EXT-X-STREAM-INF:BANDWIDTH=2800000,RESOLUTION=1280x720,CODECS="avc1.42e01e,mp4a.40.2"720p/playlist.m3u8

            #EXT-X-STREAM-INF:BANDWIDTH=1400000,RESOLUTION=854x480,CODECS="avc1.42e01e,mp4a.40.2"480p/playlist.m3u8

            #EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=640x360,CODECS="avc1.42e01e,mp4a.40.2"360p/playlist.m3u8
        * */
        StringBuilder master = new StringBuilder("#EXTM3U\n#EXT-X-VERSION:3\n\n");
        for (VideoQuality videoQuality : VideoUtils.VIDEO_QUALITIES) {
            String s = String.format(
                "#EXT-X-STREAM-INF:BANDWIDTH=%d,RESOLUTION=%dx%d,CODECS=\"avc1.42e01e,mp4a.40.2\"%dp/playlist.m3u8\n\n",
                videoQuality.bitrate() * 1000, videoQuality.width(), videoQuality.height(), videoQuality.height()
            );
            master.append(s);
        }
        try {
            Files.writeString(Paths.get(masterPlaylistPath), master.toString());
            log.info("Master Playlist generated at: {}", masterPlaylistPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void cleanupJobDirectory(String jobDir) {
        Path dirPath = Paths.get(jobDir);
        if (!Files.exists(dirPath)) {
            log.info("No content on {}", jobDir);
            return;
        }
        try (Stream<Path> filesStream = Files.walk(dirPath)) {
            List<Path> paths = filesStream.sorted(Comparator.reverseOrder()).toList();
            for (Path p : paths) {
                log.info("deleting file: {}", p.getFileName());
                Files.delete(p);
            }
            log.info("All files deleted on path: {}", jobDir);
        } catch (IOException e) {
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
