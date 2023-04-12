package com.driver;

import java.util.*;

import org.springframework.stereotype.Repository;

@Repository
public class SpotifyRepository {
    public HashMap<Artist, List<Album>> artistAlbumMap;
    public HashMap<Album, List<Song>> albumSongMap;
    public HashMap<Playlist, List<Song>> playlistSongMap;
    public HashMap<Playlist, List<User>> playlistListenerMap;
    public HashMap<User, Playlist> creatorPlaylistMap;
    public HashMap<User, List<Playlist>> userPlaylistMap;
    public HashMap<Song, HashSet<User>> songLikeMap;

    public HashMap<String,User> userdb;
    public HashMap<String,Song> songs;
    public HashMap<String,Playlist> playlists;
    public List<Album> albums;
    public List<Artist> artists;

    public SpotifyRepository(){
        //To avoid hitting apis multiple times, initialize all the hashmaps here with some dummy data
        artistAlbumMap = new HashMap<>();
        albumSongMap = new HashMap<>();
        playlistSongMap = new HashMap<>();
        playlistListenerMap = new HashMap<>();
        creatorPlaylistMap = new HashMap<>();
        userPlaylistMap = new HashMap<>();
        songLikeMap = new HashMap<>();

        userdb = new HashMap<>();
        songs = new HashMap<>();
        playlists = new HashMap<>();
        albums = new ArrayList<>();
        artists = new ArrayList<>();
    }

    public User createUser(String name, String mobile) {
        User user=new User(name,mobile);
        userdb.put(mobile,user);
        return user;
    }

    public Artist createArtist(String name) {
        Artist artist=new Artist(name);
        artists.add(artist);
        artistAlbumMap.put(artist, new ArrayList<Album>());
        return artist;
    }

    public Album createAlbum(String title, String artistName) {
        //If the artist does not exist, first create an artist with given name
        //Create an album with given title and artist
        boolean found=false;
        Album album = null;
       for(Artist artist:artists)
       {
           if(artist.getName().equals(artistName)) {
               found = true;
               //artist already exists
               album=new Album(title);
               albums.add(album);
               //add to artist->AlbumMap
               List<Album> albums=artistAlbumMap.get(artist);
               albums.add(album);
               artistAlbumMap.put(artist,albums);
               break;
           }
       }
       if(!found)
       {
           //artist doesnot exist
          Artist artist=createArtist(artistName);
            album=new Album(title);
           albums.add(album);
           List<Album> albumsOfartist=artistAlbumMap.get(artist);
           albumsOfartist.add(album);
           artistAlbumMap.put(artist,albums);
       }
       return album;
    }

    public Song createSong(String title, String albumName, int length) throws Exception{
        //If the album does not exist in database, throw "Album does not exist" exception
        //Create and add the song to respective album
        boolean found=false;
        Song song=null;
        for(Album album:albums)
        {
            if(album.getTitle().equals(albumName)) {
                found = true;
               //album exists
                song =new Song(title,length);
                songs.put(title,song);
                songLikeMap.put(song,new HashSet<User>());
                //is album exists in albumSong Map,if yes
                if(albumSongMap.containsKey(album))
                {
                    List<Song> songlist=albumSongMap.get(album);
                    songlist.add(song);
                    albumSongMap.put(album,songlist);
                }
                else {
                    List<Song> songlist = new ArrayList<>();
                    songlist.add(song);
                    albumSongMap.put(album, songlist);
                }
                break;
            }
        }
        if(!found)
            throw new Exception("Album does not exist");
        return song;
    }

    public Playlist createPlaylistOnLength(String mobile, String title, int length) throws Exception {
        //Create a playlist with given title and add all songs having the given length in the database to that playlist
        //The creater of the playlist will be the given user and will also be the only listener at the time of playlist creation
        //If the user does not exist, throw "User does not exist" exception
        Playlist playlist=new Playlist(title);
        playlists.put(title,playlist);
        List<Song> songlist=new ArrayList<>();
        for(Song song:songs.values())
        {
            if(song.getLength()==length)
                songlist.add(song);
        }
        playlistSongMap.put(playlist,songlist);

            if(userdb.containsKey(mobile))
            {
                creatorPlaylistMap.put(userdb.get(mobile),playlist);
                List<User> users=new ArrayList<>();
                users.add(userdb.get(mobile));
                playlistListenerMap.put(playlist,users);
            }
            else
            throw new Exception("User does not exist");

        return playlist;


    }

