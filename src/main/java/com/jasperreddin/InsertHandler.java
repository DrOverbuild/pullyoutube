package com.jasperreddin;

import java.io.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import com.google.api.client.util.ArrayMap;
import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.model.*;

public class InsertHandler {
	private BufferedWriter writer;
	private FileWriter fileWriter;

	private int insertions;
	private int numberOfChannels = 0;
	private int subcriptionsRelationships = 0;
	private int channelLinksRelationships = 0;
	private int playlistVideoRelationships = 0;
	private int playlists = 0;
	private int videos = 0;
	private int videoComments = 0;
	private int shows = 0;
	private int showChannelRelationships = 0;
	private int posts = 0;
	private int postComments = 0;

	private boolean verbose = true;

	/**
	 * If channels contains a channel id, don't insert it or its videos and playlists. They've already been inserted.
	 * This is simply a duplicate tracker.
	 */
	private List<String> channels;

	public InsertHandler() {
		this.insertions = 0;
		try {
			File file = new File("output.sql");
			if (file.exists()) {
				file.delete();
			}

			file.createNewFile();

			this.fileWriter = new FileWriter(file);
			this.writer = new BufferedWriter(this.fileWriter);
		} catch (IOException e) {
			System.out.println("Unable to write to file.");
			e.printStackTrace();
			System.exit(1);
		}
		channels = new ArrayList<>();
	}

	public int getInsertions() {
		return insertions;
	}

	public void insert() {
		this.insertions++;
	}

	public void overview() {
		System.out.println();
		System.out.println("Insertions: " + getInsertions());
		System.out.println(" - Channels: " + numberOfChannels);
		System.out.println(" - Subscriptions: " + subcriptionsRelationships);
		System.out.println(" - Channel Links: " + channelLinksRelationships);
		System.out.println(" - Playlist X Videos: " + playlistVideoRelationships);
		System.out.println(" - Playlists: " + playlists);
		System.out.println(" - Videos: " + videos);
		System.out.println(" - Video Comments: " + videoComments);
		System.out.println(" - Shows: " + shows);
		System.out.println(" - Show x Channels: " + showChannelRelationships);
		System.out.println(" - Posts: " + posts);
		System.out.println(" - Post Comments: " + postComments);

	}

	public boolean canInsertChannel(String id) {
		if (id == null) {
			if (verbose) System.out.println(" [WARN] channel ID is null.");
			return false;
		}

		return !channels.contains(id);
	}

	private void addInsertedChannel(String id) {
		if (canInsertChannel(id)) {
			channels.add(id);
		}
	}




