package com.netflix.common_lib.utils;

import java.util.Arrays;
import java.util.List;

public class VideoUtils {
    public static final List<VideoQuality> VIDEO_QUALITIES = Arrays.asList(
            new VideoQuality(5000, 1920, 1080),
            new VideoQuality(2800, 1280, 720),
            new VideoQuality(1400, 854, 480),
            new VideoQuality(800, 640, 360)
    );
}
