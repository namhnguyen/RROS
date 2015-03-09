package rros.java;

////////////////////////////////////////////////////////////////////////////////

import rros.Response;
import rros.core.RROSProtocolImpl;
import scala.Function1;
import scala.Option;
import scala.runtime.AbstractFunction1;
import scala.runtime.BoxedUnit;

/**
 * Created by namnguyen on 3/9/15.
 */
public class RROSProtocolJavaImpl implements RROSProtocol {

    public RROSProtocolJavaImpl(SocketAdapter adapter) {
        this.adapter = adapter;
        this.rrosProtocol = new RROSProtocolImpl(adapter.toScalaSocketAdapter());
        this.rrosProtocol.onMessageReceived(Option.apply(new AbstractFunction1<rros.Message, BoxedUnit>() {
            @Override
            public BoxedUnit apply(rros.Message v1) {
                if (RROSProtocolJavaImpl.this.messageReceivedAction!=null){
                    Message javaMessage = new Message(v1.value());
                    RROSProtocolJavaImpl.this.messageReceivedAction.onReceived(javaMessage);
                }
                return BoxedUnit.UNIT;
            }
        }));
        this.rrosProtocol.onRequestReceived(Option.apply(new AbstractFunction1<rros.Request, Response>() {
            @Override
            public Response apply(rros.Request v1) {
                if (RROSProtocolJavaImpl.this.requestReceivedAction!=null){
                    Request javaRequest;
                    if (v1.body().isDefined()) {
                        javaRequest = new Request(v1.verb(), v1.uri(), v1.body().get());
                    } else {
                        javaRequest = new Request(v1.verb(), v1.uri());
                    }
                    rros.java.Response javaResponse =
                            RROSProtocolJavaImpl.this.requestReceivedAction.onReceived(javaRequest);
                    rros.Response scalaResponse = new rros.Response(javaResponse.getCode()
                            , Option.apply(javaResponse.getResponse()));
                    return scalaResponse;
                } else {
                    throw new RuntimeException("There is no request received implementation");
                }

            }
        }));
    }

    @Override
    public void send(Request request, SentAction action, long timeout) {
        Option<String> body = Option.apply(request.getBody());
        rros.Request scalaRequest = new rros.Request(request.getVerb(), request.getUri(), body);
        Function1<Response,BoxedUnit> onCompleted = new AbstractFunction1<Response, BoxedUnit>(){
            @Override
            public BoxedUnit apply(Response v1) {
                rros.java.Response javaResponse;
                if (v1.response().isDefined())
                    javaResponse = new rros.java.Response(v1.code(),v1.response().get());
                else
                    javaResponse = new rros.java.Response(v1.code());
                action.onComplete(javaResponse);
                return BoxedUnit.UNIT;
            }
        };
        Function1<Exception,BoxedUnit> onFailure = new AbstractFunction1<Exception, BoxedUnit>() {
            @Override
            public BoxedUnit apply(Exception v1) {
                action.onFailure(v1);
                return BoxedUnit.UNIT;
            }
        };
        rrosProtocol.send(scalaRequest,onCompleted,timeout,onFailure);
    }

    @Override
    public void send(Message message) {
        rros.Message scalaMessage = new rros.Message(message.getValue());
        rrosProtocol.send(scalaMessage);
    }

    @Override
    public void onMessageReceived(MessageReceivedAction action) {
        this.messageReceivedAction = action;
    }

    @Override
    public void onRequestReceived(RequestReceivedAction action) {
        this.requestReceivedAction = action;
    }

    private SocketAdapter adapter;
    private rros.RROSProtocol rrosProtocol;
    private MessageReceivedAction messageReceivedAction = null;
    private RequestReceivedAction requestReceivedAction = null;
}
////////////////////////////////////////////////////////////////////////////////