package NetworkShit.ServerSide.Handlers;

import ClientAndHandlerCommunication.Commands.Command;
import ClientAndHandlerCommunication.Commands.RecieveChatCommand;
import ClientAndHandlerCommunication.Commands.SendChatCommand;
import ClientAndHandlerCommunication.Responses.Response;
import Enums.ChatChannelType;
import Game.Profile;
import NetworkShit.ServerSide.Server;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ChatHandler implements Runnable {
    private ObjectInputStream ois;
    private ObjectOutputStream oos;

    private Socket chatSocket;

    public ChatHandler(Socket chatSocket) {
        this.chatSocket = chatSocket;
        try {
            oos = new ObjectOutputStream(chatSocket.getOutputStream());
            ois = new ObjectInputStream(chatSocket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    @Override
    public void run() {
        try {

            while (true) {

                SendChatCommand chatCommand = this.getChat();
                sendChat(chatCommand);


            }
        } catch (IOException e) {
            if (e instanceof EOFException) {
                System.out.println("join socket disconnected.");
            }

        }


    }


    private SendChatCommand getChat()throws IOException{
        SendChatCommand chatCommand=null;
        try {
            chatCommand=(SendChatCommand) this.ois.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }


        return chatCommand;
    }

    private void sendChat(SendChatCommand chatCommand)throws IOException{

            if (chatCommand.getChatChannelType()== ChatChannelType.AUDIENCES_CHANNEL){
                for (Profile audience:chatCommand.getMatch().getAudience()) {
                    if (!audience.getUserName().equals(chatCommand.getSender())) {
                        ChatHandler targetHandler = Server.chatHandlers.get(Server.userHandlers.get(audience));
                        targetHandler.oos.writeObject(new RecieveChatCommand(chatCommand.getMsg(), chatCommand.getSender(),ChatChannelType.AUDIENCES_CHANNEL));
                    }
                }
            }
            else {
                if (chatCommand.getSender().equals(chatCommand.getMatch().getHostProfile().getUserName())) {
                    ChatHandler targetHandler = Server.chatHandlers.get(Server.userHandlers.get(chatCommand.getMatch().getGuestProfile()));
                    targetHandler.oos.writeObject(new RecieveChatCommand(chatCommand.getMsg(), chatCommand.getSender(),ChatChannelType.RIVAL_CHANNEL));
                }
                else {
                    ChatHandler targetHandler = Server.chatHandlers.get(Server.userHandlers.get(chatCommand.getMatch().getHostProfile()));
                    targetHandler.oos.writeObject(new RecieveChatCommand(chatCommand.getMsg(), chatCommand.getSender(),ChatChannelType.RIVAL_CHANNEL));
                }
            }

    }

}
