package JavaBeta;

public class Work {
    private final String title;
    private final String author;
    private final String url;

    public Work(String title, String author, String url) {
        this.title = title;
        this.author = author;
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getUrl() {
        return url;
    }

    // This controls how the object looks in the ListView
    @Override
    public String toString() {
        return title + " by " + author;
    }
}