import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import gc.ConfigLoader;
import gc.Message;
import gc.OrderedGroupCommunicator;

public class BroadcastSample {
	static boolean stop = false;

	public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
		Scanner scanner = new Scanner(System.in);
		Integer paramId = null;
		OrderedGroupCommunicator loaded = null;
		try {
			paramId = Integer.valueOf(args[0]);
		} catch (Exception e) {
		}
		if (paramId == null) {
			loaded = ConfigLoader.loadOGC("node.config");
		} else {
			loaded = ConfigLoader.loadOGC("node.config", paramId);
		}
		final OrderedGroupCommunicator gc = loaded;

		System.out.println("Init");
		gc.init();
		System.out.println("Done");

		CopyOnWriteArrayList<Message> pending = gc.getPending();
		LinkedBlockingQueue<Message> delivered = gc.getDelivered();

		NodeWindow window = new NodeWindow(paramId);

		Thread receiveingThread = new Thread(() -> {
			try {
				while (!stop) {
					//System.out.println("---------------ESPERA--------------");
					ArrayList<Object> pendingList = new ArrayList<>();
					for (Iterator<Message> iterator = pending.iterator(); iterator.hasNext();) {
						Message message = (Message) iterator.next();
						String str = "Nº Sequencia: " + message.getSequence() + " -> " + (String) message.getPayload();
						pendingList.add(str);
					}
					
					for (Iterator<Message> iterator = delivered.iterator(); iterator.hasNext();) {
						Message message = (Message) iterator.next();
						String str = "Nº Sequencia: " + message.getSequence() + " -> " + (String) message.getPayload();
						pendingList.add(str);
					}
					window.updatePending(pendingList.toArray());
					Thread.sleep(1000);
					
					Message received = gc.receive(100);
					
					
					pendingList.clear();
					for (Iterator<Message> iterator = pending.iterator(); iterator.hasNext();) {
						Message message = (Message) iterator.next();
						String str = "Nº Sequencia: " + message.getSequence() + " -> " + (String) message.getPayload();
						pendingList.add(str);
					}
					
					for (Iterator<Message> iterator = delivered.iterator(); iterator.hasNext();) {
						Message message = (Message) iterator.next();
						String str = "Nº Sequencia: " + message.getSequence() + " -> " + (String) message.getPayload();
						pendingList.add(str);
					}
					
					if (received != null) {

						//System.out.println("--------------------------------------");

						//System.out.println("---------------RECEBIDO---------------");
						Thread.sleep(1000);
						String str = "Nº Sequencia: " + received.getSequence() + " -> " + (String) received.getPayload();
						//System.err.println((String) received.getPayload());
						pendingList.add(str);
						window.updatePending(pendingList.toArray());

						pendingList.remove(str);
						window.updatePending(pendingList.toArray());
						
						window.updateReceived(received.getPayload());
						//System.out.println("--------------------------------------");
					} else {
						window.updatePending(pendingList.toArray());
						//System.out.println("--------------------------------------");

					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});

		receiveingThread.start();

		gc.setDelayedBroadcast(true);
		int broadcastsAmount = Integer.valueOf(args[1]);
		for (int i = 0; i < broadcastsAmount; i++) {
			String m = gc.getId() + "-" + i;
			Thread.sleep(1000);
			gc.broadcast(m);
		}
		scanner.nextLine();
		stop = true;
		receiveingThread.join();

		gc.stop();
		gc.close();

		scanner.close();
	}
}
