import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;
import javafx.scene.control.TextArea;
import javafx.stage.WindowEvent;
import javafx.event.EventHandler;


import java.io.*;
import java.net.*;
import java.util.Date;


public class Server extends Application implements Constants {

    private int sessionNb = 1;
    private int port = 60000;


    @Override
    public void start(Stage stage) {
        TextArea textInfo = new TextArea();


        Scene board = new Scene(new ScrollPane(textInfo), 450,200);
        stage.setTitle("Tic-Tac-Toe-Server");
        stage.setScene(board);
        stage.show();

        new Thread(() -> {

            try{

                ServerSocket serverSocket = new ServerSocket(port);
                Platform.runLater(() -> textInfo.appendText(new Date()+ ": Server started at port "
                        + port +'\n'));

                while(true){
                    Platform.runLater(()-> textInfo.appendText(new Date()
                            + ": waiting for players to join session "
                            + sessionNb + "\n"));

                    Socket firstPL = serverSocket.accept();

                    Platform.runLater(() -> {
                        textInfo.appendText(new Date()
                                + " : Player 1 has joined the session"
                                + sessionNb +"\n");
                        textInfo.appendText("First player IP: "
                                + firstPL.getInetAddress().getHostAddress() + "\n");
                            });

                    new DataOutputStream(firstPL.getOutputStream()).writeInt(PL_1);

                    Socket secondPL = serverSocket.accept();

                    Platform.runLater(() -> {
                        textInfo.appendText(new Date()
                                + ": Player 2 has joined the session"
                                + sessionNb +"\n");
                        textInfo.appendText("Second player IP: "
                                + secondPL.getInetAddress().getHostAddress() + "\n");
                    });

                    new DataOutputStream(secondPL.getOutputStream()).writeInt(PL_2);

                    Platform.runLater(()-> textInfo.appendText(new Date()
                            + ": Start new thread for session"
                            + sessionNb++ + "\n"));


                    stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                        @Override
                        public void handle(WindowEvent event) {
                            Platform.exit();
                            System.exit(0);
                        }
                    });
                    new Thread(new SessionHandler(firstPL,secondPL)).start();

                }
            }
            catch(IOException ex){
                ex.printStackTrace();
            }

        }).start();

    }

    class SessionHandler implements Runnable, Constants {
        private Socket firstPL;
        private Socket secondPL;

        private char[][] cell = new char[3][3];

        private DataInputStream fromPl1;
        private DataOutputStream toPl1;

        private DataInputStream fromPl2;
        private DataOutputStream toPl2;

        private boolean continueGame = true;

        public SessionHandler(Socket firstPL, Socket secondPL){
            this.firstPL = firstPL;
            this.secondPL = secondPL;

            for (int i = 0; i < 3; i++)
                for(int j = 0; j <3; j++)
                    cell [i][j] = ' ';
        }

        @Override
        public void run() {

            try {


                fromPl1 = new DataInputStream(firstPL.getInputStream());
                toPl1 = new DataOutputStream(firstPL.getOutputStream());

                fromPl2 = new DataInputStream(secondPL.getInputStream());
                toPl2 = new DataOutputStream(secondPL.getOutputStream());

                toPl1.writeInt(1);


                while(continueGame){

                    int r = fromPl1.readInt();
                    int c = fromPl1.readInt();
                    cell[r][c] = 'X';


                    System.out.println(r + " " + c);



                    if (isWon('X')) {
                        toPl1.writeInt(PL_1_WON);
                        toPl2.writeInt(PL_1_WON);

                        sendMove(toPl2, r , c);


                        break;
                    }
                    else if (isFull()){
                        toPl1.writeInt(DRAW);
                        toPl2.writeInt(DRAW);

                        sendMove(toPl2, r , c);

                        break;
                    }
                    else {
                       toPl2.writeInt(CONTINUE);

                        sendMove(toPl2, r , c);

                    }

                    r = fromPl2.readInt();
                    c = fromPl2.readInt();
                    cell[r][c] = 'O';


                    if (isWon('O')) {
                        toPl1.writeInt(PL_2_WON);
                        toPl2.writeInt(PL_2_WON);
                        sendMove(toPl1, r , c);

                        break;
                    }else{
                        toPl1.writeInt(CONTINUE);

                        sendMove(toPl1, r , c);

                    }
                }


            }catch (IOException ex){
                ex.printStackTrace();
            }
        }

        private void sendMove(DataOutputStream out, int r, int c) throws IOException{

            out.writeInt(r);
            out.writeInt(c);

        }

        private boolean isFull(){
            for (int i = 0; i<3; i++)
                for (int j = 0; j<3; j++)
                    if (cell[i][j] == ' ')
                        return false;
            return true;
        }

        private boolean isWon(char symbol){

            for(int i = 0; i < 3; i++){
                if ((cell[i][0] == symbol)
                        && (cell[i][1] == symbol)
                        && (cell[i][2] == symbol)){

                    return true;
                }

            }

            for(int j = 0; j < 3; j++){
                if ((cell[0][j] == symbol)
                        && (cell[1][j] == symbol)
                        && (cell[2][j] == symbol)){

                    return true;
                }

            }

            if ((cell[0][0] == symbol)
                    && (cell[1][1] == symbol)
                    && (cell[2][2] == symbol)){
                return true;
            }

            if ((cell[0][2] == symbol)
                    && (cell[1][1] == symbol)
                    && (cell[2][0] == symbol)){
                return true;
            }

            return false; //CONTINUE


        }

    }


}

