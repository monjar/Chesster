package NetworkStuff.ServerSide.Handlers;

import BasicClasses.LoginInformation;
import ClientAndHandlerCommunication.Commands.Command;
import ClientAndHandlerCommunication.Commands.Common.ChangeGameStateCommand;
import ClientAndHandlerCommunication.Commands.FirstPageCommands.CheckLoginValidnessCommand;
import ClientAndHandlerCommunication.Commands.FirstPageCommands.CreateProfileCommand;
import ClientAndHandlerCommunication.Commands.FirstPageCommands.GetProfileCommand;
import ClientAndHandlerCommunication.Commands.FirstPageCommands.SetProfileCommand;
import ClientAndHandlerCommunication.Commands.NewChallengeCommands.CreateMatchCommand;
import ClientAndHandlerCommunication.Commands.NewChallengeCommands.DeleteChallengesCommand;
import ClientAndHandlerCommunication.Commands.NewChallengeCommands.GetChallengesCommand;
import ClientAndHandlerCommunication.Commands.ParentCommands.UsernameExistenceCommand;
import ClientAndHandlerCommunication.Responses.Common.ChangeGameStateResponse;
import ClientAndHandlerCommunication.Responses.FirstPageResponses.GetProfileResponse;
import ClientAndHandlerCommunication.Responses.FirstPageResponses.LoginIsValidResponse;
import ClientAndHandlerCommunication.Responses.FirstPageResponses.ProfileCreationResponse;
import ClientAndHandlerCommunication.Responses.NewChallengeResponse.GetChallengesResponse;
import ClientAndHandlerCommunication.Responses.ParentResponds.UsernameExistenceRespond;
import ClientAndHandlerCommunication.Responses.Response;
import Enums.GameState;
import Game.Match;
import Game.Profile;
import NetworkStuff.ServerSide.Log.ServerLogWriter;
import NetworkStuff.ServerSide.SemiDataBase.DataBaseUpdator;
import NetworkStuff.ServerSide.Server;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//Client UserHandler
public class UserHandler implements Runnable, Serializable {

    private GameState gameState; //current state of game!
    private Socket socket;    //Socket that refers to client-e marboote

    private Profile profile;    //Profile-e marboot be Client-e Marboote :))

    private boolean isclientConnected;

    //	iosHaa-e Mored-e Niaaz
    private ObjectInputStream ois;
    private ObjectOutputStream oos;

