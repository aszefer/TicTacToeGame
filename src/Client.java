import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;


import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client extends Application implements Constants {

    private boolean myMove = false;

    private char mySymbol = ' ';

    private char enemySymbol = ' ';


    private Cell[][] cell = new Cell[3][3];

    private Label title = new Label();
    private Label status = new Label();

    private int selRow;
    private int selCol;

    private DataInputStream fromServer;
    private DataOutputStream toServer;

    private boolean continueGame = true;
    private boolean waiting = true;

    private String host = "localhost";
    private int port = 60000;


    @Override
    public void start(Stage stage) {

        host = getServerHost();

        GridPane pane = new GridPane();
        for(int i = 0; i < 3 ; i++)
            for (int j = 0; j < 3; j++)
                pane.add(cell[i][j] = new Cell(i,j), i, j);


        BorderPane borderPane = new BorderPane();
        borderPane.setTop(title);
        borderPane.setCenter(pane);
        borderPane.setBottom(status);

        Scene board = new Scene(borderPane, 320, 350);
        stage.setTitle("Tic-Tac-Toe-Client");
        stage.setScene(board);
        stage.show();

        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                Platform.exit();
                System.exit(0);
            }
        });

        connectToServer();


    }

    public String getServerHost(){

        Scanner sc = new Scanner(System.in);
        System.out.println("Enter server's IP: ");
        String newHost = sc.nextLine();

        if (newHost.length() != 0){
            return newHost;
        }
            return host;

    }

    public void connectToServer(){

        try{

            Socket socket = new Socket(host, port);

            fromServer = new DataInputStream(socket.getInputStream());

            toServer = new DataOutputStream(socket.getOutputStream());
        }
        catch(Exception ex){
            System.out.println("Could not connect to server");
            return;
//            ex.printStackTrace();
        }

        new Thread(() -> {

            try{

                int player = fromServer.readInt();

                if (player == PL_1){
                    mySymbol = 'X';
                    enemySymbol = 'O';
                    Platform.runLater(() -> {
                        title.setText("Player 1 (X)");
                        status.setText("Waiting for second player...");
                    });

                    fromServer.readInt();

                    Platform.runLater(()->
                            status.setText("Player 2 has joined. You can start"));

                    myMove = true;
                }

                else if (player == PL_2) {
                    mySymbol = 'O';
                    enemySymbol = 'X';
                    Platform.runLater(() -> {
                        title.setText("Player 2 (O)");
                        status.setText("Waiting for first player's move");
                    });

                }

                while(continueGame){

                    if(player == PL_1){
                        waitForPlAction();
                        sendMove();
                        receiveInfo();

                    }else if(player == PL_2){
                        receiveInfo();
                        waitForPlAction();
                        sendMove();

                    }
                }

            }
            catch(Exception ex){
                ex.printStackTrace();
            }

        }).start();
    }

    private void waitForPlAction() throws InterruptedException {
        while(waiting){
            Thread.sleep(100);
        }

        waiting = true;
    }

    private void sendMove() throws IOException{
        toServer.writeInt(selRow);
        toServer.writeInt(selCol);
    }

    private void receiveInfo() throws IOException {

        int currentStatus = fromServer.readInt();

        if (currentStatus == PL_1_WON){
            continueGame = false;
            if (mySymbol == 'X') {

                Platform.runLater(() ->
                        status.setText("You win!"));

            }else if(mySymbol == 'O'){

                Platform.runLater(() ->
                        status.setText("Player 1 (X) has won!"));
                receiveMove();
            }
        }
        else if(currentStatus == PL_2_WON){

            continueGame = false;
            if (mySymbol == 'O'){
                Platform.runLater(()->
                        status.setText("You win!"));

            }else if (mySymbol == 'X'){

                Platform.runLater(() ->
                        status.setText("Player 2 (O) has won!"));
                receiveMove();

            }
        }
        else if (currentStatus == DRAW){

            continueGame = false;
            Platform.runLater(()->
                    status.setText("Draw"));

            if(mySymbol == 'O'){
                receiveMove();
            }
        }
        else if (currentStatus == CONTINUE) {
            receiveMove();
            Platform.runLater(() -> status.setText("My turn"));
            myMove = true;
        }

    }

    private void receiveMove() throws IOException{

        int r = fromServer.readInt();
        int c = fromServer.readInt();

        Platform.runLater(()-> cell[r][c].setSymbol(enemySymbol));
    }


    public class Cell extends Pane{

        private int r;
        private int c;
        private char symbol = ' ';

        public Cell(int r, int c){
            this.r = r;
            this.c = c;
            this.setPrefSize(2000,2000);
            setStyle("-fx-border-color: black");
            this.setOnMouseClicked(e -> handleMouseClick());
        }

        public char getSymbol(){
            return symbol;
        }

        public void setSymbol(char c){
            symbol = c;
            repaint();
        }

        protected void repaint() {
            if (symbol == 'X'){

                Line l1 = new Line(10, 10,
                        this.getWidth() - 10, this.getHeight() - 10);

                l1.endXProperty().bind(this.widthProperty().subtract(10));
                l1.endYProperty().bind(this.heightProperty().subtract(10));

                Line l2 = new Line(10, this.getHeight() - 10,
                        this.getWidth() - 10, 10);

                l1.endYProperty().bind(this.heightProperty().subtract(10));
                l2.endXProperty().bind(this.widthProperty().subtract(10));

                l1.setStroke(Color.BLACK);
                l2.setStroke(Color.BLACK);

                this.getChildren().addAll(l1,l2);
            }
            else if (symbol == 'O'){
                Ellipse el = new Ellipse(this.getWidth()/2,
                        this.getHeight() / 2,
                        this.getWidth() / 2-10,
                        this.getHeight() / 2-10);

                el.centerXProperty().bind(this.widthProperty().divide(2));
                el.centerYProperty().bind(this.heightProperty().divide(2));

                el.radiusXProperty().bind(this.widthProperty().divide(2).subtract(10));
                el.radiusYProperty().bind(this.heightProperty().divide(2).subtract(10));

                el.setStroke(Color.BLACK);
                el.setFill(Color.rgb(179, 230, 179));

                getChildren().add(el);
            }
        }

        private void handleMouseClick(){

            if (symbol == ' ' && myMove){
                setSymbol(mySymbol);
                myMove = false;

                selRow = r;
                selCol = c;

                status.setText("Waiting for other player's move...");
                waiting = false;
            }
        }


    }



}
