import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.Naming;
import java.net.InetAddress;

public class OtsuRMIServer {
  public static void main(String[] args) {
    try {
      RemoteOtsu otsu = new OtsuTresholding();
      // Lan
      Naming.bind("rmi://172.20.10.3:1234/Otsu", otsu);
      // localhost
      // Naming.bind("Otsu", otsu);
      System.out.println("Running...");
    } catch (Exception e) {
      System.out.println(e);
    }
  }
}
