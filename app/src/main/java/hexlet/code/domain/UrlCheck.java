package hexlet.code.domain;

import io.ebean.Model;
import io.ebean.annotation.NotNull;
import io.ebean.annotation.WhenCreated;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "url_check")
public final class UrlCheck extends Model {

    @Id
    private long id;

    private int statusCode;

    private String title;

    private String h1;

    @Lob
    private String description;

    @ManyToOne
    @NotNull
    private Url url;

    @WhenCreated
    private Instant createdAt;

    public UrlCheck() {
    }
    public UrlCheck(int fieldStatusCode, String fieldTitle, String fieldH1, String fieldDescription, Url fieldUrl) {

        this.statusCode = fieldStatusCode;
        this.title = fieldTitle;
        this.h1 = fieldH1;
        this.description = fieldDescription;
        this.url = fieldUrl;
    }

    public long getId() {
        return this.id;
    }

    public int getStatusCode() {
        return this.statusCode;
    }

    public String getTitle() {
        return this.title;
    }

    public String getH1() {
        return this.h1;
    }

    public String getDescription() {
        return this.description;
    }

    public Url getUrl() {
        return this.url;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }
}
