package com.jasperreddin;

import com.google.api.client.util.ArrayMap;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import org.mortbay.util.IO;

import java.io.*;
import java.net.Inet4Address;
import java.util.List;

public class PullYoutube {

	private YouTube youtube;

	private ListRequestHandler list;
	private InsertHandler insert;

	private boolean verbose = false;

	public PullYoutube(YouTube youtube) {
		this.youtube = youtube;
		this.insert = new InsertHandler();
		this.list = new ListRequestHandler(youtube);
	}

	/**
	 * Saves resources, probably won't crash due to stack overflow, and will have more necessary data. Downside: it will
	 * take a while due to user intervention.
	 */
	public void pullButNotSoAutomatically() {
		// load text file with channel usernames or ids
		FileReader reader;
		try {
			reader = new FileReader("channels.txt");
		} catch (IOException e) {
			System.out.println("ERROR: channels.txt not found. Aborting.");
			return;
		}

		try {
			BufferedReader bReader = new BufferedReader(reader);
			String idUsername = bReader.readLine();

			while (idUsername != null) {
				Channel channel = list.channel(idUsername);
				insert.channel(channel);

				List<PlaylistItem> items = list.channelVideos(channel);

				// go though videos...
				for (PlaylistItem plItem : items) {
					Video video = list.video(plItem.getContentDetails().getVideoId());

					insert.video(video);

					// go through comments
					for (CommentThread commentThread : list.videoComments(plItem)) {
						Comment comment = commentThread.getSnippet().getTopLevelComment();
						if (comment.getSnippet().getAuthorChannelId() == null) {
							System.out.println(" [WARN] Comment Author Channel ID is null");
						} else {
							String author = (String) ((ArrayMap) comment.getSnippet().getAuthorChannelId()).get("value");
							pullChannelAndVideos(author);
							insert.videoComment(commentThread, video);
						}
					}
				}

				// go through playlists...
				List<Playlist> playlists = list.playlists(channel.getId());

				for (Playlist playlist : playlists) {
					insert.playlist(playlist);

					List<PlaylistItem> plItems = list.playlist(playlist.getId());

					for (PlaylistItem item : plItems) {
						pullChannelAndVideos(item.getSnippet().getChannelId(), false);
						Video video = list.video(item.getContentDetails().getVideoId());
						if (video != null) {
							insert.video(video);
							insert.playlistVideoRelationship(playlist.getSnippet().getChannelId(), playlist.getId(), video.getId(), video.getSnippet().getChannelId());
						}
					}

				}

				idUsername = bReader.readLine();
			}

			readTourInformationFromUser();

			readPostInformationFromUser();

			readChannelLinks();

			// better algorithm:
			// until user says quit...
			// ask for channel id
			// go through videos...
			// insert video
			// go through comments...
			// insert just the channel and videos
			// insert comment
			// go through posts...
			// go through comments...
			// insert just the channel and videos
			// insert comment
			// go through playlists...
			// insert playlist
			// go through videos...
			// insert just the channel and videos
			// insert video x playlist relationship

		} catch (IOException e) {
			System.out.println("Exception. Abortiing.");
			e.printStackTrace();
		}

		insert.closeWriter();
		insert.overview();
	}

	/**
	 * Pulls and inserts a channel and its recent videos. If we want to insert a comment or playlist item, we need the
	 * channel first. Also, we have more insertions.
	 *
	 * @param channelID
	 * @throws IOException
	 */
	public String pullChannelAndVideos(String channelID) throws IOException {
		return pullChannelAndVideos(channelID, true);
	}

	public String pullChannelAndVideos(String channelID, boolean recurse) throws IOException {
		if (!insert.canInsertChannel(channelID)) {
			return channelID;
		}

		Channel channel = list.channel(channelID);
		insert.channel(channel);

		if (recurse && insert.getInsertions() < 500) {
			List<PlaylistItem> items = list.channelVideos(channel);

			for (PlaylistItem plItem : items) {
				Video video = list.video(plItem.getContentDetails().getVideoId());
				insert.video(video);
			}

			List<Subscription> subscriptions = list.subscriptions(channelID);

			for (Subscription subscription : subscriptions) {
				pullChannelAndVideos(subscription.getSnippet().getResourceId().getChannelId(), false);

				insert.subscribesToRelationship(subscription);

			}
		}

		return channel.getId();
	}

