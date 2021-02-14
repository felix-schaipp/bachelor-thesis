package svgTransformation;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import org.javatuples.Pair;
import org.opencv.core.Mat;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

public class Utils {

    /**
     * Convert a Mat object (OpenCV) in the corresponding Image for JavaFX
     *
     * @param frame the {@link Mat} representing the current frame
     * @return the {@link Image} to show
     */
    public static Image mat2Image(Mat frame) {
        try {
            return SwingFXUtils.toFXImage(matToBufferedImage(frame), null);
        } catch (Exception e) {
            System.err.println("Cannot convert the Mat obejct: " + e);
            return null;
        }
    }

    /**
     * Generic method for putting element running on a non-JavaFX thread on the
     * JavaFX thread, to properly update the UI
     *
     * @param property a {@link ObjectProperty}
     * @param value    the value to set for the given {@link ObjectProperty}
     */
    public static <T> void onFXThread(final ObjectProperty<T> property, final T value) {
        Platform.runLater(() -> {
            property.set(value);
        });
    }

    /**
     * Support for the mat2Image() method
     *
     * @param original the {@link Mat} object in BGR or grayscale
     * @return the corresponding {@link BufferedImage}
     */
    public static BufferedImage matToBufferedImage(Mat original) {
        // init
        BufferedImage image = null;
        int width = original.width(), height = original.height(), channels = original.channels();
        byte[] sourcePixels = new byte[width * height * channels];
        original.get(0, 0, sourcePixels);

        if (original.channels() > 1) {
            image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        } else {
            image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        }
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);

        return image;
    }

    /**
     * check if an arraylist is empty or not
     *
     * @param list is an arraylist
     * @return boolean value (false by default)
     */
    public static boolean IsEmpty(ArrayList list) {
        boolean empty = false;

        if (list.size() == 0) {
            empty = true;
        }

        return empty;
    }

    /**
     * @param haystack the array in which you are searching for the needle
     * @param needle   the string you want to know the frequency of
     */
    public static int countFrequencies(ArrayList<String> haystack, String needle) {
        Map<String, Integer> Count = new HashMap<String, Integer>();
        int i = 0;
        for (String t : haystack) {
            if (t.equals(needle)) {
                i++;
            }
        }
        return i;
    }

    /**
     * format a given float value to 2 decimal places
     *
     * @param val floating value
     * @return floating decimal with 2 decimal places as a string
     */
    public static String roundOffTo2DecPlaces(double val) {
        return String.format("%.2f", val);
    }

    /**
     * -------------------
     * |        |        |
     * |   II.  |    I.  |
     * |        |        |
     * |--------|--------|
     * |        |        |
     * |   III. |    IV. |
     * |        |        |
     * -------------------
     * prints in wich quadrant in the cartesian coordinate system a given point lies
     *
     * @param coordinatesUnordered arraylist with coordinates in unordered positions
     * @param width                of the given svg
     * @param height               of the given svg
     * @param runtimes             determines how many printing marks needs to get ordered
     */
    public static ArrayList quadrant(ArrayList<Pair> coordinatesUnordered, float width, float height, int runtimes) {

        ArrayList<Pair> orderedCoordinates = new ArrayList<>();
        for (int h = 0; h < runtimes; h++) {
            Pair<Float, Float> placeholder = new Pair<>(0.00f, 0.00f);
            orderedCoordinates.add(placeholder);
        }

        for (int i = 0; i < runtimes; i++) {

            Pair XandY = coordinatesUnordered.get(i);
            Float x = (Float) XandY.getValue0();
            Float y = (Float) XandY.getValue1();

            if (x > width / 2 && y < height / 2) {
                //  System.out.println("First Quadrant");
                // calculate cartisian coordiantes
                x = x - width / 2;
                y = height / 2 - y;
                Pair<Float, Float> coordinateValues1 = new Pair<>(x, y);
                orderedCoordinates.add(0, coordinateValues1);
            } else if (x < width / 2 && y < height / 2) {
                //System.out.println("Second Quadrant");
                x = -(x - width / 2);
                y = y + height / 2;
                Pair<Float, Float> coordinateValues2 = new Pair<>(x, y);
                orderedCoordinates.add(1, coordinateValues2);
            } else if (x < width / 2 && y > height / 2) {
                //System.out.println("Third Quadrant");
                x = -(x + width / 2);
                y = y - height / 2;
                Pair<Float, Float> coordinateValues3 = new Pair<>(x, y);
                orderedCoordinates.add(2, coordinateValues3);
            } else {
                //System.out.println("Fourth Quadrant");
                x = x - width / 2;
                y = -(y - height / 2);
                Pair<Float, Float> coordinateValues4 = new Pair<>(x, y);
                orderedCoordinates.add(3, coordinateValues4);
            }
        }

        orderedCoordinates.removeIf(new Predicate<Pair>() {
            @Override
            public boolean test(Pair pair) {
                float x = (Float) pair.getValue0();
                float y = (Float) pair.getValue1();
                if (x == 0.00f && y == 0.00f) {
                    return true;
                } else {
                    return false;
                }
            }
        });

        return orderedCoordinates;
    }

    /**
     * function for matrix multiplicaiton
     * @param a matrix
     * @param b matrix
     * @return the new matrix
     */
    public static double[][] multiply(double[][] a, double[][] b) {
        int m1 = a.length;
        int n1 = a[0].length;
        int m2 = b.length;
        int n2 = b[0].length;
        if (n1 != m2) throw new RuntimeException("Illegal matrix dimensions.");
        double[][] c = new double[m1][n2];
        for (int i = 0; i < m1; i++)
            for (int j = 0; j < n2; j++)
                for (int k = 0; k < n1; k++)
                    c[i][j] += a[i][k] * b[k][j];
        return c;
    }

    /**
     * build a Document with DOM
     * @param file svg
     * @return the parsed svg as document
     */
    public static Document buildDOM(File file) {

        DocumentBuilderFactory factory = null;
        DocumentBuilder builder = null;
        Document doc;

        try {
            //  Build DOM
            factory = DocumentBuilderFactory.newInstance();
            builder = factory.newDocumentBuilder();
            doc = builder.parse(file);
            return doc;

        } catch (Exception e) {
            System.err.println();
        }

        doc = builder.newDocument();
        return doc;
    }

    /**
     * build the xpath object
     * @return
     */
    public static XPath buildXPath() {

        //  Create xPath object
        XPathFactory xpf = XPathFactory.newInstance();
        return xpf.newXPath();

    }

}
