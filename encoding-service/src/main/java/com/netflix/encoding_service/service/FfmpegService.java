package com.netflix.encoding_service.service;

import com.netflix.common_lib.dto.exception.CommonException;
import com.netflix.common_lib.utils.VideoQuality;
import com.netflix.common_lib.utils.VideoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class FfmpegService {
    @Value("${ffmpeg.path}")
    private String ffmpegPath;

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
//                    "#EXT-X-STREAM-INF:BANDWIDTH=%d,RESOLUTION=%dx%d,CODECS=\"avc1.42e01e,mp4a.40.2\"%dp/playlist.m3u8\n\n",
                    "#EXT-X-STREAM-INF:BANDWIDTH=%d,RESOLUTION=%dx%d,CODECS=\"avc1.42e01e,mp4a.40.2\"\n%dp/playlist.m3u8\n",
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

    public void encodeToHls(String inputPath, String outputPath, VideoQuality videoQuality) {
        log.info("Encoding to HLS...");
        List<String> command = createFfmpegCommandForHlsEncoding(inputPath, outputPath, videoQuality);
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        pb.inheritIO();
        try {
            Process process = pb.start();
            int exit_code = process.waitFor();
            if (exit_code != 0) {
                String message = String.format("ffmpeg encoding filed with exit_code=%s", exit_code);
                throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, message);
            }
            log.info("Encoded {}p successfully!", videoQuality.height());
        } catch (IOException | InterruptedException e) {
            log.info("encoding failed, reason: {}", e.getMessage());
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public List<String> createFfmpegCommandForHlsEncoding(String inputPath, String outputPath, VideoQuality videoQuality) {
        String playlistPath = outputPath + "/playlist.m3u8";
        String segmentPattern = outputPath + "/segment_%03d.ts";
        return Arrays.asList(
                ffmpegPath,
                "-i", inputPath,
                "-vf", "scale=" + videoQuality.width() + ":" + videoQuality.height(),
                "-c:v", "libx264",
                "-b:v", videoQuality.bitrate() + "k",
                "-c:a", "aac",
                "-b:a", "128k",
                "-hls_time", "10",
                "-hls_list_size", "0",
                "-hls_segment_filename", segmentPattern,
                "-f", "hls",
                playlistPath
        );
    }
}
