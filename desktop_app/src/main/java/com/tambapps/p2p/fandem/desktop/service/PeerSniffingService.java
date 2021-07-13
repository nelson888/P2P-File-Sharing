package com.tambapps.p2p.fandem.desktop.service;

import com.tambapps.p2p.fandem.Fandem;
import com.tambapps.p2p.fandem.SenderPeer;
import com.tambapps.p2p.fandem.util.FileUtils;
import com.tambapps.p2p.speer.Peer;
import com.tambapps.p2p.speer.datagram.service.MulticastReceiverService;
import javafx.beans.property.ObjectProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ToggleButton;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

@Service
public class PeerSniffingService implements MulticastReceiverService.DiscoveryListener<List<SenderPeer>> {

  private final BiConsumer<File, Peer> receiveTaskLauncher;
  private final MulticastReceiverService<List<SenderPeer>> receiverService;
  private final ObjectProperty<File> folderProperty;
  private ToggleButton toggleButton;

  public PeerSniffingService(BiConsumer<File, Peer> receiveTaskLauncher,
      MulticastReceiverService<List<SenderPeer>> receiverService,
      ObjectProperty<File> folderProperty) {
    this.receiveTaskLauncher = receiveTaskLauncher;
    this.receiverService = receiverService;
    this.folderProperty = folderProperty;
  }

  public void start(ToggleButton toggleButton) {
    this.toggleButton = toggleButton;
    receiverService.setListener(this);
    try {
      receiverService.start();
    } catch (IOException e) {
      onError(e);
    }
  }

  private boolean proposePeer(SenderPeer senderPeer) {
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
        String.format("%s wants to send %s (%s)", senderPeer.getDeviceName(), senderPeer.getFileName(),
            FileUtils.toFileSize(senderPeer.getFileSize())),
        new ButtonType("Choose this Peer", ButtonBar.ButtonData.YES),
        new ButtonType("Continue research", ButtonBar.ButtonData.NO));
    alert.setTitle("Sender found");
    alert.setHeaderText(String.format("Sender: %s\nPeer key: %s",
        senderPeer.getDeviceName(), Fandem.toHexString(senderPeer)));

    Optional<ButtonBar.ButtonData> optButton = alert.showAndWait().map(ButtonType::getButtonData);
    switch (optButton.orElse(ButtonBar.ButtonData.OTHER)) {
      case YES:
        return true;
      case NO:
        break;
    }
    return false;
  }

  private void errorDialog(IOException e) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle("An error occurred");
    alert.setHeaderText("an error occurred while searching for sender");
    alert.setContentText(e.getMessage());
    alert.showAndWait();
  }

  @Override
  public void onDiscovery(List<SenderPeer> senderPeers) {
    if (folderProperty.get() == null) {
      // user hasn't picked a directory, let's not propose the peer
      return;
    }
    for (SenderPeer senderPeer : senderPeers) {
      if (proposePeer(senderPeer)) {
        receiveTaskLauncher.accept(folderProperty.get(), senderPeer);
        break;
      }
    }
  }

  @Override
  public void onError(IOException e) {
    errorDialog(e);
    toggleButton.setSelected(false);
  }

}
