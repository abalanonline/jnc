/*
 * Copyright 2020 Aleksei Balan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ab;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

import java.io.File;

public class TestJavaFX extends Application {

  public static void main(String[] args) {
    launch(args);
  }

  public static Button newButton(String name, EventHandler<ActionEvent> action) {
    Button button = new Button();
    button.setText(name);
    button.setOnAction(action);
    return button;
  }

  @Override
  public void start(Stage primaryStage) {
    MediaPlayer sound = new MediaPlayer(new Media(new File("sound.mp3").toURI().toString()));
    MediaPlayer music = new MediaPlayer(new Media(new File("music.mp3").toURI().toString()));

    primaryStage.setTitle("Hello World!");
    final int[] i = {0};

    StackPane root = new StackPane();
    ObservableList<Node> nodes = root.getChildren();
    nodes.add(newButton("play music", event -> {
      switch (i[0]++) {
        case 0: music.play(); break;
        case 1: sound.stop(); sound.play(); break;
        case 2: sound.stop(); sound.play(); break;
        case 3: music.stop(); break;
        default: i[0] = 0;
      }
    }));
    primaryStage.setScene(new Scene(root, 300, 250));
    primaryStage.show();
  }
}