	public void channel(Channel channel) {
		addInsertedChannel(channel.getId());
		String id = channel.getId();
		String name = process(channel.getSnippet().getTitle());
		String joinDate = convertDate(channel.getSnippet().getPublishedAt());
		String description = process(channel.getSnippet().getDescription());
		BigInteger subscribers = channel.getStatistics().getSubscriberCount();

		try {
			writer.write("INSERT INTO channel VALUES ('" + id + "', '" + name + "', '" + joinDate + "', '" + description + "', " + subscribers + ");\n\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		if (verbose) System.out.println(" [INSERT] Channel: " + channel.getSnippet().getTitle());
		if (verbose) System.out.println("                                                                  " + numberOfChannels + " channels");

		numberOfChannels++;
		insert();
	}

	public void subscribesToRelationship(Subscription subscription) {
		String id = subscription.getSnippet().getChannelId();
		String subTo = subscription.getSnippet().getResourceId().getChannelId();

		try {
			writer.write("INSERT INTO subscribes_to VALUES ('" + id + "', '" + subTo + "');\n\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		if (verbose) System.out.println(" [INSERT] Subscription: " + subscription.getSnippet().getResourceId().getChannelId());
		if (verbose) System.out.println("                                                                  " + subcriptionsRelationships + " subscriptions");

		subcriptionsRelationships++;
		insert();
	}

	public void channelLinksRelationship(String channelID, String link) {
		try {
			writer.write("INSERT INTO channel_links VALUES ('" + channelID + "', '" + link + "');\n\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		if (verbose) System.out.println(" [INSERT] Channel link: " + channelID + ", " + link);

		channelLinksRelationships++;
		insert();
	}

	public void playlistVideoRelationship(String channelID, String playlistID, String videoID, String videoChannelID) {
		try {
			writer.write("INSERT INTO playlist_videos VALUES ('" + channelID + "', '" + playlistID + "', '" + videoID + "', '" + videoChannelID + "');\n\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		if (verbose) System.out.println(" [INSERT] Video x Playlist Relationship");
		playlistVideoRelationships++;
		insert();
	}

	public void playlist(Playlist playlist) {
		String channelID = playlist.getSnippet().getChannelId();
		String playlistID = playlist.getId();
		String name = process(playlist.getSnippet().getTitle());
		String description = process(playlist.getSnippet().getDescription());
		long numberVideos = playlist.getContentDetails().getItemCount();

		try {
			writer.write("INSERT INTO playlist VALUES ('" + channelID + "', '" + playlistID + "', '" + name
					+ "', '" + description + "', " + numberVideos +");\n\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		if (verbose) System.out.println(" [INSERT] Playlist");
		playlists++;
		insert();
	}

	public void video(Video video) {
		String channelID = video.getSnippet().getChannelId();
		String videoID = video.getId();
		String name = process(video.getSnippet().getTitle());
		String duration = video.getContentDetails().getDuration();
		String datePublished = convertDate(video.getSnippet().getPublishedAt());
		String description = process(video.getSnippet().getDescription());
		BigInteger views = video.getStatistics().getViewCount();
		BigInteger likes = video.getStatistics().getLikeCount();
		BigInteger dislikes = video.getStatistics().getDislikeCount();

		try {
			writer.write("INSERT INTO music_video VALUES ('" + channelID + "', '" + videoID + "', '" + name + "', '"
					+ duration + "', '" + datePublished + "', '" + description + "', " + views + ", " + likes + ", "
					+ dislikes + ");\n\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		if (verbose) System.out.println(" [INSERT] Video: " + video.getSnippet().getTitle());
		if (verbose) System.out.println("                                                                  " + videos + " videos");
		videos++;

		insert();
	}

	public void videoComment(CommentThread commentThread, Video video) {
		Comment comment = commentThread.getSnippet().getTopLevelComment();

		String videoID = video.getId();
		String videoChannel = video.getSnippet().getChannelId();
		String author = (String) ((ArrayMap) comment.getSnippet().getAuthorChannelId()).get("value");
		String commentID = commentThread.getId();
		String date = convertDate(comment.getSnippet().getPublishedAt());
		String content = process(comment.getSnippet().getTextDisplay());
		long likes = comment.getSnippet().getLikeCount();

		// todo remove dislikes from all comment tables in sql code

		try {
			writer.write("INSERT INTO video_comment VALUES ('" + videoChannel + "', '" + videoID + "', '" + commentID + "', '" + date + "', '"
					+ content + "', " + likes + ", '" + author + "');\n\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		videoComments++;
		if (verbose) System.out.println(" [INSERT] Comment                                                 " + videoComments + " comments");
		insert();
	}

	public void post(String channelID, String postID, String postDate, String text, int likes) {
		try {
			writer.write("INSERT INTO post VALUES ('" + convertDate(postDate) + "', '" + channelID + "', '" + postID + "', '"
					+ process(text) + "', " + likes + ");\n\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		if (verbose) System.out.println(" [INSERT] Post " + channelID);

		posts++;
		insert();
	}

	public void postComment(String channelID, String authorID, String postID, String postCommentID, String date, String text, int likes) {
		try {
			writer.write("INSERT INTO post_comment VALUES ('" + convertDate(date) + "', '" + channelID + "', '" + postID + "', '" + postCommentID + "', '"
					+ authorID + "', '" + process(text) + "', " + likes + ");\n\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		if (verbose) System.out.println(" [INSERT] Post Comment " + channelID);

		postComments++;
		insert();
	}

	public void show(String tourDate, String tourLocation, int tourTime, String ticketink) {
		try {
			writer.write("INSERT INTO shows VALUES ('" + convertDate(tourDate) + "', '" + process(tourLocation) + "', '" + tourTime + "', '"
					+ process(ticketink) + "');\n\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		if (verbose) System.out.println(" [INSERT] Show " + tourDate + " " + tourLocation);

		shows++;
		insert();
	}

	public void showChannelRelationship(String channelID, String tourDate, String tourLocation) {
		try {
			writer.write("INSERT INTO performs VALUES ('" + channelID + "', '" + convertDate(tourDate) + "', '" + process(tourLocation) + "');\n\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		if (verbose) System.out.println(" [INSERT] Show x Channel " + convertDate(tourDate) + " " + tourLocation + " x " + channelID);

		showChannelRelationships++;
		insert();
	}





	public int getSubcriptionsRelationships() {
		return subcriptionsRelationships;
	}

	public int getChannelLinksRelationships() {
		return channelLinksRelationships;
	}

	public int getPlaylistVideoRelationships() {
		return playlistVideoRelationships;
	}

	public int getPlaylists() {
		return playlists;
	}

	public int getVideos() {
		return videos;
	}

	public int getVideoComments() {
		return videoComments;
	}

	public int getShows() {
		return shows;
	}

	public int getShowChannelRelationships() {
		return showChannelRelationships;
	}

	public int getPosts() {
		return posts;
	}

	public int getPostComments() {
		return postComments;
	}





	/**
	 * Get a workable string from the API. Sometimes the description or titles of videos will be too long or will have
	 * single quotes, and this method fixes that.
	 * @param input
	 * @return
	 */
	public static String process(String input) {
		String output = input.replace("'","''");
		output = output.replace("\n", " ");
		output = output.replace("&", " ");
		if (output.length() > 200) {
			output = output.substring(0,200);
		}

		return output;
	}

	/**
	 * Convert date from the Youtube API to the Oracle date format.
	 * @param dateTime
	 * @return
	 */
	public static String convertDate(DateTime dateTime) {
		return convertDate(dateTime.toStringRfc3339().split("T")[0]);

	}

	public static String convertDate(String date) {
		try {
			String[] components = date.split("-");

			String year = components[0].substring(2);

			String[] months = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};
			int monthNum = Integer.parseInt(components[1]);

			String month = months[monthNum - 1];

			String day = components[2];

			return day + "-" + month + "-" + year;
		} catch (Throwable e) {
			e.printStackTrace();
			return "01-JAN-19";
		}
	}

	public void closeWriter() {
		try {
			writer.close();
			fileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
