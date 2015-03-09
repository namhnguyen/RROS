package rros.java;

/**
 * Created by namnguyen on 3/9/15.
 */
public class Response {
    public Response(String code,String response) {
        this.code = code;
        this.response = response;
    }
    public Response(String code) {
        this(code,null);
    }

    public String getCode() {
        return code;
    }

    public String getResponse() {
        return response;
    }

    private String code;
    private String response;
}