    public UserHandler(Socket socket) {
        this.setSocket(socket);    //Socket ro set mikonim!
        gameState = GameState.MAIN_MENU; //Avvalesh too-e MainMenuEm!
        try {
            this.oos = new ObjectOutputStream(this.socket.getOutputStream());
            this.ois = new ObjectInputStream(this.socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        this.isclientConnected = true;

        while (isclientConnected) {
            while (this.gameState == GameState.IN_GAME_PAGE) {
//				KaarHaa-e marboot be baaZ ro anjaam bede :))
            }
            Command clientCommand = this.getCommandFromClient();
            Response response = this.produceResponse(clientCommand);
            this.sendResponseToClient(response);
        }

    }

    private Command getCommandFromClient() {
        Command returnValue = null;
        try {
            returnValue = (Command) this.ois.readObject();

        } catch (IOException | ClassNotFoundException e) {
            if (e instanceof EOFException) {
                try {

                    ServerLogWriter.getInstance().writeLog(this.profile.getUserName() + " Dissconnected.");

                } catch (NullPointerException e1) {

                    ServerLogWriter.getInstance().writeLog("A Client Has DissConnected");
                }
                this.isclientConnected = false;

            } else
                e.printStackTrace();
        }
        return returnValue;
    }

    private void sendResponseToClient(Response response) {
        try {
            this.oos.writeObject(response);
        } catch (IOException e) {
            if (e instanceof EOFException) {
                ServerLogWriter.getInstance().writeLog(this.profile.getUserName() + " Dissconnected.");
                this.isclientConnected = false;

            } else
                e.printStackTrace();
        }
    }

    private Response produceResponse(Command command) {
        //System.out.println(this.profile);
        Response returnValue = null;
        if (command instanceof CheckLoginValidnessCommand) {
            LoginInformation temp = ((CheckLoginValidnessCommand) command).getLoginInformation();
            ServerLogWriter.getInstance().writeLog("Check login validness command recieved from: "+temp.getUsername());
            boolean answer = this.isLoginValid(temp);

            returnValue = new LoginIsValidResponse(answer);
//			return ( new LoginIsValidResponse( answer ) );
        } else if (command instanceof CreateProfileCommand) {
            Profile newProfile = ((CreateProfileCommand) command).getProfile();
            ServerLogWriter.getInstance().writeLog("create profile command recieved from: "+newProfile.getUserName());
            returnValue = this.addProfile(newProfile);
        } else if (command instanceof GetProfileCommand) {
            Profile profile = Server.profiles.get(((GetProfileCommand) command).getUserName());
            ServerLogWriter.getInstance().writeLog("Get profile command recieved from: "+profile.getUserName());
            returnValue = new GetProfileResponse(profile);
        } else if (command instanceof SetProfileCommand) {

            ServerLogWriter.getInstance().writeLog("set profile command recieved from: "+((SetProfileCommand) command).getProfile().getUserName());
            this.setProfile(((SetProfileCommand) command).getProfile());
        } else if (command instanceof UsernameExistenceCommand) {
            ServerLogWriter.getInstance().writeLog("User existance command recieved from: "+((UsernameExistenceCommand) command).getUsername());
            if (Server.profiles.get(((UsernameExistenceCommand) command).getUsername()) == null)
                returnValue = new UsernameExistenceRespond(false);
            else returnValue = new UsernameExistenceRespond(true);
        } else if (command instanceof ChangeGameStateCommand) {
            returnValue = this.changeGameStateThings((ChangeGameStateCommand) command);
        } else if (command instanceof GetChallengesCommand) {

            GetChallengesResponse challengesResponse = new GetChallengesResponse();
            Map<Match, Profile> serverChallenges = new ConcurrentHashMap<>(Server.challenges);
            challengesResponse.setChallenges(serverChallenges);
            //System.out.println(challengesResponse.getChallenges());
            returnValue = challengesResponse;
        } else if (command instanceof CreateMatchCommand) {
            Match match = ((CreateMatchCommand) command).getMatch();
            ServerLogWriter.getInstance().writeLog("Create match command recieved from: "+match.getHostProfile().getUserName());
            Server.challenges.put(match, match.getHostProfile());

            returnValue = null;
        } else if (command instanceof DeleteChallengesCommand) {
            Server.challenges.keySet().removeAll(((DeleteChallengesCommand) command).getList());
            if (((DeleteChallengesCommand) command).getActive() != null)
                Server.challenges.put(((DeleteChallengesCommand) command).getActive(), ((DeleteChallengesCommand) command).getActive().getHostProfile());

        }
        return returnValue;
    }

    private Response changeGameStateThings(ChangeGameStateCommand command) {
        this.gameState = command.getGameState();
        return new ChangeGameStateResponse(true);
    }

    private ProfileCreationResponse addProfile(Profile profile) {
        ProfileCreationResponse returnValue;
        if (this.isLoginValid(new LoginInformation(profile.getUserName(), profile.getPassword())))
            returnValue = new ProfileCreationResponse(false, "Username already exists");
        else if (this.profileExists(profile))
            returnValue = new ProfileCreationResponse(false, "Try another username and password");
        else {
            returnValue = new ProfileCreationResponse(true, "User created successfully");
            Server.profiles.put(profile.getUserName(), profile);
            DataBaseUpdator.getInstance().updateDataBase();
            ServerLogWriter.getInstance().writeLog("User: "+profile.getUserName()+" created an account.");


/*			for (Map.Entry<String,Profile> iterator : Server.profiles.entrySet() )
				System.out.print ( iterator.getKey() + " " + iterator.getValue()+ "     " );*/
            //System.out.println();
        }
        return returnValue;
    }

    private boolean profileExists(Profile profile) {
        if (this.isLoginValid(new LoginInformation(profile.getUserName(), profile.getPassword())))
            if (Server.profiles.get(profile.getUserName()).getPassword().equals(profile.getPassword()))
                return true;
        return false;

    }

    private boolean isLoginValid(LoginInformation loginInformation) {
        return (Server.profiles.get(loginInformation.getUsername()) != null);
    }

    //	Getters and Setters

    private void setSocket(Socket socket) {
        this.socket = socket;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
        Server.userHandlers.put(profile, this);
        //System.out.println(Server.userHandlers);


    }


	/*@Override
	public int hashCode() {
		return this.profile.getUserName().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		UserHandler handler=(UserHandler) obj;
		return (handler.profile.getUserName().equals(this.profile.getUserName()));
	}*/
}
