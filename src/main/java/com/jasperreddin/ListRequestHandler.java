package com.jasperreddin;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ListRequestHandler {
    private YouTube youtube;
    private boolean verbose = true;

    public ListRequestHandler(YouTube youtube) {
        this.youtube = youtube;
    }

    public Channel channel(String id) throws IOException {
		YouTube.Channels.List channelsList = youtube.channels().list("snippet,contentDetails,statistics");
		channelsList.setId(id);
		ChannelListResponse response = channelsList.execute();

		if (response.getItems().size() == 0) {
			channelsList = youtube.channels().list("snippet,contentDetails,statistics");
			channelsList.setForUsername(id);
			response = channelsList.execute();
			System.out.println("Using username");
		}

		if (response.getItems().size() == 0) {
			return null;
		}

		return response.getItems().get(0);

	}

	public Video video(String id) throws IOException {
    	YouTube.Videos.List request = youtube.videos().list("snippet,contentDetails,statistics");
    	request.setId(id);
    	VideoListResponse response = request.execute();

    	if (response.getItems().size() > 0) {
    		return response.getItems().get(0);
		} else {
    		return null;
		}
	}

    public List<PlaylistItem> channelVideos(String channelID) throws IOException {

    	Channel channel = channel(channelID);

    	if (channel == null) {
			return new ArrayList<>();

		}

		String playlistID = channel.getContentDetails().getRelatedPlaylists().getUploads();

		return playlist(playlistID);
	}

	public List<PlaylistItem> channelVideos(Channel channel) throws IOException {
		if (channel == null) {
			return new ArrayList<>();

		}

		String playlistID = channel.getContentDetails().getRelatedPlaylists().getUploads();

		return playlist(playlistID);
	}


	/**
	 * Top level comments of a video
	 */
	public List<CommentThread> videoComments(Video video) throws IOException {
		return videoComments(video.getId());
	}

	public List<CommentThread> videoComments(PlaylistItem video) throws IOException {
		return videoComments(video.getContentDetails().getVideoId());
	}

	public List<CommentThread> videoComments(String videoId) throws IOException {
		try {
			YouTube.CommentThreads.List commentThreadsList = youtube.commentThreads().list("snippet");
			commentThreadsList.setVideoId(videoId);
			commentThreadsList.setMaxResults(4L);
			CommentThreadListResponse response = commentThreadsList.execute();

			return response.getItems();
		} catch (GoogleJsonResponseException e) {
			if (e.getDetails().getCode() == 403) {
				if (verbose) System.out.println(" [WARN] For some reason we don't have permission to access comments on this video");
			} else {
				if (verbose) System.out.println(" [WARN] Could not get the comments on this video");
			}
		}

		return new ArrayList<>();
	}

	/**
	 * Get the playlists of a channel
	 * @param channelID
	 * @return
	 * @throws IOException
	 */
    public List<Playlist> playlists(String channelID) throws IOException {
        YouTube.Playlists.List playlistListRequest = youtube.playlists().list("snippet,contentDetails");
        playlistListRequest.setChannelId(channelID);
        playlistListRequest.setMaxResults(5L);
        PlaylistListResponse plResponse = playlistListRequest.execute();

        return plResponse.getItems();
    }

    public List<PlaylistItem> playlist(String plID) throws IOException {
        YouTube.PlaylistItems.List request = youtube.playlistItems().list("snippet,contentDetails");
        request.setPlaylistId(plID);
        request.setMaxResults(5L);
        PlaylistItemListResponse response = request.execute();

        return response.getItems();
    }

    public List<Subscription> subscriptions(String channelID) throws IOException {
    	YouTube.Subscriptions.List request = youtube.subscriptions().list("snippet");
    	request.setChannelId(channelID);
    	request.setMaxResults(5L);

    	try {
			SubscriptionListResponse response = request.execute();
			return response.getItems();
		} catch (GoogleJsonResponseException e) {
    		if (e.getDetails().getCode() == 403) {
				if (verbose) System.out.println(" [WARN] Subscriptions for " + channelID + " probably aren't public");
			} else {
				if (verbose) System.out.println(" [WARN] Could not get subscriptions");
			}
		}

    	return new ArrayList<>();
	}
}
