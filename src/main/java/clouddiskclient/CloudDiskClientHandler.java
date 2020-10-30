package clouddiskclient;

import clouddiskserver.CloudDiskServerHandler;
import common.SignalByte;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class CloudDiskClientHandler extends ChannelInboundHandlerAdapter {
    
    public enum State {
        IDLE,
        AUTHORIZATION_RESULT,
        FILE_SENDING_RESULT,
        FILE_RECEIVING_RESULT
    }
    
    private CloudDiskClient client; 
    private State currentState = State.IDLE;
    
    public CloudDiskClientHandler(CloudDiskClient client) {
        this.client = client;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = ((ByteBuf) msg);
        while (buf.readableBytes() > 0) {
            if (currentState == State.IDLE) {
                byte readed = buf.readByte();
                if (readed == (byte) SignalByte.AUTHORIZATION.getValue()) {
                    currentState = State.AUTHORIZATION_RESULT;
                    System.out.println("CLIENT STATE: Waiting for authorization result");
                } else {
                    System.out.printf("CLIENT ERROR: De nuevo me fallas, amigo - %d%n", readed);
                }
            }
            
            if (currentState == State.AUTHORIZATION_RESULT) {
                if (buf.readableBytes() >= 1) {
                    boolean authSuccessful = buf.readBoolean();
                    System.out.printf("CLIENT STATE: Authorization result is %s%n", authSuccessful);
                    if (authSuccessful) {
                        client.returnAuthorizationResult(authSuccessful);
                    }
                }
            }
        }
        if (buf.readableBytes() == 0) buf.release();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
    
}