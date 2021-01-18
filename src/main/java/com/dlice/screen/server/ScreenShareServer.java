package com.dlice.screen.server;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

/**
 * @author mubai
 * @apiNote:
 * @date 2021/1/18 19:04
 **/
@ServerEndpoint("/websocket/screen")
public class ScreenShareServer {
    private static String screenSize;

    //判断文件是否为屏幕分辨率
    private static Pattern pSize = Pattern.compile("^(\\d{3,4})-(\\d{3,4})$");

    //等待分享的浏览器客户端
    private static Set<Session> waitForCommandList = new HashSet<Session>();

    @OnOpen
    public void onOpen(Session session){//一个客户端已连上
        System.out.println("一个客户端已连上! " + this);
    }

    @OnClose
    public void onClose(){

    }

    @OnError
    public void onError(@PathParam("userId") String userId,
                        Throwable throwable,
                        Session session){
        System.out.println(throwable.getMessage());
    }

    //接收到文本消息
    @OnMessage
    public void onMessage(Session session, String msg){
        try {
            System.out.printf("msg: %s, bytes.len: %d\n", msg, msg.getBytes("utf-8").length);
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }
        Matcher m = pSize.matcher(msg);
        if(m.matches()){//来自于分享端
            screenSize = msg;
            return;
        }
        try {
            //System.out.printf("msg: %s, len: %d\n", msg, msg.getBytes("utf-8").length);
            if("-1".equals(msg)){//客户端发起的请求
                if(screenSize != null){
                    session.getBasicRemote().sendText(screenSize);
                }else{
                    session.getBasicRemote().sendText("屏幕还未开始分享，请稍候重试！");
                }
                waitForCommandList.add(session);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //接收到二进制消息
    @OnMessage
    public void onMessage(ByteBuffer byteBuffer){
        System.out.println("recived byte size:" + byteBuffer.array().length);
        //发送到每个客户端
        for(Session client : waitForCommandList){
            if(client.isOpen()){
                try {
                    client.getBasicRemote().sendBinary(byteBuffer);
                } catch (Exception e) {
                    System.out.println("连接已经断开!");
                    waitForCommandList.remove(client);
                }
            }else{
                waitForCommandList.remove(client);
            }
        }
    }

}
