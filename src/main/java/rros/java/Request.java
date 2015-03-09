package rros.java;

/**
 * Created by namnguyen on 3/9/15.
 */
public class Request {
    public Request(String verb,String uri) {
        this(verb,uri,null);
    }
    public Request(String verb,String uri,String body){
        this.verb = verb;
        this.uri = uri;
        this.body = body;
    }

    public String getVerb() {
        return verb;
    }

    public String getUri() {
        return uri;
    }

    public String getBody() {
        return body;
    }

    private String verb;
    private String uri;
    private String body;
}