	public void readTourInformationFromUser() throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader("tours.txt"));
		if (verbose) System.out.println();
		if (verbose)
			System.out.println("------------------------------------------------------------------------------------");
		if (verbose) System.out.println();
		if (verbose) System.out.println("Tour Information");
		String newTour = "";
		String channelID = "";

		while (newTour != null && !newTour.equalsIgnoreCase("n")) {
			if (verbose) System.out.print("Enter Tour Date (YYYY-MM-DD): ");
			String tourDate = reader.readLine();
			if (verbose) System.out.println(tourDate);

			if (verbose) System.out.print("Enter location: ");
			String tourLocation = reader.readLine();
			if (verbose) System.out.println(tourLocation);

			if (verbose) System.out.print("Enter time (HHMM): ");
			int tourTime = Integer.parseInt(reader.readLine());
			if (verbose) System.out.println(tourTime);

			if (verbose) System.out.print("Enter link: ");
			String ticketink = reader.readLine();
			if (verbose) System.out.println(ticketink);

			insert.show(tourDate, tourLocation, tourTime, ticketink);

			if (verbose) System.out.print(" - Enter Channel ID: ");
			channelID = reader.readLine();

			while (channelID != null && !channelID.equalsIgnoreCase("n")) {
				if (verbose) System.out.println(channelID);

				try {
					channelID = pullChannelAndVideos(channelID);
					insert.showChannelRelationship(channelID, tourDate, tourLocation);
				} catch (IOException e) {
					if (verbose) System.out.println("Unable to enter channel associated with tour.");
				}

				if (verbose)
					System.out.print(" - Number of channel x show relations: " + insert.getShowChannelRelationships() + ". New Channel (channelid/n)? ");
				channelID = reader.readLine();
			}

			if (verbose) System.out.println();
			if (verbose) System.out.print("Number of Tours: " + insert.getShows() + ". New Tour (y/n)? ");
			newTour = reader.readLine();
			if (verbose) System.out.println();
		}
	}

	public void readPostInformationFromUser() throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader("posts.txt"));
		if (verbose) System.out.println();
		if (verbose)
			System.out.println("------------------------------------------------------------------------------------");
		if (verbose) System.out.println();
		if (verbose) System.out.println("Post Information");
		String newPost = "";

		while (newPost != null && !newPost.equalsIgnoreCase("n")) {

			if (verbose) System.out.print("Enter Channel ID: ");
			String channelID = reader.readLine();
			if (verbose) System.out.println(channelID);

			if (verbose) System.out.print("Enter Post ID:  ");
			String postID = reader.readLine();
			if (verbose) System.out.println(postID);

			if (verbose) System.out.print("Post date (YYYY-M M-DD): ");
			String postDate = reader.readLine();
			if (verbose) System.out.println(postDate);

			if (verbose) System.out.print("Enter text: ");
			String text = reader.readLine();
			if (verbose) System.out.println(text);

			if (verbose) System.out.print("Enter Likes: ");
			int likes = Integer.parseInt(reader.readLine());
			if (verbose) System.out.println(likes);

			try {
				channelID = pullChannelAndVideos(channelID);
				insert.post(channelID, postID, postDate, text, likes);
				readPostCommentInformationFromUser(channelID, postID, reader);
			} catch (IOException e) {
				if (verbose) System.out.println("Unable to enter channel associated with post.");
			}

			if (verbose) System.out.print("Number of posts: " + insert.getPosts() + ". New Post (y/n)? ");
			newPost = reader.readLine();
			if (verbose) System.out.println();
		}
	}

	public void readPostCommentInformationFromUser(String channelID, String postID, BufferedReader reader) throws IOException {
		String newComment = "";

		while (newComment != null && !newComment.equalsIgnoreCase("n")) {

			if (verbose) System.out.println(" - Comment #" + (insert.getPostComments() + 1));

			if (verbose) System.out.print(" | Enter Channel ID: ");
			String authorID = reader.readLine();
			if (verbose) System.out.println(authorID);

			if (verbose) System.out.print(" | Enter post comment ID: ");
			String postCommentID = reader.readLine();
			if (verbose) System.out.println(postCommentID);

			if (verbose) System.out.print(" | Enter date posted (YYYY-MM-DD): ");
			String date = reader.readLine();
			if (verbose) System.out.println(date);

			if (verbose) System.out.print(" | Enter comment text: ");
			String text = reader.readLine();
			if (verbose) System.out.println(text);

			if (verbose) System.out.print(" | Enter likes: ");
			int likes = Integer.parseInt(reader.readLine());
			if (verbose) System.out.println(likes);

			try {
				authorID = pullChannelAndVideos(authorID);
				insert.postComment(channelID, authorID, postID, postCommentID, date, text, likes);
			} catch (IOException e) {
				if (verbose) System.out.println("Unable to enter channel associated with post.");
			}

			if (verbose) System.out.print("Number of comments: " + insert.getPostComments() + ". New comment (y/n)? ");
			newComment = reader.readLine();
			if (verbose) System.out.println();
		}
	}

	public void readChannelLinks() throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader("links.txt"));
		String line = "";
		String channelID = "";
		String link = "";

		while (line != null) {

			channelID = reader.readLine();
			link = reader.readLine();

			if (channelID != null && link != null) {
				channelID = pullChannelAndVideos(channelID, false);
				insert.channelLinksRelationship(channelID, link);
			}
			line = reader.readLine();

		}
	}

	/**
	 * It'll still take a while, but it also will use much more resources at a time, crash due to stack overflow, and
	 * will give wild useless data.
	 * <p>
	 * Also this will never be finished but it will hold on to an idea for an algorithm that I might document later if
	 * I ever make a video on this particular project.
	 *
	 * @param channel
	 * @param recursively
	 */
	public void pullChannels(Channel channel, boolean recursively) {
		// insert channel

		// if channel has links, insert links

		// if subscriptions are public, add subscriptions

		if (!recursively) {
			return;
		}

		// get channel posts
		// for each post:
		// insert post
		// get post comments
		// for each post comment:
		// pull channel non-recursively
		// insert post comment

		// get channel playlists
		// for each playlist:
		// insert playlist
		// for each video in playlist:
		// pull channel recursively
		// insert video
		// get top 10 comments
		// for each comment:
		// pull channel
		// insert comment

		// get channel shows
		// for each show:
		// insert show
		// for all channels:
		// pull channel recursively
	}

	/**
	 * Pull channel recursively
	 *
	 * @param channel
	 */
	public void pullChannel(Channel channel) {
//        pullChannel(channel, true);
	}
}
