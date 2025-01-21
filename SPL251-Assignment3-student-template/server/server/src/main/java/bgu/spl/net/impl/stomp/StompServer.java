package bgu.spl.net.impl.stomp;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.srv.Server;

public class StompServer {

    public static void main(String[] args) {
        // Port number, נבדוק אם נשלח כפרמטר לקומנד ליין
        int port = (args.length > 1) ? Integer.parseInt(args[1]) : 7777;

        // Server type - האם Thread Per Client או Reactor
        String serverType = args[0].toLowerCase();

        if (serverType.equals("tpc")) {
            // Thread Per Client Server
            Server.threadPerClient(
                    port,
                    StompMessagingProtocolImpl::new, // Protocol factory
                    StompMessageEncoderDecoderImpl::new // Encoder/Decoder factory
            ).serve();
        } else if (serverType.equals("reactor")) {
            // Reactor Server
            Server.reactor(
                    Runtime.getRuntime().availableProcessors(),
                    port,
                    StompMessagingProtocolImpl::new, // Protocol factory
                    StompMessageEncoderDecoderImpl::new // Encoder/Decoder factory
            ).serve();
        } else {
            System.out.println("Invalid server type. Use 'tpc' for Thread Per Client or 'reactor' for Reactor.");
        }
    }
}
