package models;

public class ChannelVideo {
    public Video video;
    public Channel channel;

    public ChannelVideo(Video video, Channel channel) {
        this.channel = channel;
        this.video = video;
    }
}
