package rros.java;

/**
 * Created by namnguyen on 3/9/15.
 */
public interface RROSProtocol {
    void send(Request request,SentAction action,long timeout);
    void send(Message message);
    void onMessageReceived(MessageReceivedAction action);
    void onRequestReceived(RequestReceivedAction action);

    public static interface SentAction {
        void onComplete(Response response);
        void onFailure(Exception exc);
    }
    public static interface MessageReceivedAction {
        void onReceived(Message message);
    }
    public static interface RequestReceivedAction {
        Response onReceived(Request request);
    }
}
