package com.tambapps.p2p.fandem.fandemdesktop.controller;

import com.tambapps.p2p.fandem.Peer;
import com.tambapps.p2p.fandem.fandemdesktop.util.NodeUtils;
import com.tambapps.p2p.fandem.fandemdesktop.util.PropertyUtils;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.UnknownHostException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Component
public class ReceivePaneController {

  private final Supplier<File> directoryChooser;
  private final Supplier<Boolean> canAddTaskSupplier;
  private final BiConsumer<File, Peer> receiveTaskLauncher;

  @FXML
  private Label pathLabel;
  @FXML
  private TextField ipField0;
  @FXML
  private TextField ipField1;
  @FXML
  private TextField ipField2;
  @FXML
  private TextField ipField3;
  @FXML
  private TextField portField;
  @FXML
  private TextField hexCodeField;

  private List<TextField> ipFields;
  private ObjectProperty<File> folderProperty = new SimpleObjectProperty<>();

  public ReceivePaneController(@Qualifier("directoryChooser") Supplier<File> directoryChooser,
                               Supplier<Boolean> canAddTaskSupplier,
                               BiConsumer<File, Peer> receiveTaskLauncher) {
    this.directoryChooser = directoryChooser;
    this.canAddTaskSupplier = canAddTaskSupplier;
    this.receiveTaskLauncher = receiveTaskLauncher;
  }

  @FXML
  private void initialize() {
    ipFields = List.of(ipField0, ipField1, ipField2, ipField3);
    ipFields.forEach(NodeUtils::numberTextField);
    portField.setText("8081");
    NodeUtils.numberTextField(portField);
    PropertyUtils
      .bindMapNullableToStringProperty(folderProperty, File::getPath, pathLabel.textProperty());
  }

  @FXML
  private void pickFolder() {
    File file = directoryChooser.get();
    if (file == null) {
      return;
    }
    folderProperty.set(file);
  }

  private Peer verifiedPeer() throws UnknownHostException, IllegalArgumentException {
    String hexCode = hexCodeField.getText();
    if (hexCode != null && !hexCode.isEmpty()) {
      try {
        return Peer.fromHexString(hexCode);
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("Hex code is malformed");
      }
    }
    if (ipFields.stream().anyMatch(ipField -> ipField.textProperty().get().isEmpty())) {
      throw new IllegalArgumentException("You must provide the sender's IP");
    }
    if (portField.textProperty().get().isEmpty()) {
      throw new IllegalArgumentException("You must provide the sender's port");
    }
    return Peer.of(getAddress(), Integer.parseInt(portField.textProperty().get()));
  }

  @FXML
  private void receiveFile() {
    Peer peer;
    try {
      peer = verifiedPeer();
    } catch (IllegalArgumentException e) {
      Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK);
      alert.show();
      return;
    } catch (UnknownHostException e) {
      Alert alert = new Alert(Alert.AlertType.ERROR, "Couldn't find the host", ButtonType.OK);
      alert.show();
      return;
    }
    File file = folderProperty.get();
    if (file == null) {
      Alert alert = new Alert(Alert.AlertType.INFORMATION, "You haven't picked a directory yet",
        ButtonType.OK);
      alert.show();
      return;
    }
    if (!canAddTaskSupplier.get()) {
      Alert alert = new Alert(Alert.AlertType.INFORMATION,
        "Maximum tasks number reached. Wait until one task is over to start another.",
        ButtonType.OK);
      alert.show();
      return;
    }
    receiveTaskLauncher.accept(file, peer);
    folderProperty.set(null);
    ipFields.forEach(field -> field.setText(""));
    hexCodeField.setText("");
    portField.setText("8081");
  }

  private String getAddress() {
    return ipFields.stream()
      .map(field -> field.textProperty().get())
      .collect(Collectors.joining("."));
  }

}