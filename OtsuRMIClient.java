import java.rmi.Naming;
import java.rmi.RemoteException;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.Image;
import java.awt.BorderLayout;
import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.awt.Color;

public class OtsuRMIClient {
  final JFileChooser fc = new JFileChooser("/Users/rodrigocs/School/Paralela/3/OtsuRMI/images");
  BufferedImage sourceImage;
  JFrame frame;
  JPanel panel, bottomPanel, imageContainer, actionsPanel;
  Image scaledImage;
  JButton selectImage, compute, parallel;
  Action actionLoad, actionCompute, actionParallel;
  JLabel imageLabel, executionTime;
  JProgressBar progressBar = new JProgressBar();
  int[][] matrix;
  static RemoteOtsu otsu;
  public static void main(String[] args) throws RemoteException {
    try {
      OtsuRMIClient client = new OtsuRMIClient();
      // Lan
      otsu = (RemoteOtsu) Naming.lookup("rmi://172.20.10.3:1234/Otsu");
      // localhost
      // otsu = (RemoteOtsu) Naming.lookup("Otsu");
      client.initUI();
    } catch (Exception e) {
      System.out.println("Error: " + e);
    }
  }

  void initUI() {
    actionLoad = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          otsu.clearMatrix();
          FileFilter imageFilter = new FileNameExtensionFilter("Image files", ImageIO.getReaderFileSuffixes());
          fc.addChoosableFileFilter(imageFilter);
          if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fc.getSelectedFile();
            try {
              imageContainer.removeAll();
              sourceImage = ImageIO.read(selectedFile);
              int width = sourceImage.getWidth();
              int height = sourceImage.getHeight();
              matrix = imageToMatrix(sourceImage, width, height);
              int scaledWidth = (width * 400) / height;
              scaledImage = sourceImage.getScaledInstance(scaledWidth, 400, Image.SCALE_SMOOTH);
              imageLabel = new JLabel(new ImageIcon(scaledImage));
              imageContainer.add(imageLabel);
              frame.revalidate();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
          }
        } catch (Exception err) {
          //TODO: handle exception
        }
      }
    };
    actionCompute = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          progressBar.setStringPainted(true);
          progressBar.setIndeterminate(true);
          progressBar.setVisible(true);
          otsu.appendMatrix(matrix, false);
          Boolean waiting = true;
          while (waiting) {
            int [][] o = otsu.getOtsuMatrix();
            long elapsedTime = otsu.getElapsedTime();
            System.out.println("OtsuLength" + o.length);
            if (o.length > 1) {
              int width = o.length;
              int height = o[0].length;
              int scaledWidth = (width * 400) / height;
              sourceImage = getImageFromMatrix(o, width, height);
              imageContainer.removeAll();
              scaledImage = sourceImage.getScaledInstance(scaledWidth, 400, Image.SCALE_SMOOTH);
              imageLabel = new JLabel(new ImageIcon(scaledImage));
              imageContainer.add(imageLabel);
              executionTime.setText("Tiempo: " + elapsedTime + "ms");
              frame.revalidate();
              waiting = false;
              progressBar.setVisible(false);
            }
          }          
        } catch (Exception err) {
          err.printStackTrace();
        }
      }
    };
    actionParallel = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          progressBar.setStringPainted(true);
          progressBar.setIndeterminate(true);
          progressBar.setVisible(true);
          otsu.appendMatrix(matrix, true);
          Boolean waiting = true;
          while (waiting) {
            int [][] o = otsu.getOtsuMatrix();
            long elapsedTime = otsu.getElapsedTime();
            System.out.println("OtsuLength" + o.length);
            if (o.length > 1) {
              int width = o.length;
              int height = o[0].length;
              int scaledWidth = (width * 400) / height;
              sourceImage = getImageFromMatrix(o, width, height);
              imageContainer.removeAll();
              scaledImage = sourceImage.getScaledInstance(scaledWidth, 400, Image.SCALE_SMOOTH);
              imageLabel = new JLabel(new ImageIcon(scaledImage));
              imageContainer.add(imageLabel);
              executionTime.setText("Tiempo: " + elapsedTime + "ms");
              frame.revalidate();
              waiting = false;
              progressBar.setVisible(false);
            }
          }          
        } catch (Exception err) {
          err.printStackTrace();
        }
      }
    };
    frame = new JFrame("Otsu Client");
    panel = new JPanel(new BorderLayout());
    imageContainer = new JPanel();
    bottomPanel = new JPanel(new BorderLayout()); 
    actionsPanel = new JPanel(new BorderLayout());
    selectImage = new JButton("Select image");
    compute = new JButton("Compute image");
    parallel = new JButton("Compute parallel");
    executionTime = new JLabel("Tiempo: 0ms");
    selectImage.addActionListener(actionLoad);
    compute.addActionListener(actionCompute);
    parallel.addActionListener(actionParallel);
    actionsPanel.add(executionTime);
    actionsPanel.add(compute, BorderLayout.WEST);
    actionsPanel.add(parallel, BorderLayout.EAST);
    bottomPanel.add(progressBar, BorderLayout.NORTH);
    bottomPanel.add(actionsPanel, BorderLayout.SOUTH);
    panel.add(selectImage, BorderLayout.NORTH);
    panel.add(imageContainer, BorderLayout.CENTER);
    panel.add(bottomPanel, BorderLayout.SOUTH);
    frame.add(panel);
    frame.setSize(500, 500);
    frame.setVisible(true);
  }

    /**
   * 
   * @param image
   * @param width
   * @param height
   * @return A Matrix with the gray values of the image
   */
  private static int[][] imageToMatrix(BufferedImage image, int width, int height) {
    int[][] result = new int[height][width];
    for (int row = 0; row < height; row++) {
      for (int col = 0; col < width; col++) {
        int rgb = image.getRGB(col, row);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = (rgb & 0xFF);
        int gray = (r + g + b) / 3;
        result[row][col] = gray;
      }
    }

    return result;
  }

  /**
   * 
   * @param matrix
   * @param width
   * @param height
   * @return A buffered image from a matrix
   */
  private static BufferedImage getImageFromMatrix(int[][] matrix, int width, int height) {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    for (int i = 0; i < height; i++) {
      for (int j = 0; j < width; j++) {
        int pixel = matrix[i][j];
        int color = new Color(0, 0, pixel).getRGB();
        image.setRGB(j, i, color);
      }
    }
    return image;
  }

}