package svgTransformation;

//java libraries
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

//openCV libraries
import afester.javafx.svg.SvgLoader;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.shape.SVGPath;
import org.apache.commons.math3.linear.*;
import org.javatuples.*;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

//xPath libraries
import org.w3c.dom.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;

//javaFX libraries
import java.io.*;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class Controller {

    // init the the javafx variables for the ui

    @FXML
    private Label passermarken;
    @FXML
    private Button cameraButton;
    //  Buttons for controlling the xy-table
    @FXML
    private Button upButton;
    @FXML
    private Button downButton;
    @FXML
    private Button leftButton;
    @FXML
    private Button rightButton;
    @FXML
    private Button startTestButton;
    @FXML
    private Button connectTableButton;
    @FXML
    private Button svgImageButton;
    // the first Image View which shows the converted to grayscale image
    @FXML
    private ImageView convertedImage;
    // the second Image View which shows the image with the drawn circles
    @FXML
    private ImageView circleImage;
    // FXML slider for setting HSV ranges
    @FXML
    private Slider param1;
    @FXML
    private Slider param2;
    // FXML label to show the current values set with the sliders
    @FXML
    private Label paramValues;
    @FXML
    private Label centerCoordinates;
    @FXML
    private Label connectionLabel;
    @FXML
    private Label testingLabel;


    // init all other variables used in the methods of the class

    // a timer for acquiring the video stream
    private ScheduledExecutorService timer;
    // the OpenCV object that performs the video capture
    private VideoCapture capture = new VideoCapture();
    // a flag to change the button behavior
    private boolean cameraActive;

    // init the svg Object
    private SVGPath svg = new SVGPath();

    // property for object binding
    private ObjectProperty<String> paramValuesProp;
    private ObjectProperty<String> centerCoordinatesProp;

    // init array which holds the values of the coordinates for the found circle
    private ArrayList<Long> circleCenterCoordinates = new ArrayList<>();

    // init the array which holds the values of the offset of the center coordinates of the frame and the found circle
    private ArrayList<Double> frameCenterOffset = new ArrayList<>();

    // init the array which holds the values of the printing marks from the svg
    private ArrayList<Pair> printingMarks = new ArrayList<>();

    // init the array which holds the values of the printing marks from the svg in ordered by quadrant
    private ArrayList<Pair> printingMarksOrdered = new ArrayList<>();

    // init the array which holds the value of the height and width of the svg [0:height] [1:width]
    private ArrayList<Float> SVGWH = new ArrayList<>();

    // init a counter to check on which printing mark we currently are
    private int countingPrintingMarks = 0;

    private final CountDownLatch mark = new CountDownLatch(1);     // countdown size is equal to one mark
    private int currentMark = 1; // count the threads in startCamera() function

    private ArrayList<double[][]> vectorsForAffineCalculation = new ArrayList<>();
    private File SVG;


    /**
     * start testing for the different points
     */

    public void startTest() {

        // check marks in console
        System.out.println("[GET] the points from the array list.");
        System.out.println(printingMarks);
        System.out.println("[ORDER] the points from the array list.");

        // order by quadrant
        int printingMarksSize = printingMarks.size();
        printingMarksOrdered = Utils.quadrant(printingMarks, SVGWH.get(0), SVGWH.get(1), printingMarksSize);
        System.out.println(printingMarksOrdered);

        // move table to the position of the first mark
        System.out.println("[MOVE] the table to the Mark-" + currentMark + " | Mark Counter: " + countingPrintingMarks);
        this.testingLabel.setVisible(true);
        this.testingLabel.setText("Move to printing Mark-" + currentMark);
        Pair XY = printingMarksOrdered.get(countingPrintingMarks);
        Float x = (Float) XY.getValue0();
        Float y = (Float) XY.getValue1();
        moveToPrintingMark(x, y);

    }

    /**
     * connect to the xy table via com port with the help of the TableCommunication class
     * checking the Port/Bus number on mac via terminal before with the following commands:
     * ls -l /dev/tty.*
     * screen /dev/tty.[device] baudrate
     * <p>
     * on windows go to settings -> device manager -> find COM port on usb RS-232 connection
     */
    public void connectToTable() {

        // init the communication class
        System.out.println("Connecting to table");
        this.connectionLabel.setVisible(true);
        RW rw = new RW();
        // copy with timer from "start camera" function

    }


    /**
     * start camera on button click
     */
    public void startCamera() {

        System.out.println("Camera got started.");

        // bind a text property with the string containing the current range of
        // params for object detection
        paramValuesProp = new SimpleObjectProperty<>();
        centerCoordinatesProp = new SimpleObjectProperty<>();
        this.paramValues.setVisible(true);

        this.paramValues.textProperty().bind(paramValuesProp);

        // set a fixed width for all the image to show and preserve image ratio
        this.imageViewProperties(this.convertedImage, 405);
        this.imageViewProperties(this.circleImage, 405);

        if (!this.cameraActive) {
            // start the video capture
            // set 1 for the non native camera -> usb camera
            this.capture.open(0);

            // is the video stream available?
            if (this.capture.isOpened()) {
                this.cameraActive = true;


                // grab a frame every 33 ms (30 frames/sec)
                Runnable frameGrabber = new Runnable() {
                    @Override
                    public void run() {
                        // effectively grab and process every single frame
                        Mat frame = grabFrame();

                        //update the label if the array has a value
                        if (circleCenterCoordinates.size() != 0) {
                            centerCoordinates.setVisible(true);
                            centerCoordinates.textProperty().bind(centerCoordinatesProp); //bind observable to label
                            // save the vector from the offset to arraylist
                            double[][] offset = {{frameCenterOffset.get(0)}, {frameCenterOffset.get(1)}};
                            vectorsForAffineCalculation.add(offset);
                            if (frameCenterOffset.get(0) == 0 && frameCenterOffset.get(1) == 0) {
                                System.out.println("Circle is in the center of the frame");
                                String messageToPrint = "Circle is in the center of the frame";
                                countingPrintingMarks++;
                                currentMark++;
                                int printingMarksSize = printingMarks.size();
                                if (countingPrintingMarks <= printingMarksSize) {
                                    moveToNextPrintingMark();
                                } else {
                                    System.out.println("All printing Marks found. -> build matrix");
                                    if (SVG != null) {
                                        calculateAffineTransformation(SVG);
                                    } else {
                                        System.out.println("No SVG file for manipulating");
                                    }
                                }
                                Utils.onFXThread(centerCoordinatesProp, messageToPrint);
                            } else {
                                //  set string for label
                                String valuesToPrint2 = "Cirlce center: [" + circleCenterCoordinates.get(0) + ", " + circleCenterCoordinates.get(1) + "] | Center Offset: [" + frameCenterOffset.get(0) + ", " + frameCenterOffset.get(1) + "]";
                                Utils.onFXThread(centerCoordinatesProp, valuesToPrint2); //update
                            }
                        }

                        // convert and show the frame
                        Image imageToShow = Utils.mat2Image(frame);
                        circleImage.setImage(imageToShow);
                        updateImageView(circleImage, imageToShow);
                    }
                };

                this.timer = Executors.newSingleThreadScheduledExecutor();
                this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);

                // update the button content
                this.cameraButton.setText("Stop Camera");
            } else {
                // log the error
                System.err.println("Failed to open the camera connection...");
            }
        } else {
            // the camera is not active at this point
            this.cameraActive = false;
            // update again the button content
            this.cameraButton.setText("Start Camera");

            // stop the timer
            this.stopAcquisition();
        }
    }

    private void moveToNextPrintingMark() {

        // get the current printing mark
        System.out.println("CountingPrintingMarks: " + countingPrintingMarks);
        System.out.println("CurrentMark Index for Strings: " + currentMark);

        // move to the next point in the array and wait for mark to be find
        Pair xy = printingMarksOrdered.get(countingPrintingMarks);
        Float x = (Float) xy.getValue0();
        Float y = (Float) xy.getValue1();
        System.out.println("[MOVE] the table to Mark-" + currentMark + " | Mark Counter: " + countingPrintingMarks);
        moveToPrintingMark(x, y);
    }

    /**
     * Get a frame from the opened video stream (if any)
     *
     * @return the {@link Image} to show
     */
    private Mat grabFrame() {

        Mat frame = new Mat();

        // check if the capture is open
        if (this.capture.isOpened()) {
            try {
                // read the current frame
                this.capture.read(frame);

                // if the frame is not empty, process it
                if (!frame.empty()) {
                    // init the different mats
                    Mat greyImage = new Mat();
                    Mat removedNoiseImage = new Mat();

                    // convert the frame to gray
                    Imgproc.cvtColor(frame, greyImage, Imgproc.COLOR_BGR2GRAY);

                    // remove some noise
                    Imgproc.medianBlur(greyImage, removedNoiseImage, 5);

                    //update the first view
                    convertedImage.setImage(Utils.mat2Image(removedNoiseImage));
                    this.updateImageView(this.convertedImage, Utils.mat2Image(removedNoiseImage));

                    // get thresholding values from the UI
                    Scalar cannyEdge = new Scalar(this.param1.getValue(), this.param2.getValue());

                    // show the current selected CannyEdge selection range rounded to 2 decimal places
                    String valuesToPrint = "Upper threshold for internal canny edge detector: " + Utils.roundOffTo2DecPlaces(cannyEdge.val[0]) + System.lineSeparator() + "Threshold center detection: " + Utils.roundOffTo2DecPlaces(cannyEdge.val[1]);
                    Utils.onFXThread(this.paramValuesProp, valuesToPrint);

                    // find the printing mark contours and show them
                    frame = this.findAndDrawCircles(removedNoiseImage, frame);


                }

            } catch (Exception e) {
                // log the (full) error
                System.err.print("Exception during the image elaboration...");
                e.printStackTrace();
            }
        }

        return frame;
    }

    /**
     * Given a binary image containing one or more closed surfaces, use it as a
     * mask to find and highlight the objects contours
     *
     * @param maskedImage the binary image to be used as a mask
     * @param frame       the original {@link Mat} image to be used for drawing the
     *                    objects contours
     * @return the {@link Mat} image with the objects contours framed
     */
    private Mat findAndDrawCircles(Mat maskedImage, Mat frame) {
        // init of the array list and the circle mat
        Mat circles = new Mat();

        // directly changing the paramaters for the Hough Circle Detection | init value: 300
        // param1: Upper threshold for the internal Canny edge detector    | init value: 4
        // param2: Threshold for center detection
        double param1 = this.param1.getValue();
        double param2 = this.param2.getValue();
        int minRadius = 5;
        int maxRadius = 0;

        // applying the hough circle transformation to the maskedImage
        Imgproc.HoughCircles(maskedImage, circles, Imgproc.CV_HOUGH_GRADIENT, 1.0, (double) maskedImage.rows() / 1, param1, param2, minRadius, maxRadius);

        // iterating through found circle and drawing them to the frame
        for (int x = 0; x < circles.cols(); x++) {
            double[] c = circles.get(0, x);
            Point center = new Point(Math.round(c[0]), Math.round(c[1]));

            //  center of the frame
            //  Reminder Size of the Frame is 1280 x 720
            int centerOfFrameY = maskedImage.rows() / 2;
            int centerOfFrameX = maskedImage.cols() / 2;
            Point centerOfFrame = new Point(centerOfFrameX, centerOfFrameY);
            Imgproc.circle(frame, centerOfFrame, 4, new Scalar(0, 0, 255), 5, 8, 0);

            // circle center in green
            Imgproc.circle(frame, center, 1, new Scalar(0, 255, 0), 3, 8, 0);
            // circle outline in red
            int radius = (int) Math.round(c[2]);
            Imgproc.circle(frame, center, radius, new Scalar(255, 0, 0), 3, 8, 0);

            //  add the first coordinates to the array
            if (circles.cols() == 1) {
                circleCenterCoordinates.clear();
                circleCenterCoordinates.add(0, Math.round(c[0])); //x coordinate
                circleCenterCoordinates.add(1, Math.round(c[1])); //y coordinate
                frameCenterOffset.clear(); //clear array to only log the current coordinates
                checkFoundCircleOffset(centerOfFrame, center);  //calculates the offset and adds it to the array
            }
        }

        return frame;
    }

    /**
     * Set typical {@link ImageView} properties: a fixed width and the
     * information to preserve the original image ration
     *
     * @param image     the {@link ImageView} to use
     * @param dimension the width of the image to set
     */
    private void imageViewProperties(ImageView image, int dimension) {
        // set a fixed width for the given ImageView
        image.setFitWidth(dimension);
        // preserve the image ratio
        image.setPreserveRatio(true);
    }

    /**
     * Stop the acquisition from the camera and release all the resources
     */
    private void stopAcquisition() {
        if (this.timer != null && !this.timer.isShutdown()) {
            try {
                // stop the timer
                this.timer.shutdown();
                this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // log any exception
                System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
            }
        }

        if (this.capture.isOpened()) {
            // release the camera
            this.capture.release();
        }
    }

    /**
     * Update the {@link ImageView} in the JavaFX main thread
     *
     * @param view  the {@link ImageView} to update
     * @param image the {@link Image} to show
     */
    private void updateImageView(ImageView view, Image image) {
        Utils.onFXThread(view.imageProperty(), image);
    }

    /**
     * On application close, stop the acquisition from the camera
     */
    protected void setClosed() {
        this.stopAcquisition();
    }


    /**
     * gets all the printer marks from the svg after "uploading" the svg to the application
     *
     * @param event
     */
    public void fileUpload(ActionEvent event) {

        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().addAll(new ExtensionFilter("SVG Datein", "*.svg"));
        File selectedFile = fc.showOpenDialog(null);

        //  if a file is selected
        if (selectedFile != null) {
            SVG = selectedFile;
            try {
                // load the svg image as filestream
                InputStream svgFile = new FileInputStream(selectedFile);
                SvgLoader loader = new SvgLoader();
                Group svgImage = loader.loadSvg(svgFile);
                //  scale the image and warp it in a group
                svgImage.setScaleX(0.1);
                svgImage.setScaleY(0.1);
                Group graphic = new Group(svgImage);
                //  set graphic to button
                svgImageButton.setGraphic(graphic);
            } catch (IOException io) {
                io.printStackTrace();
            }
            ArrayList<Pair> printingMarksFromSVG = new ArrayList<>();
            printingMarksFromSVG = getPrintingMarks("#FF7D7D", "#FF7D7D", selectedFile);
            printingMarks = printingMarksFromSVG;
            int counter = 1;
            StringBuilder printingMarks_Builder = new StringBuilder();
            for (Pair a : printingMarksFromSVG) {
                Float x = (Float) a.getValue0();
                Float y = (Float) a.getValue1();
                String printingMarkSubString = "Passermarke " + counter + " X-Wert: " + x + " | Y-Wert: " + y + System.lineSeparator();
                printingMarks_Builder.append(printingMarkSubString);
                counter++;
            }
            String printingMarks_OUTPUT = printingMarks_Builder.toString();
            passermarken.setVisible(true);
            passermarken.setText(printingMarks_OUTPUT);
        } else {
            System.out.println("Choose a SVG to start finding the printing marks.");
            passermarken.setText("Choose a SVG to start finding the printing marks.");
        }
    }

    /**
     * get the x and y values of the printing marks by specifying the filling color and the id of the circle
     *
     * @param fill   hex value for the color of the printing mark
     * @param stroke ideally the id of the circle
     * @return the x and y value of the printing marks
     */
    private ArrayList<Pair> getPrintingMarks(String fill, String stroke, File file) {

        ArrayList<Pair> PrintingMarksList = new ArrayList<>();

        try {
            Document document = Utils.buildDOM(file);
            XPath xpath = Utils.buildXPath();

            //  create search string for height, width and all marks
            String SVGHeight_Expression = "/*/@height";
            String SVGWidth_Expression = "/*/@width";
            String xpathExpression = "/*/circle[@fill='" + fill + "' and @stroke='" + stroke + "']";

            //  compile expressions for xPath to read
            XPathExpression SVGHeight_Compiled = xpath.compile(SVGHeight_Expression);
            XPathExpression SVGWidth_Compiled = xpath.compile(SVGWidth_Expression);
            XPathExpression compliledExpression = xpath.compile(xpathExpression);       // circles

            //  save results to variable
            Node SVGHeight_Result = (Node) SVGHeight_Compiled.evaluate(document, XPathConstants.NODE);
            Node SVGWidth_Result = (Node) SVGWidth_Compiled.evaluate(document, XPathConstants.NODE);
            NodeList result = (NodeList) compliledExpression.evaluate(document, XPathConstants.NODESET);

            //  get the x and y value of the printing marks
            String SVGHeight_Clean = SVGHeight_Result.getNodeValue().replace("px", "");
            String SVGWidth_Clean = SVGWidth_Result.getNodeValue().replace("px", "");
            Float SVGHeight = Float.parseFloat(SVGHeight_Clean);
            Float SVGWidth = Float.parseFloat(SVGWidth_Clean);
            SVGWH.add(SVGHeight);
            SVGWH.add(SVGWidth);

            for (int j = 0; j < result.getLength(); j++) {
                Element childNode = (Element) result.item(j);
                PrintingMarksList.add(new Pair<>(Float.parseFloat(childNode.getAttribute("cx")), Float.parseFloat(childNode.getAttribute("cy"))));
            }


        } catch (Exception ex) {
            System.out.println(ex);
            System.out.println("The printing marks couldn't be found.");
            passermarken.setText("The printing marks couldn't be found.");
        }

        return PrintingMarksList;

    }


    //cartesian coordinate system
    ////////////////////y/////////////////////
    //..................^...................//
    //..................|...................//
    //..................|...................//
    //..................|...................//
    //..................|...................//
    //..................|...................//
    //..................|...................//
    //..................|...................//
    //------------------|------------------>x/
    //..................|...................//
    //..................|...................//
    //..................|...................//
    //..................|...................//
    //..................|...................//
    //..................|...................//
    //..................|...................//
    //..................|...................//
    //////////////////////////////////////////

    //svg coordinate system
    //////////////////////////////////////////
    //|------------------------------------>x/
    //|.....................................//
    //|.....................................//
    //|.....................................//
    //|.....................................//
    //|.....................................//
    //|.....................................//
    //|.....................................//
    //|.....................................//
    //|.....................................//
    //|.....................................//
    //|.....................................//
    //|.....................................//
    //|.....................................//
    //|.....................................//
    //↓.....................................//
    //y.....................................//
    //////////////////////////////////////////

    /**
     * on button press "up" move the xy-table in the up direction (+y)
     */
    public void moveUp() {
        //detect the press of the up button
        System.out.println("↑ Up");
        String command = "up";
        RW rw = new RW();
        rw.move(command);
    }

    /**
     * on button press "down" move the xy-table in the down direction (-y)
     */
    public void moveDown() {
        //detect the press of the down button
        System.out.println("↓ Down");
        String command = "down";
        RW rw = new RW();
        rw.move(command);
    }

    /**
     * on button press "left" move the xy-table in the left direction (-x)
     */
    public void moveLeft() {
        //detect the press of the left button
        System.out.println("← Left");
        String command = "left";
        RW rw = new RW();
        rw.move(command);
    }

    /**
     * on button press "up" move the xy-table in the up-direction (+x)
     */
    public void moveRight() {
        //detect the press of the right button
        System.out.println("→ Right");
        String command = "right";
        RW rw = new RW();
        rw.move(command);
    }

    /**
     * stops the motion of the xy-table when the key is released
     */
    public void stopXYTable() {
        System.out.println("Stop the table!");
        String command = "stop";
        RW rw = new RW();
        rw.move(command);
    }

    /**
     * moving the motor to the x and y value of the given printing mark
     *
     * @param x position in the svg of the printing mark
     * @param y position in the svg of the printing mark
     */
    public void moveToPrintingMark(float x, float y) {
        System.out.println("Moving to X: " + x + " and Y: " + y);
        // convertCoordinates()
        RW.moveTo(x, y);
        // move table with the move commands to the right position
    }


    /**
     * get the current position of the xy table
     */
    public void getTableCoordinates() {

        // work with the RW CLass


    }

    /**
     * 0 0 Coordinates at the xy-table -> cartesian coordinate system
     * 0 0 Coordiantes in SVG (y-axis reversed)
     * 1px == n mm
     * converting from the units system of the svg (f.E. 841 x 595 Pixel) to real world values
     */
    public Float convertUnits(int format) {

        // initialise variables
        Float DINA4Width = 0.00f;
        Float DINA4Height = 0.00f;

        //  define Formats in micrometer sideways
        switch (format) {
            case 0:
                DINA4Width = 1189000.00f;
                DINA4Height = 841000.00f;
            case 1:
                DINA4Width = 841000.00f;
                DINA4Height = 594000.00f;
            case 2:
                DINA4Width = 594000.00f;
                DINA4Height = 420000.00f;
            case 3:
                DINA4Width = 420000.00f;
                DINA4Height = 297000.00f;
            case 4:
               DINA4Width = 297000.00f;
               DINA4Height = 210000.00f;
            case 5:
                DINA4Width = 210000.00f;
                DINA4Height = 148000.00f;
            case 6:
                DINA4Width = 148000.00f;
                DINA4Height = 105000.00f;
                default:
                    DINA4Width = 297000.00f;
                    DINA4Height = 210000.00f;
        }

        // get height and with of the svg
        Float SVGWidth = SVGWH.get(0);
        Float SVGHeight = SVGWH.get(1);

        // calculate units
        Float ConversionWidth = DINA4Width/SVGWidth;
        Float ConversionHeight = DINA4Height/SVGHeight;

        // calculate and return average

        return (ConversionHeight+ConversionWidth)/2;
    }


    /**
     * get the offsets vectors and calculate the affine transformation vector
     */
    public void calculateAffineTransformation(File file) {
        // get the realworld offsets for all points p
        // calculate the matrix as affine illustration according to Dr. Leonhard Riedl
        int size = vectorsForAffineCalculation.size();

        // fill vectors up to 6 when we have lesser printing marks
        while (size < 6) {
            double[][] eigenVector = {{0}, {0}};
            vectorsForAffineCalculation.add(eigenVector);
            size++;
        }

        // initiate the 2D vectors as matrices
        RealMatrix v1 = MatrixUtils.createRealMatrix(vectorsForAffineCalculation.get(0));
        RealMatrix v2 = MatrixUtils.createRealMatrix(vectorsForAffineCalculation.get(1));
        RealMatrix v3 = MatrixUtils.createRealMatrix(vectorsForAffineCalculation.get(2));
        RealMatrix w1 = MatrixUtils.createRealMatrix(vectorsForAffineCalculation.get(3));
        RealMatrix w2 = MatrixUtils.createRealMatrix(vectorsForAffineCalculation.get(4));
        RealMatrix w3 = MatrixUtils.createRealMatrix(vectorsForAffineCalculation.get(5));

        // substract v2 - v1 and w2 - w1 and v3 - v1 and w3 - w1
        RealMatrix vNew1 = v2.subtract(v1);
        RealMatrix vNew2 = v3.subtract(v1);
        RealMatrix wNew1 = w2.subtract(w1);
        RealMatrix wNew2 = w3.subtract(w1);

        // build matrices V and W
        RealMatrix V = MatrixUtils.createRealMatrix(2, 2);
        V.setColumnMatrix(1, vNew1);
        V.setColumnMatrix(2, vNew2);
        RealMatrix W = MatrixUtils.createRealMatrix(2, 2);
        W.setColumnMatrix(1, wNew1);
        W.setColumnMatrix(2, wNew2);

        // transpose V
        V.transpose();

        // multiply W * V^T
        RealMatrix M = W.multiply(V);

        // t = w1 - M * v1
        RealMatrix t = w1.subtract(M.multiply(v1));

        // modify the svg with the vector inside the svg transform matrix
        modifySVG(file, t);
    }

    /**
     * take the values from "calculateAffineTransformation()" as the transposed vector for transforming the svg canvas
     * write in svg
     * return the modfied svg
     */
    private void modifySVG(File file, RealMatrix t) {
        try {

            Document doc = Utils.buildDOM(file);
            XPath xPath = Utils.buildXPath();

            // cut matrix into x and y
            double tX = t.getEntry(0, 0);
            double tY = t.getEntry(0, 1);

            // store data
            storeData(tX, tY);

            //  add the matrix to every level of the svg
            String SVG = "/*/@g/@transform->matrix(1,0,0,1," + tX + "," + tY + ")";

            // compile the expression
            XPathExpression compliledExpression = xPath.compile(SVG);

            //  apply to svg
            compliledExpression.evaluate(doc);

            // serialze for output
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            Source input = new DOMSource(doc);

            // get old filename
            String filename = file.getName();
            // split into array -> [0] nameOfTheFile [1]format (should be svg)
            String[] filenameParts = filename.split(".", 2);
            // concat new filename by appending a "new" to the old filename
            String newFilename = filenameParts[0] + "New" + filenameParts[1];
            // specify saving location
            Result output = new StreamResult(System.getProperty("user.dir") + "/src/svgTransformation/" + newFilename);
            // output
            transformer.transform(input, output);

        } catch (Exception e) {
            System.err.println(e);
        }
    }


    /**
     * save our transform Matrix to a Database so that we can compare our latest results
     * saving the matrix to our json database, frontend button isn't working in the current build but was working before
     */
    @FXML
    public void storeData(double tX, double tY) {

        HandlingData storage = new HandlingData();
        // get current entry number if exists
        if (storage.DBExists()) {

            // storage already exists -> create new entry
            Double[][] t = {{tX, tY}};
            storage.insertNewRecord(t);
            storage.ReadFromFile();

        } else {

            // storage doesn't exist yet -> create new DB
            Double[][] t = {{tX, tY}};
            storage.createNewDB(t);
            storage.ReadFromFile();
        }

    }


    /**
     * check if the found circle is in the center of the given frame -> if not tell user to move the table
     *
     * @param centerOfFrame       coordinate of the center of the circle
     * @param centerOfFoundCircle coordinate of the center of the found circle
     */
    private void checkFoundCircleOffset(Point centerOfFrame, Point centerOfFoundCircle) {

        // init the variables
        double centerOfFrameX = centerOfFrame.x;
        double centerOfFrameY = centerOfFrame.y;
        double centerOfFoundCircleX = centerOfFoundCircle.x;
        double centerOfFoundCircleY = centerOfFoundCircle.y;

        // calculate the offset
        double frameCenterOffsetX = centerOfFrameX - centerOfFoundCircleX;
        double frameCenterOffsetY = centerOfFrameY - centerOfFoundCircleY;

        //add to array
        frameCenterOffset.add(frameCenterOffsetX);
        frameCenterOffset.add(frameCenterOffsetY);

    }

}
