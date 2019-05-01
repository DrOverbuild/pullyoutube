package com.jasperreddin;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ArrayMap;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeScopes;
import com.google.api.services.youtube.model.*;

import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

/**
 * Hello world!
 */

// client id: 408194677855-kvob6pbgf6q73tdqsuqnlrokh9vuejfj.apps.googleusercontent.com
// secret: 72iDxRZW0EsismE-XKj3uTa8
public class App {

    /**
     * Application name.
     */
    private static final String APPLICATION_NAME = "databaseproj";
    /**
     * Directory to store user credentials for this application.
     */
    private static final java.io.File DATA_STORE_DIR = new java.io.File("credentials/databaseproj");
    /**
     * Global instance of the {@link FileDataStoreFactory}.
     */
    private static FileDataStoreFactory DATA_STORE_FACTORY;
    /**
     * Global instance of the JSON factory.
     */
    private static final JsonFactory JSON_FACTORY =
            JacksonFactory.getDefaultInstance();
    /**
     * Global instance of the HTTP transport.
     */
    private static HttpTransport HTTP_TRANSPORT;

    private static final List<String> SCOPES = Arrays.asList(YouTubeScopes.YOUTUBE, YouTubeScopes.YOUTUBE_FORCE_SSL);

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    public static Credential authorize() throws IOException {
        // Load client secrets.
        InputStream in = App.class.getResourceAsStream("/client_secret2.json");
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                        .setDataStoreFactory(DATA_STORE_FACTORY)
                        .setAccessType("offline")
                        .build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
        return credential;
    }

    public static void main(String[] args) throws IOException {
    	long start = System.currentTimeMillis();

        YouTube youtube = getYouTubeService();
        PullYoutube pullYoutube = new PullYoutube(youtube);
        pullYoutube.pullButNotSoAutomatically();

        long end = System.currentTimeMillis();

        System.out.println("Total Time: " + (end - start) + " milliseconds");

    }

    public static void testvideo(YouTube youtube) {
    	try {
    		String commentid = System.console().readLine();

			YouTube.CommentThreads.List commentreq = youtube.commentThreads().list("snippet");

			commentreq.setId(commentid);
			CommentThreadListResponse ctLR = commentreq.execute();
			List<CommentThread> commentThreads = ctLR.getItems();

			for (CommentThread ct : commentThreads) {
				String author = ct.getSnippet().getTopLevelComment().getSnippet().getAuthorDisplayName();

				System.out.println();
				System.out.println("------------------------------------------------------------------------------------");
				System.out.println();
				System.out.println("From " + author);;
				System.out.println();
				System.out.println(ct.getSnippet().getTopLevelComment().getSnippet().getTextDisplay());
			}
		} catch (Throwable t) {
    		t.printStackTrace();
		}
	}

    public static void testMethod(YouTube youtube) {
		try {
			YouTube.Channels.List channelsListByUsernameRequest = youtube.channels().list("snippet,contentDetails,statistics");

			String userid = System.console().readLine();

//            channelsListByUsernameRequest.setForUsername(username);
			channelsListByUsernameRequest.setId(userid);

			ChannelListResponse response = channelsListByUsernameRequest.execute();


			if (response.getItems().size() == 0) {
				channelsListByUsernameRequest = youtube.channels().list("snippet,statistics,brandingSettings");
				channelsListByUsernameRequest.setForUsername(userid);
				response = channelsListByUsernameRequest.execute();
				System.out.println("Using username");
			}

			Channel channel = response.getItems().get(0);


			System.out.printf(
					"This channel's ID is %s. Its title is '%s', and it has %s views.\n",
					channel.getId(),
					channel.getSnippet().getTitle(),
					channel.getStatistics().getViewCount());

			System.out.println("Published: " + InsertHandler.convertDate(channel.getSnippet().getPublishedAt()));

		} catch (GoogleJsonResponseException e) {
			e.printStackTrace();
			System.err.println("There was a service error: " +
					e.getDetails().getCode() + " : " + e.getDetails().getMessage());
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

    public static YouTube getYouTubeService() throws IOException {
        Credential credential = authorize();

//        return new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, null).setApplicationName("database-proj").build();

        return new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
}