    public Playlist createPlaylistOnName(String mobile, String title, List<String> songTitles) throws Exception {
        //Create a playlist with given title and add all songs having the given titles in the database to that playlist
        //The creater of the playlist will be the given user and will also be the only listener at the time of playlist creation
        //If the user does not exist, throw "User does not exist" exception
        Playlist playlist=new Playlist(title);
        playlists.put(title,playlist);
        List<Song> songlist=new ArrayList<>();
        for(String s:songTitles)
        {
            for(Song song:songs.values())
            if(song.getTitle().equals(s))
                songlist.add(song);
        }
        playlistSongMap.put(playlist,songlist);
            if(userdb.containsKey(mobile))
            {
                creatorPlaylistMap.put(userdb.get(mobile),playlist);
                List<User> users=new ArrayList<>();
                users.add(userdb.get(mobile));
                playlistListenerMap.put(playlist,users);
            }
            else {
                throw new Exception("User does not exist");
            }
        return playlist;
    }

    public Playlist findPlaylist(String mobile, String playlistTitle) throws Exception {
        //Find the playlist with given title and add user as listener of that playlist and update user accordingly
        //If the user is creater or already a listener, do nothing
        //If the user does not exist, throw "User does not exist" exception
        //If the playlist does not exists, throw "Playlist does not exist" exception
        // Return the playlist after updating
        Playlist playlist=null;
        if(playlists.containsKey(playlistTitle))
        {
            playlist=playlists.get(playlistTitle);
            if(userdb.containsKey(mobile))
            {
                User user=userdb.get(mobile);
                List<User> userlist=playlistListenerMap.get(playlist);
                boolean found=false;
                for(User u:userlist)
                {
                    if(u==user){
                        found=true;
                        break;
                    }
                }
                if(!found)
                {
                    userlist.add(user);
                    playlistListenerMap.put(playlist,userlist);
                }
            }
            else {
                throw new Exception("User does not exist");
            }
        }
        else {
            throw new Exception("Playlist does not exist");
        }
        return playlist;
    }

    public Song likeSong(String mobile, String songTitle) throws Exception {
        //The user likes the given song. The corresponding artist of the song gets auto-liked
        //A song can be liked by a user only once. If a user tried to like a song multiple times, do nothing
        //However, an artist can indirectly have multiple likes from a user, if the user has liked multiple songs of that artist.
        //If the user does not exist, throw "User does not exist" exception
        //If the song does not exist, throw "Song does not exist" exception
        //Return the song after updating
        Song song=null;
        if(userdb.containsKey(mobile))
        {
            User user=userdb.get(mobile);
          if(songs.containsKey(songTitle))
          {
              song=songs.get(songTitle);
              //song like
              HashSet<User> hs= songLikeMap.get(song);
              hs.add(user);
              songLikeMap.put(song,hs);
              //artist like
              //go to album in which this song is
              Album album=null;
             for(Album albu: albumSongMap.keySet())
             {
                 for(Song so:albumSongMap.get(album))
                 {
                   if(so==song)
                   {
                      album=albu; break;
                   }
                 }
             }
             Artist artist=null;
              for(Artist arti: artistAlbumMap.keySet())
              {
                  for(Album albu:artistAlbumMap.get(arti))
                  {
                      if(albu==album)
                      {
                          arti.setLikes(arti.getLikes()+1);
                          break;
                      }
                  }
              }
          }
          else {
              throw new Exception("Song does not exist");
          }
        }
        else
        {
            throw new Exception("User does not exist");
        }
        return song;
    }

    public String mostPopularArtist() {
        String artistName=null;
        int max=0;
        for(Artist ar:artists)
        {
            if(ar.getLikes()>max)
                artistName=ar.getName();
        }
        return artistName;

    }

    public String mostPopularSong() {
        String songName=null;int max=0;
       for(Song song:songLikeMap.keySet())
       {
           if(songLikeMap.get(song).size()>max)
               songName=song.getTitle();
       }
       return songName;

    }
}
