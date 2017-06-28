package com.example.aksha.tryit.Model;

/**
 * Created by Aksha on 28/06/2017.
 */

public class Thumbnail {
    private String path;
    private String thumbnail;

    public Thumbnail() {
    }

    public String getPath() {

        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public Thumbnail(String path, String thumbnail) {

        this.path = path;
        this.thumbnail = thumbnail;
    }
}
