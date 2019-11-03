package com.tambapps.p2p.fandem.desktop.service;

import com.tambapps.p2p.fandem.FileSharer;
import com.tambapps.p2p.fandem.Peer;
import com.tambapps.p2p.fandem.desktop.model.SendingTask;
import com.tambapps.p2p.fandem.listener.TransferListener;
import com.tambapps.p2p.fandem.util.IPUtils;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.Future;

public class FileSharingService {

  private final FileSharer fileSharer = new FileSharer();

  public SendingTask sendFile(File file) throws IOException {
    SendingTask sendingTask = new SendingTask();
    sendingTask.file.set(file);
    Peer peer;
    try {
      peer = IPUtils.getAvailablePeer();
    } catch (SocketException e) {
      throw new IOException("Couldn't retrieve IP. Are you connected to internet?");
    }
    Future future = fileSharer.sendFile(file, peer, new TransferListener() {
      @Override
      public void onConnected(Peer selfPeer, Peer remotePeer, String fileName, long fileSize) {
        sendingTask.peer.set(selfPeer);
        sendingTask.remotePeer.set(remotePeer);
        sendingTask.totalBytes.set(fileSize);
      }

      @Override
      public void onProgressUpdate(int progress, long byteProcessed, long totalBytes) {
        sendingTask.percentage.setValue(progress);
        sendingTask.bytesProcessed.set(byteProcessed);
      }
    }, e -> {
      // TODO
    });
    sendingTask.setCanceler(() -> future.cancel(true));
    return sendingTask;
  }
}
