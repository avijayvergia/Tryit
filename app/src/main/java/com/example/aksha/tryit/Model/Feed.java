package com.example.aksha.tryit.Model;

/**
 * Created by Aksha on 27/06/2017.
 */

public class Feed {

    private String downloadUrl;
    private String uid;
    private String filePath;

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Feed() {

    }

    public Feed(String downloadUrl, String uid, String filePath) {

        this.downloadUrl = downloadUrl;
        this.uid = uid;
        this.filePath = filePath;
    }
}
