package models;

import java.util.List;

public class ChannelPlaylist {
  public Channel channel;
  public List<Video> latestVideos;

  public ChannelPlaylist(Channel channel, List<Video> latestVideos) {
    this.channel = channel;
    this.latestVideos = latestVideos;
  }

}
