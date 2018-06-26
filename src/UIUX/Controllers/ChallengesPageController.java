package UIUX.Controllers;

import ClientAndHandlerCommunication.Commands.NewChallengeCommands.GetChallengesCommand;
import ClientAndHandlerCommunication.Responses.NewChallengeResponse.GetChallengesResponse;
import Game.Match;
import NetworkShit.ServerSide.Server;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ChallengesPageController extends ParentController implements Initializable {

    @FXML
    VBox challengesVBox;


    @Override
    public void initialize(URL location, ResourceBundle resources) {

        List<Match> challenges;

        GetChallengesResponse response=(GetChallengesResponse)this.sendCommand(new GetChallengesCommand());

        challenges=response.getChallenges();

        // vaghti safhe baz mishe... challengaye tooye server roo miad tooye vboxesh add mikone
        for (Match match: challenges) {
            System.out.println("gettin matches");
            this.challengesVBox.getChildren().add(match.getMatchTile());
        }

    }

    public void createNewChallenge(){
        this.loadPage("NewChallengePage");

    }

    public void backToMenu(){
        this.loadPage("MainMenu");
    }


}
