package com.example.detectionexample.tv.model

/**
 * Domain model representing a video or surveillance/facial recognition clip for the 10-foot TV UI.
 */
data class TvVideo(
    val id: String,
    val title: String,
    val description: String,
    val cardImageUrl: String,
    val bgImageUrl: String,
    val videoUrl: String,
    val category: String,
    val duration: String = "01:30"
)

/**
 * Sample provider of catalog media items grouped into categorized rows.
 */
object TvMediaCatalog {
    val sampleVideos = listOf(
        TvVideo(
            id = "vid_1",
            title = "Face Detection Live Stream",
            description = "High-definition camera stream with real-time bounding boxes and landmark detection.",
            cardImageUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=500&auto=format&fit=crop&q=60",
            bgImageUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=1280&auto=format&fit=crop&q=80",
            videoUrl = "https://storage.googleapis.com/exoplayer-test-media-1/mp4/dizzy-short.mp4",
            category = "Surveillance Feeds"
        ),
        TvVideo(
            id = "vid_2",
            title = "Entrance Camera Analytics",
            description = "Continuous person detection at main office entrance using MobileFaceNet model.",
            cardImageUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=500&auto=format&fit=crop&q=60",
            bgImageUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=1280&auto=format&fit=crop&q=80",
            videoUrl = "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4",
            category = "Surveillance Feeds"
        ),
        TvVideo(
            id = "vid_3",
            title = "Lobby Access Log Review",
            description = "Batch biometric recognition replay from visitor kiosk checkpoint.",
            cardImageUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=500&auto=format&fit=crop&q=60",
            bgImageUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=1280&auto=format&fit=crop&q=80",
            videoUrl = "https://storage.googleapis.com/exoplayer-test-media-1/mp4/dizzy-short.mp4",
            category = "Access Logs"
        ),
        TvVideo(
            id = "vid_4",
            title = "Conference Room Monitor",
            description = "Autonomous meeting room presence tracking and occupancy analytics.",
            cardImageUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=500&auto=format&fit=crop&q=60",
            bgImageUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=1280&auto=format&fit=crop&q=80",
            videoUrl = "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4",
            category = "Access Logs"
        ),
        TvVideo(
            id = "vid_5",
            title = "Perimeter Security Drone",
            description = "Wide-angle outdoor drone footage analyzing perimeter motion and facial matches.",
            cardImageUrl = "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=500&auto=format&fit=crop&q=60",
            bgImageUrl = "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=1280&auto=format&fit=crop&q=80",
            videoUrl = "https://storage.googleapis.com/exoplayer-test-media-1/mp4/dizzy-short.mp4",
            category = "Perimeter Alerts"
        ),
        TvVideo(
            id = "vid_6",
            title = "Night Vision Infrared Archive",
            description = "Low-light facial recognition verification under infrared illumination.",
            cardImageUrl = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=500&auto=format&fit=crop&q=60",
            bgImageUrl = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=1280&auto=format&fit=crop&q=80",
            videoUrl = "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4",
            category = "Perimeter Alerts"
        )
    )

    fun getCategorizedVideos(): Map<String, List<TvVideo>> {
        return sampleVideos.groupBy { it.category }
    }
}
